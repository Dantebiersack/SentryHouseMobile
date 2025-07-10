package com.angel.sentryhouse.utils

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para acceder al DataStore, mantente igual
private val Context.dataStore by preferencesDataStore("sensor_images")

object SensorImageStore {

    // Clave única basada en pantalla + sensor, se mantiene igual
    private fun getKeyForSensor(screenName: String, sensorId: Int): Preferences.Key<String> {
        return stringPreferencesKey("${screenName}_sensor_$sensorId")
    }

    // Modificado: ahora acepta Uri? (puede ser nulo)
    suspend fun saveImageUri(context: Context, screenName: String, sensorId: Int, uri: Uri?) {
        context.dataStore.edit { prefs ->
            val key = getKeyForSensor(screenName, sensorId)
            if (uri != null) {
                // Si la URI no es nula, guarda su representación de String
                prefs[key] = uri.toString()
            } else {
                // Si la URI es nula, remueve la clave (elimina la foto)
                prefs.remove(key)
            }
        }
    }

    // Se mantiene igual, ya que ya manejaba el caso de Uri nula
    fun getImageUri(context: Context, screenName: String, sensorId: Int): Flow<Uri?> {
        return context.dataStore.data.map { prefs ->
            prefs[getKeyForSensor(screenName, sensorId)]?.let { Uri.parse(it) }
        }
    }
}