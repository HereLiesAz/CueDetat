package com.hereliesaz.cuedetat.data

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class IntegrityResult {
    object Idle : IntegrityResult()
    object Pending : IntegrityResult()
    data class Success(val token: String) : IntegrityResult()
    data class Failure(val error: String) : IntegrityResult()
}

@Singleton
class IntegrityRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val integrityManager = IntegrityManagerFactory.create(context)
    private val standardIntegrityManager = IntegrityManagerFactory.createStandard(context)

    private val _result = MutableStateFlow<IntegrityResult>(IntegrityResult.Idle)
    val result = _result.asStateFlow()

    /**
     * Requests an integrity token using the Snapshot API.
     * Best for one-off checks where a full verdict is needed.
     */
    suspend fun fetchSnapshotToken(nonce: String): String? {
        _result.value = IntegrityResult.Pending
        return try {
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()
            
            val response = integrityManager.requestIntegrityToken(request).await()
            val token = response.token()
            _result.value = IntegrityResult.Success(token)
            token
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown integrity error"
            Log.e("IntegrityRepository", "Failed to fetch integrity token: $msg", e)
            _result.value = IntegrityResult.Failure(msg)
            null
        }
    }

    /**
     * Standard Integrity API support. 
     * Requires a Cloud Project Number to be configured in the Play Console.
     */
    suspend fun fetchStandardToken(projectNumber: Long, requestHash: String): String? {
        _result.value = IntegrityResult.Pending
        return try {
            // 1. Warm up the integrity manager
            val integrityTokenProvider = standardIntegrityManager.prepareIntegrityToken(
                StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(projectNumber)
                    .build()
            ).await()

            // 2. Request the token
            val tokenRequest = StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()

            val response = integrityTokenProvider.request(tokenRequest).await()
            val token = response.token()
            _result.value = IntegrityResult.Success(token)
            token
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown standard integrity error"
            Log.e("IntegrityRepository", "Failed to fetch standard integrity token: $msg", e)
            _result.value = IntegrityResult.Failure(msg)
            null
        }
    }
}
