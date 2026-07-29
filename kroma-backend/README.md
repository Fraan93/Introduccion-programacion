# Kroma backend

Node.js + Express + MongoDB Atlas. Catálogo masivo de fondos (imágenes) y **vídeos verticales en bucle** (live wallpapers) desde **Pexels, Unsplash y Pixabay**, con pestaña **IA** y **buscador / fondos premium de pago**.

## Subir a Render (5 min)
1. Sube esta carpeta `kroma-backend/` a un repo de GitHub (o usa este mismo).
2. Render → **New → Web Service** → conecta el repo → *Root Directory:* `kroma-backend`.
3. **Build Command:** `npm install` — **Start Command:** `npm start`.
4. En **Environment** añade las variables del `.env.example` (MONGODB_URI, PEXELS_API_KEY, UNSPLASH_ACCESS_KEY, PIXABAY_API_KEY, INGEST_SECRET, PREMIUM_TOKEN).
5. Deploy. Al arrancar con la BD vacía, ingiere el catálogo inicial automáticamente.

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/wallpapers?category=&type=image\|video&page=&limit=` | Catálogo paginado. |
| GET | `/wallpapers?type=video` | Live Wallpapers (vídeos verticales). |
| GET | `/categories` | Lista de categorías. |
| GET | `/ai?prompt=&page=` | **Pestaña IA** (siempre funciona, gratis). |
| GET | `/search?q=&page=` | **Buscador — de pago** (402 sin premium). |
| POST | `/ingest?secret=...&pages=2` | Recargar catálogo desde las APIs. |

## Premium / suscripción
- ~25% de los fondos se marcan `premium` (configurable con `PREMIUM_RATE`).
- La app envía la cabecera `x-premium-token: <PREMIUM_TOKEN>` cuando el usuario es PRO.
- Sin token: los fondos premium llegan con `locked: true` y `fullUrl: null` (se ve la miniatura + candado); `/search` responde **402**.

## Estructura de cada wallpaper (idéntica al modelo del APK + extras)
```json
{
  "id": "pexels_p_123",
  "title": "...", "author": "... · Pexels",
  "category": "Naturaleza",
  "type": "image",           // o "video" (live wallpaper)
  "source": "pexels",
  "thumbUrl": "...", "fullUrl": "...", "videoUrl": null,
  "resolution": "1080x1920",
  "premium": false, "locked": false
}
```
