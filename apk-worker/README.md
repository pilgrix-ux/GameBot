# Pilgrix APK Worker

First supported transformation:

    Change the app name to Nova

Pipeline: receive APK -> decode -> modify -> rebuild -> zipalign -> sign -> verify -> return.

Jobs are disposable and are deleted after the response.

The service requires APK_WORKER_TOKEN. Do not run it publicly without authentication.
