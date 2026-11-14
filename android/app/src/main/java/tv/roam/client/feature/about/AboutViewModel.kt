package tv.roam.client.feature.about

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.dto.TranslatorsCredit
import tv.roam.client.dto.TranslatorsParser
import tv.roam.client.mvvm.BaseViewModel
import tv.roam.client.util.Utils

internal data class AboutUiState(
    val licenseLines: List<String> = emptyList(),
    val translatorLines: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

internal fun htmlToPlainLines(html: String): List<String> {
    val body = html
        .replace(Regex("(?is)<style.*?</style>"), "")
        .replace(Regex("(?is)<head.*?</head>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)<li[^>]*>"), "• ")
        .replace(Regex("(?i)</(p|div|h[1-6]|pre|li|tr|table|ul|ol)>"), "\n")
        .replace(Regex("(?s)<[^>]+>"), "")
    return body.lineSequence()
        .map { decodeHtmlEntities(it).trimEnd() }
        .filter { it.isNotBlank() }
        .toList()
}

private val numericHtmlEntity = Regex("&#(x?[0-9a-fA-F]+);")

internal fun decodeHtmlEntities(value: String): String {
    val numeric = numericHtmlEntity.replace(value) { match ->
        val raw = match.groupValues[1]
        val code = if (raw.startsWith("x", ignoreCase = true)) {
            raw.drop(1).toIntOrNull(16)
        } else {
            raw.toIntOrNull()
        }
        if (code != null && Character.isValidCodePoint(code)) {
            String(Character.toChars(code))
        } else {
            match.value
        }
    }
    val entities = listOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&nbsp;" to " ",
        "&copy;" to "©",
        "&reg;" to "®",
        "&trade;" to "™",
        "&mdash;" to "—",
        "&ndash;" to "–",
        "&hellip;" to "…",
    )
    return entities.fold(numeric) { text, (entity, replacement) ->
        text.replace(entity, replacement)
    }
}

internal fun translatorLines(credits: List<TranslatorsCredit>): List<String> =
    credits.flatMap { credit ->
        listOf(credit.language) + credit.contributors.map { contributor ->
            "  " + (contributor.displayName ?: contributor.name)
        }
    }

class AboutViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(AboutUiState())
    internal val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val licenses = Utils.readTextFromAssets(app, "open_source_licenses.html")
            val translators = Utils.readTextFromAssets(app, "translators.json")
            _uiState.value = AboutUiState(
                licenseLines = htmlToPlainLines(licenses),
                translatorLines = translatorLines(TranslatorsParser.parse(translators)),
                isLoading = false,
            )
        }
    }
}
