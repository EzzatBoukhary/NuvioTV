package com.nuvio.tv.core.tmdb

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbCollectionPart
import com.nuvio.tv.data.remote.api.TmdbDiscoverResult
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.CollectionCatalogSource
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.TMDB_PRESET_ADDON_ID
import com.nuvio.tv.domain.model.TMDB_PRESET_ADDON_NAME
import com.nuvio.tv.data.remote.api.TmdbWatchProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbPresetCatalogService @Inject constructor(
    private val tmdbApi: TmdbApi
) {
    private val watchProviderIndexCache = ConcurrentHashMap<String, Map<Int, TmdbWatchProvider>>()
    private val keywordIdsCache = ConcurrentHashMap<String, List<Int>>()

    suspend fun loadCatalog(
        source: CollectionCatalogSource,
        page: Int,
        language: String
    ): CatalogRow {
        val spec = parseSpec(source.catalogId)
        if (spec.kind == PresetKind.FRANCHISE) {
            return loadFranchiseCatalog(source, spec, language)
        }
        if (spec.kind == PresetKind.AWARD) {
            return loadAwardCatalog(source, spec, page, language)
        }

        val normalizedPage = page.coerceAtLeast(1)
        val normalizedLanguage = normalizeLanguage(language)
        val today = LocalDate.now().toString()
        val isMovie = source.type.equals("movie", ignoreCase = true)
        val upperDateBound = spec.endYear?.let { "$it-12-31" }
            ?: if (spec.rail == PresetRail.RECENT) today else null
        val withCast = if (spec.personRole == "actor") spec.personId?.toString() else null
        val withCrew = if (spec.personRole == "director") spec.personId?.toString() else null

        val body = if (isMovie) {
            tmdbApi.discoverMovies(
                apiKey = BuildConfig.TMDB_API_KEY,
                language = normalizedLanguage,
                page = normalizedPage,
                sortBy = movieSortBy(spec.rail),
                withCompanies = spec.companyId?.toString(),
                releaseDateLte = upperDateBound,
                voteCountGte = if (spec.rail == PresetRail.TOP_RATED) TOP_RATED_VOTE_COUNT_FLOOR else null,
                withGenres = spec.movieGenreId?.toString(),
                withWatchProviders = resolveProviderId(spec)?.toString(),
                watchRegion = if (spec.providerId != null || spec.name.isNotBlank()) WATCH_REGION else null,
                withCast = withCast,
                withCrew = withCrew,
                releaseDateGte = spec.startYear?.let { "$it-01-01" }
            ).body()
        } else {
            tmdbApi.discoverTv(
                apiKey = BuildConfig.TMDB_API_KEY,
                language = normalizedLanguage,
                page = normalizedPage,
                sortBy = tvSortBy(spec.rail),
                withCompanies = spec.companyId?.toString(),
                withNetworks = spec.networkId?.toString(),
                firstAirDateLte = upperDateBound,
                voteCountGte = if (spec.rail == PresetRail.TOP_RATED) TOP_RATED_VOTE_COUNT_FLOOR else null,
                withGenres = spec.tvGenreId?.toString(),
                withWatchProviders = resolveProviderId(spec)?.toString(),
                watchRegion = if (spec.providerId != null || spec.name.isNotBlank()) WATCH_REGION else null,
                withCast = withCast,
                withCrew = withCrew,
                firstAirDateGte = spec.startYear?.let { "$it-01-01" }
            ).body()
        }

        val results = body?.results.orEmpty()
        val items = results.mapNotNull { mapDiscoverResult(it, isMovie) }
        val totalPages = body?.totalPages ?: normalizedPage

        return CatalogRow(
            addonId = TMDB_PRESET_ADDON_ID,
            addonName = TMDB_PRESET_ADDON_NAME,
            addonBaseUrl = "",
            catalogId = source.catalogId,
            catalogName = describeCatalog(source),
            type = ContentType.fromString(source.type),
            items = items,
            isLoading = false,
            hasMore = normalizedPage < totalPages && items.isNotEmpty(),
            currentPage = normalizedPage,
            supportsSkip = false,
            skipStep = 1
        )
    }

    fun describeCatalog(source: CollectionCatalogSource): String {
        val spec = parseSpec(source.catalogId)
        if (spec.kind == PresetKind.FRANCHISE) {
            return if (spec.name.isBlank()) "Collection" else "Collection - ${spec.name}"
        }
        if (spec.kind == PresetKind.AWARD) {
            return if (spec.name.isBlank()) "Awards" else "Awards - ${spec.name}"
        }

        val media = if (source.type.equals("movie", ignoreCase = true)) "Movies" else "Series"
        val rail = when (spec.rail) {
            PresetRail.POPULAR -> "Popular"
            PresetRail.TOP_RATED -> "Top Rated"
            PresetRail.RECENT -> "Recent"
            PresetRail.TRENDING -> "Trending"
            PresetRail.RELEASE -> "Release Order"
        }
        return if (spec.name.isBlank()) {
            "$rail $media"
        } else {
            "$rail $media - ${spec.name}"
        }
    }

    private fun parseSpec(catalogId: String): PresetSpec {
        val parts = catalogId.split("|")
        if (parts.size < 3 || parts[0] != "tmdbpreset") {
            return PresetSpec(
                kind = PresetKind.CUSTOM,
                name = catalogId,
                rail = PresetRail.POPULAR
            )
        }

        return when (parts[1]) {
            "provider" -> PresetSpec(
                kind = PresetKind.PROVIDER,
                name = parts.getOrNull(2).orEmpty(),
                providerId = parts.getOrNull(3)?.toIntOrNull(),
                rail = parseRail(parts.getOrNull(4))
            )
            "genre" -> PresetSpec(
                kind = PresetKind.GENRE,
                name = parts.getOrNull(2).orEmpty(),
                movieGenreId = parts.getOrNull(3)?.toIntOrNull(),
                tvGenreId = parts.getOrNull(4)?.toIntOrNull(),
                rail = parseRail(parts.getOrNull(5))
            )
            "company" -> PresetSpec(
                kind = PresetKind.COMPANY,
                name = parts.getOrNull(2).orEmpty(),
                companyId = parts.getOrNull(3)?.toIntOrNull(),
                rail = parseRail(parts.getOrNull(4))
            )
            "network" -> PresetSpec(
                kind = PresetKind.NETWORK,
                name = parts.getOrNull(2).orEmpty(),
                networkId = parts.getOrNull(3)?.toIntOrNull(),
                rail = parseRail(parts.getOrNull(4))
            )
            "person" -> PresetSpec(
                kind = PresetKind.PERSON,
                name = parts.getOrNull(2).orEmpty(),
                personId = parts.getOrNull(3)?.toIntOrNull(),
                personRole = parts.getOrNull(4)?.lowercase(),
                rail = parseRail(parts.getOrNull(5))
            )
            "franchise" -> PresetSpec(
                kind = PresetKind.FRANCHISE,
                name = parts.getOrNull(2).orEmpty(),
                franchiseCollectionId = parts.getOrNull(3)?.toIntOrNull(),
                rail = parseRail(parts.getOrNull(4))
            )
            "award" -> PresetSpec(
                kind = PresetKind.AWARD,
                name = parts.getOrNull(2).orEmpty(),
                rail = parseRail(parts.getOrNull(4))
            )
            "decade" -> PresetSpec(
                kind = PresetKind.DECADE,
                name = parts.getOrNull(2).orEmpty(),
                startYear = parts.getOrNull(3)?.toIntOrNull(),
                endYear = parts.getOrNull(4)?.toIntOrNull(),
                rail = parseRail(parts.getOrNull(5))
            )
            else -> PresetSpec(
                kind = PresetKind.CUSTOM,
                name = parts.getOrNull(2).orEmpty(),
                rail = PresetRail.POPULAR
            )
        }
    }

    private fun parseRail(raw: String?): PresetRail {
        return when (raw?.lowercase()) {
            "top_rated" -> PresetRail.TOP_RATED
            "recent" -> PresetRail.RECENT
            "trending" -> PresetRail.TRENDING
            "release" -> PresetRail.RELEASE
            else -> PresetRail.POPULAR
        }
    }

    private suspend fun resolveProviderId(spec: PresetSpec): Int? {
        spec.providerId?.let { return it }
        val normalizedName = normalizeKey(spec.name)
        if (normalizedName.isBlank()) return null
        val providers = resolveWatchProvidersIndex()
        return providers.values.firstOrNull { provider ->
            val providerName = provider.providerName.orEmpty()
            normalizeKey(providerName) == normalizedName || providerAliases(providerName).any { normalizeKey(it) == normalizedName }
        }?.providerId
    }

    private suspend fun loadFranchiseCatalog(
        source: CollectionCatalogSource,
        spec: PresetSpec,
        language: String
    ): CatalogRow {
        val collectionId = spec.franchiseCollectionId
        if (collectionId == null) {
            return emptyCatalog(source, name = "Collection")
        }

        val normalizedLanguage = normalizeLanguage(language)
        val parts = tmdbApi.getCollectionDetails(
            collectionId = collectionId,
            apiKey = BuildConfig.TMDB_API_KEY,
            language = normalizedLanguage
        ).body()?.parts.orEmpty()

        val items = coroutineScope {
            parts.map { part ->
                async {
                    val popularity = runCatching {
                        tmdbApi.getMovieDetails(part.id, BuildConfig.TMDB_API_KEY, normalizedLanguage).body()?.popularity ?: 0.0
                    }.getOrDefault(0.0)
                    popularity to part
                }
            }.awaitAll()
                .sortedWith(
                    compareByDescending<Pair<Double, TmdbCollectionPart>> { it.first }
                        .thenByDescending { it.second.releaseDate ?: "" }
                )
                .mapNotNull { (_, part) -> mapCollectionPart(part) }
        }

        return CatalogRow(
            addonId = TMDB_PRESET_ADDON_ID,
            addonName = TMDB_PRESET_ADDON_NAME,
            addonBaseUrl = "",
            catalogId = source.catalogId,
            catalogName = "Collection",
            type = ContentType.fromString(source.type),
            items = items,
            isLoading = false,
            hasMore = false,
            currentPage = 1,
            supportsSkip = false,
            skipStep = 1
        )
    }

    private suspend fun loadAwardCatalog(
        source: CollectionCatalogSource,
        spec: PresetSpec,
        page: Int,
        language: String
    ): CatalogRow {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedLanguage = normalizeLanguage(language)
        val profile = resolveAwardProfile(spec.name)
        val isMovie = source.type.equals("movie", ignoreCase = true)
        if (isMovie && !profile.includeMovies) {
            return emptyCatalog(source, name = describeCatalog(source))
        }
        if (!isMovie && !profile.includeSeries) {
            return emptyCatalog(source, name = describeCatalog(source))
        }

        val keywordIds = resolveAwardKeywordIds(profile)
        if (keywordIds.isEmpty()) {
            return emptyCatalog(source, name = describeCatalog(source))
        }
        val withKeywords = keywordIds.takeIf { it.isNotEmpty() }?.joinToString(separator = "|")
        val response = if (isMovie) {
            tmdbApi.discoverMovies(
                apiKey = BuildConfig.TMDB_API_KEY,
                language = normalizedLanguage,
                page = normalizedPage,
                sortBy = movieSortBy(spec.rail),
                withKeywords = withKeywords,
                releaseDateGte = AWARD_RELEASE_DATE_FLOOR,
                voteCountGte = AWARD_VOTE_COUNT_FLOOR
            ).body()
        } else {
            tmdbApi.discoverTv(
                apiKey = BuildConfig.TMDB_API_KEY,
                language = normalizedLanguage,
                page = normalizedPage,
                sortBy = tvSortBy(spec.rail),
                withKeywords = withKeywords,
                firstAirDateGte = AWARD_RELEASE_DATE_FLOOR,
                voteCountGte = AWARD_VOTE_COUNT_FLOOR
            ).body()
        }

        val items = response?.results.orEmpty()
            .mapNotNull { mapDiscoverResult(it, isMovie) }
            .distinctBy { it.id }
        val totalPages = response?.totalPages ?: normalizedPage

        return CatalogRow(
            addonId = TMDB_PRESET_ADDON_ID,
            addonName = TMDB_PRESET_ADDON_NAME,
            addonBaseUrl = "",
            catalogId = source.catalogId,
            catalogName = describeCatalog(source),
            type = ContentType.fromString(source.type),
            items = items,
            isLoading = false,
            hasMore = normalizedPage < totalPages && items.isNotEmpty(),
            currentPage = normalizedPage,
            supportsSkip = false,
            skipStep = 1
        )
    }

    private suspend fun resolveAwardKeywordIds(profile: AwardProfile): List<Int> {
        val cacheKey = profile.cacheKey
        keywordIdsCache[cacheKey]?.let { return it }

        val keywordMatches = mutableListOf<Int>()
        profile.keywordQueries.forEach { query ->
            val results = runCatching {
                tmdbApi.searchKeywords(
                    apiKey = BuildConfig.TMDB_API_KEY,
                    query = query,
                    page = 1
                ).body()?.results.orEmpty()
            }.getOrDefault(emptyList())

            results.forEach { result ->
                val normalizedResult = normalizeKey(result.name.orEmpty())
                val matches = profile.matchTokens.any { token -> normalizedResult.contains(normalizeKey(token)) }
                if (matches) {
                    keywordMatches += result.id
                }
            }
        }

        val ids = keywordMatches.distinct()

        keywordIdsCache[cacheKey] = ids
        return ids
    }

    private fun resolveAwardProfile(name: String): AwardProfile {
        val normalized = normalizeKey(name)
        return when {
            normalized.contains("academy") || normalized.contains("oscar") -> AwardProfile(
                cacheKey = "oscars",
                keywordQueries = listOf("academy awards", "oscar winner", "best picture"),
                matchTokens = listOf("academy", "oscar", "best picture"),
                includeMovies = true,
                includeSeries = false
            )
            normalized.contains("goldenglobe") -> AwardProfile(
                cacheKey = "golden_globes",
                keywordQueries = listOf("golden globe", "golden globes winner"),
                matchTokens = listOf("golden globe"),
                includeMovies = true,
                includeSeries = true
            )
            normalized.contains("britishacademy") || normalized.contains("bafta") -> AwardProfile(
                cacheKey = "bafta",
                keywordQueries = listOf("bafta", "british academy film awards"),
                matchTokens = listOf("bafta", "british academy"),
                includeMovies = true,
                includeSeries = false
            )
            normalized.contains("cannes") -> AwardProfile(
                cacheKey = "cannes",
                keywordQueries = listOf("cannes", "palme d'or"),
                matchTokens = listOf("cannes", "palme"),
                includeMovies = true,
                includeSeries = false
            )
            normalized.contains("venice") -> AwardProfile(
                cacheKey = "venice",
                keywordQueries = listOf("venice film festival", "golden lion"),
                matchTokens = listOf("venice", "golden lion"),
                includeMovies = true,
                includeSeries = false
            )
            normalized.contains("spirit") || normalized.contains("independent") -> AwardProfile(
                cacheKey = "spirit_awards",
                keywordQueries = listOf("independent spirit awards", "film independent spirit"),
                matchTokens = listOf("spirit awards", "film independent"),
                includeMovies = true,
                includeSeries = false
            )
            else -> AwardProfile(
                cacheKey = normalized.ifBlank { "awards" },
                keywordQueries = listOf(name),
                matchTokens = listOf(name),
                includeMovies = true,
                includeSeries = true
            )
        }
    }

    private fun mapCollectionPart(part: TmdbCollectionPart): MetaPreview? {
        val title = part.title?.takeIf { it.isNotBlank() } ?: return null
        val poster = buildImageUrl(part.posterPath, "w500")
            ?: buildImageUrl(part.backdropPath, "w780")
            ?: return null
        return MetaPreview(
            id = "tmdb:${part.id}",
            type = ContentType.MOVIE,
            name = title,
            poster = poster,
            posterShape = PosterShape.POSTER,
            background = buildImageUrl(part.backdropPath, "w1280"),
            logo = null,
            description = part.overview?.takeIf { it.isNotBlank() },
            releaseInfo = part.releaseDate?.take(4),
            imdbRating = part.voteAverage?.toFloat(),
            genres = emptyList()
        )
    }

    private fun emptyCatalog(source: CollectionCatalogSource, name: String): CatalogRow {
        return CatalogRow(
            addonId = TMDB_PRESET_ADDON_ID,
            addonName = TMDB_PRESET_ADDON_NAME,
            addonBaseUrl = "",
            catalogId = source.catalogId,
            catalogName = name,
            type = ContentType.fromString(source.type),
            items = emptyList(),
            isLoading = false,
            hasMore = false,
            currentPage = 1,
            supportsSkip = false,
            skipStep = 1
        )
    }

    private fun mapDiscoverResult(result: TmdbDiscoverResult, isMovie: Boolean): MetaPreview? {
        val title = result.title?.takeIf { it.isNotBlank() }
            ?: result.name?.takeIf { it.isNotBlank() }
            ?: result.originalTitle?.takeIf { it.isNotBlank() }
            ?: result.originalName?.takeIf { it.isNotBlank() }
            ?: return null

        val poster = buildImageUrl(result.posterPath, "w500")
            ?: buildImageUrl(result.backdropPath, "w780")
            ?: return null
        val background = buildImageUrl(result.backdropPath, "w1280")
        val releaseInfo = if (isMovie) result.releaseDate?.take(4) else result.firstAirDate?.take(4)

        return MetaPreview(
            id = "tmdb:${result.id}",
            type = if (isMovie) ContentType.MOVIE else ContentType.SERIES,
            name = title,
            poster = poster,
            posterShape = PosterShape.POSTER,
            background = background,
            logo = null,
            description = result.overview?.takeIf { it.isNotBlank() },
            releaseInfo = releaseInfo,
            imdbRating = result.voteAverage?.toFloat(),
            genres = emptyList()
        )
    }

    private fun movieSortBy(rail: PresetRail): String = when (rail) {
        PresetRail.POPULAR -> "popularity.desc"
        PresetRail.TOP_RATED -> "vote_average.desc"
        PresetRail.RECENT -> "primary_release_date.desc"
        PresetRail.TRENDING -> "popularity.desc"
        PresetRail.RELEASE -> "primary_release_date.asc"
    }

    private fun tvSortBy(rail: PresetRail): String = when (rail) {
        PresetRail.POPULAR -> "popularity.desc"
        PresetRail.TOP_RATED -> "vote_average.desc"
        PresetRail.RECENT -> "first_air_date.desc"
        PresetRail.TRENDING -> "popularity.desc"
        PresetRail.RELEASE -> "first_air_date.asc"
    }

    private fun normalizeLanguage(raw: String?): String {
        val value = raw?.trim()?.replace('_', '-')?.takeIf { it.isNotBlank() } ?: return "en"
        val parts = value.split("-")
        return if (parts.size == 2) {
            "${parts[0].lowercase()}-${parts[1].uppercase()}"
        } else {
            value.lowercase()
        }
    }

    private fun buildImageUrl(path: String?, size: String): String? {
        val clean = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return "https://image.tmdb.org/t/p/$size$clean"
    }

    private suspend fun resolveWatchProvidersIndex(): Map<Int, TmdbWatchProvider> {
        watchProviderIndexCache[WATCH_REGION]?.let { return it }
        val movieProviders = tmdbApi.getMovieWatchProviders(BuildConfig.TMDB_API_KEY, WATCH_REGION).body()?.results.orEmpty()
        val tvProviders = tmdbApi.getTvWatchProviders(BuildConfig.TMDB_API_KEY, WATCH_REGION).body()?.results.orEmpty()
        val index = (movieProviders + tvProviders)
            .distinctBy { it.providerId }
            .associateBy { it.providerId }
        watchProviderIndexCache[WATCH_REGION] = index
        return index
    }

    private fun normalizeKey(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]+"), "")
    }

    private fun providerAliases(value: String): List<String> {
        return when (normalizeKey(value)) {
            "hbomax", "hbo" -> listOf("max", "hbo max")
            "appletv", "appletvplus" -> listOf("apple tv", "apple tv+")
            "discoveryplus" -> listOf("discovery+", "discovery plus")
            "curiositystream" -> listOf("curiosity stream")
            "magllentv", "magellantv" -> listOf("magellan tv")
            "primevideo" -> listOf("prime video")
            "skyshowtime" -> listOf("sky showtime")
            else -> emptyList()
        }
    }

    private data class PresetSpec(
        val kind: PresetKind,
        val name: String,
        val providerId: Int? = null,
        val movieGenreId: Int? = null,
        val tvGenreId: Int? = null,
        val companyId: Int? = null,
        val networkId: Int? = null,
        val personId: Int? = null,
        val personRole: String? = null,
        val franchiseCollectionId: Int? = null,
        val startYear: Int? = null,
        val endYear: Int? = null,
        val rail: PresetRail
    )

    private data class AwardProfile(
        val cacheKey: String,
        val keywordQueries: List<String>,
        val matchTokens: List<String>,
        val includeMovies: Boolean,
        val includeSeries: Boolean
    )

    private enum class PresetKind {
        PROVIDER,
        GENRE,
        COMPANY,
        NETWORK,
        PERSON,
        FRANCHISE,
        AWARD,
        DECADE,
        CUSTOM
    }

    private enum class PresetRail {
        POPULAR,
        TOP_RATED,
        RECENT,
        TRENDING,
        RELEASE
    }

    companion object {
        private const val TOP_RATED_VOTE_COUNT_FLOOR = 120
        private const val AWARD_VOTE_COUNT_FLOOR = 80
        private const val AWARD_RELEASE_DATE_FLOOR = "1950-01-01"
        private const val WATCH_REGION = "US"
    }
}
