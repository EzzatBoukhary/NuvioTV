package com.nuvio.tv.ui.screens.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import coil3.compose.AsyncImage
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
private val GridContentPadding = 10.dp
private const val AverageRowLabel = "Avg"

private val ColorAwesome = Color(0xFF186A3B)
private val ColorGreat = Color(0xFF28B463)
private val ColorGood = Color(0xFFF4D03F)
private val ColorRegular = Color(0xFFF39C12)
private val ColorBad = Color(0xFFE74C3C)
private val ColorGarbage = Color(0xFF633974)
private val ColorMutedCell = Color(0xFF111111)
private val ColorCurrentSeason = Color(0xFF1976D2)

internal enum class RatingsLayoutMode {
    EPISODES_ACROSS,
    SEASONS_ACROSS
}

private data class RatingsGridMetrics(
    val cellWidth: androidx.compose.ui.unit.Dp,
    val cellHeight: androidx.compose.ui.unit.Dp,
    val rowHeaderWidth: androidx.compose.ui.unit.Dp,
    val gridSpacing: androidx.compose.ui.unit.Dp,
    val cellPadding: androidx.compose.ui.unit.Dp,
    val summaryBarWidth: androidx.compose.ui.unit.Dp,
    val summaryBarWidthEpisodesAcross: androidx.compose.ui.unit.Dp,
    val summaryBarHeight: androidx.compose.ui.unit.Dp,
    val currentSeasonIconSize: androidx.compose.ui.unit.Dp,
    val unreleasedIconBoxSize: androidx.compose.ui.unit.Dp,
    val unreleasedIconSize: androidx.compose.ui.unit.Dp
)

private fun rememberRatingsGridMetrics(displayModel: RatingsDisplayModel): RatingsGridMetrics {
    val rowCount = displayModel.rows.size
    val columnCount = displayModel.columnHeaders.size
    val scale = when {
        rowCount <= 2 || columnCount <= 2 -> 1.80f
        rowCount <= 3 || columnCount <= 3 -> 1.55f
        rowCount <= 4 || columnCount <= 4 -> 1.32f
        rowCount <= 6 && columnCount <= 6 -> 1.16f
        else -> 1f
    }
    return RatingsGridMetrics(
        cellWidth = CellWidth * scale,
        cellHeight = CellHeight * scale,
        rowHeaderWidth = RowHeaderWidth * scale.coerceAtMost(1.35f),
        gridSpacing = if (scale > 1.6f) 8.dp else if (scale > 1.3f) 7.dp else if (scale > 1f) 5.dp else 4.dp,
        cellPadding = if (scale > 1.3f) 5.dp else if (scale > 1f) 4.dp else 3.dp,
        summaryBarWidth = if (scale > 1.6f) 20.dp else if (scale > 1.3f) 18.dp else if (scale > 1f) 14.dp else 12.dp,
        summaryBarWidthEpisodesAcross = if (scale > 1.6f) 22.dp else if (scale > 1.3f) 20.dp else if (scale > 1f) 16.dp else 14.dp,
        summaryBarHeight = if (scale > 1.3f) 4.dp else if (scale > 1f) 3.dp else 2.dp,
        currentSeasonIconSize = if (scale > 1.6f) 20.dp else if (scale > 1.3f) 18.dp else 14.dp,
        unreleasedIconBoxSize = if (scale > 1.6f) 24.dp else if (scale > 1.3f) 22.dp else 18.dp,
        unreleasedIconSize = if (scale > 1.6f) 16.dp else if (scale > 1.3f) 14.dp else 12.dp
    )
}

@Composable
fun EpisodeRatingsOverlayDialog(
    meta: Meta,
    episodes: List<Video>,
    ratings: Map<Pair<Int, Int>, Double>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    backdropModel: Any? = meta.backdropUrl
) {
    val chartData = remember(episodes, ratings) {
        buildEpisodeRatingsChartData(episodes = episodes, ratings = ratings)
    }
    var layoutMode by rememberSaveable(meta.id) { mutableStateOf(RatingsLayoutMode.EPISODES_ACROSS) }

    when {
        isLoading -> {
            EpisodeRatingsOverlayMessageDialog(
                title = meta.name,
                backdropModel = backdropModel,
                message = stringResource(R.string.ratings_loading),
                onDismiss = onDismiss
            )
        }
        error != null -> {
            EpisodeRatingsOverlayMessageDialog(
                title = meta.name,
                backdropModel = backdropModel,
                message = error,
                onDismiss = onDismiss
            )
        }
        chartData.displaySeasonNumbers.isEmpty() -> {
            EpisodeRatingsOverlayMessageDialog(
                title = meta.name,
                backdropModel = backdropModel,
                message = stringResource(R.string.ratings_unavailable),
                onDismiss = onDismiss
            )
        }
        else -> {
            EpisodeRatingsOverlay(
                meta = meta,
                chartData = chartData,
                backdropModel = backdropModel,
                layoutMode = layoutMode,
                onLayoutModeChanged = { layoutMode = it },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun EpisodeRatingsBackdrop(backdropModel: Any?) {
    if (backdropModel != null) {
        AsyncImage(
            model = backdropModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
    )
}

@Composable
private fun OverlayHeaderBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(OverlayShape)
            .background(NuvioColors.Surface.copy(alpha = 0.60f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), OverlayShape)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RatingsCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.shape(RoundedCornerShape(999.dp)),
        border = ButtonDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, NuvioColors.FocusRing),
                shape = RoundedCornerShape(999.dp)
            )
        ),
        colors = ButtonDefaults.colors(
            containerColor = NuvioColors.BackgroundCard.copy(alpha = 0.92f),
            focusedContainerColor = Color.White,
            contentColor = NuvioColors.TextPrimary,
            focusedContentColor = Color.Black
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 3.dp
        )
    ) {
        Text(
            text = stringResource(R.string.ratings_close_overlay),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRatingsOverlayMessageDialog(
    title: String,
    backdropModel: Any?,
    message: String,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val closeRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            EpisodeRatingsBackdrop(backdropModel = backdropModel)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OverlayHeaderBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            color = NuvioColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        RatingsCloseButton(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(closeRequester)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(OverlayShape)
                        .background(NuvioColors.Surface.copy(alpha = 0.60f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), OverlayShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                        color = NuvioColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRatingsOverlay(
    meta: Meta,
    chartData: EpisodeRatingsChartData,
    backdropModel: Any?,
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
        Box(modifier = Modifier.fillMaxSize()) {
            EpisodeRatingsBackdrop(backdropModel = backdropModel)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OverlayHeaderBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = meta.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            color = NuvioColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .onFocusChanged { toggleFocused = it.isFocused }
                            ) {
                                Text(
                                    text = "Inverted",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                    color = NuvioColors.TextSecondary
                                )
                                Switch(
                                    checked = layoutMode == RatingsLayoutMode.SEASONS_ACROSS,
                                    onCheckedChange = { checked ->
                                        onLayoutModeChanged(
                                            if (checked) {
                                                RatingsLayoutMode.SEASONS_ACROSS
                                            } else {
                                                RatingsLayoutMode.EPISODES_ACROSS
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
                            RatingsCloseButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .focusRequester(closeRequester)
                                    .focusProperties {
                                        up = toggleRequester
                                        down = firstCellFocusRequester ?: Cancel
                                    }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        RatingLegendStrip(modifier = Modifier.fillMaxWidth())
                    }
                }

                RatingsGridPanel(
                    displayModel = displayModel,
                    layoutMode = layoutMode,
                    focusedEpisodeId = focusedEpisodeId,
                    onEpisodeFocused = { focusedEpisodeId = it },
                    focusRequesters = focusRequesters,
                    upFocusRequester = closeRequester,
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
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Start,
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
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(item.color!!)
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = NuvioColors.TextSecondary
                )
            }
            if (index < 5) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                CurrentSeasonClockIcon(
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Current",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                color = NuvioColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusClockBadge(
                iconTint = Color.White,
                containerColor = ColorMutedCell,
                modifier = Modifier.size(18.dp),
                iconSize = 13.dp
            )
            Text(
                text = "Unreleased",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                color = NuvioColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
private fun HeaderBadge(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = NuvioColors.TextPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    contentPadding: androidx.compose.ui.unit.Dp = 3.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentPadding, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = fontSize),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
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

@Composable
private fun CurrentSeasonClockIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.AccessTime,
        contentDescription = null,
        tint = Color.Black,
        modifier = modifier
    )
}

@Composable
private fun StatusClockBadge(
    iconTint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 10.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SummaryCell(
    cell: RatingsDisplayCell,
    metrics: RatingsGridMetrics,
    isEpisodesAcross: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(width = metrics.cellWidth, height = metrics.cellHeight)
            .padding(horizontal = metrics.cellPadding, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = cell.ratingLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = NuvioColors.TextSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .width(
                        if (isEpisodesAcross) {
                            metrics.summaryBarWidthEpisodesAcross
                        } else {
                            metrics.summaryBarWidth
                        }
                    )
                    .height(metrics.summaryBarHeight.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(99.dp))
                    .background(cell.backgroundColor)
            )
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
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
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
        LegendRow(
            LegendItem(
                iconColor = ColorCurrentSeason,
                label = stringResource(R.string.ratings_warning_current_season)
            )
        )
        LegendRow(
            LegendItem(
                iconColor = NuvioColors.TextSecondary,
                iconContainerColor = ColorMutedCell,
                label = stringResource(R.string.ratings_warning_upcoming_season)
            )
        )
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
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(item.color)
            )
        } else if (item.iconColor != null && item.iconContainerColor != null) {
            StatusClockBadge(
                iconTint = item.iconColor,
                containerColor = item.iconContainerColor,
                modifier = Modifier.size(16.dp),
                iconSize = 12.dp
            )
        } else if (item.iconColor != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                CurrentSeasonClockIcon(modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
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
    val metrics = remember(displayModel) { rememberRatingsGridMetrics(displayModel) }

    Column(
        modifier = modifier
            .background(NuvioColors.Surface.copy(alpha = 0.60f), PanelShape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), PanelShape)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = GridContentPadding, end = GridContentPadding, top = GridContentPadding, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderBadge(
                label = displayModel.leadingHeader,
                modifier = Modifier
                    .width(metrics.rowHeaderWidth)
                    .height(metrics.cellHeight),
                fontSize = 15.sp,
                contentPadding = metrics.cellPadding
            )
            Spacer(modifier = Modifier.width(metrics.gridSpacing))

            Row(
                modifier = Modifier.horizontalScroll(horizontalScrollState),
                horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing)
            ) {
                displayModel.columnHeaders.forEach { header ->
                    HeaderBadge(
                        label = header.label,
                        modifier = Modifier
                            .width(metrics.cellWidth)
                            .height(metrics.cellHeight),
                        fontSize = 16.sp,
                        contentPadding = metrics.cellPadding
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = GridContentPadding, end = GridContentPadding, bottom = GridContentPadding)
        ) {
            Column(
                modifier = Modifier
                    .width(metrics.rowHeaderWidth)
                    .verticalScroll(verticalScrollState)
                    .padding(end = metrics.gridSpacing)
            ) {
                Column(modifier = Modifier.padding(vertical = metrics.gridSpacing)) {
                    displayModel.rows.forEachIndexed { rowIndex, row ->
                        Box(
                            modifier = Modifier
                                .width(metrics.rowHeaderWidth)
                                .height(metrics.cellHeight)
                                .padding(bottom = if (rowIndex == displayModel.rows.lastIndex) 0.dp else metrics.gridSpacing),
                            contentAlignment = Alignment.Center
                        ) {
                            HeaderBadge(
                                label = row.label,
                                modifier = Modifier.fillMaxSize(),
                                color = NuvioColors.TextSecondary,
                                fontSize = 16.sp,
                                contentPadding = metrics.cellPadding
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                        .padding(vertical = metrics.gridSpacing)
                ) {
                    displayModel.rows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier
                                .height(metrics.cellHeight)
                                .padding(bottom = if (rowIndex == displayModel.rows.lastIndex) 0.dp else metrics.gridSpacing),
                            horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing)
                        ) {
                            row.cells.forEachIndexed { columnIndex, cell ->
                                when {
                                    cell.state == EpisodeRatingCellState.SUMMARY -> {
                                        SummaryCell(
                                            cell = cell,
                                            metrics = metrics,
                                            isEpisodesAcross = layoutMode == RatingsLayoutMode.EPISODES_ACROSS
                                        )
                                    }
                                    cell.episodeId == null -> {
                                        Box(modifier = Modifier.size(width = metrics.cellWidth, height = metrics.cellHeight))
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
                                                    val resolvedUp =
                                                        upCell?.episodeId?.let(focusRequesters::get)
                                                            ?: upFocusRequester
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
                                            scale = CardDefaults.scale(focusedScale = 1f)
                                        ) {
                                            if (focusedEpisodeId == episodeId) {
                                                LaunchedEffect(episodeId) {
                                                    bringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(width = metrics.cellWidth, height = metrics.cellHeight)
                                                    .padding(metrics.cellPadding),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                when (cell.state) {
                                                    EpisodeRatingCellState.RATED -> {
                                                        Text(
                                                            text = cell.ratingLabel,
                                                            style = MaterialTheme.typography.titleSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 18.sp
                                                            ),
                                                            color = if (cell.useDarkText) Color(0xFF1D1D1F) else Color.White,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    EpisodeRatingCellState.UNRATED -> {
                                                        Text(
                                                            text = "—",
                                                            style = MaterialTheme.typography.titleSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 18.sp
                                                            ),
                                                            color = NuvioColors.TextSecondary,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    EpisodeRatingCellState.UNAIRED -> {
                                                        StatusClockBadge(
                                                            iconTint = Color.White,
                                                            containerColor = ColorMutedCell,
                                                            modifier = Modifier.size(metrics.unreleasedIconBoxSize),
                                                            iconSize = metrics.unreleasedIconSize
                                                        )
                                                    }
                                                    EpisodeRatingCellState.SUMMARY -> Unit
                                                }

                                                if (cell.showCurrentSeasonBadge) {
                                                    CurrentSeasonClockIcon(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(top = 1.dp, end = 1.dp)
                                                            .size(metrics.currentSeasonIconSize)
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
    val iconColor: Color? = null,
    val iconContainerColor: Color? = null
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
