FROM python:3.12-slim

ENV DEBIAN_FRONTEND=noninteractive
ENV APKTOOL_VERSION=3.0.3
ENV BUILD_TOOLS_VERSION=35.0.1
ENV PYTHONUNBUFFERED=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jre-headless curl unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt/tools /opt/work \
    && curl -fsSL "https://github.com/iBotPeaches/Apktool/releases/download/v\${APKTOOL_VERSION}/apktool_\${APKTOOL_VERSION}.jar" -o /opt/tools/apktool.jar \
    && echo "dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423  /opt/tools/apktool.jar" | sha256sum -c - \
    && curl -fsSL "https://dl.google.com/android/repository/build-tools_r35.0.1_linux.zip" -o /tmp/build-tools.zip \
    && echo "5993499f3229a021b89f87088c57242aeefaa62316bf3d69da7de40bfd5350f1  /tmp/build-tools.zip" | sha256sum -c - \
    && unzip -q /tmp/build-tools.zip -d /opt/tools \
    && rm /tmp/build-tools.zip \
    && mv /opt/tools/android-*/ /opt/tools/build-tools \
    && chmod +x /opt/tools/build-tools/{aapt2,apksigner,zipalign}

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY server.py .

EXPOSE 8080
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--timeout", "900", "server:app"]
