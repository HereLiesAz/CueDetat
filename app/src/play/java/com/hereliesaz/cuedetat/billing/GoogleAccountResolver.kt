// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/GoogleAccountResolver.kt

package com.hereliesaz.cuedetat.billing

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the device's Google account email so we can match it against the
 * tester-license allowlist. Two access patterns:
 *
 *  - [resolveSilently] — non-interactive. Returns an email if the user is
 *    already signed in with our app's GoogleSignInClient; null otherwise.
 *  - [signInIntent] / [parseSignInResult] — interactive picker. Caller
 *    launches the intent, gets the activity result, and passes it back to
 *    parseSignInResult to extract the email.
 *
 * The basic GoogleSignInOptions.DEFAULT_SIGN_IN configuration requests
 * profile + ID, which includes the email address, and does not require any
 * web/OAuth client ID to be configured in Google Cloud Console. This keeps
 * the wiring minimal: as long as the device has a Google account and Play
 * Services available, the silent path returns an email after the first
 * interactive sign-in.
 */
@Singleton
class GoogleAccountResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }

    private val client: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Returns the last cached signed-in account's email without showing UI.
     * Null if the user has never signed in (with our client) on this device.
     */
    fun resolveSilently(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val email = account?.email
        if (email.isNullOrBlank()) {
            Log.i(TAG, "resolveSilently: no cached account")
            return null
        }
        Log.i(TAG, "resolveSilently: cached account hash=${TestLicenseAllowlist.sha256Hex(email.lowercase())}")
        return email
    }

    /**
     * Attempts silent sign-in. Unlike [resolveSilently], this re-validates the
     * cached credentials with Play Services and refreshes the token. Returns
     * the email on success.
     */
    suspend fun resolveSilentlyRefresh(): String? {
        return try {
            val account: GoogleSignInAccount = client.silentSignIn().await()
            val email = account.email
            if (email.isNullOrBlank()) {
                Log.i(TAG, "silentSignIn: account present but no email scope granted")
                null
            } else {
                Log.i(TAG, "silentSignIn: hash=${TestLicenseAllowlist.sha256Hex(email.lowercase())}")
                email
            }
        } catch (e: ApiException) {
            Log.i(TAG, "silentSignIn failed (statusCode=${e.statusCode}); caller should launch interactive picker", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "silentSignIn threw", e)
            null
        }
    }

    /** Intent to launch via Activity.startActivityForResult to show the picker. */
    fun signInIntent(): Intent = client.signInIntent

    /** Extracts the email from a sign-in activity result. Null on failure. */
    fun parseSignInResult(data: Intent?): String? {
        if (data == null) return null
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (email.isNullOrBlank()) {
                Log.i(TAG, "parseSignInResult: account present but no email scope granted")
                null
            } else {
                Log.i(TAG, "parseSignInResult: hash=${TestLicenseAllowlist.sha256Hex(email.lowercase())}")
                email
            }
        } catch (e: ApiException) {
            Log.w(TAG, "parseSignInResult: ApiException statusCode=${e.statusCode}", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "parseSignInResult threw", e)
            null
        }
    }

    /** Forgets the cached sign-in so the next interactive call re-prompts. */
    suspend fun signOut() {
        runCatching { client.signOut().await() }
    }

    companion object {
        private const val TAG = "TesterLicense"
    }
}
