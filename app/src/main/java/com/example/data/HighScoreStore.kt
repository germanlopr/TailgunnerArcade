package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tailGunnerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tail_gunner_prefs"
)

class HighScoreStore(private val context: Context) {
    private val highScoreKey = intPreferencesKey("tg_b17_hi")

    val highScoreFlow: Flow<Int> = context.tailGunnerDataStore.data.map { prefs ->
        prefs[highScoreKey] ?: 0
    }

    suspend fun saveHighScoreIfBetter(score: Int) {
        context.tailGunnerDataStore.edit { prefs ->
            val current = prefs[highScoreKey] ?: 0
            if (score > current) {
                prefs[highScoreKey] = score
            }
        }
    }
}
