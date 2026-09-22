package com.joshfactory.qurantafseel.data

import android.content.Context
import org.json.JSONObject

/**
 * One physical PDF file (a whole surah, or one "part" of a longer surah)
 * placed at its absolute position inside the combined flipbook.
 */
data class PdfFileEntry(
    val surahNum: Int,
    val surahName: String,
    val partNum: Int,
    val file: String,
    val pages: Int,
    val startPageIndex: Int
)

/** A surah's entry point into the flipbook (its first page's absolute index). */
data class SurahEntry(
    val surahNum: Int,
    val surahName: String,
    val startPageIndex: Int
)

/**
 * Loads assets/manifest.json once and exposes the full reading order
 * (Surah 1 -> Surah 114, parts in order within a surah) as a single flat
 * sequence of "absolute" page indices, the way a real book is paginated.
 */
object PdfManifest {

    lateinit var files: List<PdfFileEntry>
        private set
    lateinit var surahs: List<SurahEntry>
        private set
    var totalPages: Int = 0
        private set

    private var loaded = false

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        val json = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        totalPages = obj.getInt("total_pages")

        val arr = obj.getJSONArray("files")
        val list = ArrayList<PdfFileEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                PdfFileEntry(
                    surahNum = o.getInt("surah_num"),
                    surahName = o.getString("surah_name"),
                    partNum = o.getInt("part_num"),
                    file = o.getString("file"),
                    pages = o.getInt("pages"),
                    startPageIndex = o.getInt("start_page_index")
                )
            )
        }
        files = list

        surahs = list.groupBy { it.surahNum }
            .map { (num, parts) ->
                val first = parts.minByOrNull { it.partNum }!!
                SurahEntry(num, first.surahName, first.startPageIndex)
            }
            .sortedBy { it.surahNum }

        loaded = true
    }

    /**
     * Maps a flipbook-wide absolute page index to the physical PDF file that
     * contains it, plus the 0-based page number *within that file*.
     */
    fun locate(absolutePage: Int): Pair<PdfFileEntry, Int>? {
        if (absolutePage < 0) return null
        var lo = 0
        var hi = files.size - 1
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (files[mid].startPageIndex <= absolutePage) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (ans == -1) return null
        val entry = files[ans]
        val local = absolutePage - entry.startPageIndex
        if (local < 0 || local >= entry.pages) return null
        return entry to local
    }

    fun surahForPage(absolutePage: Int): SurahEntry? {
        val loc = locate(absolutePage) ?: return null
        return surahs.find { it.surahNum == loc.first.surahNum }
    }
}
