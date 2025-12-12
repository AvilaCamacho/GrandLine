# Add project specific ProGuard rules here.

# Proguard rules for Retrofit, Gson and other related libraries
-keep class retrofit2.Call
-keep class retrofit2.Callback
-keep class com.google.gson.stream.** { *; }

# Keep data classes used by Gson for serialization/deserialization
-keep class mx.edu.utez.data.model.** { *; }

# Proguard rules for Room
-keep class androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase_Impl
-keep class androidx.room.RoomOpenHelper

# Keep all the data classes that are annotated with @Entity
-keep @androidx.room.Entity class * {
    <fields>;
    <methods>;
}

# Keep all the DAOs
-keep @androidx.room.Dao interface * {
    <methods>;
}

# Proguard rules for Jetpack Compose
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
-keepclassmembers class * { @androidx.compose.ui.tooling.preview.Preview <methods>; }
-keep class androidx.compose.runtime.internal.ComposableLambda
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl
-keep class androidx.compose.runtime.internal.ComposableLambdaN
-keep class androidx.compose.runtime.internal.ComposableLambdaP
