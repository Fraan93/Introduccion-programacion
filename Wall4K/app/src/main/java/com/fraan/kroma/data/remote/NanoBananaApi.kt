package com.fraan.kroma.data.remote

import com.fraan.kroma.data.AiStyle
import com.fraan.kroma.data.AspectRatio
import com.fraan.kroma.data.model.Wallpaper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * PRO HD generator ("Nano Banana" = Gemini 2.5 Flash Image), served by the Kroma
 * backend. The API key lives on the server; the app only sends the prompt plus the
 * premium token. Returns hosted image URLs the backend produced.
 *
 * Returns `null` when the backend is not configured or the request fails, so the
 * repository can fall back to the pollinations HD model instead of showing nothing.
 */
object NanoBananaApi {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun generate(
        baseUrl: String,
        premiumToken: String,
        prompt: String,
        style: AiStyle,
        aspect: AspectRatio,
        page: Int,
        count: Int
    ): List<Wallpaper>? {
        if (baseUrl.isBlank()) return null

        val body = JSONObject()
            .put("prompt", prompt.ifBlank { "beautiful abstract wallpaper" })
            .put("style", style.suffix)
            .put("styleLabel", style.label)
            .put("aspect", if (aspect == AspectRatio.SQUARE) "square" else "phone")
            .put("count", count)
            .put("page", page)
            .toString()

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/generate")
            .addHeader("x-premium-token", premiumToken)
            .post(body.toRequestBody(JSON))
            .build()

        return runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val payload = resp.body?.string().orEmpty()
                val items = JSONObject(payload).optJSONArray("items") ?: return null
                (0 until items.length()).map { i ->
                    val it = items.getJSONObject(i)
                    val url = it.getString("url")
                    Wallpaper(
                        id = "nb_" + it.optString("id", url.hashCode().toString()),
                        title = prompt.ifBlank { style.label },
                        author = "Kroma AI · Nano Banana",
                        category = style.label,
                        thumbUrl = it.optString("thumbUrl", url),
                        fullUrl = url,
                        resolution = it.optString(
                            "resolution",
                            "${aspect.hdWidth}x${aspect.hdHeight}"
                        )
                    )
                }
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}
