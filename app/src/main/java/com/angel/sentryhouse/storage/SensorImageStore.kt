package com.angel.sentryhouse.utils

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("sensor_images")

object SensorImageStore {

    // Clave única basada en pantalla + sensor
    private fun getKeyForSensor(screenName: String, sensorId: Int): Preferences.Key<String> {
        return stringPreferencesKey("${screenName}_sensor_$sensorId")
    }

    suspend fun saveImageUri(context: Context, screenName: String, sensorId: Int, uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[getKeyForSensor(screenName, sensorId)] = uri.toString()
        }
    }

    fun getImageUri(context: Context, screenName: String, sensorId: Int): Flow<Uri?> {
        return context.dataStore.data.map { prefs ->
            prefs[getKeyForSensor(screenName, sensorId)]?.let { Uri.parse(it) }
        }
    }
}
