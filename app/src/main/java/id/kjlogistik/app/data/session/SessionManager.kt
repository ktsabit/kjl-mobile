package id.kjlogistik.app.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject // NEW

class SessionManager @Inject constructor( // ADD @Inject
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "auth_prefs"
        const val KEY_AUTH_TOKEN = "auth_token"
    }

    fun saveAuthToken(token: String) {
        prefs.edit { putString(KEY_AUTH_TOKEN, token) }
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearAuthToken() {
        prefs.edit { remove(KEY_AUTH_TOKEN) }
    }
}