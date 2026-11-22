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
# The map needs CARTO: without the key the app falls back to OpenStreetMap, which blocks it ("403 Access blocked")
KEY=farex-app/src/main/resources/com/farex/ui/common/carto.key
[ -s "$KEY" ] || { echo "Missing $KEY (the CARTO API key, one line). The map would show 'Access blocked'."; exit 1; }
mvn -q install -DskipTests -pl farex-shared -am        # the app needs the shared module (and the parent pom) installed

cd farex-app
cp pom.xml pom.xml.desktop
trap 'mv pom.xml.desktop pom.xml' EXIT
sed -i 's#<version>1.0.29</version>#<version>1.0.25</version>#' pom.xml
mvn -Dgluonfx.target=android gluonfx:build

# NFC card reader, Android side (src/android/java). GluonFX does not compile that folder: it only takes Android
# classes from a jar under META-INF/substrate/dalvik/precompiled/classes, so build that jar here. Needs any
# Android SDK's android.jar: ANDROID_JAR, else the SDK Gluon downloaded.
ANDROID_JAR="${ANDROID_JAR:-$(ls ~/.gluon/substrate/Android/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1)}"
[ -f "$ANDROID_JAR" ] || { echo "No android.jar found. Set ANDROID_JAR to <android sdk>/platforms/android-XX/android.jar"; exit 1; }
NFC=target/nfc-dalvik
rm -rf "$NFC" && mkdir -p "$NFC/META-INF/substrate/dalvik/precompiled/classes"
javac -Xlint:-options -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
  -d "$NFC/META-INF/substrate/dalvik/precompiled/classes" src/android/java/com/farex/android/*.java
jar cf target/farex-nfc-dalvik.jar -C "$NFC" META-INF
mvn -q install:install-file -Dfile=target/farex-nfc-dalvik.jar -DgroupId=com.farex -DartifactId=farex-nfc-dalvik \
  -Dversion=1.0-SNAPSHOT -Dpackaging=jar
# the pom's android-nfc profile turns on now that target/farex-nfc-dalvik.jar exists, and packages it into the APK
mvn -Dgluonfx.target=android gluonfx:package
echo "APK: $(pwd)/target/gluonfx/aarch64-android/gvm/farex-app.apk"
