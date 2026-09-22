package com.misaka9981.alarm.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.misaka9981.alarm.core.AppLanguage
import com.misaka9981.alarm.core.LanguageRepository
import java.util.Locale
import kotlinx.coroutines.launch

/** The owner's chosen in-app language; [AppLanguage.System] until it loads. */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.System }

/** Changes the in-app language, persisting it and applying it immediately. */
val LocalSetAppLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

/**
 * Wrap [content] in a Context localised to the owner's chosen language.
 *
 * Every string resource resolved underneath — screen titles, dialogs, and
 * anything else that goes through `stringResource` — uses the localised
 * [android.content.res.Resources], so changing the language recomposes the whole
 * app in the new language with no restart. [AppLanguage.System] applies the
 * phone's own locale, whose default resources are Chinese.
 *
 * The choice is read once from the [LanguageRepository] and kept in memory; a
 * selection from Settings updates both the composition and the stored value. The
 * notification, which is built outside the composition, gets the same treatment
 * through [localizedFor].
 */
@Composable
fun AppLanguageProvider(
    languageRepository: LanguageRepository,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf(AppLanguage.System) }

    LaunchedEffect(languageRepository) {
        language = languageRepository.load()
    }

    val localizedContext = remember(baseContext, language) { baseContext.localizedFor(language) }
    val setLanguage: (AppLanguage) -> Unit = { chosen ->
        language = chosen
        scope.launch { languageRepository.save(chosen) }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalAppLanguage provides language,
        LocalSetAppLanguage provides setLanguage,
        content = content,
    )
}

/**
 * A Context whose resources resolve in [language].
 *
 * [AppLanguage.System] keeps the device's own locale; a pinned choice applies its
 * tag. This is the adapter half of the decision `core` records in
 * [AppLanguage.explicitTag], and it is what lets the Alarm notification be built
 * in the chosen language even when the app is not on screen.
 */
internal fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration)
    val tag = language.explicitTag ?: Locale.getDefault().toLanguageTag()
    configuration.setLocale(Locale.forLanguageTag(tag))
    return createConfigurationContext(configuration)
}
