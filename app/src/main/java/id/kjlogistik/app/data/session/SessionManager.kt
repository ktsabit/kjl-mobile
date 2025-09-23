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
        const val KEY_CHUCKER_ENABLED = "chucker_enabled" // <-- ADD THIS
        const val KEY_AUTH_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_GROUPS = "user_groups"
        // Key for persisting the active manifest
        const val KEY_ACTIVE_MANIFEST_ID = "active_manifest_id"
    }

    // --- ADD THESE NEW FUNCTIONS FOR CHUCKER ---
    fun setChuckerEnabled(isEnabled: Boolean) {
        prefs.edit { putBoolean(KEY_CHUCKER_ENABLED, isEnabled) }
    }

    fun isChuckerEnabled(): Boolean {
        return prefs.getBoolean(KEY_CHUCKER_ENABLED, false)
    }
    // --- END OF ADDITION ---

    fun saveAuthToken(token: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    // --- New Functions for Manifest Persistence ---

    fun saveActiveManifestId(manifestId: String) {
        prefs.edit { putString(KEY_ACTIVE_MANIFEST_ID, manifestId) }
    }

    fun fetchActiveManifestId(): String? {
        return prefs.getString(KEY_ACTIVE_MANIFEST_ID, null)
    }

    fun clearActiveManifestId() {
        prefs.edit { remove(KEY_ACTIVE_MANIFEST_ID) }
    }

    // --- Existing Functions ---

    fun saveUserGroups(groups: List<String>) {
        val groupsString = groups.joinToString(",")
        prefs.edit { putString(KEY_USER_GROUPS, groupsString) }
    }

    fun fetchAuthToken(): String? {
        return  prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun fetchRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun fetchUserGroups(): List<String> {
        val groupsString = prefs.getString(KEY_USER_GROUPS, null)
        return groupsString?.split(",")?.map { it.trim() } ?: emptyList()
    }

    fun clearAuthToken() {
        prefs.edit {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_GROUPS)
            remove(KEY_REFRESH_TOKEN)
            // Also clear the manifest ID on logout
            remove(KEY_ACTIVE_MANIFEST_ID)
            remove(KEY_CHUCKER_ENABLED) // <-- Also clear the flag on logout

        }
    }
}