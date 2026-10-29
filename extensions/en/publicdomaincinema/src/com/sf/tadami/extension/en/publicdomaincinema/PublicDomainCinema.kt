package com.sf.tadami.extension.en.publicdomaincinema

import com.sf.tadami.network.GET
import com.sf.tadami.source.AnimesPage
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.utils.Lang
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * A Tadami source for Internet Archive video items whose metadata explicitly identifies a
 * Creative Commons Public Domain Mark or CC0 license.
 *
 * This deliberately does not scrape the third-party streaming directory. Each third-party site
 * needs its own compatible adapter and stream handling, and its distribution rights must be checked.
 */
class PublicDomainCinema : AnimeHttpSource() {

    override val id: Long = BuildConfig.SOURCE_ID
    override val name: String = "Public Domain Cinema"
    override val baseUrl: String = DEFAULT_BASE_URL
    override val lang: Lang = Lang.ENGLISH
    override val supportRecent: Boolean = true

    private val rowsPerPage = 24
    private val searchBase = "mediatype:movies AND licenseurl:http*publicdomain*"

    override fun latestAnimesRequest(page: Int): Request =
        searchRequest(searchBase, page, sortRecent = true)

    override fun latestAnimeParse(response: Response): AnimesPage =
        parseSearchResults(response)

    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean,
    ): Request {
        val terms = query
            .replace(Regex("[^\\p{L}\\p{N}\\s-]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        val q = if (terms.isBlank()) searchBase else "$searchBase AND title:($terms)"
        return searchRequest(q, page, sortRecent = false)
    }

    override fun searchAnimeParse(response: Response): AnimesPage =
        parseSearchResults(response)

    private fun searchRequest(query: String, page: Int, sortRecent: Boolean): Request {
        val url = "$baseUrl/advancedsearch.php".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("output", "json")
            .addQueryParameter("rows", rowsPerPage.toString())
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("fl[]", "identifier")
            .addQueryParameter("fl[]", "title")
            .addQueryParameter("fl[]", "description")
            .addQueryParameter("fl[]", "year")
            .addQueryParameter("fl[]", "licenseurl")
        if (sortRecent) url.addQueryParameter("sort[]", "publicdate desc")
        return GET(url.build().toString(), headers)
    }

    private fun parseSearchResults(response: Response): AnimesPage {
        val root = JSONObject(response.body?.string() ?: throw IOException("Empty search response"))
        val responseObject = root.optJSONObject("response")
            ?: throw IOException("Unexpected Internet Archive search response")
        val docs = responseObject.optJSONArray("docs") ?: JSONArray()
        val results = mutableListOf<SAnime>()

        for (index in 0 until docs.length()) {
            val doc = docs.optJSONObject(index) ?: continue
            if (!hasPublicDomainLicense(doc.optString("licenseurl"))) continue
            val identifier = doc.optString("identifier").trim()
            if (!IDENTIFIER_REGEX.matches(identifier)) continue

            results += SAnime.create().apply {
                title = doc.optString("title").takeUnless { it.isBlank() } ?: identifier
                thumbnailUrl = "$baseUrl/services/img/$identifier"
                setUrlWithoutDomain("$baseUrl/metadata/$identifier")
                description = doc.stringValue("description")
                release = doc.optString("year").takeUnless { it.isBlank() || it == "null" }
                status = SAnimeStatus.COMPLETED
                genres = listOf("Public domain")
            }
        }

        val requestedPage = response.request.url.queryParameter("page")?.toIntOrNull() ?: 1
        val total = responseObject.optInt("numFound", 0)
        return AnimesPage(results, hasNextPage = requestedPage * rowsPerPage < total)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val root = JSONObject(response.body?.string() ?: throw IOException("Empty item metadata"))
        val metadata = root.optJSONObject("metadata")
            ?: throw IOException("Item metadata is missing")
        val identifier = metadata.optString("identifier")
        if (!hasPublicDomainLicense(metadata.optString("licenseurl"))) {
            throw IOException("This item does not declare a supported public-domain license")
        }

        return SAnime.create().apply {
            title = metadata.optString("title").takeUnless { it.isBlank() } ?: identifier
            thumbnailUrl = "$baseUrl/services/img/$identifier"
            description = metadata.stringValue("description")
            release = metadata.optString("year").takeUnless { it.isBlank() || it == "null" }
            status = SAnimeStatus.COMPLETED
            genres = listOf("Public domain")
        }
    }

    override fun episodesListParse(response: Response): List<SEpisode> {
        val root = JSONObject(response.body?.string() ?: throw IOException("Empty item metadata"))
        val metadata = root.optJSONObject("metadata")
            ?: throw IOException("Item metadata is missing")
        val identifier = metadata.optString("identifier")
        if (!IDENTIFIER_REGEX.matches(identifier) ||
            !hasPublicDomainLicense(metadata.optString("licenseurl"))
        ) {
            return emptyList()
        }

        val files = root.optJSONArray("files") ?: JSONArray()
        val videoFiles = mutableListOf<JSONObject>()
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val fileName = file.optString("name")
            if (isSupportedVideoFile(fileName)) videoFiles += file
        }

        // Prefer common, built-in-player-friendly formats. Keep a modest list per item.
        val orderedFiles = videoFiles
            .distinctBy { it.optString("name") }
            .sortedWith(
                compareBy<JSONObject> { formatRank(it.optString("name")) }
                    .thenBy { it.optLong("size", Long.MAX_VALUE) }
            )
            .take(MAX_FILES_PER_ITEM)

        return orderedFiles.mapIndexed { index, file ->
            val fileName = file.optString("name")
            val episodeUrl = "$baseUrl/metadata/$identifier".toHttpUrl().newBuilder()
                .addQueryParameter("stream_file", fileName)
                .build()
                .toString()

            SEpisode.create().apply {
                setUrlWithoutDomain(episodeUrl)
                name = fileName
                episodeNumber = (index + 1).toFloat()
            }
        }
    }

    override fun episodeSourcesParse(response: Response): List<StreamSource> {
        val root = JSONObject(response.body?.string() ?: throw IOException("Empty item metadata"))
        val metadata = root.optJSONObject("metadata") ?: return emptyList()
        val identifier = metadata.optString("identifier")
        val targetFile = response.request.url.queryParameter("stream_file") ?: return emptyList()

        if (!IDENTIFIER_REGEX.matches(identifier) ||
            !hasPublicDomainLicense(metadata.optString("licenseurl")) ||
            !isSupportedVideoFile(targetFile)
        ) {
            return emptyList()
        }

        // Confirm that the requested file is actually part of the licensed item.
        val files = root.optJSONArray("files") ?: return emptyList()
        val exists = (0 until files.length()).any { i ->
            files.optJSONObject(i)?.optString("name") == targetFile
        }
        if (!exists) return emptyList()

        val directUrl = "$baseUrl/download/$identifier".toHttpUrl().newBuilder()
            .addPathSegment(targetFile)
            .build()
            .toString()

        val quality = QUALITY_REGEX.find(targetFile)?.value ?: targetFile.substringAfterLast('.', "Video")
        return listOf(
            StreamSource(
                url = directUrl,
                fullName = "Internet Archive · $quality",
                quality = quality,
                server = "Internet Archive",
                headers = headers,
            )
        )
    }

    private fun hasPublicDomainLicense(value: String): Boolean {
        val license = value.lowercase()
        return license.contains("creativecommons.org/publicdomain/mark/1.0") ||
            license.contains("creativecommons.org/publicdomain/zero/1.0")
    }

    private fun isSupportedVideoFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".m4v") || lower.endsWith(".webm")
    }

    private fun formatRank(name: String): Int = when {
        name.lowercase().endsWith(".mp4") -> 0
        name.lowercase().endsWith(".m4v") -> 1
        else -> 2
    }

    private fun JSONObject.stringValue(key: String): String {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> ""
            is JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }.joinToString(", ")
            else -> value.toString()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://archive.org"
        private const val MAX_FILES_PER_ITEM = 8
        private val IDENTIFIER_REGEX = Regex("[A-Za-z0-9_-]{1,100}")
        private val QUALITY_REGEX = Regex("(?i)\\b(?:2160p|1440p|1080p|720p|480p|360p)\\b")
    }
}
