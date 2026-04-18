package com.nuvio.tv.data.repository

import android.util.Log
import com.nuvio.tv.data.remote.api.RedditApi
import com.nuvio.tv.data.remote.dto.reddit.RedditThingDto
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.TraktCommentReview
import com.nuvio.tv.domain.model.Video
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.min

private const val REDDIT_SEARCH_LIMIT = 20
private const val REDDIT_FETCH_DEPTH = 8
private const val REDDIT_FETCH_LIMIT = 100
private const val REDDIT_PAGE_SIZE = 30
private const val REDDIT_CACHE_TTL_MS = 10 * 60_000L
private const val REDDIT_MAX_ROOT_COMMENTS = 200
private const val REDDIT_MAX_REPLIES_PER_THREAD = 60
private const val REDDIT_MAX_RENDERED_REPLY_DEPTH = 6
private const val REDDIT_MAX_THREAD_CHARS = 6000
private const val REDDIT_CACHE_SCHEMA_VERSION = "v6"
private const val TAG = "RedditCommentsService"
private val NOISE_TOKENS = setOf("the", "and", "for", "with", "from", "this", "that", "episode", "season")
private val GENERIC_DISCUSSION_SUBREDDITS = setOf(
    "television",
    "tv",
    "netflix",
    "hulu",
    "amazonprimevideo",
    "disneyplus",
    "hbomax",
    "max",
    "primevideo",
    "appletv",
    "entertainment"
)

data class RedditCommentsPage(
    val items: List<TraktCommentReview>,
    val currentPage: Int,
    val pageCount: Int,
    val itemCount: Int,
    val sourceTitle: String? = null,
    val sourceSubtitle: String? = null
)

@Singleton
class RedditCommentsService @Inject constructor(
    private val redditApi: RedditApi
) {
    private data class SearchContext(
        val queries: List<String>,
        val preferredSubredditQueries: List<String>,
        val imdbId: String?,
        val title: String?
    )

    private data class TimedCache(
        val comments: List<TraktCommentReview>,
        val sourceTitle: String?,
        val sourceSubtitle: String?,
        val updatedAtMs: Long
    )

    private data class ResolvedRedditComments(
        val comments: List<TraktCommentReview>,
        val sourceTitle: String?,
        val sourceSubtitle: String?
    )

    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<String, TimedCache>()

    suspend fun getCommentsPage(
        meta: Meta,
        fallbackItemId: String? = null,
        targetEpisode: Video? = null,
        page: Int = 1,
        forceRefresh: Boolean = false
    ): RedditCommentsPage {
        val safePage = page.coerceAtLeast(1)
        val context = buildSearchContext(meta, fallbackItemId, targetEpisode)
        val cacheKey = buildCacheKey(context.queries, targetEpisode)

        if (forceRefresh) {
            cacheMutex.withLock { cache.remove(cacheKey) }
        }

        val resolved = resolveComments(
            cacheKey = cacheKey,
            context = context,
            targetEpisode = targetEpisode,
            forceRefresh = forceRefresh
        )

        val comments = resolved.comments
        if (comments.isEmpty()) {
            return RedditCommentsPage(
                items = emptyList(),
                currentPage = safePage,
                pageCount = 0,
                itemCount = 0,
                sourceTitle = resolved.sourceTitle,
                sourceSubtitle = resolved.sourceSubtitle
            )
        }

        val pageCount = ceil(comments.size / REDDIT_PAGE_SIZE.toDouble()).toInt().coerceAtLeast(1)
        if (safePage > pageCount) {
            return RedditCommentsPage(
                items = emptyList(),
                currentPage = safePage,
                pageCount = pageCount,
                itemCount = comments.size,
                sourceTitle = resolved.sourceTitle,
                sourceSubtitle = resolved.sourceSubtitle
            )
        }

        val start = (safePage - 1) * REDDIT_PAGE_SIZE
        val end = (start + REDDIT_PAGE_SIZE).coerceAtMost(comments.size)

        return RedditCommentsPage(
            items = comments.subList(start, end),
            currentPage = safePage,
            pageCount = pageCount,
            itemCount = comments.size,
            sourceTitle = resolved.sourceTitle,
            sourceSubtitle = resolved.sourceSubtitle
        )
    }

    private suspend fun resolveComments(
        cacheKey: String,
        context: SearchContext,
        targetEpisode: Video?,
        forceRefresh: Boolean
    ): ResolvedRedditComments {
        if (!forceRefresh) {
            cacheMutex.withLock {
                val cached = cache[cacheKey]
                if (cached != null && System.currentTimeMillis() - cached.updatedAtMs <= REDDIT_CACHE_TTL_MS) {
                    return ResolvedRedditComments(
                        comments = cached.comments,
                        sourceTitle = cached.sourceTitle,
                        sourceSubtitle = cached.sourceSubtitle
                    )
                }
            }
        }

        Log.d(
            TAG,
            "Starting Reddit match: title=${context.title}, imdb=${context.imdbId}, mode=${if (targetEpisode != null) "episode" else "show"}, episode=${targetEpisode?.season}x${targetEpisode?.episode}"
        )
        Log.d(TAG, "Search queries: ${(context.queries + context.preferredSubredditQueries).joinToString(" | ")}")

        val candidatePosts = mutableListOf<Map<String, Any?>>()
        (context.queries + context.preferredSubredditQueries).forEach { query ->
            val discussionsResponse = redditApi.searchDiscussions(
                query = query,
                limit = REDDIT_SEARCH_LIMIT
            )

            if (!discussionsResponse.isSuccessful) {
                throw IllegalStateException("Failed to search Reddit discussions (${discussionsResponse.code()})")
            }

            val children = discussionsResponse.body()
                ?.data
                ?.children
                .orEmpty()

            candidatePosts += children.mapNotNull { it.data }
        }

        val uniquePosts = candidatePosts.distinctBy { post -> post["id"].asString().orEmpty() }
        Log.d(TAG, "Candidates: raw=${candidatePosts.size}, unique=${uniquePosts.size}")

        val identityMatchedPosts = uniquePosts.filter { post ->
            matchesShowIdentity(post = post, imdbId = context.imdbId, title = context.title)
        }
        Log.d(TAG, "Identity matches: ${identityMatchedPosts.size}")
        if (identityMatchedPosts.isEmpty() && !context.title.isNullOrBlank()) {
            Log.d(TAG, "No identity-matched posts found, returning empty result")
            return ResolvedRedditComments(
                comments = emptyList(),
                sourceTitle = null,
                sourceSubtitle = null
            )
        }

        val preferredSubredditPosts = identityMatchedPosts.filter { post ->
            isPreferredShowSubreddit(post = post, title = context.title)
        }

        val nonGenericIdentityPosts = identityMatchedPosts.filterNot { post ->
            post["subreddit"].asString().orEmpty().lowercase(Locale.US) in GENERIC_DISCUSSION_SUBREDDITS
        }

        val genericIdentityPosts = identityMatchedPosts.filter { post ->
            post["subreddit"].asString().orEmpty().lowercase(Locale.US) in GENERIC_DISCUSSION_SUBREDDITS
        }

        val pool = when {
            preferredSubredditPosts.isNotEmpty() -> preferredSubredditPosts
            nonGenericIdentityPosts.isNotEmpty() -> nonGenericIdentityPosts
            genericIdentityPosts.isNotEmpty() -> genericIdentityPosts
            else -> uniquePosts
        }
        Log.d(
            TAG,
            "Pool selected: size=${pool.size}, preferred=${preferredSubredditPosts.size}, nonGeneric=${nonGenericIdentityPosts.size}, generic=${genericIdentityPosts.size}"
        )

        val modeFilteredPool = if (targetEpisode?.season != null && targetEpisode.episode != null) {
            val exactInTitlePosts = pool.filter { post ->
                hasExactEpisodeMarker(
                    buildTitleBlob(post),
                    targetEpisode.season,
                    targetEpisode.episode
                )
            }
            val exactEpisodePosts = if (exactInTitlePosts.isNotEmpty()) {
                exactInTitlePosts
            } else {
                pool.filter { post ->
                    val blob = buildPostBlob(post)
                    hasExactEpisodeMarker(blob, targetEpisode.season, targetEpisode.episode) &&
                        !looksLikeSeasonIndexPost(post, targetEpisode.season)
                }
            }
            if (exactEpisodePosts.isEmpty()) {
                Log.d(TAG, "Episode mode: no exact episode marker posts found for ${targetEpisode.season}x${targetEpisode.episode}")
                return ResolvedRedditComments(
                    comments = emptyList(),
                    sourceTitle = null,
                    sourceSubtitle = null
                )
            }
            Log.d(TAG, "Episode mode: exact-marker posts=${exactEpisodePosts.size}")
            exactEpisodePosts
        } else {
            val showLevelPosts = pool.filter { post ->
                extractEpisodeMarkers(buildPostBlob(post)).isEmpty()
            }
            if (showLevelPosts.isNotEmpty()) {
                Log.d(TAG, "Show mode: using show-level posts=${showLevelPosts.size}")
                showLevelPosts
            } else {
                Log.d(TAG, "Show mode: no pure show-level posts, fallback to pool=${pool.size}")
                pool
            }
        }

        val scoredPosts = modeFilteredPool.map { post ->
            post to scorePost(
                post = post,
                targetEpisode = targetEpisode,
                imdbId = context.imdbId,
                title = context.title
            )
        }
        scoredPosts
            .sortedByDescending { it.second }
            .take(5)
            .forEachIndexed { index, (post, score) ->
                Log.d(
                    TAG,
                    "Top candidate #${index + 1}: score=$score, subreddit=${post["subreddit"].asString()}, title=${post["title"].asString()}, markers=${extractEpisodeMarkers(buildPostBlob(post))}"
                )
            }

        val bestPostPair = scoredPosts.maxByOrNull { it.second }
        val bestPost = bestPostPair?.first
        Log.d(
            TAG,
            "Selected post: score=${bestPostPair?.second}, subreddit=${bestPost?.get("subreddit")?.asString()}, title=${bestPost?.get("title")?.asString()}"
        )
        val permalink = bestPost
            ?.get("permalink")
            ?.asString()
            ?.trim()
            ?.removePrefix("/")
            ?.takeIf { it.isNotBlank() }

        if (permalink == null) {
            return ResolvedRedditComments(
                comments = emptyList(),
                sourceTitle = null,
                sourceSubtitle = null
            )
        }

        val commentsResponse = redditApi.getComments(
            permalinkWithoutLeadingSlash = permalink,
            depth = REDDIT_FETCH_DEPTH,
            limit = REDDIT_FETCH_LIMIT
        )

        if (!commentsResponse.isSuccessful) {
            throw IllegalStateException("Failed to fetch Reddit comments (${commentsResponse.code()})")
        }

        val rootChildren = commentsResponse.body()
            ?.getOrNull(1)
            ?.data
            ?.children
            .orEmpty()

        val threaded = mutableListOf<TraktCommentReview>()
        rootChildren.forEach { thing ->
            buildThreadComment(thing = thing, out = threaded)
            if (threaded.size >= REDDIT_MAX_ROOT_COMMENTS) {
                return@forEach
            }
        }

        val sourceTitle = bestPost["title"].asString()?.takeIf { it.isNotBlank() }
        val subreddit = bestPost["subreddit"].asString()?.takeIf { it.isNotBlank() }
        val sourceSubtitle = subreddit?.let { "r/$it" }
        Log.d(TAG, "Fetched thread comments: subreddit=$sourceSubtitle, title=$sourceTitle, roots=${threaded.size}")

        cacheMutex.withLock {
            cache[cacheKey] = TimedCache(
                comments = threaded,
                sourceTitle = sourceTitle,
                sourceSubtitle = sourceSubtitle,
                updatedAtMs = System.currentTimeMillis()
            )
        }

        return ResolvedRedditComments(
            comments = threaded,
            sourceTitle = sourceTitle,
            sourceSubtitle = sourceSubtitle
        )
    }

    private fun buildSearchContext(meta: Meta, fallbackItemId: String?, targetEpisode: Video?): SearchContext {
        val parsedMetaIds = parseContentIds(meta.id)
        val parsedFallback = parseContentIds(fallbackItemId)
        val imdbId = meta.imdbId
            ?.takeIf { it.isNotBlank() }
            ?: parsedMetaIds.imdb
            ?: parsedFallback.imdb

        val title = meta.name.trim().takeIf { it.isNotBlank() }
        val season = targetEpisode?.season
        val episode = targetEpisode?.episode
        val episodeTitle = targetEpisode?.title?.trim()?.takeIf { it.isNotBlank() }
        val preferredSubreddits = title?.let { guessPreferredSubreddits(it) }.orEmpty()

        val queries = if (season != null && episode != null) {
            val sxe = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
            val shortX = "${season}x${episode.toString().padStart(2, '0')}"
            val coreParts = listOfNotNull(imdbId, title)

            buildList {
                add((coreParts + listOf(sxe, "discussion")).joinToString(" "))
                add((coreParts + listOf(shortX, "discussion")).joinToString(" "))
                add((coreParts + listOf("season", season.toString(), "episode", episode.toString(), "discussion")).joinToString(" "))
                if (!episodeTitle.isNullOrBlank()) {
                    add((coreParts + listOf('"' + episodeTitle + '"', "discussion")).joinToString(" "))
                }
                if (!title.isNullOrBlank()) {
                    add((listOf('"' + title + '"', sxe, "discussion thread")).joinToString(" "))
                }
            }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        } else {
            val movieQuery = buildString {
                if (!imdbId.isNullOrBlank()) {
                    append(imdbId)
                    append(' ')
                }
                if (!title.isNullOrBlank()) {
                    append('"')
                    append(title)
                    append('"')
                    append(' ')
                }
                append("discussion")
            }.trim()

            listOf(movieQuery)
        }

        val preferredSubredditQueries = buildList {
            preferredSubreddits.forEach { subreddit ->
                if (season != null && episode != null) {
                    val sxe = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
                    add("subreddit:$subreddit ${title.orEmpty()} $sxe discussion")
                    add("subreddit:$subreddit ${title.orEmpty()} season $season episode $episode")
                } else {
                    add("subreddit:$subreddit ${title.orEmpty()} discussion")
                }
            }
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return SearchContext(
            queries = queries,
            preferredSubredditQueries = preferredSubredditQueries,
            imdbId = imdbId,
            title = title
        )
    }

    private fun buildCacheKey(queries: List<String>, targetEpisode: Video?): String {
        return buildString {
            append(REDDIT_CACHE_SCHEMA_VERSION)
            append('|')
            append(queries.joinToString("||").lowercase(Locale.US))
            if (targetEpisode?.season != null && targetEpisode.episode != null) {
                append('|')
                append(targetEpisode.season)
                append('|')
                append(targetEpisode.episode)
            }
        }
    }

    private fun scorePost(
        post: Map<String, Any?>,
        targetEpisode: Video?,
        imdbId: String?,
        title: String?
    ): Int {
        val comments = post["num_comments"].asInt().coerceAtLeast(0)
        val score = post["score"].asInt().coerceAtLeast(0)
        var total = min(comments, 500) * 120 + min(score, 10_000) * 8

        val blob = buildPostBlob(post)

        if (!imdbId.isNullOrBlank()) {
            if (blob.contains(imdbId.lowercase(Locale.US))) {
                total += 450_000
            } else {
                total -= 260_000
            }
        }

        if (!title.isNullOrBlank()) {
            val titleTokens = tokenize(title)
            val titleHits = titleTokens.count { token -> blob.contains(token) }
            val titleThreshold = minOf(titleTokens.size, 3)
            when {
                titleThreshold > 0 && titleHits >= titleThreshold -> total += 300_000
                titleHits >= 2 -> total += 120_000
                else -> total -= 220_000
            }
        }

        if (targetEpisode?.season != null && targetEpisode.episode != null) {
            val season = targetEpisode.season
            val episode = targetEpisode.episode
            val hasExactMatch = hasExactEpisodeMarker(blob, season, episode)
            val titleBlob = buildTitleBlob(post)

            if (hasExactEpisodeMarker(titleBlob, season, episode)) {
                total += 900_000
            } else if (hasExactMatch) {
                total += 650_000
            }

            val mentionedMarkers = extractEpisodeMarkers(blob).distinct()
            val targetMarker = season to episode
            if (mentionedMarkers.isNotEmpty() && targetMarker !in mentionedMarkers) {
                return Int.MIN_VALUE / 4
            }

            if (mentionedMarkers.size > 1) {
                total -= 450_000
            }

            if (looksLikeSeasonIndexPost(post, season)) {
                total -= 900_000
            }

            val epTitle = targetEpisode.title.trim().lowercase(Locale.US)
            if (!epTitle.isNullOrBlank()) {
                val epTokens = tokenize(epTitle)
                val epHits = epTokens.count { token -> blob.contains(token) }
                val epThreshold = minOf(epTokens.size, 2)
                if (epThreshold > 0 && epHits >= epThreshold) {
                    total += 120_000
                }
            }
        }

        return total
    }

    private fun matchesShowIdentity(
        post: Map<String, Any?>,
        imdbId: String?,
        title: String?
    ): Boolean {
        val titleText = post["title"].asString().orEmpty().lowercase(Locale.US)
        val subreddit = post["subreddit"].asString().orEmpty().lowercase(Locale.US)
        val flair = post["link_flair_text"].asString().orEmpty().lowercase(Locale.US)
        val identityBlob = "$titleText $subreddit $flair"

        if (!imdbId.isNullOrBlank() && identityBlob.contains(imdbId.lowercase(Locale.US))) {
            return true
        }

        if (title.isNullOrBlank()) {
            return true
        }

        val normalizedTitle = normalizeText(title)
        val normalizedPostTitle = normalizeText(titleText)
        if (
            subreddit in GENERIC_DISCUSSION_SUBREDDITS &&
            normalizedTitle.isNotBlank() &&
            !normalizedPostTitle.contains(normalizedTitle)
        ) {
            return false
        }

        if (normalizedTitle.isNotBlank() && normalizedPostTitle.contains(normalizedTitle)) {
            return true
        }

        val strongTitleTokens = tokenize(title)
            .filter { token -> token !in NOISE_TOKENS }
            .filter { token -> token.length >= 4 }
        if (strongTitleTokens.isEmpty()) {
            return true
        }

        return strongTitleTokens.any { token ->
            titleText.contains(token) || subreddit.contains(token)
        }
    }

    private fun isPreferredShowSubreddit(post: Map<String, Any?>, title: String?): Boolean {
        if (title.isNullOrBlank()) return false

        val subreddit = post["subreddit"].asString().orEmpty()
        if (subreddit.isBlank()) return false

        val normalizedSubreddit = normalizeText(subreddit)
        val normalizedTitle = normalizeText(title)
        val normalizedTitleNoArticles = removeLeadingArticle(normalizedTitle)

        if (normalizedTitle.isNotBlank() && normalizedSubreddit == normalizedTitle) {
            return true
        }

        if (normalizedTitleNoArticles.isNotBlank() && normalizedSubreddit == normalizedTitleNoArticles) {
            return true
        }

        if (normalizedTitle.isNotBlank() && normalizedSubreddit.contains(normalizedTitle)) {
            return true
        }

        if (normalizedTitleNoArticles.isNotBlank() && normalizedSubreddit.contains(normalizedTitleNoArticles)) {
            return true
        }

        return false
    }

    private fun buildPostBlob(post: Map<String, Any?>): String {
        val title = post["title"].asString().orEmpty()
        val body = post["selftext"].asString().orEmpty()
        val subreddit = post["subreddit"].asString().orEmpty()
        val flair = post["link_flair_text"].asString().orEmpty()
        return "$title $body $subreddit $flair".lowercase(Locale.US)
    }

    private fun buildTitleBlob(post: Map<String, Any?>): String {
        val title = post["title"].asString().orEmpty()
        val flair = post["link_flair_text"].asString().orEmpty()
        return "$title $flair".lowercase(Locale.US)
    }

    private fun looksLikeSeasonIndexPost(post: Map<String, Any?>, season: Int): Boolean {
        val titleBlob = buildTitleBlob(post)
        val seasonWidePatterns = listOf(
            Regex("overall\\s+season\\s+discussion"),
            Regex("season\\s*$season\\s*\\|\\s*overall"),
            Regex("season\\s*$season\\s*discussion\\s*thread"),
            Regex("season\\s*$season\\s*episode\\s*discussion\\s*threads"),
            Regex("episode\\s*discussion\\s*threads"),
            Regex("all\\s*episodes")
        )
        if (seasonWidePatterns.any { it.containsMatchIn(titleBlob) }) {
            return true
        }

        val markerCount = extractEpisodeMarkers(buildPostBlob(post)).distinct().size
        return markerCount >= 3
    }

    private fun tokenize(value: String): List<String> {
        return value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
            .distinct()
    }

    private fun normalizeText(value: String): String {
        return value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    private fun removeLeadingArticle(value: String): String {
        return value
            .removePrefix("the")
            .removePrefix("a")
            .removePrefix("an")
            .trim()
    }

    private fun guessPreferredSubreddits(title: String): List<String> {
        val normalized = title
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (normalized.isEmpty()) return emptyList()

        val joined = normalized.joinToString(separator = "")
        val noArticle = removeLeadingArticle(joined)

        return listOf(joined, noArticle)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun hasExactEpisodeMarker(blob: String, season: Int, episode: Int): Boolean {
        val s = season.toString().padStart(2, '0')
        val e = episode.toString().padStart(2, '0')
        val patterns = listOf(
            "s${s}e${e}",
            "s${season}e${episode}",
            "${season}x${e}",
            "${season}x${episode}",
            "season $season episode $episode",
            "season $season, episode $episode"
        )
        return patterns.any { blob.contains(it) }
    }

    private fun extractEpisodeMarkers(blob: String): List<Pair<Int, Int>> {
        val markers = mutableListOf<Pair<Int, Int>>()
        Regex("s(\\d{1,2})e(\\d{1,3})").findAll(blob).forEach { match ->
            val s = match.groupValues[1].toIntOrNull() ?: return@forEach
            val e = match.groupValues[2].toIntOrNull() ?: return@forEach
            markers += s to e
        }
        Regex("(\\d{1,2})x(\\d{1,3})").findAll(blob).forEach { match ->
            val s = match.groupValues[1].toIntOrNull() ?: return@forEach
            val e = match.groupValues[2].toIntOrNull() ?: return@forEach
            markers += s to e
        }
        Regex("season\\s*(\\d{1,2})\\s*[,\\-:]?\\s*episode\\s*(\\d{1,3})").findAll(blob).forEach { match ->
            val s = match.groupValues[1].toIntOrNull() ?: return@forEach
            val e = match.groupValues[2].toIntOrNull() ?: return@forEach
            markers += s to e
        }
        return markers
    }

    private fun buildThreadComment(
        thing: RedditThingDto,
        out: MutableList<TraktCommentReview>
    ) {
        if (out.size >= REDDIT_MAX_ROOT_COMMENTS) return
        if (!thing.kind.equals("t1", ignoreCase = true)) return

        val data = thing.data ?: return
        val body = data["body"].asString()?.trim().orEmpty()
        if (body.isBlank() || body == "[deleted]" || body == "[removed]") return

        val author = data["author"].asString()?.takeIf { it.isNotBlank() } ?: "reddit user"
        val commentIdRaw = data["id"].asString() ?: "unknown"
        val score = data["score"].asInt().coerceAtLeast(0)
        val createdUtc = data["created_utc"].asLongOrNull()?.toString()

        val replies = data["replies"].asMap()
            ?.get("data").asMap()
            ?.get("children") as? List<*>

        val threadText = buildString {
            append(body)
            appendRepliesInline(
                replyNodes = replies,
                depth = 1,
                repliesBudget = REDDIT_MAX_REPLIES_PER_THREAD
            )
        }
            .trim()
            .take(REDDIT_MAX_THREAD_CHARS)

        out += TraktCommentReview(
            id = stableLongId("reddit:$commentIdRaw"),
            authorDisplayName = author,
            authorUsername = author,
            comment = threadText,
            review = true,
            likes = score,
            rating = null,
            createdAt = createdUtc,
            updatedAt = null
        )
    }

    private fun StringBuilder.appendRepliesInline(
        replyNodes: List<*>?,
        depth: Int,
        repliesBudget: Int
    ): Int {
        if (replyNodes.isNullOrEmpty()) return 0
        if (repliesBudget <= 0 || depth > REDDIT_MAX_RENDERED_REPLY_DEPTH) return 0

        var used = 0
        replyNodes.forEach { child ->
            if (used >= repliesBudget) return@forEach
            val childMap = child as? Map<*, *> ?: return@forEach
            val childKind = childMap["kind"] as? String
            if (!childKind.equals("t1", ignoreCase = true)) return@forEach

            val rawData = childMap["data"] as? Map<*, *> ?: return@forEach
            val data = rawData.entries.associate { (k, v) -> k.toString() to v }
            val body = data["body"].asString()?.trim().orEmpty()
            if (body.isBlank() || body == "[deleted]" || body == "[removed]") return@forEach

            val author = data["author"].asString()?.takeIf { it.isNotBlank() } ?: "reddit user"
            val depthClamped = depth.coerceAtMost(REDDIT_MAX_RENDERED_REPLY_DEPTH)
            val levelPrefix = "│   ".repeat(depthClamped)
            val bodyIndent = " ".repeat((author.length + 1).coerceAtMost(24))

            // Start this reply block with a guide-aware separator so guides are continuous.
            append("\n")
            append(levelPrefix)
            append("\n")
            append(levelPrefix)
            append(author)
            append(":")

            val wrapped = body
                .lineSequence()
                .flatMap { line ->
                    wrapLineForDisplay(line.trimEnd(), maxChars = 46).asSequence()
                }
                .toList()

            if (wrapped.isEmpty()) {
                append("\n")
                append(levelPrefix)
                append(bodyIndent)
            } else {
                wrapped.forEach { chunk ->
                    append("\n")
                    append(levelPrefix)
                    append(bodyIndent)
                    append(chunk)
                }
            }
            used += 1

            val nestedReplies = data["replies"].asMap()
                ?.get("data").asMap()
                ?.get("children") as? List<*>

            used += appendRepliesInline(
                replyNodes = nestedReplies,
                depth = depth + 1,
                repliesBudget = repliesBudget - used
            )
        }

        return used
    }

    private fun stableLongId(value: String): Long {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        var result = 0L
        repeat(8) { index ->
            result = (result shl 8) or (bytes[index].toLong() and 0xffL)
        }
        return result and Long.MAX_VALUE
    }

    private fun wrapLineForDisplay(line: String, maxChars: Int): List<String> {
        if (line.isBlank()) return listOf("")
        if (line.length <= maxChars) return listOf(line)

        val words = line.split(Regex("\\s+"))
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        words.forEach { word ->
            if (current.isEmpty()) {
                if (word.length > maxChars) {
                    var start = 0
                    while (start < word.length) {
                        val end = (start + maxChars).coerceAtMost(word.length)
                        chunks += word.substring(start, end)
                        start = end
                    }
                } else {
                    current.append(word)
                }
            } else if (current.length + 1 + word.length <= maxChars) {
                current.append(' ').append(word)
            } else {
                chunks += current.toString()
                current = StringBuilder(word)
            }
        }

        if (current.isNotEmpty()) {
            chunks += current.toString()
        }

        return chunks
    }
}

private fun Any?.asString(): String? = this as? String

private fun Any?.asInt(): Int {
    return when (this) {
        is Int -> this
        is Long -> this.toInt()
        is Double -> this.toInt()
        is Float -> this.toInt()
        is String -> this.toIntOrNull() ?: 0
        else -> 0
    }
}

private fun Any?.asLongOrNull(): Long? {
    return when (this) {
        is Long -> this
        is Int -> this.toLong()
        is Double -> this.toLong()
        is Float -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }
}

private fun Any?.asMap(): Map<String, Any?>? {
    val raw = this as? Map<*, *> ?: return null
    return raw.entries.associate { (k, v) -> k.toString() to v }
}
