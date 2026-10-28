package com.lixfnet.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.repository.WatchProgressRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DuplicateTest {

    @Test
    fun testDeduplicationLogic() {
        val prefsFile = File("C:/Users/trong/.gemini/antigravity-ide/brain/32650b6c-e687-42d8-964a-5b3ea2b163c1/scratch/prefs_device.xml")
        val content = prefsFile.readText(Charsets.UTF_16LE)
        val regex = Regex("<string name=\"watch_progress_list\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(content)
        if (match != null) {
            val json = match.groupValues[1]
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
            val gson = Gson()
            val type = object : TypeToken<List<Movie>>() {}.type
            val list: List<Movie> = gson.fromJson(json, type)
            println("Initial raw list size from prefs: ${list.size}")
            assertEquals(15, list.size)

            val deduplicated = WatchProgressRepository.deduplicateMovies(list)
            println("Deduplicated list size: ${deduplicated.size}")
            assertEquals(8, deduplicated.size)

            deduplicated.forEachIndexed { i, m ->
                println("[$i] id='${m.id}' slug='${m.slug}' title='${m.title}' durationText='${m.durationText}'")
            }

            // Verify no duplicate titles exist
            val titles = deduplicated.map { it.title }
            assertEquals(titles.toSet().size, deduplicated.size)
            println("Verified: Zero duplicates remaining!")
        }
    }
}
