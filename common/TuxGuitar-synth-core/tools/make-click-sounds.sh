#!/bin/sh
# Makes the built-in click sounds again (see src/main/resources/app/tuxguitar/midi/synth/impl/click-sounds/README.md):
# downloads the VCSL recordings at the pinned commit into the folder given (a new, empty one: downloads are untrusted
# data), checks the licence and each file's SHA-256, then evens them (MakeClickSounds, TGClickRecording) into the
# click-sounds resources. Needs curl, sha256sum, Maven and tuxguitar-lib installed in ~/.m2.
#
#   common/TuxGuitar-synth-core/tools/make-click-sounds.sh <download folder>
set -eu

VCSL=https://raw.githubusercontent.com/sgossner/VCSL
COMMIT=c1ea7bcc3c7309650ab0da9d15c9cd1fbc4a4c7e
MODULE=$(cd "$(dirname "$0")/.." && pwd)
OUT="$MODULE/src/main/resources/app/tuxguitar/midi/synth/impl/click-sounds"
DOWNLOADS=$1
mkdir -p "$DOWNLOADS"

curl -sfL -o "$DOWNLOADS/LICENSE" "$VCSL/$COMMIT/LICENSE"
grep -q "CC0 1.0 Universal" "$DOWNLOADS/LICENSE" || { echo "VCSL's licence is not CC0 1.0 at $COMMIT"; exit 1; }

# id | SHA-256 of the file downloaded | its path in VCSL
while IFS='|' read -r id sha path; do
	url=$(printf '%s' "$path" | sed 's/ /%20/g; s/,/%2C/g')
	curl -sfL -o "$DOWNLOADS/$id.wav" "$VCSL/$COMMIT/$url"
	echo "$sha  $DOWNLOADS/$id.wav" | sha256sum -c --quiet - || { echo "$id: not the file pinned"; exit 1; }
done <<'SOUNDS'
wood-block|9a911c66212d50a09494821bf9af8ce6e007612e232b5905f838348cb1d63b75|Idiophones/Struck Idiophones/Woodblock/wood_click_ff.wav
cowbell|e32a7a646bddfbc63595de9f64e3e435999c932ebadf0fe98759e754096002a0|Idiophones/Struck Idiophones/Cowbells/Cowbell1_Hit_v4_rr1_Mid.wav
snare|ea0adea05227c09002fb7305613cb2877003dc3ba3467d3b2d60c20f5664d59a|Membranophones/Struck Membranophones/Snare Drum, Modern 1/Snare2_HitSN_v9_rr1_Mid.wav
side-stick|98258bc22a11aca0a8eb1d50a2e0a82cbe6357d30e0b55116db0b188015ff648|Membranophones/Struck Membranophones/Snare Drum, Modern 1/Snare2_stick_v1_rr1_Mid.wav
claves|12285f65ed125f4140d1e8a86b2d6a08bf3283b7bf30c10d68290b8e8230a927|Idiophones/Struck Idiophones/Claves/Claves1_Hit_v3_rr1_Mid.wav
hi-hat|ccaf3b3a353c22d7aac00c261229c42a1ef083285069f4c76dbafba454b46edd|Idiophones/Struck Idiophones/Hi-Hat Cymbal/HiHat_HitC_v4_rr1_Mid.wav
clap|6611570b169578e3f6be2c253c4148420396e4f8ec1ef3fc3c2df158484ce769|Idiophones/Struck Idiophones/Claps/SoloClap_vl4.wav
SOUNDS

mvn -q -o -f "$MODULE/pom.xml" test-compile
LIB="$HOME/.m2/repository/app/tuxguitar/tuxguitar-lib/9.99-SNAPSHOT/tuxguitar-lib-9.99-SNAPSHOT.jar"
mkdir -p "$OUT"
# Java on Windows (Git Bash) takes Windows paths
path() { case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) cygpath -w "$1" ;; *) printf '%s' "$1" ;; esac; }
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
java -cp "$(path "$MODULE/target/classes")$SEP$(path "$MODULE/target/test-classes")$SEP$(path "$LIB")" app.tuxguitar.midi.synth.impl.MakeClickSounds "$(path "$DOWNLOADS")" "$(path "$OUT")"
