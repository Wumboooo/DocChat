package com.example.docchat

import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApi {
    @GET("search")
    suspend fun getLocations(
        @Query("q") query: String,
        @Query("format") format: String,
        @Query("accept-language") language: String
    ): List<LocationResult>
}

data class LocationResult(
    val display_name: String
)


