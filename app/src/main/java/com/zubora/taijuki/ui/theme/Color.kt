package com.zubora.taijuki.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Warm notebook/journal palette — matches project/plan.txt's "手帳・ノート風".
 * Kept as literal tokens (not Material tonal roles) because the design was
 * authored as fixed hex values, not a generated Material color scheme.
 */
object AppColors {
    val PageBackground = Color(0xFFEAE3D3)
    val Surface = Color(0xFFF6EFDC)
    val Card = Color(0xFFFFFCF5)
    val CardAlt = Color(0xFFFDF5E6)
    val InputBackground = Color(0xFFFDF9EF)
    val NavBar = Color(0xFFFBF6E9)

    val TextPrimary = Color(0xFF4A3722)
    val TextSecondary = Color(0xFF8A7355)
    val TextPlaceholder = Color(0xFFB9A988)
    val NavInactive = Color(0xFFA89877)

    val BorderDashed = Color(0xFFD8C49A)
    val BorderCard = Color(0xFFE3D5B8)
    val BorderLight = Color(0xFFE9DDC4)

    val Negative = Color(0xFFB1503C)
    val Positive = Color(0xFF6B8F5E)
    val Sunday = Color(0xFFC0574A)
    val Saturday = Color(0xFF5F7CB5)

    val ModalScrim = Color(0x594A3722) // #4a3722 at 35% alpha

    /** Selectable accent colors — surfaced as a picker in Settings > 見た目. */
    val AccentOptions = listOf(
        Color(0xFFC9762E), // orange (default)
        Color(0xFFB1503C), // red / terracotta
        Color(0xFF6B8F5E), // green
        Color(0xFF4A6FA5), // blue
    )
    val DefaultAccent = AccentOptions[0]
}

enum class StampType(val id: String, val label: String, val color: Color) {
    Tabesugi("tabesugi", "食べすぎ", Color(0xFFC96F2E)),
    Osake("osake", "お酒", Color(0xFF8B5E34)),
    Undou("undou", "運動", Color(0xFF6B8F5E)),
    Benzuu("benzuu", "便通", Color(0xFFA68A5B)),
    Gohoubi("gohoubi", "ご褒美", Color(0xFFB1503C)),
    Ganbatta("ganbatta", "がんばった", Color(0xFFCAA23C));

    companion object {
        fun fromId(id: String): StampType? = entries.firstOrNull { it.id == id }

        /** The three stamps considered most relevant to weight correlation (graph "主要3種" mode). */
        val KeyStamps = setOf(Undou, Osake, Tabesugi)
    }
}
