import os
import re
import shutil
import subprocess
import tempfile
import uuid
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

from flask import Flask, jsonify, request, send_file

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 250 * 1024 * 1024

APKTOOL = "/opt/tools/apktool.jar"
ZIPALIGN = "/opt/tools/build-tools/zipalign"
APKSIGNER = "/opt/tools/build-tools/apksigner"
WORK_ROOT = Path("/opt/work")
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ANDROID_LABEL = "{%s}label" % ANDROID_NS
TOKEN = os.environ.get("APK_WORKER_TOKEN", "")

def run(cmd, cwd=None, timeout=900):
    p = subprocess.run(
        cmd, cwd=cwd, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, text=True, timeout=timeout,
    )
    if p.returncode != 0:
        raise RuntimeError(p.stdout[-12000:] or "command failed")
    return p.stdout

def authorized():
    return bool(TOKEN) and request.headers.get("Authorization", "") == "Bearer " + TOKEN

def app_name_from_instruction(instruction):
    patterns = [
        r'(?:change|set|rename)\s+(?:the\s+)?(?:app(?:lication)?\s+)?name\s+to\s+["“”\']?([^"“”\'\n]+)',
        r'rename\s+(?:the\s+)?app\s+(?:as|to)\s+["“”\']?([^"“”\'\n]+)',
    ]
    for pattern in patterns:
        m = re.search(pattern, instruction, re.IGNORECASE)
        if m:
            value = m.group(1).strip().rstrip(" .")
            if 1 <= len(value) <= 80:
                return value
    return None

def add_or_update_name(decoded, new_name):
    values = decoded / "res" / "values" / "strings.xml"
    values.parent.mkdir(parents=True, exist_ok=True)
    if values.exists():
        tree = ET.parse(values)
        root = tree.getroot()
    else:
        root = ET.Element("resources")
        tree = ET.ElementTree(root)

    target = next((n for n in root.findall("string")
                   if n.get("name") == "apk_agent_app_name"), None)
    if target is None:
        target = ET.SubElement(root, "string", {"name": "apk_agent_app_name"})
    target.text = new_name
    tree.write(values, encoding="utf-8", xml_declaration=True)

    manifest = decoded / "AndroidManifest.xml"
    tree = ET.parse(manifest)
    root = tree.getroot()
    application = root.find("application")
    if application is None:
        raise RuntimeError("Decoded APK has no application element.")
    application.set(ANDROID_LABEL, "@string/apk_agent_app_name")
    tree.write(manifest, encoding="utf-8", xml_declaration=True)

def verify_apk(apk):
    run([APKSIGNER, "verify", "--verbose", str(apk)], timeout=120)
    with zipfile.ZipFile(apk) as z:
        names = set(z.namelist())
        if "AndroidManifest.xml" not in names or "classes.dex" not in names:
            raise RuntimeError("Verification failed: rebuilt APK is missing required entries.")

@app.get("/health")
def health():
    return jsonify({"ok": True, "engine": "apktool-3.0.3"})

@app.post("/modify")
def modify():
    if not authorized():
        return jsonify({"error": "Unauthorized"}), 401

    instruction = request.form.get("instruction", "").strip()
    uploaded = request.files.get("apk")
    if uploaded is None:
        return jsonify({"error": "Missing apk upload"}), 400

    new_name = app_name_from_instruction(instruction)
    if not new_name:
        return jsonify({
            "error": "The first APK transformation supports changing the app name.",
            "example": "Change the app name to Nova",
        }), 422

    job_id = uuid.uuid4().hex
    job = WORK_ROOT / job_id
    original = job / "original.apk"
    decoded = job / "decoded"
    unsigned = job / "unsigned.apk"
    aligned = job / "modified.apk"
    keystore = job / "worker-release.jks"
    job.mkdir(parents=True, exist_ok=False)

    try:
        uploaded.save(original)
        run(["java", "-jar", APKTOOL, "d", "-f", str(original), "-o", str(decoded)])
        add_or_update_name(decoded, new_name)
        run(["java", "-jar", APKTOOL, "b", str(decoded), "-o", str(unsigned)])

        run([
            "keytool", "-genkeypair", "-noprompt",
            "-keystore", str(keystore),
            "-storepass", "pilgrix-worker",
            "-keypass", "pilgrix-worker",
            "-alias", "pilgrix",
            "-keyalg", "RSA", "-keysize", "2048",
            "-validity", "3650",
            "-dname", "CN=Pilgrix APK Worker,O=Pilgrix",
        ], timeout=120)

        run([ZIPALIGN, "-f", "-p", "4", str(unsigned), str(aligned)], timeout=120)
        run([
            APKSIGNER, "sign",
            "--ks", str(keystore),
            "--ks-pass", "pass:pilgrix-worker",
            "--key-pass", "pass:pilgrix-worker",
            "--ks-key-alias", "pilgrix",
            "--v1-signing-enabled", "true",
            "--v2-signing-enabled", "true",
            "--v3-signing-enabled", "true",
            str(aligned),
        ], timeout=120)
        verify_apk(aligned)

        response = send_file(
            aligned,
            mimetype="application/vnd.android.package-archive",
            as_attachment=True,
            download_name=f"{new_name}.apk",
        )
        response.headers["X-Pilgrix-Job-Id"] = job_id
        response.headers["X-Pilgrix-Status"] = "verified"
        return response
    except subprocess.TimeoutExpired:
        return jsonify({"error": "APK processing timed out"}), 504
    except Exception as exc:
        app.logger.exception("APK job %s failed", job_id)
        return jsonify({"error": str(exc)}), 500
    finally:
        shutil.rmtree(job, ignore_errors=True)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
