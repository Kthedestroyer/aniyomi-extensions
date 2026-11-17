package eu.kanade.tachiyomi.animeextension.en.hentaigasm

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document

/**
 * Each post on the site is a single episode, so every post is listed as its own
 * entry with one episode.
 */
class Hentaigasm : AnimeHttpSource() {

    override val name = "Hentaigasm"

    override val baseUrl = "https://hentaigasm.com"

    override val lang = "en"

    override val supportsLatest = false

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    // ============================== Listing ===============================
    private fun pageUrl(page: Int) = if (page <= 1) "$baseUrl/" else "$baseUrl/page/$page/"

    override fun popularAnimeRequest(page: Int): Request = GET(pageUrl(page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response.asJsoup())

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = pageUrl(page).toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .build()
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response.asJsoup())

    private fun parseList(document: Document): AnimesPage {
        val animes = document.select("a.clip-link").map { link ->
            SAnime.create().apply {
                setUrlWithoutDomain(link.attr("href"))
                title = link.attr("title").ifEmpty { link.text() }
                // Small static image instead of the heavy animated .webp preview
                thumbnail_url = thumbnailUrl(title)
            }
        }.distinctBy { it.url }

        val hasNext = document.selectFirst(
            "a.nextpostslink, a.next, a[rel=next], .pagination a.next, .nav-previous a",
        ) != null || animes.isNotEmpty()

        return AnimesPage(animes, hasNext)
    }

    // =========================== Anime Details ============================
    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()

        return SAnime.create().apply {
            // Blank title / null thumbnail = keep the ones from the list.
            // The episode page only has the site's generic title and image.
            title = ""
            thumbnail_url = null
            description = document.selectFirst("meta[property=og:description], meta[name=description]")
                ?.attr("content")
                ?.takeUnless { it.contains("hentaigasm", ignoreCase = true) }
            genre = document.select("a[rel=tag], a[rel='category tag']")
                .map { it.text() }
                .distinct()
                .joinToString()
                .ifEmpty { null }
            status = SAnime.COMPLETED
            initialized = true
        }
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val title = titleFromSlug(response.request.url.pathSegments) ?: "Episode"
        return listOf(
            SEpisode.create().apply {
                setUrlWithoutDomain(response.request.url.toString())
                name = title
                episode_number = EPISODE_NUMBER_REGEX.find(title)?.groupValues?.get(1)?.toFloatOrNull() ?: 1F
            },
        )
    }

    // ============================ Video Links =============================
    override fun videoListParse(response: Response): List<Video> {
        val html = response.body.string()
        val videoHeaders = headersBuilder().build()

        val quoted = QUOTED_VIDEO_URL_REGEX.findAll(html).map { it.groupValues[1] }
        val bare = VIDEO_URL_REGEX.findAll(html).map { it.value }
        val urls = (quoted + bare)
            .map { it.replace("\\/", "/").fixUrl() }
            .distinct()
            .toList()
            .ifEmpty {
                // Fallback: the site stores videos as <title>.mp4 on hgasm2.com
                val title = titleFromSlug(response.request.url.pathSegments)
                listOfNotNull(title?.let { "https://hgasm2.com/$it.mp4".fixUrl() })
            }

        return urls.map { url ->
            val quality = when {
                url.contains(".m3u8") -> "HLS"
                else -> "MP4 - ${url.toHttpUrl().host}"
            }
            Video(url, quality, url, headers = videoHeaders)
        }
    }

    // ============================== Helpers ===============================
    private fun String.fixUrl(): String = trim().replace(" ", "%20")

    private fun thumbnailUrl(title: String): String = baseUrl.toHttpUrl().newBuilder()
        .addPathSegment("thumbnail")
        .addPathSegment("$title.jpg")
        .build()
        .toString()

    // "hitori-tsuma-1-subbed" -> "Hitori Tsuma 1 Subbed"
    private fun titleFromSlug(segments: List<String>): String? =
        segments.firstOrNull { it.isNotBlank() }
            ?.split("-")
            ?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    companion object {
        // Direct video links, e.g. https://hgasm2.com/Some%20Title%201%20Subbed.mp4
        private val VIDEO_URL_REGEX = Regex("""https?:\\?/\\?/[^"'\s<>]+?\.(?:mp4|m3u8)(?:\?[^"'\s<>]*)?""")
        private val QUOTED_VIDEO_URL_REGEX = Regex("""["'](https?:\\?/\\?/[^"'<>]+?\.(?:mp4|m3u8)(?:\?[^"'<>]*)?)["']""")
        private val EPISODE_NUMBER_REGEX = Regex("""\b(\d+(?:\.\d+)?)\b(?!.*\b\d+\b)""")
    }
}
