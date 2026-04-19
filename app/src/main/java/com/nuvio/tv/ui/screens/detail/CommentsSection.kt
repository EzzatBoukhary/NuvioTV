package com.nuvio.tv.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.domain.model.TraktCommentReview
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioColors
import com.nuvio.tv.ui.util.localizeEpisodeTitle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun CommentsSection(
    comments: List<TraktCommentReview>,
    contentTitle: String,
    showTraktSource: Boolean,
    commentsSource: CommentsSource,
    commentsContextTitle: String?,
    commentsContextSubtitle: String?,
    commentsMode: CommentsMode,
    canToggleEpisodeComments: Boolean,
    traktSourceFocusRequester: FocusRequester? = null,
    redditSourceFocusRequester: FocusRequester? = null,
    titleModeFocusRequester: FocusRequester? = null,
    episodeModeFocusRequester: FocusRequester? = null,
    selectedEpisode: Video?,
    allEpisodes: List<Video>,
    watchedEpisodes: Set<Pair<Int, Int>>,
    nextToWatch: NextToWatch?,
    selectedSeason: Int?,
    availableSeasons: List<Int>,
    entryFocusToken: Int = 0,
    onEntryFocusHandled: () -> Unit = {},
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    upFocusRequester: FocusRequester? = null,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentsSourceSelected: (CommentsSource) -> Unit,
    onCommentsModeSelected: (CommentsMode) -> Unit,
    onEpisodeSelected: (Video) -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(16.dp)
    val firstItemFocusRequester = remember { FocusRequester() }
    val internalTraktSourceFocusRequester = remember { FocusRequester() }
    val internalRedditSourceFocusRequester = remember { FocusRequester() }
    val internalTitleModeFocusRequester = remember { FocusRequester() }
    val internalEpisodeModeFocusRequester = remember { FocusRequester() }
    val resolvedTraktSourceFocusRequester = traktSourceFocusRequester ?: internalTraktSourceFocusRequester
    val resolvedRedditSourceFocusRequester = redditSourceFocusRequester ?: internalRedditSourceFocusRequester
    val resolvedTitleModeFocusRequester = titleModeFocusRequester ?: internalTitleModeFocusRequester
    val resolvedEpisodeModeFocusRequester = episodeModeFocusRequester ?: internalEpisodeModeFocusRequester
    val commentFocusRequesters = remember(comments) { mutableMapOf<Long, FocusRequester>() }
    val listState = rememberLazyListState()
    var showEpisodePicker by remember { mutableStateOf(false) }
    var expandedSelector by remember { mutableStateOf<CommentsSelectorMenu?>(null) }
    var focusedSourceMenuItem by remember { mutableStateOf<String?>(null) }
    var focusedScopeMenuItem by remember { mutableStateOf<String?>(null) }
    var pickerSeason by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastFocusedCommentId by rememberSaveable { mutableStateOf<Long?>(null) }
    val modeFocusRequester = if (commentsMode == CommentsMode.EPISODE) {
        resolvedEpisodeModeFocusRequester
    } else {
        resolvedTitleModeFocusRequester
    }
    val sourceFocusRequester = if (!showTraktSource || commentsSource == CommentsSource.REDDIT) {
        resolvedRedditSourceFocusRequester
    } else {
        resolvedTraktSourceFocusRequester
    }
    val controlsFocusRequester = sourceFocusRequester
    val visibleFirstCommentId = remember(comments, listState.firstVisibleItemIndex) {
        comments.getOrNull(max(listState.firstVisibleItemIndex, 0))?.id
    }
    val visibleWindowCommentIds = remember(comments, listState.layoutInfo.visibleItemsInfo) {
        listState.layoutInfo.visibleItemsInfo
            .mapNotNull { info -> comments.getOrNull(info.index)?.id }
            .toSet()
    }
    val commentsTargetFocusRequester = remember(
        comments,
        lastFocusedCommentId,
        controlsFocusRequester,
        visibleFirstCommentId,
        visibleWindowCommentIds
    ) {
        val targetId = when {
            lastFocusedCommentId != null && lastFocusedCommentId in visibleWindowCommentIds -> lastFocusedCommentId
            visibleFirstCommentId != null -> visibleFirstCommentId
            else -> comments.firstOrNull()?.id
        }
        targetId?.let { commentFocusRequesters.getOrPut(it) { FocusRequester() } } ?: firstItemFocusRequester
    }
    val sortedEpisodes = remember(allEpisodes) {
        allEpisodes.sortedWith(compareBy<Video>({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
    }
    val latestWatchedEpisode = remember(allEpisodes, watchedEpisodes) {
        watchedEpisodes
            .maxWithOrNull(compareBy<Pair<Int, Int>>({ it.first }, { it.second }))
            ?.let { latestWatched ->
                allEpisodes.firstOrNull { episode ->
                    episode.season == latestWatched.first && episode.episode == latestWatched.second
                }
            }
    }
    val nextToWatchEpisode = remember(allEpisodes, nextToWatch) {
        nextToWatch?.nextVideoId?.let { nextId ->
            allEpisodes.firstOrNull { it.id == nextId }
        } ?: nextToWatch?.let { target ->
            allEpisodes.firstOrNull { it.season == target.nextSeason && it.episode == target.nextEpisode }
        }
    }
    val episodeBeforeNextToWatch = remember(sortedEpisodes, nextToWatchEpisode) {
        val nextIndex = sortedEpisodes.indexOfFirst { it.id == nextToWatchEpisode?.id }
        if (nextIndex > 0) sortedEpisodes[nextIndex - 1] else null
    }
    val defaultEpisodeTarget = remember(
        allEpisodes,
        latestWatchedEpisode,
        episodeBeforeNextToWatch,
        nextToWatchEpisode,
        selectedEpisode,
        selectedSeason
    ) {
        selectedEpisode
            ?: latestWatchedEpisode
            ?: episodeBeforeNextToWatch
            ?: nextToWatchEpisode
            ?: allEpisodes.firstOrNull { it.season == selectedSeason }
            ?: allEpisodes.firstOrNull()
    }
    val pickerDefaultSeason = defaultEpisodeTarget?.season
        ?: selectedSeason
        ?: availableSeasons.firstOrNull()
    val pickerEpisodes = remember(allEpisodes, pickerSeason) {
        val season = pickerSeason
        if (season == null) {
            emptyList<Video>()
        } else {
            allEpisodes
                .filter { it.season == season }
                .sortedBy { it.episode }
        }
    }
    val upFocusModifier = if (upFocusRequester != null) {
        Modifier.focusProperties { up = upFocusRequester }
    } else {
        Modifier
    }
    LaunchedEffect(listState, comments.size, canLoadMore, isLoadingMore, isLoading, error) {
        if (isLoading || !error.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            canLoadMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) onLoadMore()
            }
    }

    LaunchedEffect(showEpisodePicker, pickerDefaultSeason) {
        if (showEpisodePicker) {
            pickerSeason = pickerDefaultSeason
        }
    }

    LaunchedEffect(commentsMode, selectedEpisode?.id, commentsSource) {
        lastFocusedCommentId = null
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(entryFocusToken) {
        if (entryFocusToken > 0) {
            sourceFocusRequester.requestFocusAfterFrames()
            onEntryFocusHandled()
        }
    }

    val openGoogleSearch: () -> Unit = {
        val url = buildGoogleDiscussionSearchUrl(
            contentTitle = contentTitle,
            commentsMode = commentsMode,
            selectedEpisode = selectedEpisode
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_comments_title),
                style = MaterialTheme.typography.titleLarge,
                color = NuvioColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .focusRestorer(sourceFocusRequester),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                CommentSelectorCard(
                    title = "Source",
                    value = if (commentsSource == CommentsSource.REDDIT || !showTraktSource) {
                        stringResource(R.string.detail_comments_source_reddit)
                    } else {
                        stringResource(R.string.detail_comments_source_trakt)
                    },
                    selected = true,
                    showIndicator = true,
                    width = 158.dp,
                    focusRequester = sourceFocusRequester,
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = commentsTargetFocusRequester,
                    leftFocusRequester = FocusRequester.Cancel,
                    rightFocusRequester = if (canToggleEpisodeComments) modeFocusRequester else FocusRequester.Cancel,
                    onClick = { expandedSelector = CommentsSelectorMenu.SOURCE }
                )
                DropdownMenu(
                    expanded = expandedSelector == CommentsSelectorMenu.SOURCE,
                    onDismissRequest = {
                        expandedSelector = null
                        focusedSourceMenuItem = null
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = NuvioColors.BackgroundCard,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, NuvioColors.Border)
                ) {
                    if (showTraktSource) {
                        val isFocused = focusedSourceMenuItem == "trakt"
                        val isSelected = commentsSource == CommentsSource.TRAKT
                        val itemTextColor = when {
                            isFocused -> NuvioColors.OnSecondary
                            else -> NuvioColors.TextPrimary
                        }
                        val itemBackgroundColor = when {
                            isFocused -> NuvioColors.Secondary
                            isSelected -> NuvioColors.FocusBackground
                            else -> Color.Transparent
                        }
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    color = itemBackgroundColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { state ->
                                    val hasFocus = state.isFocused || state.hasFocus
                                    focusedSourceMenuItem = if (hasFocus) "trakt" else if (focusedSourceMenuItem == "trakt") null else focusedSourceMenuItem
                                },
                            text = { androidx.compose.material3.Text(stringResource(R.string.detail_comments_source_trakt), color = itemTextColor) },
                            onClick = {
                                expandedSelector = null
                                focusedSourceMenuItem = null
                                onCommentsSourceSelected(CommentsSource.TRAKT)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = itemTextColor,
                                disabledTextColor = NuvioColors.TextDisabled
                            )
                        )
                    }
                    val isRedditFocused = focusedSourceMenuItem == "reddit"
                    val isRedditSelected = commentsSource == CommentsSource.REDDIT
                    val redditTextColor = when {
                        isRedditFocused -> NuvioColors.OnSecondary
                        else -> NuvioColors.TextPrimary
                    }
                    val redditBackgroundColor = when {
                        isRedditFocused -> NuvioColors.Secondary
                        isRedditSelected -> NuvioColors.FocusBackground
                        else -> Color.Transparent
                    }
                    DropdownMenuItem(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .background(
                                color = redditBackgroundColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .onFocusChanged { state ->
                                val hasFocus = state.isFocused || state.hasFocus
                                focusedSourceMenuItem = if (hasFocus) "reddit" else if (focusedSourceMenuItem == "reddit") null else focusedSourceMenuItem
                            },
                        text = { androidx.compose.material3.Text(stringResource(R.string.detail_comments_source_reddit), color = redditTextColor) },
                        onClick = {
                            expandedSelector = null
                            focusedSourceMenuItem = null
                            onCommentsSourceSelected(CommentsSource.REDDIT)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = redditTextColor,
                            disabledTextColor = NuvioColors.TextDisabled
                        )
                    )
                    val isGoogleFocused = focusedSourceMenuItem == "google"
                    val googleTextColor = if (isGoogleFocused) NuvioColors.OnSecondary else NuvioColors.TextPrimary
                    DropdownMenuItem(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .background(
                                color = if (isGoogleFocused) NuvioColors.Secondary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .onFocusChanged { state ->
                                val hasFocus = state.isFocused || state.hasFocus
                                focusedSourceMenuItem = if (hasFocus) "google" else if (focusedSourceMenuItem == "google") null else focusedSourceMenuItem
                            },
                        text = { androidx.compose.material3.Text(stringResource(R.string.detail_comments_search_google_external), color = googleTextColor) },
                        onClick = {
                            expandedSelector = null
                            focusedSourceMenuItem = null
                            openGoogleSearch()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = googleTextColor,
                            disabledTextColor = NuvioColors.TextDisabled
                        )
                    )
                }
            }

            if (canToggleEpisodeComments) {
                Box {
                    val scopeEpisodeLabel = defaultEpisodeTarget?.let { selectedEpisodeLabel(it) }
                        ?: "S--E--"
                    CommentSelectorCard(
                        title = "Scope",
                        value = if (commentsMode == CommentsMode.EPISODE) {
                            stringResource(R.string.detail_comments_mode_pick_episode, scopeEpisodeLabel)
                        } else {
                            stringResource(R.string.detail_comments_mode_show)
                        },
                        selected = true,
                        minWidth = 132.dp,
                        maxWidth = 420.dp,
                        focusRequester = modeFocusRequester,
                        upFocusRequester = upFocusRequester,
                        downFocusRequester = commentsTargetFocusRequester,
                        leftFocusRequester = sourceFocusRequester,
                        rightFocusRequester = FocusRequester.Cancel,
                        onClick = {
                            expandedSelector = CommentsSelectorMenu.SCOPE
                        }
                    )
                    DropdownMenu(
                        expanded = expandedSelector == CommentsSelectorMenu.SCOPE,
                        onDismissRequest = {
                            expandedSelector = null
                            focusedScopeMenuItem = null
                        },
                        modifier = Modifier
                            .width(340.dp)
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = NuvioColors.BackgroundCard,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, NuvioColors.Border)
                    ) {
                        val isShowFocused = focusedScopeMenuItem == "show"
                        val isShowSelected = commentsMode == CommentsMode.TITLE
                        val showTextColor = when {
                            isShowFocused -> NuvioColors.OnSecondary
                            else -> NuvioColors.TextPrimary
                        }
                        val showBackgroundColor = when {
                            isShowFocused -> NuvioColors.Secondary
                            isShowSelected -> NuvioColors.FocusBackground
                            else -> Color.Transparent
                        }
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    color = showBackgroundColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { state ->
                                    val hasFocus = state.isFocused || state.hasFocus
                                    focusedScopeMenuItem = if (hasFocus) "show" else if (focusedScopeMenuItem == "show") null else focusedScopeMenuItem
                                },
                            text = { androidx.compose.material3.Text(stringResource(R.string.detail_comments_mode_show), color = showTextColor) },
                            onClick = {
                                expandedSelector = null
                                focusedScopeMenuItem = null
                                onCommentsModeSelected(CommentsMode.TITLE)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = showTextColor,
                                disabledTextColor = NuvioColors.TextDisabled
                            )
                        )
                        val isPickFocused = focusedScopeMenuItem == "pick_episode"
                        val isPickSelected = commentsMode == CommentsMode.EPISODE
                        val pickTextColor = when {
                            isPickFocused -> NuvioColors.OnSecondary
                            else -> NuvioColors.TextPrimary
                        }
                        val pickBackgroundColor = when {
                            isPickFocused -> NuvioColors.Secondary
                            isPickSelected -> NuvioColors.FocusBackground
                            else -> Color.Transparent
                        }
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    color = pickBackgroundColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { state ->
                                    val hasFocus = state.isFocused || state.hasFocus
                                    focusedScopeMenuItem = if (hasFocus) "pick_episode" else if (focusedScopeMenuItem == "pick_episode") null else focusedScopeMenuItem
                                },
                            text = { androidx.compose.material3.Text(stringResource(R.string.detail_comments_mode_pick_episode, scopeEpisodeLabel), color = pickTextColor) },
                            onClick = {
                                expandedSelector = null
                                focusedScopeMenuItem = null
                                showEpisodePicker = true
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = pickTextColor,
                                disabledTextColor = NuvioColors.TextDisabled
                            )
                        )
                    }
                }
            }

        }
        Spacer(modifier = Modifier.height(10.dp))

        when {
            isLoading -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRestorer { firstItemFocusRequester },
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(3) { index ->
                        LoadingCommentCard(
                            shape = cardShape,
                            modifier = Modifier.then(
                                if (index == 0) {
                                    Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .then(
                                            if (canToggleEpisodeComments) {
                                                Modifier.focusProperties {
                                                    up = controlsFocusRequester
                                                }
                                            } else {
                                                Modifier.focusProperties {
                                                    up = controlsFocusRequester
                                                }
                                            }
                                        )
                                } else {
                                    Modifier.then(upFocusModifier)
                                }
                            )
                        )
                    }
                }
            }

            !error.isNullOrBlank() -> {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NuvioColors.TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .focusRequester(firstItemFocusRequester)
                                .focusProperties {
                                    up = controlsFocusRequester
                                },
                            colors = ButtonDefaults.colors(
                                containerColor = NuvioColors.BackgroundCard,
                                contentColor = NuvioColors.TextPrimary
                            )
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                        Button(
                            onClick = openGoogleSearch,
                            modifier = Modifier.focusProperties {
                                up = controlsFocusRequester
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = NuvioColors.BackgroundCard,
                                contentColor = NuvioColors.TextPrimary
                            )
                        ) {
                            Text(stringResource(R.string.detail_comments_search_google_external))
                        }
                    }
                }
            }

            comments.isEmpty() -> {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_comments_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NuvioColors.TextSecondary
                    )
                    Button(
                        onClick = openGoogleSearch,
                        modifier = Modifier
                            .focusRequester(firstItemFocusRequester)
                            .then(
                                Modifier.focusProperties {
                                    up = controlsFocusRequester
                                }
                            ),
                        colors = ButtonDefaults.colors(
                            containerColor = NuvioColors.BackgroundCard,
                            contentColor = NuvioColors.TextPrimary
                        )
                    ) {
                        Text(stringResource(R.string.detail_comments_search_google_external))
                    }
                }
            }

            else -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRestorer { firstItemFocusRequester },
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments, key = { it.id }) { review ->
                        val commentFocusRequester = commentFocusRequesters.getOrPut(review.id) {
                            FocusRequester()
                        }
                        CommentCard(
                            review = review,
                            shape = cardShape,
                            modifier = Modifier
                                .focusRequester(commentFocusRequester)
                                .then(
                                    if (canToggleEpisodeComments) {
                                        Modifier.focusProperties {
                                            up = controlsFocusRequester
                                        }
                                    } else {
                                        Modifier.focusProperties {
                                            up = controlsFocusRequester
                                        }
                                    }
                                )
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        lastFocusedCommentId = review.id
                                    }
                                },
                            onClick = { onCommentClick(review) }
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "loading_more_comments") {
                            LoadingCommentCard(shape = cardShape)
                        }
                    }
                }
            }
        }
    }

    if (showEpisodePicker && pickerEpisodes.isNotEmpty()) {
        EpisodeCommentPickerDialog(
            seasons = availableSeasons,
            episodes = pickerEpisodes,
            season = pickerSeason ?: pickerDefaultSeason,
            selectedEpisodeId = selectedEpisode?.id ?: defaultEpisodeTarget?.id,
            onDismiss = { showEpisodePicker = false },
            onSeasonSelected = { pickerSeason = it },
            onEpisodeSelected = {
                showEpisodePicker = false
                onEpisodeSelected(it)
            }
        )
    }
}

private fun buildGoogleDiscussionSearchUrl(
    contentTitle: String,
    commentsMode: CommentsMode,
    selectedEpisode: Video?
): String {
    val episodePart = if (
        commentsMode == CommentsMode.EPISODE &&
        selectedEpisode?.season != null &&
        selectedEpisode.episode != null
    ) {
        "S${selectedEpisode.season.toString().padStart(2, '0')}E${selectedEpisode.episode.toString().padStart(2, '0')}"
    } else {
        ""
    }

    val query = listOf("reddit", contentTitle, episodePart, "discussion")
        .filter { it.isNotBlank() }
        .joinToString(" ")

    return "https://www.google.com/search?q=${Uri.encode(query)}"
}

private fun selectedEpisodeLabel(video: Video): String {
    val season = video.season ?: 0
    val episode = video.episode ?: 0
    return "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
}

private enum class CommentsSelectorMenu {
    SOURCE,
    SCOPE
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeCommentPickerDialog(
    seasons: List<Int>,
    episodes: List<Video>,
    season: Int?,
    selectedEpisodeId: String?,
    onDismiss: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (Video) -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val selectedSeasonFocusRequester = remember { FocusRequester() }
    val selectedEpisodeFocusRequester = remember { FocusRequester() }
    val seasonListState = rememberLazyListState()
    val episodeListState = rememberLazyListState()
    val sortedSeasons = remember(seasons) {
        seasons.filter { it > 0 }.sorted() + seasons.filter { it == 0 }
    }

    LaunchedEffect(season, selectedEpisodeId, episodes, sortedSeasons) {
        season?.let { activeSeason ->
            val selectedSeasonIndex = sortedSeasons.indexOf(activeSeason)
            if (selectedSeasonIndex >= 0) {
                seasonListState.scrollToItem(selectedSeasonIndex)
            }
        }
        selectedEpisodeId?.let { activeEpisodeId ->
            val selectedEpisodeIndex = episodes.indexOfFirst { it.id == activeEpisodeId }
            if (selectedEpisodeIndex >= 0) {
                episodeListState.scrollToItem(selectedEpisodeIndex)
            }
        }
        withFrameNanos { }
        if (selectedEpisodeId != null && episodes.any { it.id == selectedEpisodeId }) {
            selectedEpisodeFocusRequester.requestFocus()
        } else {
            primaryFocusRequester.requestFocus()
        }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.detail_comments_episode_picker_title),
        subtitle = stringResource(R.string.detail_comments_episode_picker_subtitle),
        width = 560.dp,
        suppressFirstKeyUp = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (seasons.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = seasonListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedSeasons, key = { it }) { seasonNumber ->
                        val seasonModifier = if (seasonNumber == season) {
                            Modifier.focusRequester(selectedSeasonFocusRequester)
                        } else {
                            Modifier
                        }
                        Button(
                            onClick = { onSeasonSelected(seasonNumber) },
                            modifier = seasonModifier,
                            colors = ButtonDefaults.colors(
                                containerColor = if (seasonNumber == season) {
                                    NuvioColors.Secondary
                                } else {
                                    NuvioColors.BackgroundCard
                                },
                                contentColor = if (seasonNumber == season) {
                                    NuvioColors.OnSecondary
                                } else {
                                    NuvioColors.TextPrimary
                                }
                            )
                        ) {
                            Text(
                                text = if (seasonNumber == 0) {
                                    stringResource(R.string.episodes_specials)
                                } else {
                                    stringResource(R.string.episodes_season, seasonNumber)
                                }
                            )
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                state = episodeListState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(episodes, key = { it.id }) { episode ->
                    val episodeModifier = when {
                        episode.id == selectedEpisodeId -> Modifier
                            .fillMaxWidth()
                            .focusRequester(selectedEpisodeFocusRequester)
                        episode.id == episodes.firstOrNull()?.id -> Modifier
                            .fillMaxWidth()
                            .focusRequester(primaryFocusRequester)
                        else -> Modifier.fillMaxWidth()
                    }
                    Button(
                        onClick = { onEpisodeSelected(episode) },
                        modifier = episodeModifier,
                        colors = ButtonDefaults.colors(
                            containerColor = if (episode.id == selectedEpisodeId) {
                                NuvioColors.FocusBackground
                            } else {
                                NuvioColors.BackgroundCard
                            },
                            contentColor = NuvioColors.TextPrimary
                        )
                    ) {
                        Text(
                            text = "${selectedEpisodeLabel(episode)}  ${episode.title.localizeEpisodeTitle(androidx.compose.ui.platform.LocalContext.current)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentSelectorCard(
    title: String,
    value: String,
    selected: Boolean,
    showIndicator: Boolean = true,
    width: Dp? = null,
    minWidth: Dp = 132.dp,
    maxWidth: Dp = 360.dp,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = Modifier
            .then(
                if (width != null) {
                    Modifier.width(width)
                } else {
                    Modifier
                        .wrapContentWidth()
                        .widthIn(min = minWidth, max = maxWidth)
                }
            )
            .height(42.dp)
            .focusRequester(focusRequester)
            .focusProperties {
                if (upFocusRequester != null) {
                    up = upFocusRequester
                }
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
                if (leftFocusRequester != null) {
                    left = leftFocusRequester
                }
                if (rightFocusRequester != null) {
                    right = rightFocusRequester
                }
            }
            .onFocusChanged { state ->
                isFocused = state.isFocused
            },
        scale = ButtonDefaults.scale(
            focusedScale = 1f,
            pressedScale = 1f
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) NuvioColors.SurfaceVariant else NuvioColors.BackgroundCard,
            focusedContainerColor = NuvioColors.Secondary,
            contentColor = NuvioColors.TextPrimary,
            focusedContentColor = NuvioColors.OnSecondary
        )
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) NuvioColors.OnSecondary.copy(alpha = 0.9f) else NuvioColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) NuvioColors.OnSecondary.copy(alpha = 0.9f) else NuvioColors.TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) NuvioColors.OnSecondary else NuvioColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showIndicator) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isFocused) NuvioColors.OnSecondary.copy(alpha = 0.9f) else NuvioColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentCard(
    review: TraktCommentReview,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyText = if (review.hasSpoilerContent) {
        stringResource(R.string.detail_comments_spoiler_hidden)
    } else {
        review.comment
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(360.dp)
            .height(230.dp),
        colors = CardDefaults.colors(
            containerColor = NuvioColors.BackgroundCard,
            focusedContainerColor = NuvioColors.BackgroundCard
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, NuvioColors.FocusRing),
                shape = shape
            )
        ),
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = 1.02f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = review.authorDisplayName,
                style = MaterialTheme.typography.titleMedium,
                color = NuvioColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (review.review) {
                    CommentChip(text = stringResource(R.string.detail_comments_badge_review))
                }
                if (review.hasSpoilerContent) {
                    CommentChip(text = stringResource(R.string.detail_comments_badge_spoiler))
                }
                review.rating?.let { rating ->
                    CommentChip(text = stringResource(R.string.detail_comments_badge_rating, rating))
                }
            }

            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = NuvioColors.TextSecondary,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Text(
                text = stringResource(R.string.detail_comments_likes, review.likes),
                style = MaterialTheme.typography.labelMedium,
                color = NuvioColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommentChip(text: String) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .background(
                color = NuvioColors.BackgroundElevated,
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = NuvioColors.TextPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun CommentOverlay(
    review: TraktCommentReview,
    commentsSource: CommentsSource,
    commentsContextTitle: String?,
    commentsContextSubtitle: String?,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    isLoadingNext: Boolean,
    transitionDirection: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val mainContentFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF070707),
                            Color(0xFF101010),
                            Color(0xFF151515)
                        )
                    )
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (canNavigatePrevious) {
                                    onPrevious()
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (canNavigateNext) {
                                    if (!isLoadingNext) onNext()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
                }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            AnimatedContent(
                targetState = review,
                transitionSpec = {
                    when {
                        transitionDirection > 0 -> {
                            slideInHorizontally(initialOffsetX = { it / 5 }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it / 5 }) + fadeOut()
                        }
                        transitionDirection < 0 -> {
                            slideInHorizontally(initialOffsetX = { -it / 5 }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { it / 5 }) + fadeOut()
                        }
                        else -> fadeIn() togetherWith fadeOut()
                    }
                },
                label = "comment_overlay_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 10.dp)
            ) {
                currentReview ->
                CommentOverlayContent(
                    review = currentReview,
                    primaryFocusRequester = primaryFocusRequester,
                    mainContentFocusRequester = mainContentFocusRequester,
                    isLoadingNext = isLoadingNext
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(232.dp)
                    .padding(top = 6.dp, end = 4.dp)
                    .focusRequester(primaryFocusRequester)
                    .focusable()
                    .focusProperties {
                        down = mainContentFocusRequester
                    },
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (commentsSource == CommentsSource.TRAKT) {
                    Image(
                        painter = painterResource(id = R.drawable.trakt_logo_wordmark),
                        contentDescription = stringResource(R.string.cd_trakt_logo),
                        modifier = Modifier.width(168.dp),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f))
                    )
                } else {
                    Text(
                        text = stringResource(R.string.detail_comments_source_reddit),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
                if (commentsSource == CommentsSource.REDDIT && (!commentsContextSubtitle.isNullOrBlank() || !commentsContextTitle.isNullOrBlank())) {
                    Text(
                        text = buildString {
                            if (!commentsContextSubtitle.isNullOrBlank()) {
                                append(commentsContextSubtitle)
                            }
                            if (!commentsContextTitle.isNullOrBlank()) {
                                if (isNotBlank()) append("  •  ")
                                append(commentsContextTitle)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.56f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.detail_comments_back_hint),
                    modifier = Modifier.padding(start = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.34f)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CommentOverlayContent(
    review: TraktCommentReview,
    primaryFocusRequester: FocusRequester,
    mainContentFocusRequester: FocusRequester,
    isLoadingNext: Boolean
) {
    val commentScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isSpoilerRevealed by rememberSaveable(review.id) { mutableStateOf(!review.hasSpoilerContent) }
    val commentText = if (review.hasSpoilerContent && !isSpoilerRevealed) {
        stringResource(R.string.detail_comments_spoiler_hidden)
    } else {
        review.comment
    }
    val commentStyle = readerCommentStyle(commentText.length)
    val formattedCommentDate = remember(review.createdAt, review.updatedAt) {
        formatCommentTimestamp(review.createdAt, review.updatedAt)
    }
    val overlayLabels = buildList {
        if (review.review) add(stringResource(R.string.detail_comments_badge_review))
        if (review.hasSpoilerContent) add(stringResource(R.string.detail_comments_badge_spoiler))
        review.rating?.let { add(stringResource(R.string.detail_comments_badge_rating, it)) }
        formattedCommentDate?.let { add(it) }
    }

    LaunchedEffect(review.id) {
        mainContentFocusRequester.requestFocus()
        withFrameNanos { }
        commentScrollState.scrollTo(0)
        withFrameNanos { }
        commentScrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = review.authorDisplayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                review.authorUsername
                    ?.takeIf { it.isNotBlank() }
                    ?.let { username ->
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.62f)
                        )
                    }
                if (overlayLabels.isNotEmpty()) {
                    OverlayMetaRow(labels = overlayLabels)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(commentScrollState)
                    .focusRequester(mainContentFocusRequester)
                    .focusable()
                    .focusProperties {
                        up = primaryFocusRequester
                    }
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.DirectionDown && commentScrollState.value < commentScrollState.maxValue -> {
                                coroutineScope.launch {
                                    commentScrollState.animateScrollTo(
                                        (commentScrollState.value + 260).coerceAtMost(commentScrollState.maxValue)
                                    )
                                }
                                true
                            }
                            event.key == Key.DirectionUp && commentScrollState.value > 0 -> {
                                coroutineScope.launch {
                                    commentScrollState.animateScrollTo(
                                        (commentScrollState.value - 260).coerceAtLeast(0)
                                    )
                                }
                                true
                            }
                            !isSpoilerRevealed && (
                                event.key == Key.DirectionCenter ||
                                    event.key == Key.Enter ||
                                    event.key == Key.NumPadEnter
                                ) -> {
                                isSpoilerRevealed = true
                                true
                            }
                            else -> false
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = commentText,
                    style = commentStyle,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_comments_likes, review.likes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.56f)
                )
                if (review.hasSpoilerContent && !isSpoilerRevealed) {
                    Text(
                        text = stringResource(R.string.detail_comments_reveal_spoiler_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.62f)
                    )
                }
                if (isLoadingNext) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.42f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayMetaRow(labels: List<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.White.copy(alpha = 0.42f), CircleShape)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun readerCommentStyle(length: Int): TextStyle {
    val typography = MaterialTheme.typography
    return when {
        length <= 160 -> typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp)
        length <= 280 -> typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 38.sp)
        length <= 420 -> typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 31.sp)
        length <= 650 -> typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp)
        length <= 900 -> typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp)
        else -> typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp)
    }
}

private fun formatCommentTimestamp(createdAt: String?, updatedAt: String?): String? {
    val rawTimestamp = createdAt?.trim()?.takeIf { it.isNotBlank() }
        ?: updatedAt?.trim()?.takeIf { it.isNotBlank() }
        ?: return null

    val instant = runCatching {
        if (rawTimestamp.all { it.isDigit() }) {
            val epoch = rawTimestamp.toLong()
            val epochMillis = if (epoch < 100_000_000_000L) epoch * 1000L else epoch
            Instant.ofEpochMilli(epochMillis)
        } else {
            runCatching { Instant.parse(rawTimestamp) }.getOrElse {
                runCatching { OffsetDateTime.parse(rawTimestamp).toInstant() }.getOrElse {
                    LocalDateTime.parse(rawTimestamp).atZone(ZoneId.systemDefault()).toInstant()
                }
            }
        }
    }.getOrNull() ?: return null

    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

private fun commentMaxLines(length: Int): Int = when {
    length <= 160 -> 7
    length <= 280 -> 10
    length <= 420 -> 13
    length <= 650 -> 17
    length <= 900 -> 22
    else -> 28
}

@Composable
private fun LoadingCommentCard(
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(360.dp)
            .height(230.dp)
            .background(
                color = NuvioColors.BackgroundCard,
                shape = shape
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(18.dp)
                .background(NuvioColors.BackgroundElevated, shape = shape)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(24.dp)
                        .background(NuvioColors.BackgroundElevated, shape = shape)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(NuvioColors.BackgroundElevated, shape = shape)
        )
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .background(NuvioColors.BackgroundElevated, shape = shape)
        )
    }
}
