package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import android.graphics.PointF
import com.hereliesaz.cuedetat.network.MyriadApi
import com.hereliesaz.cuedetat.network.TrajectoryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyriadRepository @Inject constructor(
    private val myriadApi: MyriadApi
) {
    suspend fun fetchTrajectory(
        bitmap: Bitmap,
        pokeStart: PointF,
        pokeVector: PointF
    ): Result<TrajectoryResponse> = withContext(Dispatchers.IO) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()
            
            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", "frame.jpg", requestFile)
            
            val pokeX = pokeStart.x.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val pokeY = pokeStart.y.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val pokeDx = pokeVector.x.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val pokeDy = pokeVector.y.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = myriadApi.predictTrajectory(
                imagePart, pokeX, pokeY, pokeDx, pokeDy
            )
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
