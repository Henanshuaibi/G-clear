#!/data/data/com.termux/files/usr/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/tests
javac --release 8 -encoding UTF-8 -d build/tests src/cn/g/clear/Rules.java tests/CoreCheck.java
java -cp build/tests CoreCheck
python tests/package-check.py
