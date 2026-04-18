package com.nuvio.tv.data.remote.api

import com.nuvio.tv.data.remote.dto.reddit.RedditListingDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditApi {

    @GET("search.json")
    suspend fun searchDiscussions(
        @Query("q") query: String,
        @Query("sort") sort: String = "relevance",
        @Query("t") time: String = "all",
        @Query("limit") limit: Int = 20,
        @Query("type") type: String = "link",
        @Query("raw_json") rawJson: Int = 1
    ): Response<RedditListingDto>

    @GET("{permalink}.json")
    suspend fun getComments(
        @Path(value = "permalink", encoded = true) permalinkWithoutLeadingSlash: String,
        @Query("sort") sort: String = "top",
        @Query("depth") depth: Int = 8,
        @Query("limit") limit: Int = 100,
        @Query("raw_json") rawJson: Int = 1
    ): Response<List<RedditListingDto>>
}
