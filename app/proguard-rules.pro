# Joda
-dontwarn org.joda.time.**

-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable, Signature, RuntimeVisibleAnnotations,AnnotationDefault

-keep class com.simplemobiletools.calendar.models.** { *; }

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
