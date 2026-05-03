package com.nuvio.tv.ui.screens.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.ui.theme.NuvioColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val PanelShape = RoundedCornerShape(10.dp)
private val CellShape = RoundedCornerShape(4.dp)
private val OverlayShape = RoundedCornerShape(12.dp)
private val CellWidth = 46.dp
private val CellHeight = 34.dp
private val RowHeaderWidth = 92.dp
private val SideRailWidth = 148.dp

private val ColorAwesome = Color(0xFF186A3B)
private val ColorGreat = Color(0xFF28B463)
private val ColorGood = Color(0xFFF4D03F)
private val ColorRegular = Color(0xFFF39C12)
private val ColorBad = Color(0xFFE74C3C)
private val ColorGarbage = Color(0xFF633974)
private val ColorMutedCell = Color(0xFF111111)
private val ColorCurrentSeason = Color(0xFF42A5F5)

internal enum class RatingsLayoutMode {
    EPISODES_ACROSS,
    SEASONS_ACROSS
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeRatingsSection(
    meta: Meta,
    episodes: List<Video>,
    ratings: Map<Pair<Int, Int>, Double>,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    title: String = "Ratings",
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    firstItemFocusRequester: FocusRequester? = null
) {
    val chartData = remember(episodes, ratings) {
        buildEpisodeRatingsChartData(episodes = episodes, ratings = ratings)
    }
    var showOverlay by rememberSaveable(meta.id) { mutableStateOf(false) }
    var layoutMode by rememberSaveable(meta.id) { mutableStateOf(RatingsLayoutMode.SEASONS_ACROSS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (title.isNotBlank()) 14.dp else 6.dp, bottom = 8.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NuvioColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }

        when {
            isLoading -> MessageText(stringResource(R.string.ratings_loading))
            error != null -> MessageText(error)
            chartData.displaySeasonNumbers.isEmpty() -> MessageText(stringResource(R.string.ratings_unavailable))
            else -> {
                RatingsLauncherCard(
                    chartData = chartData,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester,
                    focusRequester = firstItemFocusRequester,
                    onOpen = { showOverlay = true }
                )
            }
        }
    }

    if (showOverlay) {
        EpisodeRatingsOverlay(
            meta = meta,
            chartData = chartData,
            layoutMode = layoutMode,
            onLayoutModeChanged = { layoutMode = it },
            onDismiss = { showOverlay = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RatingsLauncherCard(
    chartData: EpisodeRatingsChartData,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    focusRequester: FocusRequester?,
    onOpen: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(NuvioColors.Surface.copy(alpha = 0.76f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), PanelShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(
                R.string.ratings_overlay_summary,
                chartData.displaySeasonNumbers.size,
                chartData.maxEpisodeNumber
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioColors.TextSecondary
        )

        if (chartData.seasonAverages.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ratings_average_label),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = NuvioColors.TextTertiary
                )
                chartData.seasonAverages.forEach { average ->
                    Text(
                        text = "S${average.seasonNumber} ${String.format("%.1f", average.average)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioColors.TextSecondary
                    )
                }
            }
        }

        Button(
            onClick = onOpen,
            modifier = Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .focusProperties {
                    if (upFocusRequester != null) up = upFocusRequester
                    if (downFocusRequester != null) down = downFocusRequester
                },
            colors = ButtonDefaults.colors(
                containerColor = NuvioColors.FocusBackground,
                focusedContainerColor = NuvioColors.FocusBackground
            )
        ) {
            Text(text = stringResource(R.string.ratings_open_overlay))
        }
    }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = NuvioColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRatingsOverlay(
    meta: Meta,
    chartData: EpisodeRatingsChartData,
    layoutMode: RatingsLayoutMode,
    onLayoutModeChanged: (RatingsLayoutMode) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val displayModel = remember(chartData, layoutMode) {
        chartData.toDisplayModel(layoutMode)
    }
    var focusedEpisodeId by rememberSaveable(meta.id, displayModel.signature) {
        mutableStateOf(displayModel.firstEpisodeId)
    }
    val focusRequesters = remember(displayModel) {
        buildMap {
            displayModel.rows.flatMap { it.cells }
                .mapNotNull { it.episodeId }
                .forEach { episodeId ->
                    put(episodeId, FocusRequester())
                }
        }
    }
    val closeRequester = remember { FocusRequester() }
    val toggleRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(OverlayShape)
                    .background(NuvioColors.BackgroundElevated)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), OverlayShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.ratings_overlay_summary,
                            chartData.displaySeasonNumbers.size,
                            chartData.maxEpisodeNumber
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioColors.TextTertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InvertedToggleButton(
                            isInverted = layoutMode == RatingsLayoutMode.EPISODES_ACROSS,
                            modifier = Modifier.focusRequester(toggleRequester),
                            onClick = {
                                onLayoutModeChanged(
                                    if (layoutMode == RatingsLayoutMode.SEASONS_ACROSS) {
                                        RatingsLayoutMode.EPISODES_ACROSS
                                    } else {
                                        RatingsLayoutMode.SEASONS_ACROSS
                                    }
                                )
                            }
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(closeRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = NuvioColors.BackgroundCard,
                                focusedContainerColor = NuvioColors.BackgroundCard
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = NuvioColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.ratings_close_overlay))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RatingsGridPanel(
                        displayModel = displayModel,
                        focusedEpisodeId = focusedEpisodeId,
                        onEpisodeFocused = { focusedEpisodeId = it },
                        focusRequesters = focusRequesters,
                        upFocusRequester = toggleRequester,
                        downFocusRequester = closeRequester,
                        modifier = Modifier.weight(1f)
                    )

                    Column(
                        modifier = Modifier.width(SideRailWidth),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RatingLegendPanel(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InvertedToggleButton(
    isInverted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = if (isInverted) NuvioColors.FocusBackground else NuvioColors.BackgroundCard,
            focusedContainerColor = if (isInverted) NuvioColors.FocusBackground else NuvioColors.BackgroundCard
        )
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
            tint = NuvioColors.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.ratings_layout_inverted))
    }
}

@Composable
private fun RatingLegendPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(NuvioColors.Surface.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), PanelShape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.ratings_scale_title),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextPrimary
        )
        val items = listOf(
            LegendItem(ColorAwesome, stringResource(R.string.ratings_legend_awesome)),
            LegendItem(ColorGreat, stringResource(R.string.ratings_legend_great)),
            LegendItem(ColorGood, stringResource(R.string.ratings_legend_good)),
            LegendItem(ColorRegular, stringResource(R.string.ratings_legend_regular)),
            LegendItem(ColorBad, stringResource(R.string.ratings_legend_bad)),
            LegendItem(ColorGarbage, stringResource(R.string.ratings_legend_garbage))
        )
        items.forEach { item -> LegendRow(item) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = ColorCurrentSeason, modifier = Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.ratings_warning_current_season),
                style = MaterialTheme.typography.labelSmall,
                color = NuvioColors.TextSecondary
            )
        }
    }
}

@Composable
private fun LegendRow(item: LegendItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(item.color)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = NuvioColors.TextSecondary
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RatingsGridPanel(
    displayModel: RatingsDisplayModel,
    focusedEpisodeId: String?,
    onEpisodeFocused: (String) -> Unit,
    focusRequesters: Map<String, FocusRequester>,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(NuvioColors.Surface.copy(alpha = 0.60f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), PanelShape)
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.padding(bottom = 4.dp)) {
            Box(
                modifier = Modifier
                    .width(RowHeaderWidth)
                    .height(CellHeight),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = displayModel.leadingHeader,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = NuvioColors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(horizontalScrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displayModel.columnHeaders.forEach { header ->
                    Box(
                        modifier = Modifier
                            .width(CellWidth)
                            .height(CellHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        HeaderBadge(label = header.label, average = header.average)
                    }
                }
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .width(RowHeaderWidth)
                    .verticalScroll(verticalScrollState)
                    .padding(end = 4.dp)
            ) {
                displayModel.rows.forEach { row ->
                    Box(
                        modifier = Modifier.height(CellHeight),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = NuvioColors.TextSecondary
                            )
                            row.average?.let { average ->
                                AverageBadge(average = average)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(modifier = Modifier.verticalScroll(verticalScrollState)) {
                    displayModel.rows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.padding(bottom = if (rowIndex == displayModel.rows.lastIndex) 0.dp else 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.cells.forEachIndexed { columnIndex, cell ->
                                if (cell.episodeId == null) {
                                    Box(modifier = Modifier.size(width = CellWidth, height = CellHeight))
                                    return@forEachIndexed
                                }

                                val episodeId = cell.episodeId
                                val bringIntoViewRequester = remember(episodeId) { BringIntoViewRequester() }
                                val upCell = displayModel.findNeighbor(rowIndex, columnIndex, -1)
                                val downCell = displayModel.findNeighbor(rowIndex, columnIndex, 1)
                                val leftCell = row.cells.findHorizontalNeighbor(columnIndex, -1)
                                val rightCell = row.cells.findHorizontalNeighbor(columnIndex, 1)

                                Card(
                                    onClick = {},
                                    modifier = Modifier
                                        .focusRequester(focusRequesters.getValue(episodeId))
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .focusProperties {
                                            left = leftCell?.episodeId?.let(focusRequesters::get) ?: Cancel
                                            right = rightCell?.episodeId?.let(focusRequesters::get) ?: Cancel
                                            val resolvedUp = upCell?.episodeId?.let(focusRequesters::get) ?: upFocusRequester
                                            up = resolvedUp ?: Cancel
                                            val resolvedDown = downCell?.episodeId?.let(focusRequesters::get) ?: downFocusRequester
                                            down = resolvedDown ?: Cancel
                                        }
                                        .onFocusChanged {
                                            if (it.isFocused) onEpisodeFocused(episodeId)
                                        },
                                    shape = CardDefaults.shape(CellShape),
                                    colors = CardDefaults.colors(
                                        containerColor = cell.backgroundColor,
                                        focusedContainerColor = cell.backgroundColor
                                    ),
                                    border = CardDefaults.border(
                                        focusedBorder = Border(
                                            border = BorderStroke(2.dp, NuvioColors.FocusRing),
                                            shape = CellShape
                                        )
                                    ),
                                    scale = CardDefaults.scale(focusedScale = 1.03f)
                                ) {
                                    if (focusedEpisodeId == episodeId) {
                                        LaunchedEffect(episodeId) {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(width = CellWidth, height = CellHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (cell.state) {
                                            EpisodeRatingCellState.RATED -> {
                                                Text(
                                                    text = cell.ratingLabel,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = if (cell.useDarkText) Color(0xFF1D1D1F) else Color.White
                                                )
                                            }
                                            EpisodeRatingCellState.UNRATED -> {
                                                Text(
                                                    text = "—",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = NuvioColors.TextSecondary
                                                )
                                            }
                                            EpisodeRatingCellState.UNAIRED -> {
                                                Icon(
                                                    imageVector = Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = NuvioColors.TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (cell.showCurrentSeasonBadge) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = ColorCurrentSeason,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 2.dp, end = 2.dp)
                                                    .size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBadge(
    label: String,
    average: Double?
) {
    if (average == null) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        AverageBadge(average = average)
    }
}

@Composable
private fun AverageBadge(average: Double) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(getRatingColor(average))
        )
        Text(
            text = String.format("%.1f", average),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextSecondary
        )
    }
}

private fun List<RatingsDisplayCell>.findHorizontalNeighbor(
    startIndex: Int,
    offset: Int
): RatingsDisplayCell? {
    var targetIndex = startIndex + offset
    while (targetIndex in indices) {
        val cell = this[targetIndex]
        if (cell.episodeId != null) return cell
        targetIndex += offset
    }
    return null
}

internal fun buildEpisodeRatingsChartData(
    episodes: List<Video>,
    ratings: Map<Pair<Int, Int>, Double>
): EpisodeRatingsChartData {
    val normalizedEpisodes = episodes
        .filter { (it.season ?: 0) > 0 && (it.episode ?: 0) > 0 }
        .sortedWith(compareBy<Video>({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))

    if (normalizedEpisodes.isEmpty()) return EpisodeRatingsChartData()

    val seasonNumbers = normalizedEpisodes.mapNotNull { it.season }.distinct().sorted()
    val maxEpisodeNumber = normalizedEpisodes.maxOfOrNull { it.episode ?: 0 } ?: 0
    val latestSeason = seasonNumbers.maxOrNull() ?: 0
    val episodeLookup = normalizedEpisodes.associateBy { requireNotNull(it.season) to requireNotNull(it.episode) }

    val seasonAverages = seasonNumbers.mapNotNull { seasonNumber ->
        val seasonRatings = (1..maxEpisodeNumber).mapNotNull { episodeNumber ->
            ratings[seasonNumber to episodeNumber]
        }
        if (seasonRatings.isEmpty()) null else SeasonAverage(seasonNumber, seasonRatings.average())
    }

    return EpisodeRatingsChartData(
        displaySeasonNumbers = seasonNumbers,
        maxEpisodeNumber = maxEpisodeNumber,
        latestSeasonNumber = latestSeason,
        episodeLookup = episodeLookup,
        ratings = ratings,
        seasonAverages = seasonAverages,
        seasonAverageBySeasonNumber = seasonAverages.associate { it.seasonNumber to it.average }
    )
}

internal data class EpisodeRatingsChartData(
    val displaySeasonNumbers: List<Int> = emptyList(),
    val maxEpisodeNumber: Int = 0,
    val latestSeasonNumber: Int = 0,
    val episodeLookup: Map<Pair<Int, Int>, Video> = emptyMap(),
    val ratings: Map<Pair<Int, Int>, Double> = emptyMap(),
    val seasonAverages: List<SeasonAverage> = emptyList(),
    val seasonAverageBySeasonNumber: Map<Int, Double> = emptyMap()
) {
    fun toDisplayModel(layoutMode: RatingsLayoutMode): RatingsDisplayModel {
        return when (layoutMode) {
            RatingsLayoutMode.EPISODES_ACROSS -> {
                val columnHeaders = (1..maxEpisodeNumber).map { RatingsDisplayHeader(label = "E$it") }
                val rows = displaySeasonNumbers.map { seasonNumber ->
                    RatingsDisplayRow(
                        label = "S$seasonNumber",
                        average = seasonAverageBySeasonNumber[seasonNumber],
                        cells = (1..maxEpisodeNumber).map { episodeNumber ->
                            buildDisplayCell(seasonNumber, episodeNumber)
                        }
                    )
                }
                RatingsDisplayModel(
                    leadingHeader = "Season",
                    columnHeaders = columnHeaders,
                    rows = rows
                )
            }
            RatingsLayoutMode.SEASONS_ACROSS -> {
                val columnHeaders = displaySeasonNumbers.map { seasonNumber ->
                    RatingsDisplayHeader(
                        label = "S$seasonNumber",
                        average = seasonAverageBySeasonNumber[seasonNumber]
                    )
                }
                val rows = (1..maxEpisodeNumber).map { episodeNumber ->
                    RatingsDisplayRow(
                        label = "E$episodeNumber",
                        cells = displaySeasonNumbers.map { seasonNumber ->
                            buildDisplayCell(seasonNumber, episodeNumber)
                        }
                    )
                }
                RatingsDisplayModel(
                    leadingHeader = "Episode",
                    columnHeaders = columnHeaders,
                    rows = rows
                )
            }
        }
    }

    private fun buildDisplayCell(seasonNumber: Int, episodeNumber: Int): RatingsDisplayCell {
        val episode = episodeLookup[seasonNumber to episodeNumber]
            ?: return RatingsDisplayCell.placeholder(seasonNumber, episodeNumber)
        val rating = ratings[seasonNumber to episodeNumber]
        val isCurrentSeasonEpisode = seasonNumber == latestSeasonNumber && isRecentlyAiredEpisode(episode)
        val isUnaired = isFutureEpisode(episode)
        return RatingsDisplayCell(
            episodeId = episode.id,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            ratingLabel = rating?.let { String.format("%.1f", it) }.orEmpty(),
            state = when {
                rating != null -> EpisodeRatingCellState.RATED
                isUnaired -> EpisodeRatingCellState.UNAIRED
                else -> EpisodeRatingCellState.UNRATED
            },
            backgroundColor = rating?.let(::getRatingColor) ?: ColorMutedCell,
            showCurrentSeasonBadge = rating != null && isCurrentSeasonEpisode,
            useDarkText = rating != null && rating >= 7.0 && rating < 8.0
        )
    }
}

internal data class RatingsDisplayModel(
    val leadingHeader: String,
    val columnHeaders: List<RatingsDisplayHeader>,
    val rows: List<RatingsDisplayRow>
) {
    val signature: String = buildString {
        append(leadingHeader)
        columnHeaders.forEach { header ->
            append('|').append(header.label).append(':').append(header.average ?: "x")
        }
        rows.forEach { row ->
            append('#').append(row.label).append(':').append(row.average ?: "x")
            row.cells.forEach { append(':').append(it.episodeId ?: "x") }
        }
    }

    val firstEpisodeId: String?
        get() = rows.flatMap { it.cells }.firstOrNull { it.episodeId != null }?.episodeId

    fun findNeighbor(rowIndex: Int, columnIndex: Int, offset: Int): RatingsDisplayCell? {
        var targetIndex = rowIndex + offset
        while (targetIndex in rows.indices) {
            val cell = rows[targetIndex].cells.getOrNull(columnIndex)
            if (cell?.episodeId != null) return cell
            targetIndex += offset
        }
        return null
    }
}

internal data class RatingsDisplayHeader(
    val label: String,
    val average: Double? = null
)

internal data class RatingsDisplayRow(
    val label: String,
    val average: Double? = null,
    val cells: List<RatingsDisplayCell>
)

internal data class SeasonAverage(
    val seasonNumber: Int,
    val average: Double
)

internal enum class EpisodeRatingCellState {
    RATED,
    UNRATED,
    UNAIRED
}

internal data class RatingsDisplayCell(
    val episodeId: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val ratingLabel: String,
    val state: EpisodeRatingCellState,
    val backgroundColor: Color,
    val showCurrentSeasonBadge: Boolean,
    val useDarkText: Boolean
) {
    companion object {
        fun placeholder(seasonNumber: Int, episodeNumber: Int) = RatingsDisplayCell(
            episodeId = null,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            ratingLabel = "",
            state = EpisodeRatingCellState.UNRATED,
            backgroundColor = Color.Transparent,
            showCurrentSeasonBadge = false,
            useDarkText = false
        )
    }
}

private data class LegendItem(
    val color: Color,
    val label: String
)

private fun getRatingColor(rating: Double): Color {
    return when {
        rating >= 9.0 -> ColorAwesome
        rating >= 8.0 -> ColorGreat
        rating >= 7.5 -> ColorGood
        rating >= 7.0 -> ColorRegular
        rating >= 6.0 -> ColorBad
        else -> ColorGarbage
    }
}

private fun isFutureEpisode(video: Video): Boolean {
    val releaseDate = parseReleaseDate(video.released) ?: return false
    return releaseDate.isAfter(LocalDate.now())
}

private fun isRecentlyAiredEpisode(video: Video): Boolean {
    val releaseDate = parseReleaseDate(video.released) ?: return false
    val now = LocalDate.now()
    if (releaseDate.isAfter(now)) return false
    val monthsDiff = (now.year - releaseDate.year) * 12 + (now.monthValue - releaseDate.monthValue)
    return monthsDiff <= 6
}

private fun parseReleaseDate(value: String?): LocalDate? {
    val normalized = value?.substringBefore('T')?.trim().orEmpty()
    if (normalized.isBlank()) return null
    return try {
        LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
}
