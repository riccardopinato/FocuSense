package com.utilitylab.focussense.domain

enum class ProgressBadge(
    val title: String,
    val description: String
) {
    FIRST_SESSION(
        "Prima sessione",
        "Completa una sessione valida di almeno 10 minuti."
    ),
    FIRST_CORE(
        "Primo Core",
        "Porta a termine una sessione e crea il tuo primo Focus Core."
    ),
    PRISM_CORE(
        "Prism",
        "Completa una sessione con Focus Integrity almeno 97%."
    ),
    DEEP_FOCUS(
        "Deep Focus",
        "Accumula almeno 30 minuti a schermo spento durante una sessione."
    ),
    THREE_DAY_STREAK(
        "Tre giorni di costanza",
        "Mantieni una serie di 3 giorni attivi."
    ),
    SEVEN_DAY_STREAK(
        "Settimana costante",
        "Mantieni una serie di 7 giorni attivi."
    ),
    TEN_SESSIONS(
        "10 sessioni",
        "Completa 10 sessioni valide."
    ),
    TEN_CORES(
        "10 Focus Core",
        "Costruisci 10 Core completando sessioni di concentrazione."
    ),
    FIFTY_SESSIONS(
        "50 sessioni",
        "Completa 50 sessioni valide."
    ),
    FIVE_HUNDRED_MINUTES(
        "500 minuti",
        "Accumula almeno 500 minuti validi."
    ),
    CALIBRATED(
        "Profilo calibrato",
        "Raggiungi una calibrazione personale utilizzabile."
    ),
    CONTEXT_EXPLORER(
        "Contesti esplorati",
        "Completa almeno 2 sessioni in 4 contesti diversi."
    ),
    CONSISTENT_WEEK(
        "Obiettivi settimanali",
        "Raggiungi entrambi gli obiettivi della settimana."
    )
}

data class WeeklyGoalSettings(
    val sessionGoal: Int = 5,
    val minuteGoal: Int = 150
)

data class WeeklyProgress(
    val validSessions: Int = 0,
    val validMinutes: Int = 0,
    val sessionGoal: Int = 5,
    val minuteGoal: Int = 150
) {
    val sessionProgress: Float
        get() = if (sessionGoal <= 0) 1f
        else (validSessions.toFloat() / sessionGoal).coerceIn(0f, 1f)

    val minuteProgress: Float
        get() = if (minuteGoal <= 0) 1f
        else (validMinutes.toFloat() / minuteGoal).coerceIn(0f, 1f)

    val complete: Boolean
        get() = validSessions >= sessionGoal && validMinutes >= minuteGoal
}

data class LevelProgress(
    val level: Int = 1,
    val totalXp: Int = 0,
    val currentLevelStartXp: Int = 0,
    val nextLevelXp: Int = 100
) {
    val progress: Float
        get() {
            val span = (nextLevelXp - currentLevelStartXp).coerceAtLeast(1)
            return ((totalXp - currentLevelStartXp).toFloat() / span).coerceIn(0f, 1f)
        }
}

data class GamificationProgress(
    val validSessions: Int = 0,
    val totalValidMinutes: Int = 0,
    val totalCores: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val level: LevelProgress = LevelProgress(),
    val weekly: WeeklyProgress = WeeklyProgress(),
    val unlockedBadges: Set<ProgressBadge> = emptySet()
)
