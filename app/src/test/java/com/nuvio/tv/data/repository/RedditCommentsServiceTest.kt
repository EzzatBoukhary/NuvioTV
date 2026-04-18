package com.nuvio.tv.data.repository

import android.util.Log
import com.nuvio.tv.data.remote.api.RedditApi
import com.nuvio.tv.data.remote.dto.reddit.RedditListingDataDto
import com.nuvio.tv.data.remote.dto.reddit.RedditListingDto
import com.nuvio.tv.data.remote.dto.reddit.RedditThingDto
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.Video
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class RedditCommentsServiceTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `show mode prefers show-level thread over episode thread`() = runTest {
        val api = mockk<RedditApi>()
        val service = RedditCommentsService(api)
        val meta = seriesMeta("The Bear", "tt14452776")

        val episodeThread = post(
            id = "p1",
            title = "The Bear S01E06 discussion",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p1/the_bear_s01e06_discussion/"
        )
        val showThread = post(
            id = "p2",
            title = "The Bear general discussion thread",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p2/the_bear_general_discussion_thread/"
        )

        stubSearch(api, listOf(episodeThread, showThread))
        stubComments(
            api,
            mapOf(
                "r/thebear/comments/p2/the_bear_general_discussion_thread" to listOf(
                    commentThing(id = "c1", author = "chef", body = "Great season")
                )
            )
        )

        val result = service.getCommentsPage(meta = meta, fallbackItemId = meta.id, targetEpisode = null)

        assertEquals("The Bear general discussion thread", result.sourceTitle)
        assertEquals("r/thebear", result.sourceSubtitle)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `episode mode picks exact episode thread over season thread`() = runTest {
        val api = mockk<RedditApi>()
        val service = RedditCommentsService(api)
        val meta = seriesMeta("The Bear", "tt14452776")
        val targetEpisode = episode(season = 1, episode = 6, title = "Ceres")

        val seasonThread = post(
            id = "p10",
            title = "The Bear season 1 discussion",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p10/the_bear_season_1_discussion/"
        )
        val exactEpisodeThread = post(
            id = "p11",
            title = "The Bear S01E06 discussion",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p11/the_bear_s01e06_discussion/"
        )

        stubSearch(api, listOf(seasonThread, exactEpisodeThread))
        stubComments(
            api,
            mapOf(
                "r/thebear/comments/p11/the_bear_s01e06_discussion" to listOf(
                    commentThing(id = "c11", author = "syd", body = "Episode 6 was amazing")
                )
            )
        )

        val result = service.getCommentsPage(meta = meta, fallbackItemId = meta.id, targetEpisode = targetEpisode)

        assertEquals("The Bear S01E06 discussion", result.sourceTitle)
        assertEquals("r/thebear", result.sourceSubtitle)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `episode mode avoids season-wide overall discussion thread even with higher engagement`() = runTest {
        val api = mockk<RedditApi>()
        val service = RedditCommentsService(api)
        val meta = seriesMeta("The Bear", "tt14452776")
        val targetEpisode = episode(season = 1, episode = 5, title = "Sheridan")

        val seasonOverall = post(
            id = "p12",
            title = "The Bear | Season 1 | Overall Season Discussion Thread",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p12/the_bear_season_1_overall_discussion/",
            numComments = 900,
            score = 9000,
            selftext = "S1E1 S1E2 S1E3 S1E4 S1E5 S1E6 S1E7 S1E8"
        )
        val exactEpisodeThread = post(
            id = "p13",
            title = "The Bear | S1E5 \"Sheridan\" | Episode Discussion",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p13/the_bear_s1e5_sheridan_discussion/",
            numComments = 120,
            score = 1500
        )

        stubSearch(api, listOf(seasonOverall, exactEpisodeThread))
        stubComments(
            api,
            mapOf(
                "r/thebear/comments/p13/the_bear_s1e5_sheridan_discussion" to listOf(
                    commentThing(id = "c13", author = "richie", body = "This one is the real episode thread")
                )
            )
        )

        val result = service.getCommentsPage(meta = meta, fallbackItemId = meta.id, targetEpisode = targetEpisode)

        assertEquals("The Bear | S1E5 \"Sheridan\" | Episode Discussion", result.sourceTitle)
        assertEquals("r/thebear", result.sourceSubtitle)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `episode mode returns empty when no exact episode thread exists`() = runTest {
        val api = mockk<RedditApi>()
        val service = RedditCommentsService(api)
        val meta = seriesMeta("The Bear", "tt14452776")
        val targetEpisode = episode(season = 1, episode = 6, title = "Ceres")

        val seasonThread = post(
            id = "p20",
            title = "The Bear season 1 discussion",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p20/the_bear_season_1_discussion/"
        )

        stubSearch(api, listOf(seasonThread))
        stubComments(api, emptyMap())

        val result = service.getCommentsPage(meta = meta, fallbackItemId = meta.id, targetEpisode = targetEpisode)

        assertTrue(result.items.isEmpty())
        assertNull(result.sourceTitle)
        assertNull(result.sourceSubtitle)
    }

    @Test
    fun `preferred show subreddit beats generic subreddit even with higher engagement`() = runTest {
        val api = mockk<RedditApi>()
        val service = RedditCommentsService(api)
        val meta = seriesMeta("The Bear", "tt14452776")
        val targetEpisode = episode(season = 1, episode = 6, title = "Ceres")

        val genericHighEngagement = post(
            id = "p30",
            title = "The Bear S01E06 discussion",
            subreddit = "television",
            permalink = "/r/television/comments/p30/the_bear_s01e06_discussion/",
            numComments = 999,
            score = 9999
        )
        val showSubredditLowerEngagement = post(
            id = "p31",
            title = "The Bear S01E06 discussion",
            subreddit = "thebear",
            permalink = "/r/thebear/comments/p31/the_bear_s01e06_discussion/",
            numComments = 10,
            score = 50
        )

        stubSearch(api, listOf(genericHighEngagement, showSubredditLowerEngagement))
        stubComments(
            api,
            mapOf(
                "r/thebear/comments/p31/the_bear_s01e06_discussion" to listOf(
                    commentThing(id = "c31", author = "carmy", body = "Heard, chef")
                )
            )
        )

        val result = service.getCommentsPage(meta = meta, fallbackItemId = meta.id, targetEpisode = targetEpisode)

        assertEquals("r/thebear", result.sourceSubtitle)
        assertEquals("The Bear S01E06 discussion", result.sourceTitle)
    }

    @Test
    fun `movie mode picks matching movie discussion`() = runTest {
        val api = mockk<RedditApi>()
        val service = RedditCommentsService(api)
        val meta = movieMeta("Inception", "tt1375666")

        val wrongTitle = post(
            id = "p40",
            title = "The Bear season discussion",
            subreddit = "television",
            permalink = "/r/television/comments/p40/the_bear_season_discussion/",
            numComments = 500,
            score = 5000
        )
        val correctMovie = post(
            id = "p41",
            title = "Inception discussion thread",
            subreddit = "movies",
            permalink = "/r/movies/comments/p41/inception_discussion_thread/",
            numComments = 30,
            score = 200
        )

        stubSearch(api, listOf(wrongTitle, correctMovie))
        stubComments(
            api,
            mapOf(
                "r/movies/comments/p41/inception_discussion_thread" to listOf(
                    commentThing(id = "c41", author = "ariadne", body = "Dream within a dream")
                )
            )
        )

        val result = service.getCommentsPage(meta = meta, fallbackItemId = meta.id, targetEpisode = null)

        assertEquals("Inception discussion thread", result.sourceTitle)
        assertEquals("r/movies", result.sourceSubtitle)
        assertEquals("ariadne", result.items.first().authorDisplayName)
    }

    private fun stubSearch(api: RedditApi, posts: List<Map<String, Any?>>) {
        val listing = RedditListingDto(
            data = RedditListingDataDto(
                children = posts.map { post -> RedditThingDto(kind = "t3", data = post) }
            )
        )
        coEvery {
            api.searchDiscussions(any(), any(), any(), any(), any(), any())
        } returns Response.success(listing)
    }

    private fun stubComments(api: RedditApi, permalinkToComments: Map<String, List<RedditThingDto>>) {
        coEvery {
            api.getComments(any(), any(), any(), any())
        } answers {
            val permalink = firstArg<String>()
            val normalized = permalink.trim('/')
            val commentThings = permalinkToComments[permalink]
                ?: permalinkToComments[normalized]
                ?: permalinkToComments["$normalized/"]
                ?: emptyList()
            Response.success(
                listOf(
                    RedditListingDto(data = RedditListingDataDto(children = emptyList())),
                    RedditListingDto(data = RedditListingDataDto(children = commentThings))
                )
            )
        }
    }

    private fun post(
        id: String,
        title: String,
        subreddit: String,
        permalink: String,
        numComments: Int = 100,
        score: Int = 100,
        selftext: String = "",
        flair: String = ""
    ): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "subreddit" to subreddit,
            "permalink" to permalink,
            "num_comments" to numComments,
            "score" to score,
            "selftext" to selftext,
            "link_flair_text" to flair
        )
    }

    private fun commentThing(
        id: String,
        author: String,
        body: String,
        score: Int = 1,
        createdUtc: Long = 1_700_000_000L
    ): RedditThingDto {
        return RedditThingDto(
            kind = "t1",
            data = mapOf(
                "id" to id,
                "author" to author,
                "body" to body,
                "score" to score,
                "created_utc" to createdUtc,
                "replies" to ""
            )
        )
    }

    private fun seriesMeta(name: String, imdbId: String): Meta {
        return Meta(
            id = imdbId,
            type = ContentType.SERIES,
            name = name,
            poster = null,
            posterShape = PosterShape.POSTER,
            background = null,
            logo = null,
            description = null,
            releaseInfo = null,
            imdbRating = null,
            genres = emptyList(),
            runtime = null,
            director = emptyList(),
            cast = emptyList(),
            videos = emptyList(),
            country = null,
            awards = null,
            language = null,
            links = emptyList(),
            imdbId = imdbId
        )
    }

    private fun movieMeta(name: String, imdbId: String): Meta {
        return Meta(
            id = imdbId,
            type = ContentType.MOVIE,
            name = name,
            poster = null,
            posterShape = PosterShape.POSTER,
            background = null,
            logo = null,
            description = null,
            releaseInfo = null,
            imdbRating = null,
            genres = emptyList(),
            runtime = null,
            director = emptyList(),
            cast = emptyList(),
            videos = emptyList(),
            country = null,
            awards = null,
            language = null,
            links = emptyList(),
            imdbId = imdbId
        )
    }

    private fun episode(season: Int, episode: Int, title: String): Video {
        return Video(
            id = "ep-$season-$episode",
            title = title,
            released = null,
            thumbnail = null,
            season = season,
            episode = episode,
            overview = null
        )
    }
}
