package id.kjlogistik.app.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class SessionManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "auth_prefs"
        // We will store the access token here, as it's used for API calls
        const val KEY_AUTH_TOKEN = "access_token"
        // You might want to store the refresh token separately if you implement token refresh logic
        // const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_GROUPS = "user_groups"

    }

    fun saveAuthToken(token: String) {
        prefs.edit { putString(KEY_AUTH_TOKEN, token) }
    }

    fun saveUserGroups(groups: List<String>) {
        val groupsString = groups.joinToString(",")
        prefs.edit { putString(KEY_USER_GROUPS, groupsString) }
    }

    fun fetchAuthToken(): String? {
        return  prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun fetchUserGroups(): List<String> {
        val groupsString = prefs.getString(KEY_USER_GROUPS, null)
        return groupsString?.split(",")?.map { it.trim() } ?: emptyList()
    }


    fun clearAuthToken() {
        prefs.edit {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_GROUPS)
        }
    }
}

