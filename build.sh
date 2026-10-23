#!/data/data/com.termux/files/usr/bin/sh
set -eu
cd "$(dirname "$0")"
gclear_sdk="${GCLEAR_SDK_DIR:-../pulse-player/tools}"
gclear_key="${GCLEAR_KEYSTORE:-tools/g-clear.keystore}"
gclear_password="${GCLEAR_SIGNING_PASSWORD_FILE:-tools/signing-password}"
for gclear_file in "$gclear_sdk/android.jar" "$gclear_sdk/android-res.jar" "$gclear_sdk/r8.jar" "$gclear_key" "$gclear_password" signing-certificate.sha256; do
 if [ ! -f "$gclear_file" ]; then echo "缺少构建文件：$gclear_file" >&2; exit 1; fi
done
gclear_cert=$(keytool -exportcert -keystore "$gclear_key" -storepass:file "$gclear_password" -alias gclear 2>/dev/null | sha256sum | cut -d ' ' -f 1)
if [ "$gclear_cert" != "$(cat signing-certificate.sha256)" ]; then echo '签名指纹不匹配，停止构建。' >&2; exit 1; fi
mkdir -p build/generated build/classes build/dex
find build/classes -name '*.class' -delete
aapt2 compile --dir res -o build/res.zip
aapt2 link -I "$gclear_sdk/android-res.jar" --manifest AndroidManifest.xml --java build/generated -o build/resources.apk build/res.zip
javac --release 8 -encoding UTF-8 -classpath "$gclear_sdk/android.jar" -d build/classes src/cn/g/clear/*.java build/generated/cn/g/clear/R.java
jar cf build/classes.jar -C build/classes .
java -Xmx512m -cp "$gclear_sdk/r8.jar" com.android.tools.r8.D8 --lib "$gclear_sdk/android.jar" --min-api 26 --output build/dex build/classes.jar
cp build/resources.apk build/unsigned.apk
jar uf build/unsigned.apk -C build/dex classes.dex
zipalign -f 4 build/unsigned.apk build/aligned.apk
apksigner sign --ks "$gclear_key" --ks-pass "file:$gclear_password" --out build/G-clear-0.1.apk build/aligned.apk
apksigner verify --verbose build/G-clear-0.1.apk
zipalign -c 4 build/G-clear-0.1.apk
