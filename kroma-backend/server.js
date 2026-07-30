"use strict";

/**
 * Kroma backend — Node.js + Express + MongoDB Atlas.
 *
 * - Ingesta automática de un catálogo masivo desde APIs externas reales:
 *   Pexels (fotos + vídeos verticales), Pixabay (fotos + vídeos) y Unsplash (fotos).
 * - GET /wallpapers con paginación, adaptado a la estructura del APK (Kroma).
 * - Vídeos verticales en bucle para Live Wallpapers (type = "video").
 * - Pestaña IA independiente (GET /ai) que siempre funciona.
 * - Buscador de pago (GET /search) — requiere suscripción (token premium).
 * - Fondos premium aleatorios (bloqueados sin suscripción).
 */

require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");

const {
  PORT = 3000,
  MONGODB_URI,
  PEXELS_API_KEY,
  UNSPLASH_ACCESS_KEY,
  PIXABAY_API_KEY,
  INGEST_SECRET = "changeme",
  PREMIUM_TOKEN = "kroma-premium",
  PREMIUM_RATE = "0.25", // proporción de fondos marcados como premium
  GEMINI_API_KEY, // motor HD "Nano Banana" (Gemini 2.5 Flash Image)
  GEMINI_MODEL = "gemini-2.5-flash-image",
  PUBLIC_BASE_URL = "", // p.ej. https://kroma.onrender.com (para servir /img)
} = process.env;

// ---------------------------------------------------------------------------
// Modelo (coincide con el modelo Wallpaper del APK + campos extra type/premium)
// ---------------------------------------------------------------------------
const WallpaperSchema = new mongoose.Schema(
  {
    extId: { type: String, unique: true, index: true },
    title: { type: String, default: "Wallpaper" },
    author: { type: String, default: "" },
    category: { type: String, index: true, default: "Popular" },
    type: { type: String, enum: ["image", "video"], default: "image", index: true },
    source: { type: String, default: "" },
    thumbUrl: { type: String, default: "" },
    fullUrl: { type: String, default: "" }, // imagen full o mp4 del vídeo
    videoUrl: { type: String, default: "" }, // sólo vídeos (live wallpaper)
    resolution: { type: String, default: "" },
    premium: { type: Boolean, default: false, index: true },
    createdAt: { type: Date, default: Date.now, index: true },
  },
  { versionKey: false }
);

const Wallpaper = mongoose.model("Wallpaper", WallpaperSchema);

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
async function getJson(url, headers = {}) {
  const res = await fetch(url, { headers });
  if (!res.ok) throw new Error(`HTTP ${res.status} en ${url}`);
  return res.json();
}

const premiumRoll = () => Math.random() < (parseFloat(PREMIUM_RATE) || 0.25);

// ---------------------------------------------------------------------------
// Motor HD "Nano Banana" (Gemini 2.5 Flash Image)
// ---------------------------------------------------------------------------
// Las imágenes generadas se guardan en memoria y se sirven en /img/:id.
// Almacén sencillo con expulsión de las más antiguas (Render no persiste disco).
const generatedImages = new Map(); // id -> { buf: Buffer, mime, ts }
const MAX_GENERATED = 400;

function storeImage(buf, mime) {
  const id = `nb_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  generatedImages.set(id, { buf, mime, ts: Date.now() });
  while (generatedImages.size > MAX_GENERATED) {
    const oldest = generatedImages.keys().next().value;
    generatedImages.delete(oldest);
  }
  return id;
}

// Una llamada a Gemini => una imagen (PNG/JPEG en base64 inline).
async function geminiImage(prompt) {
  if (!GEMINI_API_KEY) return null;
  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${GEMINI_API_KEY}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: { responseModalities: ["IMAGE"] },
    }),
  });
  if (!res.ok) throw new Error(`Gemini HTTP ${res.status}: ${(await res.text()).slice(0, 200)}`);
  const data = await res.json();
  const parts =
    (data.candidates && data.candidates[0] && data.candidates[0].content &&
      data.candidates[0].content.parts) || [];
  const img = parts.find((p) => p.inlineData && p.inlineData.data);
  if (!img) return null;
  return {
    buf: Buffer.from(img.inlineData.data, "base64"),
    mime: img.inlineData.mimeType || "image/png",
  };
}

function publicBase(req) {
  if (PUBLIC_BASE_URL) return PUBLIC_BASE_URL.replace(/\/+$/, "");
  const proto = req.get("x-forwarded-proto") || req.protocol || "https";
  return `${proto}://${req.get("host")}`;
}

// ---------------------------------------------------------------------------
// Proveedores externos (APIs reales)
// ---------------------------------------------------------------------------
async function pexelsPhotos(query, page) {
  if (!PEXELS_API_KEY) return [];
  const url = `https://api.pexels.com/v1/search?query=${encodeURIComponent(
    query
  )}&orientation=portrait&per_page=80&page=${page}`;
  const d = await getJson(url, { Authorization: PEXELS_API_KEY });
  return (d.photos || []).map((p) => ({
    extId: `pexels_p_${p.id}`,
    title: p.alt || "Wallpaper",
    author: `${p.photographer || "Pexels"} · Pexels`,
    type: "image",
    source: "pexels",
    thumbUrl: (p.src && (p.src.portrait || p.src.large)) || "",
    fullUrl: (p.src && p.src.original) || "",
    resolution: `${p.width}x${p.height}`,
  }));
}

async function pexelsVideos(query, page) {
  if (!PEXELS_API_KEY) return [];
  const url = `https://api.pexels.com/videos/search?query=${encodeURIComponent(
    query
  )}&orientation=portrait&per_page=50&page=${page}`;
  const d = await getJson(url, { Authorization: PEXELS_API_KEY });
  return (d.videos || [])
    .map((v) => {
      const files = (v.video_files || []).filter((f) => f.file_type === "video/mp4" && f.link);
      const portrait = files.filter((f) => (f.height || 0) >= (f.width || 0));
      const pick = (portrait.length ? portrait : files).sort(
        (a, b) => (b.height || 0) - (a.height || 0)
      )[0];
      if (!pick) return null;
      return {
        extId: `pexels_v_${v.id}`,
        title: "Live Wallpaper",
        author: `${(v.user && v.user.name) || "Pexels"} · Pexels`,
        type: "video",
        source: "pexels",
        thumbUrl: v.image || "",
        fullUrl: pick.link,
        videoUrl: pick.link,
        resolution: `${pick.width}x${pick.height}`,
      };
    })
    .filter(Boolean);
}

async function pixabayPhotos(query, page) {
  if (!PIXABAY_API_KEY) return [];
  const url = `https://pixabay.com/api/?key=${PIXABAY_API_KEY}&q=${encodeURIComponent(
    query
  )}&image_type=photo&orientation=vertical&per_page=100&page=${page}&safesearch=true`;
  const d = await getJson(url);
  return (d.hits || []).map((h) => ({
    extId: `pixabay_p_${h.id}`,
    title: (h.tags || "Wallpaper").split(",")[0].trim(),
    author: `${h.user || "Pixabay"} · Pixabay`,
    type: "image",
    source: "pixabay",
    thumbUrl: h.webformatURL || "",
    fullUrl: h.largeImageURL || h.fullHDURL || h.webformatURL || "",
    resolution: `${h.imageWidth}x${h.imageHeight}`,
  }));
}

async function pixabayVideos(query, page) {
  if (!PIXABAY_API_KEY) return [];
  const url = `https://pixabay.com/api/videos/?key=${PIXABAY_API_KEY}&q=${encodeURIComponent(
    query
  )}&per_page=80&page=${page}&safesearch=true`;
  const d = await getJson(url);
  return (d.hits || [])
    .map((h) => {
      const vids = h.videos || {};
      const v = vids.large && vids.large.url ? vids.large : vids.medium || vids.small || {};
      if (!v.url) return null;
      const thumb =
        v.thumbnail ||
        (h.picture_id ? `https://i.vimeocdn.com/video/${h.picture_id}_295x166.jpg` : "");
      return {
        extId: `pixabay_v_${h.id}`,
        title: (h.tags || "Live Wallpaper").split(",")[0].trim(),
        author: `${h.user || "Pixabay"} · Pixabay`,
        type: "video",
        source: "pixabay",
        thumbUrl: thumb,
        fullUrl: v.url,
        videoUrl: v.url,
        resolution: `${v.width || 1080}x${v.height || 1920}`,
      };
    })
    .filter(Boolean);
}

async function unsplashPhotos(query, page) {
  if (!UNSPLASH_ACCESS_KEY) return [];
  const url = `https://api.unsplash.com/search/photos?query=${encodeURIComponent(
    query
  )}&orientation=portrait&per_page=30&page=${page}`;
  const d = await getJson(url, { Authorization: `Client-ID ${UNSPLASH_ACCESS_KEY}` });
  return (d.results || []).map((p) => ({
    extId: `unsplash_p_${p.id}`,
    title: p.description || p.alt_description || "Wallpaper",
    author: `${(p.user && p.user.name) || "Unsplash"} · Unsplash`,
    type: "image",
    source: "unsplash",
    thumbUrl: (p.urls && (p.urls.small || p.urls.regular)) || "",
    fullUrl: (p.urls && (p.urls.full || p.urls.regular)) || "",
    resolution: `${p.width}x${p.height}`,
  }));
}

// ---------------------------------------------------------------------------
// Ingesta: categorías -> términos de búsqueda -> proveedores -> Mongo (upsert)
// ---------------------------------------------------------------------------
const CATEGORIES = {
  Popular: ["4k wallpaper", "aesthetic"],
  Naturaleza: ["nature", "landscape"],
  Espacio: ["galaxy", "outer space"],
  Anime: ["anime"],
  Coches: ["sports car"],
  Ciudad: ["city night"],
  Abstracto: ["abstract"],
  AMOLED: ["dark black minimal"],
  Minimalista: ["minimal"],
  Neon: ["neon"],
  Animales: ["animal"],
  Fantasia: ["fantasy art"],
};

async function upsertMany(items, category) {
  let n = 0;
  for (const it of items) {
    if (!it || !it.fullUrl || !it.thumbUrl) continue;
    await Wallpaper.updateOne(
      { extId: it.extId },
      { $setOnInsert: { ...it, category, premium: premiumRoll() } },
      { upsert: true }
    );
    n++;
  }
  return n;
}

let ingesting = false;
async function ingest({ pages = 2 } = {}) {
  if (ingesting) return 0;
  ingesting = true;
  let total = 0;
  try {
    for (const [category, queries] of Object.entries(CATEGORIES)) {
      for (const q of queries) {
        for (let page = 1; page <= pages; page++) {
          const results = await Promise.allSettled([
            pexelsPhotos(q, page),
            pixabayPhotos(q, page),
            unsplashPhotos(q, page),
            pexelsVideos(q, page),
            pixabayVideos(q, page),
          ]);
          for (const r of results) {
            if (r.status === "fulfilled") total += await upsertMany(r.value, category);
          }
        }
      }
    }
  } finally {
    ingesting = false;
  }
  return total;
}

// ---------------------------------------------------------------------------
// API
// ---------------------------------------------------------------------------
const app = express();
app.use(cors());
app.use(express.json());

function premiumGranted(req) {
  const token = req.get("x-premium-token") || req.query.premium_token;
  return !!PREMIUM_TOKEN && token === PREMIUM_TOKEN;
}

function present(w, granted) {
  const locked = w.premium && !granted;
  return {
    id: w.extId,
    title: w.title,
    author: w.author,
    category: w.category,
    type: w.type,
    source: w.source,
    thumbUrl: w.thumbUrl, // la miniatura siempre visible (para mostrar el candado)
    fullUrl: locked ? null : w.fullUrl,
    videoUrl: locked ? null : w.videoUrl || null,
    resolution: w.resolution,
    premium: !!w.premium,
    locked,
  };
}

app.get("/", (req, res) =>
  res.json({ name: "Kroma backend", status: "ok", endpoints: ["/generate", "/img/:id", "/wallpapers", "/ai", "/search", "/categories", "/ingest"] })
);

app.get("/health", (req, res) => res.json({ ok: true }));

app.get("/categories", async (req, res) => {
  const categories = await Wallpaper.distinct("category");
  res.json({ categories: ["Popular", ...categories.filter((c) => c !== "Popular").sort()] });
});

// Catálogo paginado (imágenes y/o vídeos). ?category=&type=image|video&page=&limit=
app.get("/wallpapers", async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(60, Math.max(1, parseInt(req.query.limit, 10) || 24));
  const filter = {};
  if (req.query.category && req.query.category !== "Popular") filter.category = req.query.category;
  if (req.query.type === "image" || req.query.type === "video") filter.type = req.query.type;

  const granted = premiumGranted(req);
  const [total, docs] = await Promise.all([
    Wallpaper.countDocuments(filter),
    Wallpaper.find(filter).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit).lean(),
  ]);

  res.json({
    data: docs.map((d) => present(d, granted)),
    page,
    limit,
    total,
    hasMore: page * limit < total,
  });
});

// Atajo: sólo Live Wallpapers (vídeos verticales en bucle)
app.get("/wallpapers/live", async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(60, Math.max(1, parseInt(req.query.limit, 10) || 24));
  const filter = { type: "video" };
  if (req.query.category && req.query.category !== "Popular") filter.category = req.query.category;
  const granted = premiumGranted(req);
  const [total, docs] = await Promise.all([
    Wallpaper.countDocuments(filter),
    Wallpaper.find(filter).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit).lean(),
  ]);
  res.json({ data: docs.map((d) => present(d, granted)), page, limit, total, hasMore: page * limit < total });
});

// Pestaña IA (independiente del buscador). Siempre funciona.
app.get("/ai", (req, res) => {
  const prompt = (req.query.prompt || "cosmic nebula galaxy").toString();
  const page = Math.max(0, parseInt(req.query.page, 10) || 0);
  const per = 12;
  const enc = encodeURIComponent(`${prompt}, phone wallpaper, ultra detailed`);
  const base = `https://image.pollinations.ai/prompt/${enc}`;
  const data = Array.from({ length: per }, (_, i) => {
    const seed = 1000 + page * per + i;
    const common = `nologo=true&model=turbo&seed=${seed}`;
    return {
      id: `ai_${seed}`,
      title: prompt,
      author: "Kroma AI",
      category: "IA",
      type: "image",
      source: "ai",
      thumbUrl: `${base}?width=360&height=640&${common}`,
      fullUrl: `${base}?width=1080&height=1920&${common}`,
      videoUrl: null,
      resolution: "1080x1920",
      premium: false,
      locked: false,
    };
  });
  res.json({ data, page, hasMore: true });
});

// Motor HD "Nano Banana" (Gemini) — función PRO. POST /generate
// body: { prompt, style, styleLabel, aspect: "phone"|"square", count, page }
app.post("/generate", async (req, res) => {
  if (!premiumGranted(req)) {
    return res.status(402).json({
      error: "premium_required",
      message: "El motor HD (Nano Banana) es una función PRO. Suscríbete para usarlo.",
    });
  }
  if (!GEMINI_API_KEY) {
    return res.status(503).json({ error: "gemini_unavailable", message: "Falta GEMINI_API_KEY." });
  }

  const body = req.body || {};
  const userPrompt = (body.prompt || "beautiful abstract wallpaper").toString().trim();
  const style = (body.style || "").toString().trim();
  const styleLabel = (body.styleLabel || "IA").toString();
  const square = body.aspect === "square";
  const count = Math.min(4, Math.max(1, parseInt(body.count, 10) || 2));
  const orientation = square
    ? "square 1:1 composition"
    : "vertical 9:16 phone wallpaper composition, full screen";
  const finalPrompt =
    [userPrompt, style, orientation, "high quality, ultra detailed, no text, no watermark"]
      .filter(Boolean)
      .join(", ");

  try {
    const settled = await Promise.allSettled(
      Array.from({ length: count }, () => geminiImage(finalPrompt))
    );
    const base = publicBase(req);
    const items = [];
    for (const r of settled) {
      if (r.status === "fulfilled" && r.value) {
        const id = storeImage(r.value.buf, r.value.mime);
        const url = `${base}/img/${id}`;
        items.push({
          id,
          url,
          thumbUrl: url,
          prompt: userPrompt,
          category: styleLabel,
          resolution: square ? "1440x1440" : "1440x2560",
        });
      }
    }
    if (!items.length) {
      return res.status(502).json({ error: "generation_failed", message: "Gemini no devolvió imágenes." });
    }
    res.json({ items });
  } catch (e) {
    res.status(500).json({ error: "generation_error", message: e.message });
  }
});

// Sirve una imagen generada por el motor HD.
app.get("/img/:id", (req, res) => {
  const rec = generatedImages.get(req.params.id);
  if (!rec) return res.status(404).send("not found");
  res.set("Content-Type", rec.mime);
  res.set("Cache-Control", "public, max-age=31536000, immutable");
  res.send(rec.buf);
});

// Buscador = función de pago (suscripción).
app.get("/search", async (req, res) => {
  if (!premiumGranted(req)) {
    return res.status(402).json({
      error: "premium_required",
      message: "La búsqueda es una función de pago. Suscríbete para usarla.",
    });
  }
  const q = (req.query.q || "").toString().trim();
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(60, Math.max(1, parseInt(req.query.limit, 10) || 24));
  const filter = q
    ? { $or: [{ title: new RegExp(escapeRegExp(q), "i") }, { category: new RegExp(escapeRegExp(q), "i") }] }
    : {};
  const docs = await Wallpaper.find(filter)
    .sort({ createdAt: -1 })
    .skip((page - 1) * limit)
    .limit(limit)
    .lean();
  res.json({ data: docs.map((d) => present(d, true)), page, limit, hasMore: docs.length === limit });
});

function escapeRegExp(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

// Disparar la ingesta manualmente. POST /ingest?secret=...&pages=2
app.post("/ingest", async (req, res) => {
  const secret = req.query.secret || req.get("x-ingest-secret");
  if (secret !== INGEST_SECRET) return res.status(403).json({ error: "forbidden" });
  const pages = Math.min(6, Math.max(1, parseInt(req.query.pages, 10) || 2));
  try {
    const ingested = await ingest({ pages });
    res.json({ ok: true, ingested });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ---------------------------------------------------------------------------
// Arranque
// ---------------------------------------------------------------------------
async function start() {
  if (!MONGODB_URI) {
    console.error("Falta MONGODB_URI en las variables de entorno.");
    process.exit(1);
  }
  await mongoose.connect(MONGODB_URI);
  console.log("MongoDB conectado.");

  app.listen(PORT, () => console.log(`Kroma backend escuchando en :${PORT}`));

  // Ingesta automática la primera vez (si la base está vacía).
  const count = await Wallpaper.estimatedDocumentCount();
  if (count === 0) {
    console.log("Base vacía: ingiriendo catálogo inicial...");
    ingest({ pages: 2 })
      .then((n) => console.log(`Ingesta inicial completada: ${n} elementos.`))
      .catch((e) => console.error("Error en ingesta inicial:", e.message));
  } else {
    console.log(`Catálogo con ${count} elementos.`);
  }
}

start().catch((e) => {
  console.error("Fallo al arrancar:", e);
  process.exit(1);
});
