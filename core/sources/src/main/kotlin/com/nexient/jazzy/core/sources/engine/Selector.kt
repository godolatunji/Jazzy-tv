package com.nexient.jazzy.core.sources.engine

import org.jsoup.nodes.Element
import java.net.URLDecoder

/**
 * Port of the x-ray selector mini-language used by torrent-search-api provider definitions:
 *
 * ```
 * css selector@attribute | filter:arg1,arg2 | filter2
 * ```
 *
 * - No `@attribute` means the element's text. `@html` is its inner HTML. `@href`/`@src` are
 *   resolved to absolute URLs, as x-ray does before selecting.
 * - An empty selector targets the scope element itself.
 * - Within a scope, only descendants are matched (cheerio `find` semantics).
 * - Filter arguments are separated by commas or spaces and may be quoted.
 */
internal class Selector private constructor(
    val css: String?,
    val attribute: String,
    val filters: List<FilterCall>,
) {
    data class FilterCall(val name: String, val args: List<String>)

    /** Evaluates against [scope]; returns null when nothing matched or the value is blank. */
    fun evaluate(scope: Element): String? {
        val target = if (css.isNullOrEmpty()) {
            scope
        } else {
            scope.select(css).firstOrNull { it !== scope } ?: return null
        }
        val raw = when (attribute) {
            "text" -> target.text()
            "html" -> target.html()
            "href", "src" -> target.absUrl(attribute).ifEmpty { target.attr(attribute) }
                .takeIf { target.hasAttr(attribute) }
            else -> target.attr(attribute).takeIf { target.hasAttr(attribute) }
        } ?: return null
        return filters.fold(raw) { value, call -> Filters.apply(call, value) }.takeIf { it.isNotBlank() }
    }

    companion object {
        private val selectorPattern = Regex("""^([^@]*)(?:@\s*([\w\-:]+))?$""")
        private val filterSeparator = Regex("""\s*\|(?!=)\s*""")
        private val argPattern = Regex(""""([^"]*)"|'([^']*)'|([^ \t,]+)""")

        fun parse(expression: String): Selector {
            val parts = expression.split(filterSeparator)
            val match = selectorPattern.find(parts.first())
            val filters = parts.drop(1).filter { it.isNotBlank() }.map(::parseFilter)
            return Selector(
                css = match?.groupValues?.get(1)?.trim(),
                attribute = match?.groupValues?.get(2)?.ifEmpty { null } ?: "text",
                filters = filters,
            )
        }

        private fun parseFilter(call: String): FilterCall {
            val name = call.substringBefore(':').trim()
            val argString = if (':' in call) call.substringAfter(':') else ""
            val args = argPattern.findAll(argString).map { m ->
                m.groups[2]?.value ?: m.groups[1]?.value ?: m.value
            }.toList()
            return FilterCall(name, args)
        }
    }
}

/** Port of `lib/utils/filters.js`. Unknown filters are ignored. */
internal object Filters {
    fun apply(call: Selector.FilterCall, value: String): String {
        val args = call.args
        return when (call.name) {
            "trim" -> value.trim()
            "reverse" -> value.reversed()
            "slice" -> jsSlice(value, args.getOrNull(0)?.toIntOrNull() ?: 0, args.getOrNull(1)?.toIntOrNull())
            "replace" -> value.replaceFirst(args.getOrElse(0) { "" }, args.getOrElse(1) { "" })
            "substr" -> jsSubstr(value, args.getOrNull(0)?.toIntOrNull() ?: 0, args.getOrNull(1)?.toIntOrNull())
            "int" -> parseInt(value)?.toString() ?: value
            "split" -> {
                val separator = args.getOrElse(0) { "" }.let { if (it == "%SPECIAL_CHAR%") "|" else it }
                val index = args.getOrNull(1)?.toIntOrNull() ?: 0
                value.split(separator).getOrNull(index) ?: value
            }
            "format" -> args.getOrElse(0) { "{0}" }.replace("{0}", value).replace("{}", value)
            "until" -> {
                val index = value.indexOf(args.getOrElse(0) { "" })
                if (index > 0) value.substring(0, index) else value
            }
            "match" -> runCatching { Regex(args.getOrElse(0) { "" }).find(value)?.groupValues?.getOrNull(1) }
                .getOrNull() ?: value
            "decodeURIComponent" -> runCatching {
                URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") // the Charset overload is API 33+
            }.getOrDefault(value)
            else -> value
        }
    }

    /**
     * JavaScript `parseInt`, except thousands separators are dropped first so `1,234` reads as
     * 1234 instead of 1.
     */
    fun parseInt(value: String): Int? =
        Regex("""^\s*([+-]?\d+)""").find(value.replace(Regex("""(?<=\d),(?=\d{3})"""), ""))
            ?.groupValues?.get(1)?.toIntOrNull()

    private fun jsSlice(value: String, start: Int, end: Int?): String {
        val len = value.length
        fun clamp(i: Int) = if (i < 0) maxOf(len + i, 0) else minOf(i, len)
        val from = clamp(start)
        val to = clamp(end ?: len)
        return if (from < to) value.substring(from, to) else ""
    }

    private fun jsSubstr(value: String, from: Int, length: Int?): String {
        val start = if (from < 0) maxOf(value.length + from, 0) else minOf(from, value.length)
        val end = if (length == null) value.length else minOf(start + maxOf(length, 0), value.length)
        return value.substring(start, end)
    }
}
