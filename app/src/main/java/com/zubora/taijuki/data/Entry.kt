package com.zubora.taijuki.data

import com.zubora.taijuki.ui.theme.StampType
import java.time.LocalDate

/** Domain-level daily entry — decoupled from the Room row shape. */
data class Entry(
    val date: LocalDate,
    val weight: Double,
    val stamps: List<StampType>,
    val memo: String,
)

fun EntryEntity.toDomain(): Entry = Entry(
    date = LocalDate.parse(date),
    weight = weight,
    stamps = stamps.split(",").filter { it.isNotBlank() }.mapNotNull(StampType::fromId),
    memo = memo,
)

fun Entry.toEntity(): EntryEntity = EntryEntity(
    date = date.toString(),
    weight = weight,
    stamps = stamps.joinToString(",") { it.id },
    memo = memo,
)
