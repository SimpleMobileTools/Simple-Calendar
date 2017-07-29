# Joda
-dontwarn org.joda.time.**

-keep class com.google.common.**
-dontwarn com.google.common.**

-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable, Signature, RuntimeVisibleAnnotations,AnnotationDefault

-keep class com.simplemobiletools.calendar.models.** { *; }

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
