package com.nexient.jazzy.core.sources.engine

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectorTest {
    private val doc = Jsoup.parse(
        """
        <table><tr id="row">
          <td class="name"><a href="/icon">i</a><a href="/torrent/1/Some-Movie/">Some Movie 1080p</a></td>
          <td class="seeds">1,204</td>
          <td class="size">1.4 GB<span class="seeds">99</span></td>
          <td class="time"><div>1696334400</div>2 hours ago | Uploader</td>
          <td class="link"><a href="/t?url=magnet%3A%3Fxt%3Durn%3Abtih%3AABC">go</a></td>
        </tr></table>
        """.trimIndent(),
        "https://example.org/search/",
    ).apply { outputSettings().prettyPrint(false) }
    private val row = doc.getElementById("row")!!

    private fun eval(expression: String) = Selector.parse(expression).evaluate(row)

    @Test fun `text is the default attribute`() = assertEquals("Some Movie 1080p", eval("a:nth-child(2)"))

    @Test fun `href is resolved against the page url`() =
        assertEquals("https://example.org/torrent/1/Some-Movie/", eval("a:nth-child(2)@href"))

    @Test fun `html then until keeps the leading text`() = assertEquals("1.4 GB", eval(".size@html | until:<sp"))

    @Test fun `int drops thousands separators`() = assertEquals("1204", eval(".seeds | int"))

    @Test fun `split with special char picks the segment and trim cleans it`() =
        assertEquals("Uploader", eval(".time | split:%SPECIAL_CHAR%,1 | trim"))

    @Test fun `match returns the first group`() =
        assertEquals("1696334400", eval("""td.time@html | match:"<div>(\d+)</div>""""))

    @Test fun `match then decodeURIComponent`() =
        assertEquals("magnet:?xt=urn:btih:ABC", eval(""".link a@href | match:"url=(.+)" | decodeURIComponent"""))

    @Test fun `substr and format build a magnet`() =
        assertEquals("magnet:?xt=urn:btih:torrent/1/Some-Movie/", eval("a:nth-child(2)@href | substr:20, | format:magnet:?xt=urn:btih:{0}"))

    @Test fun `replace replaces the first occurrence only`() = assertEquals("x-b-a", Filters.apply(call("replace", "a", "x"), "a-b-a"))

    @Test fun `replace with quoted newline argument`() {
        val selector = Selector.parse("td | replace:\"\n\" | trim")
        assertEquals("replace", selector.filters.first().name)
        assertEquals(listOf("\n"), selector.filters.first().args)
    }

    @Test fun `missing element yields null`() = assertNull(eval(".nope@href"))

    @Test fun `missing attribute yields null`() = assertNull(eval("td.seeds@title"))

    @Test fun `scope element itself is not matched`() = assertNull(Selector.parse("tr").evaluate(row))

    @Test fun `parse splits selector attribute and filters`() {
        val s = Selector.parse("""td.tdnormal:nth-child(2)@text  | match:"(.+?) -"""")
        assertEquals("td.tdnormal:nth-child(2)", s.css)
        assertEquals("text", s.attribute)
        assertEquals(listOf(Selector.FilterCall("match", listOf("(.+?) -"))), s.filters)
    }

    @Test fun `js substr and slice semantics`() {
        assertEquals("cdef", Filters.apply(call("substr", "2"), "abcdef"))
        assertEquals("cd", Filters.apply(call("substr", "2", "2"), "abcdef"))
        assertEquals("ef", Filters.apply(call("slice", "-2"), "abcdef"))
        assertEquals("fedcba", Filters.apply(call("reverse"), "abcdef"))
    }

    @Test fun `int leaves non numbers untouched`() = assertEquals("N/A", Filters.apply(call("int"), "N/A"))

    private fun call(name: String, vararg args: String) = Selector.FilterCall(name, args.toList())
}
