package com.utilitylab.focussense.domain

enum class SessionContext(val key: String, val label: String) {
    WORK("WORK", "Lavoro"),
    STUDY("STUDY", "Studio"),
    READING("READING", "Lettura"),
    CREATIVE("CREATIVE", "Creatività"),
    GAMING("GAMING", "Gaming"),
    OTHER("OTHER", "Altro");

    companion object {
        fun fromKey(value: String?): SessionContext =
            entries.firstOrNull { it.key.equals(value, ignoreCase = true) } ?: WORK
    }
}

object SessionTagCodec {
    private const val SEPARATOR = "|"
    private const val MAX_TAGS = 5
    private const val MAX_LENGTH = 24

    fun normalize(tags: Collection<String>): List<String> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<String>()

        tags.forEach { raw ->
            if (result.size >= MAX_TAGS) return@forEach
            val clean = raw
                .replace(SEPARATOR, " ")
                .trim()
                .replace(Regex("\\s+"), " ")
                .take(MAX_LENGTH)

            if (clean.isBlank()) return@forEach
            if (seen.add(clean.lowercase())) result += clean
        }
        return result
    }

    fun encode(tags: Collection<String>): String =
        normalize(tags).joinToString(SEPARATOR)

    fun decode(csv: String?): List<String> =
        if (csv.isNullOrBlank()) emptyList()
        else normalize(csv.split(SEPARATOR))
}
