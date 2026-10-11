#!/usr/bin/env bash
#
# Rebuilds the native libraries shipped by :core:ffmpeg from pinned sources:
#
#   src/main/jniLibs/<abi>/libavutil.so      FFmpeg (LGPL-2.1-or-later), shared
#   src/main/jniLibs/<abi>/libswresample.so  FFmpeg (LGPL-2.1-or-later), shared
#   src/main/jniLibs/<abi>/libavcodec.so     FFmpeg (LGPL-2.1-or-later), shared
#   src/main/jniLibs/<abi>/libffmpegJNI.so   Media3 decoder_ffmpeg JNI glue (Apache-2.0),
#                                            dynamically linked against the three libs above
#
# FFmpeg is configured LGPL-only (no --enable-gpl, --enable-version3 or --enable-nonfree) and
# built as shared libraries so it can be replaced/relinked independently of the app (LGPL §6).
# Every .so is linked with 16 KB max-page-size (Android 15+ / Google Play requirement).
#
# Usage:
#   core/ffmpeg/build-ffmpeg/build.sh [--ndk <path>] [--work <dir>] [--abis "arm64-v8a ..."]
#
# Requirements: git, GNU make (macOS /usr/bin/make is fine), Android NDK r28c (28.2.13676358).
# Defaults: NDK at $ANDROID_HOME/ndk/28.2.13676358 (or ~/Library/Android/sdk on macOS),
# work dir core/ffmpeg/build-ffmpeg/.work (git-ignored).
set -euo pipefail

# ---- Pinned inputs (change these together with LICENSE-NOTICE.md) -------------------------------
MEDIA3_REPO="https://github.com/androidx/media.git"
MEDIA3_TAG="1.11.1"
MEDIA3_COMMIT="8c6678b657ede1e7883fc164ef73ed483c7796c3"
FFMPEG_REPO="https://git.ffmpeg.org/ffmpeg.git"
FFMPEG_TAG="n7.1.5"
FFMPEG_COMMIT="3a0867c2bfda4a4d4309ca1a8cbdc6175e67f587"
NDK_VERSION="28.2.13676358"
API_LEVEL=28 # = minSdk
ENABLED_DECODERS=(
  ac3 eac3 dca truehd mlp # Dolby Digital (+), DTS (core; DTS-HD core extraction), TrueHD/MLP
  aac mp3 flac alac opus vorbis pcm_mulaw pcm_alaw
)
ABIS=(arm64-v8a armeabi-v7a x86_64)
# -------------------------------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_DIR="${MODULE_DIR}/src/main/jniLibs"
WORK_DIR="${SCRIPT_DIR}/.work"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}}"
NDK_PATH="${ANDROID_NDK_HOME:-${SDK_DIR}/ndk/${NDK_VERSION}}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ndk) NDK_PATH="$2"; shift 2 ;;
    --work) WORK_DIR="$2"; shift 2 ;;
    --abis) read -r -a ABIS <<< "$2"; shift 2 ;;
    -h|--help) sed -n '2,22p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

case "$(uname -s)" in
  Darwin) HOST_TAG="darwin-x86_64" ;; # NDK ships universal (arm64 + x86_64) binaries here
  Linux) HOST_TAG="linux-x86_64" ;;
  *) echo "Unsupported host $(uname -s)" >&2; exit 1 ;;
esac
TOOLCHAIN="${NDK_PATH}/toolchains/llvm/prebuilt/${HOST_TAG}"
[[ -d "${TOOLCHAIN}/bin" ]] || { echo "NDK toolchain not found at ${TOOLCHAIN}" >&2; exit 1; }
if [[ -f "${NDK_PATH}/source.properties" ]] && ! grep -q "${NDK_VERSION}" "${NDK_PATH}/source.properties"; then
  echo "warning: NDK at ${NDK_PATH} is not ${NDK_VERSION}; output will not be byte-identical" >&2
fi
JOBS="$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)"
mkdir -p "${WORK_DIR}"

fetch() { # <dir> <repo> <tag> <commit>
  local dir="$1" repo="$2" tag="$3" commit="$4"
  if [[ ! -d "${dir}/.git" ]]; then
    git -c advice.detachedHead=false clone --quiet --depth 1 --branch "${tag}" "${repo}" "${dir}"
  fi
  local head; head="$(git -C "${dir}" rev-parse HEAD)"
  if [[ "${head}" != "${commit}" ]]; then
    echo "${dir} is at ${head}, expected ${commit} (${tag})" >&2; exit 1
  fi
}
fetch "${WORK_DIR}/media" "${MEDIA3_REPO}" "${MEDIA3_TAG}" "${MEDIA3_COMMIT}"
fetch "${WORK_DIR}/ffmpeg" "${FFMPEG_REPO}" "${FFMPEG_TAG}" "${FFMPEG_COMMIT}"
JNI_SRC="${WORK_DIR}/media/libraries/decoder_ffmpeg/src/main/jni/ffmpeg_jni.cc"

PAGE_FLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
COMMON_OPTIONS=(
  --target-os=android
  --enable-cross-compile
  --enable-shared --disable-static --enable-pic
  --disable-autodetect
  --disable-doc --disable-programs --disable-debug
  --disable-everything
  --disable-avdevice --disable-avformat --disable-swscale --disable-postproc --disable-avfilter
  --disable-network
  --disable-symver
  --enable-swresample
  --disable-v4l2-m2m --disable-vulkan
)
for d in "${ENABLED_DECODERS[@]}"; do COMMON_OPTIONS+=("--enable-decoder=${d}"); done

for ABI in "${ABIS[@]}"; do
  case "${ABI}" in
    arm64-v8a)
      TRIPLE="aarch64-linux-android"; ARCH=(--arch=aarch64 --cpu=armv8-a)
      EXTRA_CFLAGS=""; EXTRA_LDFLAGS="" ;;
    armeabi-v7a)
      TRIPLE="armv7a-linux-androideabi"; ARCH=(--arch=arm --cpu=armv7-a)
      EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon"; EXTRA_LDFLAGS="-Wl,--fix-cortex-a8" ;;
    x86_64)
      # No nasm/yasm needed: x86_64 is emulator/Chromebook only, plain C is fine there.
      TRIPLE="x86_64-linux-android"; ARCH=(--arch=x86_64 --cpu=x86-64 --disable-asm)
      EXTRA_CFLAGS=""; EXTRA_LDFLAGS="" ;;
    *) echo "Unsupported ABI ${ABI}" >&2; exit 1 ;;
  esac
  CC="${TOOLCHAIN}/bin/${TRIPLE}${API_LEVEL}-clang"
  CXX="${TOOLCHAIN}/bin/${TRIPLE}${API_LEVEL}-clang++"
  PREFIX="${WORK_DIR}/install/${ABI}"
  BUILD="${WORK_DIR}/build/${ABI}"
  REPRO_CFLAGS="-ffile-prefix-map=${WORK_DIR}=. -ffile-prefix-map=${BUILD}=."
  rm -rf "${BUILD}" "${PREFIX}"; mkdir -p "${BUILD}" "${PREFIX}"

  echo "==> FFmpeg ${FFMPEG_TAG} for ${ABI}"
  CONFIGURE=(
    "${WORK_DIR}/ffmpeg/configure"
    --prefix="${PREFIX}"
    "${ARCH[@]}"
    --cc="${CC}" --cxx="${CXX}"
    --nm="${TOOLCHAIN}/bin/llvm-nm" --ar="${TOOLCHAIN}/bin/llvm-ar"
    --ranlib="${TOOLCHAIN}/bin/llvm-ranlib" --strip="${TOOLCHAIN}/bin/llvm-strip"
    --extra-cflags="${EXTRA_CFLAGS} ${REPRO_CFLAGS}"
    --extra-ldflags="${EXTRA_LDFLAGS} ${PAGE_FLAGS} -Wl,--build-id=sha1"
    "${COMMON_OPTIONS[@]}"
  )
  (cd "${BUILD}" && "${CONFIGURE[@]}" > configure.log)
  printf '%q ' "${CONFIGURE[@]}" > "${BUILD}/configure-line.txt"

  # Licence gate: refuse to ship anything that is not plain LGPL.
  for flag in CONFIG_GPL CONFIG_NONFREE CONFIG_VERSION3; do
    grep -q "^#define ${flag} 0" "${BUILD}/config.h" || { echo "${flag} is not 0 for ${ABI}" >&2; exit 1; }
  done
  grep -q '^!CONFIG_GPL=yes' "${BUILD}/ffbuild/config.mak"
  grep -q '^!CONFIG_NONFREE=yes' "${BUILD}/ffbuild/config.mak"

  make -C "${BUILD}" -j"${JOBS}" > "${BUILD}/make.log"
  make -C "${BUILD}" install-libs install-headers > /dev/null

  echo "==> libffmpegJNI.so for ${ABI}"
  "${CXX}" -std=c++11 -O2 -fPIC -fvisibility=hidden -fvisibility-inlines-hidden ${EXTRA_CFLAGS} ${REPRO_CFLAGS} \
    -I"${PREFIX}/include" -c "${JNI_SRC}" -o "${BUILD}/ffmpeg_jni.o"
  "${CXX}" -shared -o "${BUILD}/libffmpegJNI.so" "${BUILD}/ffmpeg_jni.o" \
    -Wl,-soname,libffmpegJNI.so -Wl,--no-undefined -Wl,-Bsymbolic -Wl,--build-id=sha1 \
    ${EXTRA_LDFLAGS} ${PAGE_FLAGS} -static-libstdc++ \
    -L"${PREFIX}/lib" -lavcodec -lswresample -lavutil -landroid -llog

  mkdir -p "${OUT_DIR}/${ABI}"
  for lib in libavutil.so libswresample.so libavcodec.so; do
    "${TOOLCHAIN}/bin/llvm-strip" --strip-unneeded -o "${OUT_DIR}/${ABI}/${lib}" "${PREFIX}/lib/${lib}"
  done
  "${TOOLCHAIN}/bin/llvm-strip" --strip-unneeded -o "${OUT_DIR}/${ABI}/libffmpegJNI.so" "${BUILD}/libffmpegJNI.so"
done

echo "==> Verifying"
for ABI in "${ABIS[@]}"; do
  for so in "${OUT_DIR}/${ABI}"/*.so; do
    align="$("${TOOLCHAIN}/bin/llvm-readelf" -lW "${so}" | awk '$1=="LOAD"{print $NF}' | sort -u | tr '\n' ' ')"
    needed="$("${TOOLCHAIN}/bin/llvm-readelf" -dW "${so}" | awk '/NEEDED/{print $NF}' | tr -d '[]' | tr '\n' ' ')"
    printf '%-12s %-18s align=%s needed=%s\n' "${ABI}" "$(basename "${so}")" "${align}" "${needed}"
  done
done
echo "Configure line (last ABI): $(cat "${BUILD}/configure-line.txt")"
echo "Done. Libraries are in ${OUT_DIR}"
