package team.newbies.core.model

/**
 * Platform-agnostic domain model representing a news or release post from the NewbiesTeam hub.
 * Designed for Kotlin Multiplatform (KMP) shared module compatibility.
 */
data class UpdatePost(
    val id: String,
    val title: String,
    val author: String,
    val tag: String,
    val body: String = "",
    val date: String = "",
    val targetUrl: String = ""
) {
    val notificationTitle: String
        get() = title.ifBlank { "NewbiesTeam Update" }

    val notificationBody: String
        get() = "$tag by $author • Tap to view"
}
