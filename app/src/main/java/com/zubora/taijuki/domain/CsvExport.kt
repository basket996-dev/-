package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.ui.theme.StampType
import java.time.LocalDate

fun buildCsv(entries: Collection<Entry>): String {
    val header = "日付,体重(kg),スタンプ,メモ"
    val rows = entries.sortedBy { it.date }.map { entry ->
        listOf(
            entry.date.toString(),
            entry.weight.toFixed1(),
            entry.stamps.joinToString(" ") { it.label },
            entry.memo,
        ).joinToString(",") { csvField(it) }
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

/** Inverse of [buildCsv]. Skips the header and any row that doesn't parse as a valid entry. */
fun parseCsv(content: String): List<Entry> =
    content.lines().drop(1).mapNotNull { line ->
        if (line.isBlank()) return@mapNotNull null
        val fields = splitCsvLine(line)
        if (fields.size < 4) return@mapNotNull null
        val date = runCatching { LocalDate.parse(fields[0]) }.getOrNull() ?: return@mapNotNull null
        val weight = fields[1].toDoubleOrNull() ?: return@mapNotNull null
        val stamps = fields[2].split(" ").filter { it.isNotBlank() }
            .mapNotNull { label -> StampType.entries.firstOrNull { it.label == label } }
        Entry(date, weight, stamps, fields[3])
    }

private fun splitCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString())
    return fields
}
