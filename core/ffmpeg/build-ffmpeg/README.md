# Rebuilding the FFmpeg native libraries

`src/main/jniLibs/<abi>/*.so` are committed so normal builds and CI need no NDK. This directory
recreates them from pinned sources (also what LGPL-2.1 §6 needs: users can rebuild and relink).

## Requirements

- macOS (Apple Silicon or Intel) or Linux x86_64, `git`, GNU make (`/usr/bin/make` on macOS works)
- Android NDK **r28c (28.2.13676358)**:
  `sdkmanager "ndk;28.2.13676358"` (cmdline-tools in `$ANDROID_HOME/cmdline-tools/latest`)
- No nasm/yasm/ninja/CMake needed (x86_64 is built with `--disable-asm`; the JNI wrapper is
  compiled directly with the NDK clang)

## Run

```sh
core/ffmpeg/build-ffmpeg/build.sh                         # NDK from $ANDROID_HOME/ndk/28.2.13676358
core/ffmpeg/build-ffmpeg/build.sh --ndk /path/to/ndk      # explicit NDK
core/ffmpeg/build-ffmpeg/build.sh --work /tmp/ffwork      # keep clones/intermediates elsewhere
core/ffmpeg/build-ffmpeg/build.sh --abis "arm64-v8a"      # subset of ABIs
```

About two minutes on an M-series Mac. The script:

1. Shallow-clones Media3 `1.11.1` and FFmpeg `n7.1.5` into the work dir (default
   `build-ffmpeg/.work`, git-ignored) and checks both commit hashes.
2. For each ABI (arm64-v8a, armeabi-v7a, x86_64) configures FFmpeg as **shared, LGPL-only** with
   only the audio decoders listed in `ENABLED_DECODERS`, and fails if `config.h` has
   `CONFIG_GPL`, `CONFIG_NONFREE` or `CONFIG_VERSION3` set.
3. Compiles Media3's `ffmpeg_jni.cc` into `libffmpegJNI.so`, dynamically linked against
   `libavcodec.so`, `libswresample.so`, `libavutil.so` (libc++ linked statically, no
   `libc++_shared.so` needed).
4. Links everything with `-Wl,-z,max-page-size=16384` (16 KB pages: Android 15+ / Google Play),
   strips, copies into `src/main/jniLibs/<abi>/` and prints each library's LOAD alignment and
   `DT_NEEDED` entries.

## Updating versions

Change `MEDIA3_TAG`/`MEDIA3_COMMIT` (keep in step with `media3` in `gradle/libs.versions.toml`),
`FFMPEG_TAG`/`FFMPEG_COMMIT`, then:

1. Re-run the script.
2. Copy the Java sources: `cp .work/media/libraries/decoder_ffmpeg/src/main/java/androidx/media3/decoder/ffmpeg/*.java ../src/main/java/androidx/media3/decoder/ffmpeg/`
3. Compare `.work/media/libraries/decoder_ffmpeg/proguard-rules.txt` with `../consumer-rules.pro`.
4. Update `../LICENSE-NOTICE.md`, the About screen entry (`feature/settings/.../AboutScreen.kt`) and
   the version assertion in `src/androidTest/.../FfmpegLibraryDeviceTest.kt`.
5. Verify on a device: `./gradlew :core:ffmpeg:connectedDebugAndroidTest`.

Media3 currently recommends FFmpeg 6.0; its JNI uses the `AVChannelLayout`/`swr_alloc_set_opts2`
APIs (FFmpeg ≥ 5.1), so 7.1 works unchanged. FFmpeg 8 has not been tried.

Output is reproducible for a given NDK and host (paths are mapped away with
`-ffile-prefix-map`), but NDK builds on Linux vs macOS may differ byte-wise.

`SHA256SUMS` lists the committed libraries (NDK r28c on macOS). Check with
`(cd core/ffmpeg/src/main/jniLibs && shasum -a 256 -c ../../../build-ffmpeg/SHA256SUMS)`.

## Device tests and test media

`./gradlew :core:ffmpeg:connectedDebugAndroidTest` (set `ANDROID_SERIAL` to pick a device) checks
that the libraries load, that DTS/TrueHD/etc. are supported, and plays two muted 1 s sine tones
through ExoPlayer with the FFmpeg renderer. The tones in `src/androidTest/assets/` were generated
(no third-party media) with a desktop FFmpeg 9.0.2 (`brew install ffmpeg`):

```sh
PAN='pan=5.1|FL=c0|FR=c0|FC=c0|LFE=c0|BL=c0|BR=c0'
ffmpeg -f lavfi -i sine=frequency=440:duration=1:sample_rate=48000 -af "$PAN" -c:a dca -strict -2 -b:a 768k sine-dts-5.1.mka
ffmpeg -f lavfi -i sine=frequency=440:duration=1:sample_rate=48000 -af "$PAN" -c:a truehd -strict -2 sine-truehd-5.1.mka
```
