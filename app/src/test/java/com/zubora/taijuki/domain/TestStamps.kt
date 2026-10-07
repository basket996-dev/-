package com.zubora.taijuki.domain

import com.zubora.taijuki.data.StampSeed
import com.zubora.taijuki.data.toDomain

/** The stamps a fresh install starts with, for building test entries. */
object TestStamps {
    val all = StampSeed.defaults.map { it.toDomain() }
    val tabesugi = all.first { it.id == "tabesugi" }
    val benzuu = all.first { it.id == "benzuu" }
    val gaishoku = all.first { it.id == "gaishoku" }
}
