package com.angel.sentryhouse

import retrofit2.http.GET

interface ApiService {
    @GET("/api/gas")
    suspend fun obtenerGas(): SensorData

    @GET("/api/gasCocina")
    suspend fun obtenerGasCocina(): SensorData

}