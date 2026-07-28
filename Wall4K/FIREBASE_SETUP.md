# Configurar el backend compartido (Firebase)

Con esto, las imágenes que suban los usuarios se guardan en la nube y **todos ven
las mismas**. Es gratis para empezar (plan Spark) y tarda unos 10 minutos.

> **Importante:** mientras NO añadas `google-services.json`, la app compila y
> funciona igual, pero en **modo local** (cada quien ve solo lo que sube en su
> teléfono). En cuanto añadas ese archivo, la app pasa sola a **modo compartido**.

## 1. Crear el proyecto

1. Entra en <https://console.firebase.google.com> con tu cuenta de Google.
2. **Agregar proyecto** → ponle un nombre (p. ej. `wall4k`) → crea el proyecto.

## 2. Registrar la app Android

1. En el proyecto, pulsa el icono de **Android**.
2. **Nombre del paquete de Android:** escribe exactamente:

   ```
   com.fraan.kroma
   ```

3. Registra la app y **descarga `google-services.json`**.
4. Copia ese archivo dentro de la carpeta **`Wall4K/app/`** del proyecto
   (al lado de `build.gradle.kts`). Debe quedar en:

   ```
   Wall4K/app/google-services.json
   ```

## 3. Activar Storage y Firestore

En la consola de Firebase:

- **Build → Storage → Comenzar** (elige una región).
- **Build → Firestore Database → Crear base de datos** (modo producción está bien;
  luego pegamos las reglas de abajo).

## 4. Reglas de seguridad (versión demo)

Estas reglas permiten que cualquiera lea y suba fondos, sin cuenta. Sirven para
probar; más abajo explico cómo hacerlo seguro.

**Firestore** (pestaña *Reglas*):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /wallpapers/{doc} {
      allow read: if true;
      allow create: if true;
      allow update, delete: if false;
    }
  }
}
```

**Storage** (pestaña *Reglas*):

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /wallpapers/{file} {
      allow read: if true;
      allow write: if request.resource.size < 20 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
  }
}
```

Pulsa **Publicar** en ambas.

## 5. Compilar

Abre `Wall4K/` en Android Studio y pulsa **Run ▶**. Al arrancar, la pantalla
**Subir** dirá “Comparte tu fondo con toda la comunidad”: eso confirma que
Firebase está activo. Las subidas aparecen para todos en **Explorar**.

## Cómo funciona por dentro

- Imagen → **Cloud Storage** en `wallpapers/{id}.jpg`.
- Metadatos (título, autor, categoría, URL) → **Firestore**, colección `wallpapers`.
- La app escucha esa colección en tiempo real, así que los fondos nuevos aparecen
  solos.

Todo esto vive en `data/remote/FirebaseWallpaperSource.kt`; el resto de la app no
cambia entre modo local y compartido.

## Hacerlo seguro (recomendado antes de publicar de verdad)

Las reglas demo dejan subir a cualquiera. Para producción:

1. Añade **Firebase Authentication** (p. ej. inicio con Google o anónimo).
2. Cambia las reglas a `allow create: if request.auth != null;` y guarda el
   `uid` del autor en cada documento.
3. Permite borrar solo al dueño:
   `allow delete: if request.auth.uid == resource.data.ownerUid;`

Con autenticación podrás también volver a mostrar el botón de eliminar para las
subidas propias (hoy está oculto en modo compartido a propósito).
