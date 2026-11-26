#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

RESOURCES=src/test/resources/projects
OUTPUT=output
JAR=target/vmtranslator.jar
EMULATOR=tools/CPUEmulator.sh

TESTS=(
  "7/StackArithmetic/SimpleAdd file"
  "7/StackArithmetic/StackTest file"
  "7/MemoryAccess/BasicTest file"
  "7/MemoryAccess/PointerTest file"
  "7/MemoryAccess/StaticTest file"
)

if ! ./mvnw -q -DskipTests package; then
  echo "build failed"
  exit 1
fi

rm -rf "$OUTPUT"
mkdir -p "$OUTPUT"

passed=0
for entry in "${TESTS[@]}"; do
  read -r path kind <<< "$entry"
  name="$(basename "$path")"
  dest="$OUTPUT/$path"
  mkdir -p "$dest"
  cp -R "$RESOURCES/$path/." "$dest/"

  if [ "$kind" = "dir" ]; then
    input="$dest"
  else
    input="$dest/$name.vm"
  fi

  if ! java -jar "$JAR" "$input" > "$dest/translator.log" 2>&1; then
    echo "FAIL $path ($(head -n 1 "$dest/translator.log"))"
  elif sh "$EMULATOR" "$dest/$name.tst" > "$dest/emulator.log" 2>&1; then
    echo "OK   $path"
    passed=$((passed + 1))
  else
    echo "FAIL $path ($(tail -n 1 "$dest/emulator.log"))"
  fi
done

echo "$passed/${#TESTS[@]} OK"
[ "$passed" -eq "${#TESTS[@]}" ]
