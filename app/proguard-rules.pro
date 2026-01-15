# 在此添加项目特定的 ProGuard 规则。
# 可以通过 build.gradle 中的 proguardFiles 设置来控制要应用的配置文件。
#
# 更多详情请参见
#   http://developer.android.com/guide/developing/tools/proguard.html

# 如果项目使用带有 JS 的 WebView，请取消下列注释并指定 JavaScript 接口类的完全限定类名：
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# 取消下面注释可保留行号信息，便于调试堆栈跟踪。
#-keepattributes SourceFile,LineNumberTable

# 如果保留行号信息，可取消下面注释以隐藏原始源文件名。
#-renamesourcefileattribute SourceFile

-dontwarn javax.annotation.Nullable
-dontwarn javax.lang.model.element.Element

# R8：抑制注解类中引用的仅用于编译期的 JDK 类型导致的警告
# 根本原因：某些注解（例如 com.google.errorprone.annotations）引用了 Android 平台上不存在的 javax.lang.model.* 类型。
# 这些类型通常只在编译期（注解处理器）使用。抑制这些警告可以避免 R8 在 release 构建时失败。
# 如果这些注解在运行时确实需要，请根据需要使用 -keep 而不是 -dontwarn。
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.lang.model.**

# 可选：在运行时保留 errorprone 注解类（仅在确实需要时取消注释）
#-keep class com.google.errorprone.annotations.** { *; }

# 自定义文件名

-obfuscationdictionary dictionary
-classobfuscationdictionary dictionary
-packageobfuscationdictionary dictionary

# 基于 sdk/tools/proguard/proguard-android-optimize.txt 的修改
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-verbose

# 不要删除无用代码（关闭 shrink）
-dontshrink

# 不混淆泛型签名信息
-keepattributes Signature

# 不混淆注解信息
-keepattributes *Annotation*




# 不混淆本地方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 不混淆在 XML 布局中通过 onClick 属性引用的 Activity 方法
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# 不混淆枚举类的 values()/valueOf()
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 不混淆实现 Parcelable 接口的类的 CREATOR
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# 不混淆实现 Serializable 的类的序列化相关成员
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 不混淆 R 文件中的字段
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 不混淆被 WebView 设置为 JS 接口的方法名
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
