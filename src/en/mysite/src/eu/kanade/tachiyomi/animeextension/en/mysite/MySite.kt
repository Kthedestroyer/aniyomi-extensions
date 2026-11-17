package eu.kanade.tachiyomi.animeextension.en.mysite

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Template source. Replace the URLs and CSS selectors with ones that match your website.
 * Open the site in a desktop browser, right-click an element > Inspect to find selectors.
 */
class MySite : ParsedAnimeHttpSource() {

    override val name = "My Site"

    override val baseUrl = "https://example.com" // TODO: your website

    override val lang = "en"

    override val supportsLatest = true

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request =
        GET("$baseUrl/popular?page=$page", headers) // TODO

    override fun popularAnimeSelector() = "div.anime-card" // TODO: one element per anime

    override fun popularAnimeFromElement(element: Element) = SAnime.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("href"))
        title = element.selectFirst(".title")?.text() ?: link.text()
        thumbnail_url = element.selectFirst("img")?.absUrl("src")
    }

    override fun popularAnimeNextPageSelector() = "a.next-page" // TODO (or null if no paging)

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/latest?page=$page", headers) // TODO

    override fun latestUpdatesSelector() = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element) = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/search?q=$query&page=$page", headers) // TODO

    override fun searchAnimeSelector() = popularAnimeSelector()

    override fun searchAnimeFromElement(element: Element) = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector() = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document) = SAnime.create().apply {
        title = document.selectFirst("h1")?.text().orEmpty() // TODO
        description = document.selectFirst(".synopsis")?.text() // TODO
        genre = document.select(".genres a").joinToString { it.text() } // TODO
        thumbnail_url = document.selectFirst(".poster img")?.absUrl("src") // TODO
        status = SAnime.UNKNOWN
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector() = "ul.episodes li a" // TODO: one element per episode

    override fun episodeFromElement(element: Element) = SEpisode.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        name = element.text()
        episode_number = name.substringAfterLast(" ").toFloatOrNull() ?: 0F
    }

    // Aniyomi expects newest episode first
    override fun episodeListParse(response: Response): List<SEpisode> =
        super.episodeListParse(response).reversed()

    // ============================ Video Links =============================
    override fun videoListSelector() = "video source" // TODO

    override fun videoFromElement(element: Element): Video {
        val url = element.absUrl("src")
        val quality = element.attr("size").ifEmpty { "Default" }
        return Video(url, quality, url, headers = headers)
    }

    override fun videoUrlParse(document: Document) = throw UnsupportedOperationException()
}
