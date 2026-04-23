package com.nuvio.tv.domain.model

import java.net.URLEncoder

object CollectionPresets {
    const val STREAMING_SERVICES_ID = "preset_streaming_services"
    const val NETWORKS_ID = "preset_networks"
    const val GENRES_ID = "preset_genres"
    const val STUDIOS_ID = "preset_studios"
    const val DECADES_ID = "preset_decades"
    const val DIRECTORS_ID = "preset_directors"
    const val FRANCHISES_ID = "preset_franchises"
    const val ACTORS_ID = "preset_actors"
    const val AWARDS_ID = "preset_awards"

    val presetIds: Set<String> = setOf(
        STREAMING_SERVICES_ID,
        GENRES_ID,
        STUDIOS_ID,
        DECADES_ID,
        DIRECTORS_ID,
        FRANCHISES_ID,
        ACTORS_ID
    )

    fun defaults(): List<Collection> {
        return listOf(
            streamingServicesPreset(),
            genresPreset(),
            studiosPreset(),
            decadesPreset(),
            directorsPreset(),
            franchisesPreset(),
            actorsPreset()
        )
    }

    fun missingPresetIds(existingCollections: List<Collection>): Set<String> {
        val existingIds = existingCollections.asSequence().map { it.id }.toSet()
        return presetIds - existingIds
    }

    private fun streamingServicesPreset(): Collection {
        return Collection(
            id = STREAMING_SERVICES_ID,
            title = "Streaming",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = true,
            folders = listOf(
                serviceFolder("Netflix", providerId = 8),
                serviceFolder("Prime Video", providerId = 9),
                serviceFolder("Disney+", providerId = 337),
                serviceFolder("Hulu", providerId = 15),
                serviceFolder("HBO Max", providerId = 1899),
                serviceFolder("Paramount+", providerId = 2303),
                serviceFolder("Peacock", providerId = null),
                serviceFolder("Apple TV+", providerId = 350),
                serviceFolder("Crunchyroll", providerId = 283),
            )
        )
    }

    private fun genresPreset(): Collection {
        return Collection(
            id = GENRES_ID,
            title = "Genres",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = true,
            folders = listOf(
                genreFolder("Action", movieGenreId = 28, tvGenreId = 10759),
                genreFolder("Adventure", movieGenreId = 12, tvGenreId = 10759),
                genreFolder("Comedy", movieGenreId = 35, tvGenreId = 35),
                genreFolder("Drama", movieGenreId = 18, tvGenreId = 18),
                genreFolder("Thriller", movieGenreId = 53, tvGenreId = 9648),
                genreFolder("Sci-fi", movieGenreId = 878, tvGenreId = 10765),
                genreFolder("Animation", movieGenreId = 16, tvGenreId = 16),
                genreFolder("Horror", movieGenreId = 27, tvGenreId = 9648),
                genreFolder("Crime", movieGenreId = 80, tvGenreId = 80),
                genreFolder("Fantasy", movieGenreId = 14, tvGenreId = 10765),
                genreFolder("Romance", movieGenreId = 10749, tvGenreId = 10749),
                genreFolder("Documentary", movieGenreId = 99, tvGenreId = 99),
                genreFolder("Kids", movieGenreId = 10751, tvGenreId = 10762),
                genreFolder("Anime", movieGenreId = 16, tvGenreId = 10765),
                genreFolder("Superheroes", movieGenreId = 28, tvGenreId = 10759),
                genreFolder("Music", movieGenreId = 10402, tvGenreId = 10402),
                genreFolder("International", movieGenreId = 18, tvGenreId = 18),
                genreFolder("War-stories", movieGenreId = 10752, tvGenreId = 10768),
                genreFolder("Westerns", movieGenreId = 37, tvGenreId = 37),
                genreFolder("Whodunits", movieGenreId = 9648, tvGenreId = 9648)
            )
        )
    }

    private fun studiosPreset(): Collection {
        return Collection(
            id = STUDIOS_ID,
            title = "Studios",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = true,
            folders = listOf(
                studioFolder("Marvel", companyId = 420),
                studioFolder("Walt Disney Pictures", companyId = 2),
                studioFolder("Pixar", companyId = 3),
                studioFolder("DreamWorks", companyId = 521),
                studioFolder("Walt Disney Animation", companyId = 3)
            )
        )
    }

    private fun decadesPreset(): Collection {
        return Collection(
            id = DECADES_ID,
            title = "Decades",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = true,
            folders = listOf(
                decadeFolder("1960s", 1960, 1969),
                decadeFolder("1970s", 1970, 1979),
                decadeFolder("1980s", 1980, 1989),
                decadeFolder("1990s", 1990, 1999),
                decadeFolder("2000s", 2000, 2009),
                decadeFolder("2010s", 2010, 2019),
                decadeFolder("2020s", 2020, 2029)
            )
        )
    }

    private fun directorsPreset(): Collection {
        return Collection(
            id = DIRECTORS_ID,
            title = "Directors",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = true,
            folders = listOf(
                personFolder("Steven Spielberg", 488, role = "director"),
                personFolder("Christopher Nolan", 525, role = "director"),
                personFolder("Martin Scorsese", 1032, role = "director"),
                personFolder("Alfred Hitchcock", 2636, role = "director"),
                personFolder("David Fincher", 10859, role = "director"),
                personFolder("Stanley Kubrick", 1099, role = "director"),
                personFolder("Denis Villeneuve", 1374276, role = "director"),
                personFolder("Wes Anderson", 5655, role = "director"),
                personFolder("John Carpenter", 23846, role = "director"),
                personFolder("Brian De Palma", 3488, role = "director")
            )
        )
    }

    private fun franchisesPreset(): Collection {
        return Collection(
            id = FRANCHISES_ID,
            title = "Franchises",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = false,
            folders = listOf(
                franchiseFolder("Star Wars", 10),
                franchiseFolder("Marvel Cinematic Universe", 86311),
                franchiseFolder("Wizarding World", 1241),
                franchiseFolder("Lord of the Rings & Hobbit", 119),
                franchiseFolder("Batman", 263),
                franchiseFolder("Spider-Man", 556),
                franchiseFolder("James Bond", 645),
                franchiseFolder("Jurassic World", 328),
                franchiseFolder("Fast & Furious", 9485),
                franchiseFolder("Avatar", 87096),
                franchiseFolder("Mission: Impossible", 87359),
                franchiseFolder("Pirates of the Caribbean", 295),
                franchiseFolder("Indiana Jones", 84),
                franchiseFolder("Transformers", 8650),
                franchiseFolder("John Wick", 404609),
                franchiseFolder("Ghostbusters", 2980),
                franchiseFolder("X-Men", 748),
                franchiseFolder("Toy Story", 10194),
                franchiseFolder("Terminator", 528),
                franchiseFolder("Die Hard", 1570),
                franchiseFolder("Hunger Games", 131635),
                franchiseFolder("Dune", 726871)
            )
        )
    }

    private fun actorsPreset(): Collection {
        return Collection(
            id = ACTORS_ID,
            title = "Actors",
            pinToTop = false,
            viewMode = FolderViewMode.FOLLOW_LAYOUT,
            showAllTab = true,
            folders = listOf(
                personFolder("Tom Hanks", 31, role = "actor"),
                personFolder("Leonardo DiCaprio", 6193, role = "actor"),
                personFolder("Brad Pitt", 287, role = "actor"),
                personFolder("Scarlett Johansson", 1245, role = "actor"),
                personFolder("Dwayne Johnson", 18918, role = "actor"),
                personFolder("Samuel L. Jackson", 2231, role = "actor"),
                personFolder("Robert Downey Jr.", 3223, role = "actor"),
                personFolder("Chris Hemsworth", 74568, role = "actor"),
                personFolder("Keanu Reeves", 6384, role = "actor"),
                personFolder("Denzel Washington", 5292, role = "actor"),
                personFolder("Tom Cruise", 500, role = "actor"),
                personFolder("Natalie Portman", 524, role = "actor"),
                personFolder("Margot Robbie", 234352, role = "actor"),
                personFolder("Will Smith", 2888, role = "actor"),
                personFolder("Sandra Bullock", 18277, role = "actor"),
                personFolder("Angelina Jolie", 11701, role = "actor"),
                personFolder("Meryl Streep", 5064, role = "actor"),
                personFolder("Jason Statham", 27578, role = "actor"),
                personFolder("Ryan Gosling", 30614, role = "actor"),
                personFolder("Emma Stone", 54693, role = "actor"),
                personFolder("Viola Davis", 19492, role = "actor"),
                personFolder("Zendaya", 505710, role = "actor"),
                personFolder("Florence Pugh", 1397778, role = "actor"),
                personFolder("Timothee Chalamet", 1190668, role = "actor")
            )
        )
    }

    private fun serviceFolder(name: String, providerId: Int?): CollectionFolder {
        return CollectionFolder(
            id = "service_${slugId(name)}",
            title = name,
            coverImageUrl = serviceCover(name),
            focusGifUrl = null,
            focusGifEnabled = false,
            tileShape = PosterShape.LANDSCAPE,
            catalogSources = listOf(
                source("movie", providerCatalog(name, providerId, "popular")),
                source("movie", providerCatalog(name, providerId, "top_rated")),
                source("movie", providerCatalog(name, providerId, "recent")),
                source("movie", providerCatalog(name, providerId, "trending")),
                source("series", providerCatalog(name, providerId, "popular")),
                source("series", providerCatalog(name, providerId, "top_rated")),
                source("series", providerCatalog(name, providerId, "recent")),
                source("series", providerCatalog(name, providerId, "trending"))
            )
        )
    }

    private fun genreFolder(
        name: String,
        movieGenreId: Int,
        tvGenreId: Int
    ): CollectionFolder {
        return CollectionFolder(
            id = "genre_${slugId(name)}",
            title = name,
            coverImageUrl = genreCover(name),
            coverEmoji = null,
            tileShape = PosterShape.LANDSCAPE,
            catalogSources = listOf(
                source("movie", genreCatalog(name, movieGenreId, tvGenreId, "popular")),
                source("movie", genreCatalog(name, movieGenreId, tvGenreId, "top_rated")),
                source("movie", genreCatalog(name, movieGenreId, tvGenreId, "recent")),
                source("movie", genreCatalog(name, movieGenreId, tvGenreId, "trending")),
                source("series", genreCatalog(name, movieGenreId, tvGenreId, "popular")),
                source("series", genreCatalog(name, movieGenreId, tvGenreId, "top_rated")),
                source("series", genreCatalog(name, movieGenreId, tvGenreId, "recent")),
                source("series", genreCatalog(name, movieGenreId, tvGenreId, "trending"))
            )
        )
    }

    private fun studioFolder(name: String, companyId: Int): CollectionFolder {
        return CollectionFolder(
            id = "studio_${slugId(name)}",
            title = name,
            coverImageUrl = studioCover(name),
            focusGifUrl = null,
            focusGifEnabled = false,
            coverEmoji = null,
            tileShape = PosterShape.LANDSCAPE,
            catalogSources = listOf(
                source("movie", companyCatalog(name, companyId, "popular")),
                source("movie", companyCatalog(name, companyId, "top_rated")),
                source("movie", companyCatalog(name, companyId, "recent")),
                source("movie", companyCatalog(name, companyId, "trending")),
                source("series", companyCatalog(name, companyId, "popular")),
                source("series", companyCatalog(name, companyId, "top_rated")),
                source("series", companyCatalog(name, companyId, "recent")),
                source("series", companyCatalog(name, companyId, "trending"))
            )
        )
    }

    private fun decadeFolder(name: String, startYear: Int, endYear: Int): CollectionFolder {
        return CollectionFolder(
            id = "decade_${startYear}",
            title = name,
            coverImageUrl = decadeCover(startYear),
            coverEmoji = null,
            tileShape = PosterShape.LANDSCAPE,
            catalogSources = listOf(
                source("movie", decadeCatalog(name, startYear, endYear, "popular")),
                source("movie", decadeCatalog(name, startYear, endYear, "top_rated")),
                source("movie", decadeCatalog(name, startYear, endYear, "recent")),
                source("movie", decadeCatalog(name, startYear, endYear, "trending")),
                source("series", decadeCatalog(name, startYear, endYear, "popular")),
                source("series", decadeCatalog(name, startYear, endYear, "top_rated")),
                source("series", decadeCatalog(name, startYear, endYear, "recent")),
                source("series", decadeCatalog(name, startYear, endYear, "trending"))
            )
        )
    }

    private fun personFolder(name: String, personId: Int, role: String): CollectionFolder {
        return CollectionFolder(
            id = "${role}_${slugId(name)}",
            title = name,
            coverImageUrl = if (role == "director") directorCover(name) else null,
            focusGifUrl = null,
            focusGifEnabled = false,
            coverEmoji = null,
            tileShape = if (role == "director") PosterShape.LANDSCAPE else PosterShape.POSTER,
            catalogSources = listOf(
                source("movie", personCatalog(name, personId, role, "popular")),
                source("movie", personCatalog(name, personId, role, "top_rated")),
                source("movie", personCatalog(name, personId, role, "recent")),
                source("movie", personCatalog(name, personId, role, "trending")),
                source("series", personCatalog(name, personId, role, "popular")),
                source("series", personCatalog(name, personId, role, "top_rated")),
                source("series", personCatalog(name, personId, role, "recent")),
                source("series", personCatalog(name, personId, role, "trending"))
            )
        )
    }

    private fun franchiseFolder(name: String, collectionId: Int): CollectionFolder {
        return CollectionFolder(
            id = "franchise_${slugId(name)}",
            title = name,
            coverImageUrl = null,
            focusGifUrl = null,
            focusGifEnabled = false,
            coverEmoji = null,
            tileShape = PosterShape.POSTER,
            catalogSources = listOf(
                source("movie", franchiseCatalog(name, collectionId))
            )
        )
    }

    private fun source(type: String, catalogId: String): CollectionCatalogSource {
        return CollectionCatalogSource(
            addonId = TMDB_PRESET_ADDON_ID,
            type = type,
            catalogId = catalogId
        )
    }

    private fun providerCatalog(name: String, providerId: Int?, rail: String): String {
        val providerToken = providerId?.toString().orEmpty()
        return "tmdbpreset|provider|$name|$providerToken|$rail"
    }

    private fun genreCatalog(name: String, movieGenreId: Int, tvGenreId: Int, rail: String): String {
        return "tmdbpreset|genre|$name|$movieGenreId|$tvGenreId|$rail"
    }

    private fun companyCatalog(name: String, companyId: Int, rail: String): String {
        return "tmdbpreset|company|$name|$companyId|$rail"
    }

    private fun personCatalog(name: String, personId: Int, role: String, rail: String): String {
        return "tmdbpreset|person|$name|$personId|$role|$rail"
    }

    private fun franchiseCatalog(name: String, collectionId: Int): String {
        return "tmdbpreset|franchise|$name|$collectionId|popular"
    }

    private fun decadeCatalog(name: String, startYear: Int, endYear: Int, rail: String): String {
        return "tmdbpreset|decade|$name|$startYear|$endYear|$rail"
    }

    private fun genreCover(name: String): String {
        return fusionAssetUrl("widgets/genres/wide/dannyrutledge/${genreAssetSlug(name)}-wide.png")
    }

    private fun decadeCover(startYear: Int): String {
        return fusionAssetUrl("widgets/decades/wide/mousa.a/${startYear}s-wide.png")
    }

    private fun serviceCover(name: String): String? {
        return when (name) {
            "Netflix" -> "https://nuvioapp.space/uploads/covers/b70ef89e-662d-4b9b-b0c8-9ab75d2843f3.jpg"
            "Prime Video" -> "https://nuvioapp.space/uploads/covers/f32bd112-9c7e-45d9-aa93-c76cffd7cbea.png"
            "Apple TV+" -> "https://nuvioapp.space/uploads/covers/3b379dee-815e-445e-8f67-d5a43b05d2da.jpg"
            "Disney+" -> "https://nuvioapp.space/uploads/covers/5928ff51-999f-491d-9b0c-5b442735b5d6.jpg"
            "HBO Max" -> "https://i.postimg.cc/MT2fZ1Rz/HBO-Max.jpg"
            "Paramount+" -> "https://i.postimg.cc/NMq2GRTg/Paramount.jpg"
            "Peacock" -> "https://nuvioapp.space/uploads/covers/0947499b-4551-46db-abf7-f6a34501e565.png"
            "Crunchyroll" -> "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQpJgaSPw7wj6Ls0fD7ZJjNhxO9-Ikx_N6HhAsKMbb_Ug&s=10"
            "Hulu" -> "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSy5CyuNVaZeNdTU3lxSlUwiZj9iDEuLuDpDduaaQzAqO-rj56_Bhboia2l&s=10"
            else -> wideTitleCover(name, subtitle = "Streaming", background = "0b1020", accent = "e50914")
        }
    }

    private fun studioCover(name: String): String {
        return when (name) {
            "Pixar" -> "https://nuvioapp.space/uploads/covers/aba80c00-dc90-481f-b477-a91c7ce337e3.png"
            "DreamWorks" -> "https://nuvioapp.space/uploads/covers/882cf7a8-9aa6-4673-81c1-d3ed7a859e95.jpg"
            "Walt Disney Animation" -> wideTitleCover(name, subtitle = "Studio", background = "111827", accent = "f59e0b")
            "Walt Disney Pictures" -> wideTitleCover(name, subtitle = "Studio", background = "111827", accent = "f59e0b")
            "Marvel" -> "https://www.justsaying.asia/wp-content/uploads/2014/08/marvel-logo-wallpaper-20367-hd-wallpapers.jpg"
            else -> wideTitleCover(name, subtitle = "Studio", background = "111827", accent = "f59e0b")
        }
    }

    private fun directorCover(name: String): String {
        return when (name) {
            "Wes Anderson" -> fusionAssetUrl("widgets/directors/wide/fexm92/anderson-wide.png")
            "John Carpenter" -> fusionAssetUrl("widgets/directors/wide/fexm92/carpenter-wide.png")
            "Brian De Palma" -> fusionAssetUrl("widgets/directors/wide/fexm92/depalma-wide.png")
            "David Fincher" -> fusionAssetUrl("widgets/directors/wide/fexm92/fincher-wide.png")
            "Alfred Hitchcock" -> fusionAssetUrl("widgets/directors/wide/fexm92/hitchcock-wide.png")
            "Stanley Kubrick" -> fusionAssetUrl("widgets/directors/wide/fexm92/kubrick-wide.png")
            "Christopher Nolan" -> fusionAssetUrl("widgets/directors/wide/fexm92/nolan-wide.png")
            "Martin Scorsese" -> fusionAssetUrl("widgets/directors/wide/fexm92/scorsese-wide.png")
            "Steven Spielberg" -> fusionAssetUrl("widgets/directors/wide/fexm92/spielberg-wide.png")
            "Denis Villeneuve" -> fusionAssetUrl("widgets/directors/wide/fexm92/villeneuve-wide.png")
            else -> wideTitleCover(name, subtitle = "Director", background = "1a1224", accent = "ecd4ff")
        }
    }

    private fun fusionAssetUrl(relativePath: String): String {
        return "https://raw.githubusercontent.com/itsrenoria/fusion-starter-kit/refs/heads/main/resources/$relativePath"
    }

    private fun wideTitleCover(title: String, subtitle: String, background: String, accent: String): String {
        return svgCover(
            width = 1280,
            height = 720,
            title = title,
            subtitle = subtitle,
            background = background,
            accent = accent,
            textColor = "ffffff",
            poster = false
        )
    }

    private fun posterTitleCover(title: String, subtitle: String, background: String, accent: String): String {
        return svgCover(
            width = 1000,
            height = 1500,
            title = title,
            subtitle = subtitle,
            background = background,
            accent = accent,
            textColor = "ffffff",
            poster = true
        )
    }

    private fun svgCover(
        width: Int,
        height: Int,
        title: String,
        subtitle: String?,
        background: String,
        accent: String,
        textColor: String,
        poster: Boolean
    ): String {
        val lines = wrapTitle(title)
        val titleText = lines.joinToString(separator = "\n") { escapeXml(it) }
        val subtitleText = subtitle?.let(::escapeXml).orEmpty()
        val svg = buildString {
            append("<svg xmlns='http://www.w3.org/2000/svg' width='$width' height='$height' viewBox='0 0 $width $height'>")
            append("<defs>")
            append("<linearGradient id='bg' x1='0' y1='0' x2='1' y2='1'>")
            append("<stop offset='0%' stop-color='#${background}'/>")
            append("<stop offset='100%' stop-color='#${accent}' stop-opacity='0.55'/>")
            append("</linearGradient>")
            append("<linearGradient id='shine' x1='0' y1='0' x2='1' y2='1'>")
            append("<stop offset='0%' stop-color='#ffffff' stop-opacity='0.22'/>")
            append("<stop offset='100%' stop-color='#ffffff' stop-opacity='0'/>")
            append("</linearGradient>")
            append("</defs>")
            append("<rect width='100%' height='100%' fill='url(#bg)' rx='48' ry='48'/>")
            append("<circle cx='${width * 0.83}' cy='${height * 0.18}' r='${minOf(width, height) / 3}' fill='#ffffff' fill-opacity='0.08'/>")
            append("<circle cx='${width * 0.12}' cy='${height * 0.88}' r='${minOf(width, height) / 4}' fill='#000000' fill-opacity='0.14'/>")
            append("<rect x='0' y='0' width='${width * 0.42}' height='${height * 0.22}' fill='url(#shine)'/>")
            append("<rect x='${width * 0.08}' y='${height * 0.1}' width='${width * 0.84}' height='${height * 0.8}' rx='40' ry='40' fill='none' stroke='#ffffff' stroke-opacity='0.12' stroke-width='4'/>")
            val titleSize = if (poster) 78 else if (lines.size > 1) 74 else 88
            val subtitleSize = if (poster) 34 else 30
            val titleY = if (poster) height * 0.58 else if (lines.size > 1) height * 0.56 else height * 0.54
            val titleX = width * 0.5
            append("<text x='$titleX' y='$titleY' text-anchor='middle' fill='#$textColor' font-family='Arial, Helvetica, sans-serif' font-size='$titleSize' font-weight='800' letter-spacing='2'>")
            if (lines.size > 1) {
                val first = escapeXml(lines[0])
                val second = escapeXml(lines[1])
                append("<tspan x='$titleX' dy='0'>$first</tspan>")
                append("<tspan x='$titleX' dy='${titleSize + 10}'>$second</tspan>")
            } else {
                append(titleText)
            }
            append("</text>")
            if (subtitleText.isNotBlank()) {
                append("<text x='$titleX' y='${titleY + if (poster) 110 else 90}' text-anchor='middle' fill='#$textColor' fill-opacity='0.86' font-family='Arial, Helvetica, sans-serif' font-size='$subtitleSize' font-weight='600' letter-spacing='3'>")
                append(subtitleText.uppercase())
                append("</text>")
            }
            append("</svg>")
        }
        val encoded = URLEncoder.encode(svg, "UTF-8").replace("+", "%20")
        return "data:image/svg+xml;charset=UTF-8,$encoded"
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun wrapTitle(title: String): List<String> {
        val words = title.split(' ').filter { it.isNotBlank() }
        if (words.size <= 2) return listOf(title)
        return listOf(words.take(words.size / 2).joinToString(" "), words.drop(words.size / 2).joinToString(" "))
    }

    private fun slugId(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }

    private fun genreAssetSlug(name: String): String {
        return when (name) {
            "Sci-fi" -> "sci-fi"
            else -> name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
    }

    private fun decadePalette(startYear: Int): Pair<String, String> = when (startYear) {
        1960 -> "3e2a20" to "ffd7b3"
        1970 -> "4a2f1c" to "ffe3c5"
        1980 -> "3f224f" to "f2d7ff"
        1990 -> "1f3550" to "d8ecff"
        2000 -> "183c34" to "d9ffef"
        2010 -> "2c2c2c" to "f5f5f5"
        2020 -> "2a1f3e" to "e9dbff"
        else -> "1f1f1f" to "f5f5f5"
    }

    private fun genrePalette(name: String): Pair<String, String> = when (name) {
        "Action", "Action-adventures", "Action-thrillers", "Superheroes" -> "3a1111" to "ffd3d3"
        "Comedy", "Animated-comedies", "Workplace-comedies" -> "3a2f11" to "ffeeb8"
        "Drama", "Tearjerkers" -> "2d213a" to "e7d5ff"
        "Science Fiction", "Sci-fi", "Robots-and-ai", "Space-epics", "Techno-thrillers" -> "11253a" to "c4ecff"
        "Fantasy", "Fantasy-adventures", "Dark-fantasy", "Myths-and-legends" -> "24113a" to "e8cfff"
        "Animation", "Adult-animation", "Anime", "Arthouse-animation", "Favorite-cartoons", "Lovable-monsters" -> "123a2a" to "c7ffe9"
        "Documentary", "Nature", "Learning-corner" -> "2e3320" to "e8f0c8"
        "Crime", "Mystery", "Whodunits", "Spies", "Thriller", "Psychological-terror", "Super-shocks", "Vhs-era-frightmares" -> "1f2330" to "cfd8ff"
        "Romance" -> "3a1225" to "ffd8e6"
        "Family", "Family-movie-night", "Kids" -> "12303a" to "d6f2ff"
        "History", "Historical-blockbusters" -> "32241a" to "ffe2bf"
        "Music", "Music-movies" -> "32202d" to "ffdff5"
        "War", "War-stories" -> "2f1c1c" to "ffd0d0"
        "Western", "Westerns", "Frontier-grit" -> "4a2f1c" to "ffe3c5"
        "Creature-features", "Lovable-monsters", "Zombie-orama" -> "2d1822" to "ffd6e1"
        else -> "1f1f1f" to "f5f5f5"
    }
}
