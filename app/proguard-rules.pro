# ==============================================================================
# 1. PDFBox Android (Obrigatório: carrega fontes e tabelas de encoding via reflexão)
# ==============================================================================
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

# ==============================================================================
# 2. Hilt / Dagger & Injeção de Dependência
# ==============================================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepnames class * extends androidx.lifecycle.ViewModel

# ==============================================================================
# 3. Kotlin Coroutines & Serialization / Parcelable
# ==============================================================================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ==============================================================================
# 4. Privacidade & Segurança em Release (Remove logs de debug da pilha)
# ==============================================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.apache.commons.logging.**
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.tom_roush.harmony.**

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.gemalto.jp2.JP2Decoder