#!/data/data/com.termux/files/usr/bin/sh
set -eu
cd "$(dirname "$0")/.."
gclear_sdk="${GCLEAR_SDK_DIR:-../pulse-player/tools}"
mkdir -p build/fixture/generated build/fixture/classes build/fixture/dex
aapt2 compile --dir tests/fixture/res -o build/fixture/res.zip
aapt2 link -I "$gclear_sdk/android-res.jar" --manifest tests/fixture/AndroidManifest.xml --java build/fixture/generated -o build/fixture/resources.apk build/fixture/res.zip
javac --release 8 -encoding UTF-8 -classpath "$gclear_sdk/android.jar" -d build/fixture/classes tests/fixture/src/cn/g/clear/fixture/*.java build/fixture/generated/cn/g/clear/fixture/R.java
jar cf build/fixture/classes.jar -C build/fixture/classes .
java -Xmx512m -cp "$gclear_sdk/r8.jar" com.android.tools.r8.D8 --lib "$gclear_sdk/android.jar" --min-api 26 --output build/fixture/dex build/fixture/classes.jar
cp build/fixture/resources.apk build/fixture/unsigned.apk
jar uf build/fixture/unsigned.apk -C build/fixture/dex classes.dex
zipalign -f 4 build/fixture/unsigned.apk build/fixture/aligned.apk
apksigner sign --ks tools/g-clear.keystore --ks-pass file:tools/signing-password --out build/G-clear-test-0.1.apk build/fixture/aligned.apk
apksigner verify build/G-clear-test-0.1.apk
