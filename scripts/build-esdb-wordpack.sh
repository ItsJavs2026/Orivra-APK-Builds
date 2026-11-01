#!/usr/bin/env bash
# Build 417 pinned on-device source — reproduce offline language asset from locally
# checked-out, pinned ESDB source. Does not download anything or build an APK.
set -euo pipefail
if [[ $# -ne 1 ]]; then
  echo "Usage: bash scripts/build-esdb-wordpack.sh /path/to/en-wl-wordlist" >&2
  exit 2
fi
source_dir="$(cd "$1" && pwd)"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
expected_commit="1e5b7d3a72f47a71da5d28686c1dd4b397178485"
expected_sha="a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600"
if [[ "$(git -C "$source_dir" rev-parse HEAD)" != "$expected_commit" ]]; then
  echo "Refusing unpinned ESDB source. Need $expected_commit" >&2
  exit 1
fi
test -s "$source_dir/Copyright"
test -s "$root/app/src/main/assets/dictionaries/ESDB_COPYRIGHT.txt"
grep -Fq 'Copyright 2000-2026 by Kevin Atkinson' "$source_dir/Copyright"
grep -Fq 'Copyright 2000-2026 by Kevin Atkinson' "$root/app/src/main/assets/dictionaries/ESDB_COPYRIGHT.txt"
temporary="$(mktemp)"
trap 'rm -f "$temporary"' EXIT
make -C "$source_dir"
(cd "$source_dir" && ./scowl word-list 60 A 1 --categories=) > "$temporary"
printf '%s  %s\n' "$expected_sha" "$temporary" | sha256sum --check
test "$(wc -c < "$temporary")" -eq 1022018
asset="$root/app/src/main/assets/dictionaries/esdb-en-US-60.esdbpack"
mkdir -p "$(dirname "$asset")"
gzip -n -9 < "$temporary" > "$asset"
gzip -t < "$asset"
roundtrip="$(mktemp)"
trap 'rm -f "$temporary" "$roundtrip"' EXIT
gzip -dc "$asset" > "$roundtrip"
printf '%s  %s\n' "$expected_sha" "$roundtrip" | sha256sum --check
printf "Generated exact-only research asset: %s\n" "$asset"
wc -c "$asset"
sha256sum "$asset"
echo "Next: verify archive bytes and license inside the final Android APK."
