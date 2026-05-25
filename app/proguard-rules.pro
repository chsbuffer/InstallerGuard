-optimizations

-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
}

# libxposed-api is compileOnly (provided by framework at runtime), so its annotation
# classes are absent during R8 processing. @SinceApi is RetentionPolicy.CLASS only.
-dontwarn io.github.libxposed.annotation.**

-repackageclasses
-allowaccessmodification
