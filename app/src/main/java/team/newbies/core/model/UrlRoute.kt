package team.newbies.core.model

/**
 * Route abstraction for web and deep link resolution.
 * Allows decoupling UI rendering from URL classification logic in KMP.
 */
sealed interface UrlRoute {
    /** Internal page or section that should render within the embedded WebView */
    data class InternalWeb(val url: String) : UrlRoute

    /** External link (e.g. GitHub, Google Drive, SourceForge, Telegram) */
    data class ExternalBrowser(val url: String) : UrlRoute

    /** Direct binary download file (.zip, .apk, .img, .bin, etc.) */
    data class DirectDownload(val url: String, val filename: String = "") : UrlRoute

    /** Custom protocol intent (e.g. intent:, tg:, tel:, mailto:) */
    data class SpecialIntent(val uriString: String, val scheme: String) : UrlRoute
}
