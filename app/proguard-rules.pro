# Archive libraries are only ever driven through their public API from direct calls in
# archive/readers and archive/writers — no reflection-based extension point of ours is exercised —
# but all four still do some internal reflection/ServiceLoader work of their own (AES providers,
# codec lookup), so they're kept whole rather than left to R8's default shrinking heuristics.
-keep class net.lingala.zip4j.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class com.github.junrar.** { *; }

# commons-compress optionally supports zstd/brotli/pack200 (via com.github.luben:zstd-jni,
# org.brotli:dec, and org.ow2.asm:asm respectively) but only if those libraries happen to be on
# the classpath — none of them are here, since this app only ever reads/writes zip, 7z, tar, and
# gzip. R8 still needs telling that the missing references are expected, not an actual bug.
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
