package com.angel.sentryhouse

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.java
import kotlin.toString

//Objeto singleton que gestiona la instancia de Retrofit para realizar peticiones HTTP.
object RetrofitClient {
    private var retrofit: Retrofit? = null
//Obtiene la instancia de la API con la URL base configurada en preferencias.
    fun getApi(context: Context): ApiService {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("base_url", "http://localhost:3000/")!!
    //Si la URL base cambió, se reinicia Retrofit
        if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
