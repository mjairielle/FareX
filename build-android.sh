#!/bin/bash
# Builds the FareX Android APK. Linux x86_64 only (Gluon cannot build Android on Windows or macOS).
#
# Needs: Maven, and Gluon's GraalVM for Java 23 unpacked somewhere, from
#   https://github.com/gluonhq/graal/releases/download/gluon-23+25.1-dev-2409082136/graalvm-java23-linux-amd64-gluon-23+25.1-dev.tar.gz
# First run downloads the Android SDK and NDK into ~/.gluon (a few GB, about 20 minutes).
#
#   GRAALVM_HOME=/path/to/graalvm-java23-linux-amd64-gluon-23+25.1-dev ./build-android.sh
#
# The APK lands in farex-app/target/gluonfx/aarch64-android/gvm/farex-app.apk
#
# Why the plugin swap: the pom's gluonfx 1.0.29 (used for the desktop with GraalVM 25) passes an
# Android option this GraalVM lacks; 1.0.25 matches it. The plugin calls itself on pom.xml, so the
# version is changed in pom.xml for the build and always put back afterwards.
set -euo pipefail
: "${GRAALVM_HOME:?Set GRAALVM_HOME to the Gluon GraalVM 23 folder, see the top of this file}"
cd "$(dirname "$0")"
mvn -q install -DskipTests -pl farex-shared -am        # the app needs the shared module (and the parent pom) installed

cd farex-app
cp pom.xml pom.xml.desktop
trap 'mv pom.xml.desktop pom.xml' EXIT
sed -i 's#<version>1.0.29</version>#<version>1.0.25</version>#' pom.xml
mvn -Dgluonfx.target=android gluonfx:build gluonfx:package
echo "APK: $(pwd)/target/gluonfx/aarch64-android/gvm/farex-app.apk"
