# Media3 FFmpeg extension (:core:ffmpeg).

# DefaultRenderersFactory finds the renderers by reflection (Class.forName + getConstructor),
# so the class names and the constructors it calls must survive R8.
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer {
    <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}
-keep class androidx.media3.decoder.ffmpeg.ExperimentalFfmpegVideoRenderer {
    <init>(long, android.os.Handler, androidx.media3.exoplayer.video.VideoRendererEventListener, int);
}

# JNI: native method names are bound by symbol (Java_androidx_media3_decoder_ffmpeg_...).
-keepclasseswithmembernames,includedescriptorclasses class androidx.media3.decoder.ffmpeg.** {
    native <methods>;
}

# Called from native code (ffmpeg_jni.cc) by name.
-keep,includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
    private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}
