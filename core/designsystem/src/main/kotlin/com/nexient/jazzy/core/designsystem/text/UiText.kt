package com.nexient.jazzy.core.designsystem.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * User-visible text produced outside composables (ViewModels, state holders), resolved to a
 * string in the UI so it follows the device locale (spec §9 "UI strings fully externalised").
 *
 * Format args may themselves be [UiText]; they are resolved first. Equality is structural so
 * equal messages compare equal in `StateFlow`s.
 */
@Immutable
sealed interface UiText {
    /** Text that is already user-facing data (a title, a server message) and must not be translated. */
    data class Raw(val value: String) : UiText

    class Res(@StringRes val id: Int, vararg args: Any) : UiText {
        val args: List<Any> = args.toList()
        override fun equals(other: Any?) = other is Res && other.id == id && other.args == args
        override fun hashCode() = 31 * id + args.hashCode()
        override fun toString() = "UiText.Res($id, $args)"
    }

    class Plural(@PluralsRes val id: Int, val count: Int, vararg args: Any) : UiText {
        val args: List<Any> = args.toList()
        override fun equals(other: Any?) = other is Plural && other.id == id && other.count == count && other.args == args
        override fun hashCode() = 31 * (31 * id + count) + args.hashCode()
        override fun toString() = "UiText.Plural($id, $count, $args)"
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> stringResource(id, *args.map { if (it is UiText) it.asString() else it }.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *args.map { if (it is UiText) it.asString() else it }.toTypedArray())
}

/** For non-composable call sites such as snackbars shown from a `LaunchedEffect`. */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> context.getString(id, *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray())
}
