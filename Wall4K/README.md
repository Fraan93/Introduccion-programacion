# Wall4K — App de fondos de pantalla 4K

App Android nativa para **explorar, descargar y subir** fondos de pantalla 4K,
inspirada en apps como *Wallcraft*. Hecha con **Kotlin + Jetpack Compose (Material 3)**.

<p align="center"><em>Explorar · Categorías · Subir · Favoritos · Detalle a pantalla completa</em></p>

## ✨ Funciones

- **Explorar**: catálogo 4K en cuadrícula escalonada (estilo *masonry*), con buscador y chips de categoría.
- **Categorías**: portadas por categoría y listado filtrado.
- **Detalle a pantalla completa**:
  - **Descargar** a la galería (carpeta `Pictures/Wall4K`).
  - **Aplicar como fondo** en pantalla de inicio, bloqueo o ambas (`WallpaperManager` real).
  - Marcar **favorito**.
- **Subir**: elige una imagen de tu galería, ponle título/autor/categoría y publícala en el feed local “Comunidad”.
- **Favoritos**: guardados de forma persistente (DataStore).
- Tema claro/oscuro automático.

## 🧱 Arquitectura

```
MainActivity (NavHost + NavigationBar)
 └─ WallViewModel (StateFlow)
     └─ WallpaperRepository  ← única fuente de datos
         ├─ SampleData        (catálogo integrado, imágenes de picsum.photos)
         ├─ DataStore         (favoritos + índice de subidas en JSON)
         └─ filesDir/uploads  (imágenes subidas por el usuario)
```

- **UI**: Jetpack Compose, Material 3, Navigation Compose.
- **Imágenes**: Coil (carga tanto URLs remotas como `file://` locales).
- **Persistencia**: DataStore Preferences + kotlinx.serialization.
- **Fondos/descargas**: `WallpaperManager` y `MediaStore`.

## ▶️ Cómo compilar

Requisitos: **Android Studio** (Ladybug o superior), JDK 17, Android SDK 35.

1. Abre la carpeta `Wall4K/` en Android Studio (*Open*).
2. Deja que sincronice Gradle (descarga el Android Gradle Plugin y las dependencias).
3. Conecta un dispositivo o abre un emulador y pulsa **Run ▶**.

O por línea de comandos (con el SDK instalado y `local.properties` apuntando a él):

```bash
cd Wall4K
./gradlew assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # instala en un dispositivo conectado
```

> Crea un `local.properties` con la ruta de tu SDK, por ejemplo:
> `sdk.dir=/Users/tu-usuario/Library/Android/sdk`

### ☁️ Descargar el APK sin instalar nada (GitHub Actions)

El repo incluye un workflow que **compila el APK en la nube** automáticamente.

1. En GitHub, ve a la pestaña **Actions** → workflow **“Compilar APK (Wall4K)”**.
2. Se ejecuta solo en cada push; también puedes lanzarlo a mano con **Run workflow**.
3. Cuando termine (✓ verde), abre la ejecución y descarga el artifact
   **`Wall4K-debug-apk`** (dentro va `Wall4K-debug.apk`).
4. Pásalo a tu teléfono Android e instálalo (activa “instalar apps de orígenes
   desconocidos”). Es un APK **debug**, ideal para probar.

> ¿Quieres una **Release** descargable con enlace fijo? Crea un tag que empiece
> por `v` (por ejemplo `v1.0`) y el workflow publicará el APK en *Releases*:
> ```bash
> git tag v1.0 && git push origin v1.0
> ```

## 🔌 Subidas compartidas con Firebase (ya integrado)

La app trae **Firebase (Cloud Storage + Firestore)** integrado para que las
subidas se **compartan entre todos los usuarios**. Funciona en dos modos, y
cambia solo:

- **Modo compartido** — si añades tu `google-services.json`, las subidas van a la
  nube y todos ven el mismo feed.
- **Modo local** (por defecto) — sin ese archivo, la app compila y funciona igual,
  pero cada subida se queda en el dispositivo.

👉 Sigue **[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md)** (unos 10 min, gratis) para
activar el modo compartido.

La lógica de red está aislada en `data/remote/FirebaseWallpaperSource.kt` y se
enchufa en `WallpaperRepository`; el resto de la app (UI, favoritos, descarga,
aplicar fondo) es idéntico en ambos modos. Si prefieres otro backend
(Supabase, REST propia), solo hay que reemplazar esa clase.

## 📋 Permisos

- `INTERNET` — cargar el catálogo.
- `SET_WALLPAPER` — aplicar fondos.
- `WRITE_EXTERNAL_STORAGE` (solo hasta Android 9) — guardar en la galería. En
  Android 10+ se usa `MediaStore`, sin permiso.

## 📝 Créditos de imágenes

Las imágenes del catálogo de ejemplo se sirven vía [picsum.photos](https://picsum.photos)
(Lorem Picsum) y solo se usan como contenido de demostración.
