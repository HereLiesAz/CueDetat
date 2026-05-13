package com.hereliesaz.cuedetat.data

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrityRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val integrityManager = IntegrityManagerFactory.create(context)

    /**
     * Requests an integrity token from Google Play.
     * @param nonce A unique, server-generated nonce to prevent replay attacks.
     * @return The integrity token, or null if the request fails.
     */
    suspend fun fetchIntegrityToken(nonce: String): String? {
        return try {
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()
            
            val response = integrityManager.requestIntegrityToken(request).await()
            response.token()
        } catch (e: Exception) {
            Log.e("IntegrityRepository", "Failed to fetch integrity token", e)
            null
        }
    }
}
