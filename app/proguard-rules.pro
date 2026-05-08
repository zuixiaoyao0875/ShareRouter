# Room components
-keep class * extends androidx.room.RoomDatabase {
  <init>();
}
-keep class * extends androidx.room.Dao
-keep class * extends androidx.room.Entity

# Keep generated Room implementation classes
-keep class com.zxy.sharerouter.ShareDatabase_Impl {
  <init>();
}