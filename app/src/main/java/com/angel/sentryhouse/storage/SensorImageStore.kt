package com.angel.sentryhouse.utils

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("sensor_images")

object SensorImageStore {

    private fun getKeyForSensor(sensorId: Int) = stringPreferencesKey("sensor_image_$sensorId")

    suspend fun saveImageUri(context: Context, sensorId: Int, uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[getKeyForSensor(sensorId)] = uri.toString()
        }
    }

    fun getImageUri(context: Context, sensorId: Int): Flow<Uri?> {
        return context.dataStore.data.map { prefs ->
            prefs[getKeyForSensor(sensorId)]?.let { Uri.parse(it) }
        }
    }
}
