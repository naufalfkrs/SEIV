package com.seiv.app.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.seiv.app.BuildConfig
import java.security.SecureRandom

/** Login dengan akun Google memakai Credential Manager → ID token → Supabase Auth. */
object GoogleAuth {
    val isConfigured: Boolean get() = SupabaseApi.isConfigured && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** @return ID token Google, atau null bila pengguna membatalkan. */
    suspend fun requestIdToken(activityContext: Context): String? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                throw SupabaseException("Kredensial Google tidak dikenali")
            }
        } catch (e: GetCredentialCancellationException) {
            null
        } catch (e: NoCredentialException) {
            throw SupabaseException("Tidak ada akun Google di perangkat ini.")
        } catch (e: GetCredentialException) {
            throw SupabaseException("Login Google gagal: ${e.message ?: e.type}")
        }
    }

    @Suppress("unused")
    private fun nonce(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
}
