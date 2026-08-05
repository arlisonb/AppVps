package com.vpsguardian.app.data.remote.api

import com.vpsguardian.app.data.remote.dto.AlertEventDto
import com.vpsguardian.app.data.remote.dto.BootDetectionDto
import com.vpsguardian.app.data.remote.dto.HealthCheckResponseDto
import com.vpsguardian.app.data.remote.dto.HistoryEntryDto
import com.vpsguardian.app.data.remote.dto.InventoryResponseDto
import com.vpsguardian.app.data.remote.dto.LogsRequestDto
import com.vpsguardian.app.data.remote.dto.LogsResponseDto
import com.vpsguardian.app.data.remote.dto.RefreshRequest
import com.vpsguardian.app.data.remote.dto.RestartRequestDto
import com.vpsguardian.app.data.remote.dto.RestartResponseDto
import com.vpsguardian.app.data.remote.dto.SystemMetricsDto
import com.vpsguardian.app.data.remote.dto.TokenRequest
import com.vpsguardian.app.data.remote.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VpsAgentApi {

    @POST("auth/token")
    suspend fun authenticate(@Body request: TokenRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): TokenResponse

    @GET("inventory")
    suspend fun getInventory(): InventoryResponseDto

    @GET("metrics")
    suspend fun getMetrics(): SystemMetricsDto

    @GET("boot-check")
    suspend fun checkBoot(): BootDetectionDto

    @POST("restart")
    suspend fun restart(@Body request: RestartRequestDto): RestartResponseDto

    @POST("logs")
    suspend fun getLogs(@Body request: LogsRequestDto): LogsResponseDto

    @GET("health-check/{serviceId}")
    suspend fun healthCheck(@Path("serviceId") serviceId: String): HealthCheckResponseDto

    @GET("history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 100,
        @Query("event_type") eventType: String? = null,
        @Query("target") target: String? = null
    ): List<HistoryEntryDto>

    @GET("alerts")
    suspend fun getAlerts(): List<AlertEventDto>
}
