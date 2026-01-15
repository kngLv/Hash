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

-keep class com.tencent.mmkv.** { *; }
