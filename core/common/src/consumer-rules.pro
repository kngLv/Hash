# Glide consumer ProGuard rules
# Keep Glide generated API classes and prevent R8 from stripping them when this module is packaged as an AAR.
# If your project uses an AppGlideModule (annotation processor) the generated GlideApp/GlideRequests
# classes must be kept, otherwise you'll get ClassNotFoundException at runtime.

# Keep the generated API classes (GlideApp / GlideRequests / GlideRequest)
-keep class * extends com.bumptech.glide.annotation.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep class **.AppGlideModule
-keep class **.GlideApp
-keep class **.GlideRequests
-keep class **.GlideRequest

# Keep Glide transformations & model loaders that may be referenced via reflection
-keep class com.bumptech.glide.** { *; }
-keep interface com.bumptech.glide.** { *; }

# Avoid warnings about javax.annotation
-dontwarn javax.annotation.**

# MMKV
-keep class com.tencent.mmkv.** { *; }

# Bugly
-dontwarn com.tencent.bugly.**
# 保留 Bugly 全部类和成员（避免 R8 删除）
-keep class com.tencent.bugly.** { *; }

# 保留通过 JNI 调用的 native 方法
-keepclassmembers class com.tencent.bugly.** { native * *(...); }
# 避免关于 Bugly 的警告
-dontwarn com.tencent.bugly.**

# 如果仍然报错，可显式保留该类
-keep class com.tencent.bugly.crashreport.crash.jni.NativeExceptionHandler { *; }
