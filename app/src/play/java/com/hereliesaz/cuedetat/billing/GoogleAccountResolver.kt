// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/GoogleAccountResolver.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.hereliesaz.cuedetat.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the device's Google account email via Credential Manager + the
 * Google ID provider, so we can match it against the tester-license
 * allowlist. Two access patterns:
 *
 *  - [resolveAuthorizedSilently] — non-interactive. Returns the email if
 *    the user has previously authorized our app for this account; null
 *    otherwise. Safe to call on cold start.
 *  - [resolveInteractive] — shows the one-tap account picker so a tester
 *    can pick their account if they haven't authorized us yet.
 *
 * Both require [BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID] to be set. When
 * the build was assembled without it (e.g. local dev without
 * `GOOGLE_OAUTH_WEB_CLIENT_ID` in local.properties), the methods short-
 * circuit to null and log a warning, rather than crashing.
 *
 * The returned email is the `id` field of [GoogleIdTokenCredential]. We
 * don't verify the token signature on-device: this email is only used as
 * input to a sha256 hash compared against a build-baked allowlist. It is
 * never sent off-device, used as an authn principal, or trusted for
 * anything else.
 */
@Singleton
class GoogleAccountResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    private val webClientId: String? = BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID.takeIf { it.isNotBlank() }

    val isConfigured: Boolean get() = webClientId != null

    /**
     * Silent (no UI) attempt. Only returns an email if the user has
     * previously authorized this app for one of their Google accounts.
     */
    suspend fun resolveAuthorizedSilently(activity: Activity): String? {
        val clientId = webClientId ?: run {
            Log.w(TAG, "resolveAuthorizedSilently: no Web OAuth client id baked in; skipping")
            return null
        }
        return resolve(activity, clientId, authorizedOnly = true)
    }

    /**
     * Interactive picker. Shows the one-tap account chooser, returns the
     * email of the picked account on success.
     */
    suspend fun resolveInteractive(activity: Activity): String? {
        val clientId = webClientId ?: run {
            Log.w(TAG, "resolveInteractive: no Web OAuth client id baked in; cannot show picker")
            return null
        }
        return resolve(activity, clientId, authorizedOnly = false)
    }

    private suspend fun resolve(
        activity: Activity,
        clientId: String,
        authorizedOnly: Boolean,
    ): String? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(authorizedOnly)
            .setAutoSelectEnabled(authorizedOnly)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val gid = GoogleIdTokenCredential.createFrom(credential.data)
                val email = gid.id
                if (email.isBlank()) {
                    Log.w(TAG, "resolve: GoogleIdTokenCredential.id was blank")
                    null
                } else {
                    Log.i(TAG, "resolve: hash=${TestLicenseAllowlist.sha256Hex(email.lowercase())}")
                    email
                }
            } else {
                Log.w(TAG, "resolve: unexpected credential type=${credential::class.java.simpleName}")
                null
            }
        } catch (e: NoCredentialException) {
            // Silent path: user has no authorized account. Non-fatal; the
            // paywall will offer an interactive picker.
            Log.i(TAG, "resolve(authorizedOnly=$authorizedOnly): no authorized account; ${e.message}")
            null
        } catch (e: GetCredentialException) {
            Log.w(TAG, "resolve(authorizedOnly=$authorizedOnly): GetCredentialException type=${e.type}", e)
            null
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "resolve: parsing exception", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "resolve(authorizedOnly=$authorizedOnly): unexpected", e)
            null
        }
    }

    companion object {
        private const val TAG = "TesterLicense"
    }
}
