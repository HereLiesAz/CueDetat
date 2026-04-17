package com.hereliesaz.cuedetat.network

import androidx.annotation.Keep
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@Keep
data class TrajectoryPoint(
    val x: Float,
    val y: Float
)

@Keep
data class TrajectoryResponse(
    val points: List<TrajectoryPoint>,
    val confidence: Float
)

interface MyriadApi {
    @Multipart
    @POST("predict")
    suspend fun predictTrajectory(
        @Part file: MultipartBody.Part,
        @Part("poke_x") pokeX: RequestBody,
        @Part("poke_y") pokeY: RequestBody,
        @Part("poke_dx") pokeDx: RequestBody,
        @Part("poke_dy") pokeDy: RequestBody
    ): TrajectoryResponse
}
