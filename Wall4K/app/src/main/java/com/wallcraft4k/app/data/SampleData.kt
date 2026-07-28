package com.wallcraft4k.app.data

import com.wallcraft4k.app.data.model.Wallpaper

/**
 * Built-in 4K catalog. Images are served by picsum.photos (no API key required),
 * so the app works out of the box. Swap [WallpaperRepository]'s catalog source
 * for a real backend (Firebase / Supabase / REST) when you have one.
 */
object SampleData {

    // picsum serves any size; we request a portrait phone ratio (9:16).
    private const val THUMB_W = 360
    private const val THUMB_H = 640
    private const val FULL_W = 2160   // 4K-class portrait
    private const val FULL_H = 3840

    private fun thumb(picId: Int) = "https://picsum.photos/id/$picId/$THUMB_W/$THUMB_H"
    private fun full(picId: Int) = "https://picsum.photos/id/$picId/$FULL_W/$FULL_H"

    private data class Seed(val picId: Int, val title: String, val author: String, val category: String)

    private val seeds = listOf(
        Seed(1018, "Cumbre nublada", "M. Rojas", "Naturaleza"),
        Seed(1015, "Río esmeralda", "L. Fernández", "Naturaleza"),
        Seed(1039, "Cascada oculta", "D. Prieto", "Naturaleza"),
        Seed(1043, "Bosque de niebla", "A. Vidal", "Naturaleza"),
        Seed(1016, "Cañón dorado", "S. Herrera", "Naturaleza"),

        Seed(1011, "Costa al atardecer", "C. Molina", "Ciudad"),
        Seed(1005, "Skyline nocturno", "R. Ortega", "Ciudad"),
        Seed(1067, "Retrato urbano", "P. Cano", "Ciudad"),
        Seed(1080, "Fresas de verano", "N. Ibáñez", "Ciudad"),

        Seed(1084, "Nebulosa", "J. Duarte", "Espacio"),
        Seed(1069, "Luces del muelle", "T. Salas", "Espacio"),
        Seed(903, "Aurora violeta", "K. Marín", "Espacio"),

        Seed(1025, "Mirada canina", "V. Peña", "Animales"),
        Seed(1074, "Camello del desierto", "H. Bravo", "Animales"),
        Seed(219, "Gato curioso", "E. Lozano", "Animales"),

        Seed(1062, "Textura mineral", "O. Campos", "Abstracto"),
        Seed(1050, "Arquitectura curva", "G. Ríos", "Abstracto"),
        Seed(1073, "Dunas infinitas", "B. Castro", "Abstracto"),

        Seed(1071, "Interior minimal", "F. Aguirre", "Minimal"),
        Seed(1082, "Nieve serena", "I. Navarro", "Minimal"),
        Seed(1059, "Carretera abierta", "Q. Reyes", "Minimal"),

        Seed(1070, "Faro solitario", "W. Vega", "Naturaleza"),
        Seed(1036, "Palmeras al sol", "Y. Guerra", "Naturaleza"),
        Seed(1057, "Puente de acero", "Z. Miranda", "Ciudad"),
    )

    val wallpapers: List<Wallpaper> = seeds.map { s ->
        Wallpaper(
            id = "cat_${s.picId}",
            title = s.title,
            author = s.author,
            category = s.category,
            thumbUrl = thumb(s.picId),
            fullUrl = full(s.picId)
        )
    }

    val categories: List<String> = wallpapers.map { it.category }.distinct().sorted()
}
