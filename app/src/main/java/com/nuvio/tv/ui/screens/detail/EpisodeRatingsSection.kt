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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
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
private val RowHeaderWidth = 60.dp
private val SideRailWidth = 148.dp
private const val AverageRowLabel = "Avg"

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
    var layoutMode by rememberSaveable(meta.id) { mutableStateOf(RatingsLayoutMode.EPISODES_ACROSS) }
    var showDialog by rememberSaveable(meta.id) { mutableStateOf(false) }

    if (showDialog) {
        EpisodeRatingsOverlay(
            meta = meta,
            chartData = chartData,
            layoutMode = layoutMode,
            onLayoutModeChanged = { layoutMode = it },
            onDismiss = { showDialog = false }
        )
    }

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
                RatingsLauncherButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
                    firstItemFocusRequester = firstItemFocusRequester,
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RatingsLauncherButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    firstItemFocusRequester: FocusRequester? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (firstItemFocusRequester != null) it.focusRequester(firstItemFocusRequester) else it
            }
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
                if (downFocusRequester != null) down = downFocusRequester
            },
        colors = ButtonDefaults.colors(
            containerColor = NuvioColors.Surface.copy(alpha = 0.76f),
            focusedContainerColor = NuvioColors.Secondary
        )
    ) {
        Text(
            text = stringResource(R.string.ratings_view_chart),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextPrimary
        )
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
    val firstCellFocusRequester = focusRequesters.values.firstOrNull()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = meta.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = NuvioColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    RatingLegendStrip(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        var toggleFocused by rememberSaveable { mutableStateOf(false) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.focusRequester(toggleRequester)
                                .focusProperties { 
                                    down = firstCellFocusRequester ?: Cancel
                                }
                                .then(
                                    if (toggleFocused) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = NuvioColors.FocusRing,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(4.dp)
                                .onFocusChanged { toggleFocused = it.isFocused }
                        ) {
                            Text(
                                text = "Layout:",
                                style = MaterialTheme.typography.labelSmall,
                                color = NuvioColors.TextSecondary
                            )
                            Switch(
                                checked = layoutMode == RatingsLayoutMode.EPISODES_ACROSS,
                                onCheckedChange = { checked ->
                                    onLayoutModeChanged(
                                        if (checked) {
                                            RatingsLayoutMode.EPISODES_ACROSS
                                        } else {
                                            RatingsLayoutMode.SEASONS_ACROSS
                                        }
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NuvioColors.Secondary,
                                    checkedTrackColor = NuvioColors.Secondary.copy(alpha = 0.3f),
                                    uncheckedThumbColor = NuvioColors.TextSecondary,
                                    uncheckedTrackColor = NuvioColors.BackgroundCard
                                )
                            )
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .focusRequester(closeRequester)
                                .focusProperties { 
                                    up = toggleRequester
                                    down = Cancel
                                },
                            colors = ButtonDefaults.colors(
                                containerColor = NuvioColors.BackgroundCard,
                                focusedContainerColor = NuvioColors.FocusBackground,
                                contentColor = NuvioColors.TextPrimary,
                                focusedContentColor = NuvioColors.Primary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.ratings_close_overlay),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }

                RatingsGridPanel(
                    displayModel = displayModel,
                    layoutMode = layoutMode,
                    focusedEpisodeId = focusedEpisodeId,
                    onEpisodeFocused = { focusedEpisodeId = it },
                    focusRequesters = focusRequesters,
                    upFocusRequester = toggleRequester,
                    downFocusRequester = closeRequester,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RatingLegendStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            LegendItem(color = ColorAwesome, label = "9+"),
            LegendItem(color = ColorGreat, label = "8+"),
            LegendItem(color = ColorGood, label = "7.5+"),
            LegendItem(color = ColorRegular, label = "7+"),
            LegendItem(color = ColorBad, label = "6+"),
            LegendItem(color = ColorGarbage, label = "<6")
        ).forEachIndexed { index, item ->
            if (index > 0) Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(item.color!!)
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = NuvioColors.TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = ColorCurrentSeason,
                modifier = Modifier
                    .size(6.dp)
                    .border(0.5.dp, ColorCurrentSeason, RoundedCornerShape(1.dp))
            )
            Text(
                text = "Current",
                style = MaterialTheme.typography.labelSmall,
                color = NuvioColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = Color(0xFF90CAF9),
                modifier = Modifier
                    .size(6.dp)
                    .border(0.5.dp, Color(0xFF90CAF9), RoundedCornerShape(1.dp))
            )
            Text(
                text = "Unreleased",
                style = MaterialTheme.typography.labelSmall,
                color = NuvioColors.TextSecondary
            )
        }
    }
}

@Composable
private fun HeaderBadge(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = NuvioColors.TextPrimary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AverageBadge(average: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = String.format("%.1f", average),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextSecondary
        )
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(getRatingColor(average))
        )
    }
}

@Composable
private fun AverageBadgeHorizontal(average: Double) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%.1f", average),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NuvioColors.TextSecondary
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(getRatingColor(average))
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SummaryCell(cell: RatingsDisplayCell, isEpisodesAcross: Boolean = false) {
    Box(
        modifier = Modifier
            .size(width = CellWidth, height = CellHeight)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (isEpisodesAcross) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = cell.ratingLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NuvioColors.TextSecondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(cell.backgroundColor)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = cell.ratingLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NuvioColors.TextSecondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(cell.backgroundColor)
                )
            }
        }
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
            LegendItem(color = ColorAwesome, label = stringResource(R.string.ratings_legend_awesome)),
            LegendItem(color = ColorGreat, label = stringResource(R.string.ratings_legend_great)),
            LegendItem(color = ColorGood, label = stringResource(R.string.ratings_legend_good)),
            LegendItem(color = ColorRegular, label = stringResource(R.string.ratings_legend_regular)),
            LegendItem(color = ColorBad, label = stringResource(R.string.ratings_legend_bad)),
            LegendItem(color = ColorGarbage, label = stringResource(R.string.ratings_legend_garbage))
        )
        items.forEach { item -> LegendRow(item) }
        Spacer(modifier = Modifier.height(4.dp))
        LegendRow(LegendItem(iconColor = ColorCurrentSeason, label = stringResource(R.string.ratings_warning_current_season)))
        LegendRow(LegendItem(iconColor = Color(0xFF90CAF9), label = stringResource(R.string.ratings_warning_upcoming_season)))
    }
}

@Composable
private fun LegendRow(item: LegendItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.color != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(item.color)
            )
        } else if (item.iconColor != null) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = item.iconColor,
                modifier = Modifier.size(14.dp)
            )
        }
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
    layoutMode: RatingsLayoutMode,
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
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.padding(bottom = 4.dp)) {
            Box(
                modifier = Modifier
                    .width(RowHeaderWidth)
                    .height(CellHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayModel.leadingHeader,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = NuvioColors.TextPrimary,
                    textAlign = TextAlign.Center
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
                        HeaderBadge(label = header.label)
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
                displayModel.rows.forEachIndexed { rowIndex, row ->
                    Box(
                        modifier = Modifier
                            .height(CellHeight)
                            .padding(bottom = if (rowIndex == displayModel.rows.lastIndex) 0.dp else 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = NuvioColors.TextSecondary,
                                textAlign = TextAlign.Center
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
                            modifier = Modifier
                                .height(CellHeight)
                                .padding(bottom = if (rowIndex == displayModel.rows.lastIndex) 0.dp else 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.cells.forEachIndexed { columnIndex, cell ->
                                when {
                                    cell.state == EpisodeRatingCellState.SUMMARY -> {
                                        SummaryCell(cell, isEpisodesAcross = layoutMode == RatingsLayoutMode.EPISODES_ACROSS)
                                    }
                                    cell.episodeId == null -> {
                                        Box(modifier = Modifier.size(width = CellWidth, height = CellHeight))
                                    }
                                    else -> {
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
                                                    val resolvedUp = if (rowIndex == 0) {
                                                        upFocusRequester
                                                    } else {
                                                        upCell?.episodeId?.let(focusRequesters::get)
                                                    }
                                                    up = resolvedUp ?: Cancel
                                                    val resolvedDown = if (rowIndex == displayModel.rows.lastIndex) {
                                                        Cancel
                                                    } else {
                                                        downCell?.episodeId?.let(focusRequesters::get)
                                                    }
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
                                                    EpisodeRatingCellState.SUMMARY -> Unit
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
    }
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
                val columnHeaders = buildList {
                    add(RatingsDisplayHeader(label = "Avg"))
                    (1..maxEpisodeNumber).forEach { episodeNumber ->
                        add(RatingsDisplayHeader(label = "E$episodeNumber"))
                    }
                }
                val rows = displaySeasonNumbers.map { seasonNumber ->
                    RatingsDisplayRow(
                        label = "S$seasonNumber",
                        cells = buildList {
                            val average = seasonAverageBySeasonNumber[seasonNumber]
                            add(
                                if (average == null) {
                                    RatingsDisplayCell.summary(
                                        seasonNumber = seasonNumber,
                                        ratingLabel = "—",
                                        backgroundColor = ColorMutedCell,
                                        useDarkText = false
                                    )
                                } else {
                                    RatingsDisplayCell.summary(
                                        seasonNumber = seasonNumber,
                                        ratingLabel = String.format("%.1f", average),
                                        backgroundColor = getRatingColor(average),
                                        useDarkText = average >= 7.0 && average < 8.0
                                    )
                                }
                            )
                            (1..maxEpisodeNumber).forEach { episodeNumber ->
                                add(buildDisplayCell(seasonNumber, episodeNumber))
                            }
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
                    RatingsDisplayHeader(label = "S$seasonNumber")
                }
                val rows = buildList {
                    add(
                        RatingsDisplayRow(
                            label = AverageRowLabel,
                            cells = displaySeasonNumbers.map { seasonNumber ->
                                buildAverageDisplayCell(seasonNumber)
                            }
                        )
                    )
                    addAll(
                        (1..maxEpisodeNumber).map { episodeNumber ->
                            RatingsDisplayRow(
                                label = "E$episodeNumber",
                                cells = displaySeasonNumbers.map { seasonNumber ->
                                    buildDisplayCell(seasonNumber, episodeNumber)
                                }
                            )
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

    private fun buildAverageDisplayCell(seasonNumber: Int): RatingsDisplayCell {
        val average = seasonAverageBySeasonNumber[seasonNumber]
        return if (average == null) {
            RatingsDisplayCell.summary(
                seasonNumber = seasonNumber,
                ratingLabel = "—",
                backgroundColor = ColorMutedCell,
                useDarkText = false
            )
        } else {
            RatingsDisplayCell.summary(
                seasonNumber = seasonNumber,
                ratingLabel = String.format("%.1f", average),
                backgroundColor = getRatingColor(average),
                useDarkText = average >= 7.0 && average < 8.0
            )
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
            append('|').append(header.label)
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
    val label: String
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
    UNAIRED,
    SUMMARY
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

        fun summary(
            seasonNumber: Int,
            ratingLabel: String,
            backgroundColor: Color,
            useDarkText: Boolean
        ) = RatingsDisplayCell(
            episodeId = null,
            seasonNumber = seasonNumber,
            episodeNumber = 0,
            ratingLabel = ratingLabel,
            state = EpisodeRatingCellState.SUMMARY,
            backgroundColor = backgroundColor,
            showCurrentSeasonBadge = false,
            useDarkText = useDarkText
        )
    }
}

private data class LegendItem(
    val color: Color? = null,
    val label: String,
    val iconColor: Color? = null
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
