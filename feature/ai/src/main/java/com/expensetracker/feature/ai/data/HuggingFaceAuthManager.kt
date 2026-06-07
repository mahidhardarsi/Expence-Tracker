package com.expensetracker.feature.ai.data

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.expensetracker.feature.ai.BuildConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val isConnected: Boolean = false,
    val accessToken: String? = null,
    val accessTokenExpiresAt: Long? = null,
    val pendingAuthUrl: String? = null,
    val errorMessage: String? = null
)

private data class PendingPkceState(
    val state: String,
    val codeVerifier: String
)

private data class TokenPayload(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Long? = null
)

@Singleton
class HuggingFaceAuthManager @Inject constructor(
    @ApplicationContext context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ai_auth_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authState = MutableStateFlow(loadState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun buildAuthorizationUri(): Uri {
        val state = UUID.randomUUID().toString()
        val verifier = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        persistPendingState(PendingPkceState(state, verifier))

        val challenge = verifier.sha256Base64Url()
        return Uri.parse("https://huggingface.co/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.HF_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.HF_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "openid profile gated-repos read-repos")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
    }

    suspend fun handleRedirect(uri: Uri): Result<Unit> {
        val returnedState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")

        if (error != null) {
            updateError("Authorization failed: $error")
            return Result.failure(IllegalStateException(error))
        }

        val pending = loadPendingState() ?: return Result.failure(IllegalStateException("Missing PKCE state"))
        if (returnedState != pending.state || code.isNullOrBlank()) {
            updateError("Invalid OAuth redirect")
            return Result.failure(IllegalStateException("Invalid OAuth redirect"))
        }

        val request = Request.Builder()
            .url("https://huggingface.co/oauth/token")
            .post(
                FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("client_id", BuildConfig.HF_CLIENT_ID)
                    .add("redirect_uri", BuildConfig.HF_REDIRECT_URI)
                    .add("code", code)
                    .add("code_verifier", pending.codeVerifier)
                    .build()
            )
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("OAuth token exchange failed (${response.code})")
                }
                val payload = gson.fromJson(response.body?.string(), TokenPayload::class.java)
                persistTokenPayload(payload)
                clearPendingState()
                _authState.value = loadState()
            }
        }.onFailure {
            updateError(it.message ?: "OAuth token exchange failed")
        }
    }

    suspend fun getValidAccessToken(): String? {
        val currentToken = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        if (!currentToken.isNullOrBlank() && System.currentTimeMillis() < expiresAt - 60_000L) {
            return currentToken
        }

        val refreshToken = prefs.getString("refresh_token", null) ?: return currentToken
        val refreshRequest = Request.Builder()
            .url("https://huggingface.co/oauth/token")
            .post(
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", BuildConfig.HF_CLIENT_ID)
                    .add("refresh_token", refreshToken)
                    .build()
            )
            .build()

        return runCatching {
            okHttpClient.newCall(refreshRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Token refresh failed (${response.code})")
                }
                val payload = gson.fromJson(response.body?.string(), TokenPayload::class.java)
                persistTokenPayload(payload.copy(refresh_token = payload.refresh_token ?: refreshToken))
                _authState.value = loadState()
                prefs.getString("access_token", null)
            }
        }.getOrNull()
    }

    fun disconnect() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("expires_at")
            .remove("pending_state")
            .remove("pending_verifier")
            .apply()
        _authState.value = loadState()
    }

    private fun persistPendingState(state: PendingPkceState) {
        prefs.edit()
            .putString("pending_state", state.state)
            .putString("pending_verifier", state.codeVerifier)
            .apply()
    }

    private fun loadPendingState(): PendingPkceState? {
        val state = prefs.getString("pending_state", null) ?: return null
        val verifier = prefs.getString("pending_verifier", null) ?: return null
        return PendingPkceState(state, verifier)
    }

    private fun clearPendingState() {
        prefs.edit().remove("pending_state").remove("pending_verifier").apply()
    }

    private fun persistTokenPayload(payload: TokenPayload) {
        val expiresAt = System.currentTimeMillis() + (payload.expires_in ?: 3600L) * 1000L
        prefs.edit()
            .putString("access_token", payload.access_token)
            .putString("refresh_token", payload.refresh_token)
            .putLong("expires_at", expiresAt)
            .apply()
    }

    private fun loadState(): AuthState {
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L).takeIf { it > 0 }
        return AuthState(
            isConnected = !token.isNullOrBlank(),
            accessToken = token,
            accessTokenExpiresAt = expiresAt,
            errorMessage = prefs.getString("last_error", null)
        )
    }

    private fun updateError(message: String) {
        prefs.edit().putString("last_error", message).apply()
        _authState.value = loadState().copy(errorMessage = message)
    }

    private fun String.sha256Base64Url(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
