package com.angel.sentryhouse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiServiceAgua {

    @GET("datos")
    suspend fun obtenerDatosAgua(): List<AguaData>

}