package com.nuvio.tv.core.tmdb

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbWatchProvider
import com.nuvio.tv.domain.model.Collection
import com.nuvio.tv.domain.model.CollectionCatalogSource
import com.nuvio.tv.domain.model.CollectionFolder
import com.nuvio.tv.domain.model.CollectionPresets
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetArtworkService @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val tmdbPresetCatalogService: TmdbPresetCatalogService
) {
    private val deprecatedPresetIds = setOf(
        CollectionPresets.NETWORKS_ID,
        CollectionPresets.AWARDS_ID
    )
    private val watchProviderIndexCache = ConcurrentHashMap<String, Map<Int, TmdbWatchProvider>>()
    private val defaultPresetFolders by lazy {
        CollectionPresets.defaults()
            .asSequence()
            .flatMap { it.folders.asSequence() }
            .associateBy { it.id }
    }

    suspend fun normalizeAndEnrich(collections: List<Collection>, language: String = "en-US"): List<Collection> {
        return coroutineScope {
            collections
                .filterNot { it.id in deprecatedPresetIds }
                .map { collection ->
                async {
                    if (!CollectionPresets.presetIds.contains(collection.id)) {
                        collection
                    } else {
                        enrichCollection(collection, language)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun enrichCollection(collection: Collection, language: String): Collection {
        val updatedFolders = collection.folders.map { folder ->
            enrichFolder(folder, language)
        }
        return collection.copy(folders = updatedFolders)
    }

    private suspend fun enrichFolder(folder: CollectionFolder, language: String): CollectionFolder {
        val defaultFolder = defaultPresetFolders[folder.id]
        val defaultCover = defaultFolder?.coverImageUrl
        val cover = when {
            folder.id.startsWith("service_") -> defaultCover ?: folder.coverImageUrl
            folder.id.startsWith("studio_") -> defaultCover ?: folder.coverImageUrl
            folder.id.startsWith("director_") || folder.id.startsWith("actor_") -> defaultCover
                ?: resolvePersonArtwork(folder)?.cover
                ?: folder.coverImageUrl
            folder.id.startsWith("franchise_") -> defaultCover
                ?: resolveFranchiseArtwork(folder)?.cover
                ?: folder.coverImageUrl
            !folder.coverImageUrl.isNullOrBlank() && !folder.coverImageUrl.contains("placehold.co") -> folder.coverImageUrl
            defaultCover?.isNotBlank() == true -> defaultCover
            folder.id.startsWith("network_") -> resolveNetworkArtwork(folder)?.cover
            else -> null
        }

        if (cover == null && folder.focusGifUrl == null) return folder
        return folder.copy(
            coverImageUrl = cover ?: folder.coverImageUrl,
            focusGifUrl = null
        )
    }

    private suspend fun resolveProviderArtwork(folder: CollectionFolder, language: String): ResolvedArtwork? {
        val provider = resolveWatchProvider(folder.title)
        val cover = provider?.logoPath?.let { tmdbImageUrl(it, "w500") }
        return ResolvedArtwork(cover = cover, focus = cover)
    }

    private suspend fun resolveNetworkArtwork(folder: CollectionFolder): ResolvedArtwork? {
        val networkId = extractCatalogId(folder, kind = "network") ?: return null
        val network = runCatching { tmdbApi.getNetworkDetails(networkId, BuildConfig.TMDB_API_KEY).body() }.getOrNull()
        val cover = network?.logoPath?.let { tmdbImageUrl(it, "w500") }
        return ResolvedArtwork(cover = cover, focus = cover)
    }

    private suspend fun resolveCompanyArtwork(folder: CollectionFolder, language: String): ResolvedArtwork? {
        val companyId = extractCatalogId(folder, kind = "company") ?: return null
        val company = runCatching { tmdbApi.getCompanyDetails(companyId, BuildConfig.TMDB_API_KEY).body() }.getOrNull()
        val cover = company?.logoPath?.let { tmdbImageUrl(it, "w500") }
        return ResolvedArtwork(cover = cover, focus = cover)
    }

    private suspend fun resolveCatalogArtwork(folder: CollectionFolder, language: String): ResolvedArtwork? {
        val candidateSources = prioritizeArtworkSources(folder.catalogSources)
        candidateSources.forEach { source ->
            val row = runCatching {
                tmdbPresetCatalogService.loadCatalog(source, page = 1, language = language)
            }.getOrNull() ?: return@forEach
            val firstItem = row.items.firstOrNull() ?: return@forEach
            val cover = firstItem.background ?: firstItem.poster
            val focus = firstItem.background ?: firstItem.poster
            if (!cover.isNullOrBlank()) {
                return ResolvedArtwork(cover = cover, focus = focus)
            }
        }
        return null
    }

    private fun prioritizeArtworkSources(sources: List<CollectionCatalogSource>): List<CollectionCatalogSource> {
        return sources.sortedWith(
            compareBy<CollectionCatalogSource> {
                when {
                    it.type.equals("movie", ignoreCase = true) && it.catalogId.contains("popular") -> 0
                    it.type.equals("series", ignoreCase = true) && it.catalogId.contains("popular") -> 1
                    it.type.equals("movie", ignoreCase = true) -> 2
                    else -> 3
                }
            }
        )
    }

    private suspend fun resolvePersonArtwork(folder: CollectionFolder): ResolvedArtwork? {
        val personId = extractCatalogId(folder, kind = "person") ?: return null
        val person = runCatching { tmdbApi.getPersonDetails(personId, BuildConfig.TMDB_API_KEY, "en-US").body() }.getOrNull()
        val cover = person?.profilePath?.let { tmdbImageUrl(it, "w500") }
        return ResolvedArtwork(cover = cover, focus = cover)
    }

    private suspend fun resolveFranchiseArtwork(folder: CollectionFolder): ResolvedArtwork? {
        val collectionId = extractCatalogId(folder, kind = "franchise") ?: return null
        val collection = runCatching { tmdbApi.getCollectionDetails(collectionId, BuildConfig.TMDB_API_KEY, "en-US").body() }.getOrNull()
        val cover = collection?.posterPath?.let { tmdbImageUrl(it, "w500") }
            ?: collection?.backdropPath?.let { tmdbImageUrl(it, "w780") }
        val focus = collection?.backdropPath?.let { tmdbImageUrl(it, "w1280") }
            ?: cover
        return ResolvedArtwork(cover = cover, focus = focus)
    }

    private suspend fun resolveWatchProvider(providerName: String): TmdbWatchProvider? {
        val normalizedName = normalizeKey(providerName)
        if (normalizedName.isBlank()) return null
        val providers = resolveWatchProvidersIndex()
        return providers.values.firstOrNull { provider ->
            val candidate = provider.providerName.orEmpty()
            normalizeKey(candidate) == normalizedName || providerAliases(candidate).any { normalizeKey(it) == normalizedName }
        }
    }

    private suspend fun resolveWatchProvidersIndex(): Map<Int, TmdbWatchProvider> = coroutineScope {
        watchProviderIndexCache["US"]?.let { return@coroutineScope it }
        val movieDeferred = async {
            runCatching { tmdbApi.getMovieWatchProviders(BuildConfig.TMDB_API_KEY, "US").body()?.results.orEmpty() }
                .getOrDefault(emptyList())
        }
        val tvDeferred = async {
            runCatching { tmdbApi.getTvWatchProviders(BuildConfig.TMDB_API_KEY, "US").body()?.results.orEmpty() }
                .getOrDefault(emptyList())
        }
        val index = (movieDeferred.await() + tvDeferred.await())
            .distinctBy { it.providerId }
            .associateBy { it.providerId }
        watchProviderIndexCache["US"] = index
        index
    }

    private fun tmdbImageUrl(path: String, size: String): String {
        return "https://image.tmdb.org/t/p/$size$path"
    }

    private fun extractCatalogId(folder: CollectionFolder, kind: String): Int? {
        val catalogId = folder.catalogSources.firstOrNull()?.catalogId ?: return null
        val parts = catalogId.split("|")
        return when {
            parts.getOrNull(0) != "tmdbpreset" -> null
            parts.getOrNull(1) != kind -> null
            kind == "network" -> parts.getOrNull(3)?.toIntOrNull()
            kind == "company" -> parts.getOrNull(3)?.toIntOrNull()
            kind == "person" -> parts.getOrNull(3)?.toIntOrNull()
            kind == "franchise" -> parts.getOrNull(3)?.toIntOrNull()
            else -> null
        }
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

    private data class ResolvedArtwork(
        val cover: String? = null,
        val focus: String? = null
    )
}
