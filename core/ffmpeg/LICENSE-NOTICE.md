# :core:ffmpeg licence notice

This module bundles two separately licensed components.

## 1. FFmpeg 7.1.5 — LGPL-2.1-or-later

| | |
|---|---|
| Component | FFmpeg `libavcodec`, `libswresample`, `libavutil` |
| Version | 7.1.5 (tag `n7.1.5`, commit `3a0867c2bfda4a4d4309ca1a8cbdc6175e67f587`) |
| Source | https://git.ffmpeg.org/ffmpeg.git (mirror: https://github.com/FFmpeg/FFmpeg/tree/n7.1.5) |
| Licence | GNU Lesser General Public License, version 2.1 or (at your option) any later version. Full text: [`build-ffmpeg/COPYING.LGPLv2.1`](build-ffmpeg/COPYING.LGPLv2.1) |
| Linking | **Dynamic.** Each library is a separate shared object (`libavcodec.so`, `libswresample.so`, `libavutil.so`) in `src/main/jniLibs/<abi>/`; `libffmpegJNI.so` only lists them as `DT_NEEDED`. They can be replaced by a user-built FFmpeg with the same ABI without relinking the app. |
| Modifications | None. FFmpeg sources are used unmodified. |

FFmpeg is configured LGPL-only: no `--enable-gpl`, no `--enable-version3`, no `--enable-nonfree`.
`build-ffmpeg/build.sh` refuses to produce libraries unless the generated `config.h` has
`CONFIG_GPL 0`, `CONFIG_NONFREE 0`, `CONFIG_VERSION3 0` (configure reports
"License: LGPL version 2.1 or later").

Configure line (arm64-v8a; the other ABIs differ only in `--arch/--cpu`, the compiler triple and,
for armeabi-v7a, `-march=armv7-a -mfloat-abi=softfp -mfpu=neon` / `-Wl,--fix-cortex-a8`; x86_64
adds `--disable-asm`). Absolute paths shortened to `$NDK` / `$WORK`:

```
$WORK/ffmpeg/configure --prefix=$WORK/install/arm64-v8a \
  --arch=aarch64 --cpu=armv8-a \
  --cc=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android28-clang \
  --cxx=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android28-clang++ \
  --nm=…/llvm-nm --ar=…/llvm-ar --ranlib=…/llvm-ranlib --strip=…/llvm-strip \
  --extra-cflags="-ffile-prefix-map=$WORK=. -ffile-prefix-map=$WORK/build/arm64-v8a=." \
  --extra-ldflags="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,--build-id=sha1" \
  --target-os=android --enable-cross-compile \
  --enable-shared --disable-static --enable-pic --disable-autodetect \
  --disable-doc --disable-programs --disable-debug --disable-everything \
  --disable-avdevice --disable-avformat --disable-swscale --disable-postproc --disable-avfilter \
  --disable-network --disable-symver --enable-swresample --disable-v4l2-m2m --disable-vulkan \
  --enable-decoder=ac3 --enable-decoder=eac3 --enable-decoder=dca --enable-decoder=truehd \
  --enable-decoder=mlp --enable-decoder=aac --enable-decoder=mp3 --enable-decoder=flac \
  --enable-decoder=alac --enable-decoder=opus --enable-decoder=vorbis \
  --enable-decoder=pcm_mulaw --enable-decoder=pcm_alaw
```

Toolchain: Android NDK r28c (28.2.13676358), API level 28 (= app minSdk).

### Rebuilding / relinking (LGPL §6)

`build-ffmpeg/build.sh` fetches the exact sources above and regenerates every `.so` in
`src/main/jniLibs/`; see [`build-ffmpeg/README.md`](build-ffmpeg/README.md). To use a modified
FFmpeg, build it with the same configure options (or any compatible superset of decoders, keeping
the `lib*.so` SONAMEs) and replace the three FFmpeg `.so` files in the APK or in this module.

### Where users get the source (LGPL-2.1 §6)

Every Jazzy release publishes, next to the APKs, the files produced by
[`release/package-lgpl-sources.sh`](../../release/package-lgpl-sources.sh):

| File | Contents |
|---|---|
| `ffmpeg-7.1.5.tar.xz` (+ `.asc`) | The official, unmodified FFmpeg 7.1.5 release tarball and FFmpeg's signature. SHA-256 `de668509caf9e35e3cd162473441fdb29538c6d96ed080292b3cf9e6fc5d558f`, pinned in the script. Its source files are identical to tag `n7.1.5`, which the shipped libraries were built from (checked 2026-10-11). |
| `jazzy-ffmpeg-build-<version>.tar.gz` | `build-ffmpeg/` (build script, configure flags, checksums), this notice, and the Media3 extension sources. |
| `LGPL-SOURCES.txt` | What the files are and how to rebuild or replace the libraries. |
| `SHA256SUMS` | Checksums for all of the above. |

The tag-triggered workflow `.github/workflows/release.yml` attaches these to the same release as
the APKs. Wherever APKs are hosted, these files must be hosted with them, for at least three years
after each release.

## 2. Media3 FFmpeg decoder extension — Apache-2.0

| | |
|---|---|
| Component | `androidx.media3.decoder.ffmpeg` Java sources (`src/main/java/`) and the JNI wrapper `ffmpeg_jni.cc` compiled into `libffmpegJNI.so` |
| Version | Media3 `1.11.1` (commit `8c6678b657ede1e7883fc164ef73ed483c7796c3`), path `libraries/decoder_ffmpeg` |
| Source | https://github.com/androidx/media/tree/1.11.1/libraries/decoder_ffmpeg |
| Licence | Apache License 2.0, Copyright The Android Open Source Project. Text: [`build-ffmpeg/LICENSE-media3-Apache-2.0.txt`](build-ffmpeg/LICENSE-media3-Apache-2.0.txt) |
| Modifications | None. |

## Patents / codec notice

DTS, Dolby Digital (AC-3/E-AC-3) and Dolby TrueHD are subject to patents and trademarks held by
their owners in some jurisdictions; the LGPL covers copyright only. Several of the relevant
patents (AC-3, DTS core) have expired. This notice is not legal advice.
