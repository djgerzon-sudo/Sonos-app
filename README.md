# App Sonos — v1 (código fuente Android)

Controlador Android para bocinas Sonos antiguas SP100 y ZP100, con reproductor
propio, playlists, carpetas, letras en Home, EQ de 12 bandas, compresor y reverb.

**Esto es código fuente, no una APK.** La APK se compila en GitHub Actions
(gratis), siguiendo los pasos de abajo. No se compila en el chat.

## Qué trae v1

- 5 pestañas: Inicio, Biblioteca, Carpetas, Playlists, EQ. Perfil con el avatar.
- Inicio: portada grande, play/pausa/siguiente/anterior, volumen, kbps, y la
  letra proyectada sobre la portada SOLO si la rola tiene letra disponible.
  Sin agrupar bocinas ni cambiar bocina en Inicio.
- Biblioteca: búsqueda, orden por título / artista / duración, "Recién agregados".
- Carpetas: navegar por carpetas y agregar una carpeta completa a una playlist.
- Playlists: crear, ver importadas (.m3u / .wpl) y reordenar rolas con flechas.
- EQ: 12 bandas, presets, Volumen x2, compresor y reverb apagados por defecto.
- Perfil: cuenta de ejemplo, Mi sistema, WiFi, retraso de audio, entrada de
  línea, LED, botones táctiles, actualizaciones y alarmas (ejemplo en v1).
- Control real de Sonos: descubrimiento SSDP, UPnP/SOAP al puerto 1400,
  AVTransport, RenderingControl y coordinador de grupo, en `sonos/`.
  Servidor HTTP local propio (sin librería externa) para servirle el audio a Sonos.
- Modo dark: negro, azul, blanco, grises y poco rojo.

## Versiones (no subirlas por separado)

- Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7, Java 17, compileSdk 34, targetSdk 34,
  minSdk 26, Compose BOM 2024.09.00, Media3/ExoPlayer 1.4.1.

## Paso 1: crear el repositorio en GitHub

1. Entra a https://github.com/new (necesitas cuenta de GitHub, gratis).
2. Nombre del repositorio: `Sonos-app` (el que Jonathan creó).
3. Elige **Public** (así los minutos gratis son 2,000 al mes; en privado son 500).
   Cada build tarda 3 a 5 minutos.
4. No marques "Add a README file". Crea el repositorio vacío.

## Paso 2: subir esta carpeta

1. En tu computadora, abre una terminal en la carpeta `android/` de este proyecto.
2. Corre, uno por uno:
   ```bash
   git init
   git add .
   git commit -m "App Sonos v1"
   git branch -M main
   git remote add origin https://github.com/djgerzon-sudo/Sonos-app.git
   git push -u origin main
   ```
   Cambia `TU_USUARIO` por tu usuario de GitHub.
3. Si GitHub te pide usuario y contraseña, usa un Personal Access Token
   (Settings > Developer settings > Personal access tokens), no tu contraseña.

## Paso 3: compilar la APK en GitHub Actions

1. En el repositorio, entra a la pestaña **Actions**.
2. Verás el workflow **"Build APK (debug)"** corriendo solo (se dispara con el push).
   Si no, entra al workflow y pulsa **Run workflow**.
3. Espera a que termine en verde (3 a 5 minutos la primera vez).
4. Si sale en rojo, entra al run, abre el paso que falló y copia el error.

## Paso 4: descargar e instalar la APK en tu teléfono

1. En el run ya terminado, baja hasta **Artifacts** y descarga
   `app-sonos-debug-apk`.
2. Descomprime el ZIP: dentro viene `app-debug.apk`.
3. Pásala a tu moto g52 (por USB, Drive o WhatsApp a ti mismo) y ábrela.
4. Android te pedirá permitir "Instalar apps desconocidas" para esa app:
   acéptalo solo para instalar esta APK.
5. La APK es **debug**: se instala y se prueba, pero no es para Play Store.

## Paso 5: probar con tus bocinas (sin borrar nada todavía)

1. **No borres la app de Sonos todavía.** La necesitas para configurar
   WiFi y el sistema.
2. Abre App Sonos en el teléfono, entra a **Perfil > Buscar bocinas en la WiFi**.
3. Tu teléfono y las bocinas deben estar en la misma WiFi.
4. Prueba play/pausa y volumen con una bocina.
5. Si algo falla, anota qué bocina, qué botón y qué mensaje salió.

## Límites conocidos de v1

- Las 12 rolas de la biblioteca son de **ejemplo** (títulos genéricos, sin
  archivos de audio ni letras reales). La lectura del almacenamiento
  (MediaStore) es v2.
- El EQ, compresor y reverb en v1 mueven la UI y el volumen; aplicar el
  efecto al audio antes de mandarlo a Sonos es trabajo de v2.
- Los ajustes de Perfil (WiFi, retraso, entrada de línea, etc.) son de
  ejemplo hasta probar el backend con las bocinas reales.
- Los artefactos de GitHub Actions se borran a los 30 días en este workflow:
  descarga la APK cuando se compile.

Nota: el archivo binario `gradle/wrapper/gradle-wrapper.jar` no se sube por la herramienta de GitHub de Poly; el workflow usa el Gradle 8.7 instalado por `setup-gradle` (`gradle assembleDebug`), así que no hace falta.

## Estructura del proyecto

```
android/
├── README.md
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
├── .github/workflows/build-apk.yml
└── app/
    ├── build.gradle.kts / proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/ (values, drawable, mipmap-anydpi-v26)
        └── java/com/partyrockers/appsonos/
            ├── MainActivity.kt          (5 pestañas + Perfil)
            ├── data/                    (Modelos, datos de ejemplo, PlaylistParser)
            ├── player/                  (LocalPlayer con ExoPlayer)
            ├── sonos/                   (SoapClient, SsdpDiscovery, SonosController, LocalHttpServer)
            ├── ui/theme/                (modo dark)
            ├── ui/screens/              (Inicio, Biblioteca, Carpetas, Playlists, EQ, Perfil)
            ├── ui/components/           (TrackRow)
            └── viewmodel/               (PlayerViewModel)
    └── src/test/java/.../               (pruebas de PlaylistParser y Track)
```
