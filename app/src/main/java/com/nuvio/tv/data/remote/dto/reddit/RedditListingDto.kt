package com.nuvio.tv.data.remote.dto.reddit

data class RedditListingDto(
    val data: RedditListingDataDto? = null
)

data class RedditListingDataDto(
    val children: List<RedditThingDto>? = null
)

data class RedditThingDto(
    val kind: String? = null,
    val data: Map<String, Any?>? = null
)
