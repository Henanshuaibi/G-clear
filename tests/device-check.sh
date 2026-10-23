#!/data/data/com.termux/files/usr/bin/sh
set -eu
cd "$(dirname "$0")/.."
gclear_sdk="${GCLEAR_SDK_DIR:-../pulse-player/tools}"
mkdir -p build/device-tests build/device-dex build/test-data
javac --release 8 -encoding UTF-8 -classpath "$gclear_sdk/android.jar:build/classes" -d build/device-tests tests/DeviceCheck.java
jar cf build/device-tests.jar -C build/device-tests .
if [ -f build/device-dex/classes.dex ]; then chmod 600 build/device-dex/classes.dex; fi
java -Xmx512m -cp "$gclear_sdk/r8.jar" com.android.tools.r8.D8 --lib "$gclear_sdk/android.jar" --min-api 26 --output build/device-dex build/classes.jar build/device-tests.jar
chmod 400 build/device-dex/classes.dex
CLASSPATH="$PWD/build/device-dex/classes.dex" /system/bin/app_process /system/bin cn.g.clear.DeviceCheck "$PWD/build/test-data"
