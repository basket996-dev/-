package com.zubora.taijuki.data

import java.time.LocalDate

/** Domain-level daily entry — decoupled from the Room row shape. */
data class Entry(
    val date: LocalDate,
    val weight: Double,
    val stamps: List<Stamp>,
    val memo: String,
    /** See [EntryEntity.recordedAt]. */
    val recordedAt: Long? = null,
)

fun EntryEntity.toDomain(stampsById: Map<String, Stamp>): Entry = Entry(
    date = LocalDate.parse(date),
    weight = weight,
    stamps = stamps.split(",").filter { it.isNotBlank() }.mapNotNull(stampsById::get),
    memo = memo,
    recordedAt = recordedAt,
)

fun Entry.toEntity(): EntryEntity = EntryEntity(
    date = date.toString(),
    weight = weight,
    stamps = stamps.joinToString(",") { it.id },
    memo = memo,
    recordedAt = recordedAt,
)
