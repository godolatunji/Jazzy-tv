#!/usr/bin/env bash
# Builds the LGPL "corresponding source" bundle that must be published next to every Jazzy APK
# (FFmpeg is LGPL-2.1-or-later; see core/ffmpeg/LICENSE-NOTICE.md).
#
# Output (in release/out/):
#   ffmpeg-7.1.5.tar.xz            official, unmodified FFmpeg release tarball (SHA-256 checked)
#   ffmpeg-7.1.5.tar.xz.asc        the FFmpeg project's signature for it
#   jazzy-ffmpeg-build-<ver>.tar.gz  our build script, configure flags, notices and Media3 extension sources
#   LGPL-SOURCES.txt               what these files are and how to rebuild/relink
#   SHA256SUMS                     checksums for everything above
#
# Usage: release/package-lgpl-sources.sh [app-version]
set -euo pipefail

FFMPEG_VERSION="7.1.5"
FFMPEG_SHA256="de668509caf9e35e3cd162473441fdb29538c6d96ed080292b3cf9e6fc5d558f"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/release/out"
APP_VERSION="${1:-$(grep -E 'versionName *=' "$ROOT/app/build.gradle.kts" | head -1 | sed -E 's/.*"(.*)".*/\1/')}"

mkdir -p "$OUT"
cd "$OUT"

tarball="ffmpeg-$FFMPEG_VERSION.tar.xz"
if [[ ! -f "$tarball" ]]; then
  curl -fsSL -o "$tarball" "https://ffmpeg.org/releases/$tarball"
  curl -fsSL -o "$tarball.asc" "https://ffmpeg.org/releases/$tarball.asc"
fi
actual="$(shasum -a 256 "$tarball" | cut -d' ' -f1)"
if [[ "$actual" != "$FFMPEG_SHA256" ]]; then
  echo "SHA-256 mismatch for $tarball: got $actual, expected $FFMPEG_SHA256" >&2
  exit 1
fi

build_bundle="jazzy-ffmpeg-build-$APP_VERSION.tar.gz"
tar -czf "$build_bundle" -C "$ROOT/core/ffmpeg" \
  LICENSE-NOTICE.md build-ffmpeg src/main/java build.gradle.kts consumer-rules.pro

cat > LGPL-SOURCES.txt <<TXT
Jazzy $APP_VERSION — LGPL source for bundled FFmpeg libraries

Jazzy for Android includes libavcodec, libswresample and libavutil from FFmpeg $FFMPEG_VERSION,
licensed under the GNU Lesser General Public License, version 2.1 or later. They are used
unmodified and linked dynamically (lib/<abi>/libavcodec.so, libswresample.so, libavutil.so in the
APK), so you can replace them with your own build of the same ABI.

Files published with this release:
  $tarball      Complete, unmodified FFmpeg $FFMPEG_VERSION source (official release tarball)
  $tarball.asc  FFmpeg's signature for that tarball
  $build_bundle Jazzy's build script and exact configure flags (build-ffmpeg/build.sh), licence
                notices and the Media3 FFmpeg extension sources (Apache-2.0) that call FFmpeg

To rebuild: extract $build_bundle and run build-ffmpeg/build.sh (needs the Android NDK; see
build-ffmpeg/README.md). The script verifies the configuration is LGPL-only.

These files are offered for at least three years from the date of this Jazzy release.
TXT

shasum -a 256 "$tarball" "$tarball.asc" "$build_bundle" LGPL-SOURCES.txt > SHA256SUMS
echo "LGPL source bundle ready in $OUT"
ls -la "$OUT"
