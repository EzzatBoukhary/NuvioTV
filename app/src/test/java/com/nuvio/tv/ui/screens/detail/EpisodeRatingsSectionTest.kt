package com.nuvio.tv.ui.screens.detail

import com.nuvio.tv.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeRatingsSectionTest {

    @Test
    fun `buildEpisodeRatingsChartData supports inverted tv layout by default data shape`() {
        val chart = buildEpisodeRatingsChartData(
            episodes = listOf(
                episode(id = "s1e1", season = 1, episode = 1),
                episode(id = "s1e2", season = 1, episode = 2),
                episode(id = "s2e1", season = 2, episode = 1),
                episode(id = "s2e3", season = 2, episode = 3)
            ),
            ratings = mapOf(
                (1 to 1) to 8.4,
                (2 to 3) to 9.3
            )
        )

        val display = chart.toDisplayModel(RatingsLayoutMode.EPISODES_ACROSS)

        assertEquals(listOf(1, 2), chart.displaySeasonNumbers)
        assertEquals(3, chart.maxEpisodeNumber)
        assertEquals("Season", display.leadingHeader)
        assertEquals(listOf("E1", "E2", "E3"), display.columnLabels)
        assertEquals("S1", display.rows[0].label)
        assertEquals("s2e3", display.rows[1].cells[2].episodeId)
        assertEquals("9.3", display.rows[1].cells[2].ratingLabel)
    }

    @Test
    fun `seasons across layout keeps mobile style orientation available`() {
        val chart = buildEpisodeRatingsChartData(
            episodes = listOf(
                episode(id = "s1e1", season = 1, episode = 1),
                episode(id = "s1e2", season = 1, episode = 2),
                episode(id = "s2e1", season = 2, episode = 1),
                episode(id = "s2e2", season = 2, episode = 2)
            ),
            ratings = emptyMap()
        )

        val display = chart.toDisplayModel(RatingsLayoutMode.SEASONS_ACROSS)

        assertEquals("Episode", display.leadingHeader)
        assertEquals(listOf("S1", "S2"), display.columnLabels)
        assertEquals("E1", display.rows[0].label)
        assertEquals("s1e1", display.rows[0].cells[0].episodeId)
        assertEquals("s2e2", display.rows[1].cells[1].episodeId)
    }

    @Test
    fun `season averages are computed from available ratings only`() {
        val chart = buildEpisodeRatingsChartData(
            episodes = listOf(
                episode(id = "s1e1", season = 1, episode = 1),
                episode(id = "s1e2", season = 1, episode = 2),
                episode(id = "s2e1", season = 2, episode = 1)
            ),
            ratings = mapOf(
                (1 to 1) to 8.0,
                (1 to 2) to 6.0,
                (2 to 1) to 9.0
            )
        )

        assertEquals(2, chart.seasonAverages.size)
        assertEquals(7.0, chart.seasonAverages.first { it.seasonNumber == 1 }.average, 0.001)
        assertEquals(9.0, chart.seasonAverages.first { it.seasonNumber == 2 }.average, 0.001)
        assertNotNull(chart.toDisplayModel(RatingsLayoutMode.EPISODES_ACROSS).firstEpisodeId)
        assertTrue(chart.toDisplayModel(RatingsLayoutMode.SEASONS_ACROSS).rows.isNotEmpty())
    }

    private fun episode(id: String, season: Int?, episode: Int?) = Video(
        id = id,
        title = id,
        released = null,
        thumbnail = null,
        season = season,
        episode = episode,
        overview = null
    )
}
