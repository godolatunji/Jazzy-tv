# libtorrent4j: SWIG JNI glue. Native code looks up Java classes, fields and the SwigDirector_*
# callbacks by name, and Alerts.cast() maps native alert types to these classes, so none of it
# may be renamed or removed.
-keep class org.libtorrent4j.** { *; }
-keepclassmembers class org.libtorrent4j.** { native <methods>; }
-dontwarn org.libtorrent4j.**

# NanoHTTPD (loopback server for external players).
-dontwarn fi.iki.elonen.**
