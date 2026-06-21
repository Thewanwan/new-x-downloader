# Add project specific ProGuard rules here.
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** INSTANCE;
}
-keep class com.twitter.downloader.data.local.entity.** { *; }
