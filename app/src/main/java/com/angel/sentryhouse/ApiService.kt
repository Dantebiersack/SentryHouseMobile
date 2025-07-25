package com.angel.sentryhouse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("/api/gas")
    suspend fun obtenerGas(): SensorData

    @GET("/api/gasCocina")
    suspend fun obtenerGasCocina(): SensorData


    data class LoginRequest(
        val correo: String,
        val contrasena: String
    )

    data class LoginResponse(
        val id: String,
        val correo: String,
        val cotizacionId: Int
    )
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

}