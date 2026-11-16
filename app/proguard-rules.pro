# App Sonos — reglas ProGuard mínimas (minifyEnabled está en false en v1).
# Se deja el archivo porque build.gradle.kts lo referencia en release.
-keep class com.partyrockers.appsonos.** { *; }
