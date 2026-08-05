package com.vpsguardian.app.data.repository

import com.vpsguardian.app.data.local.dao.AlertDao
import com.vpsguardian.app.data.local.dao.HistoryDao
import com.vpsguardian.app.data.local.dao.ServiceDao
import com.vpsguardian.app.data.local.dao.VpsDao
import com.vpsguardian.app.data.mapper.Mappers.toDomain
import com.vpsguardian.app.data.mapper.Mappers.toEntity
import com.vpsguardian.app.data.mapper.Mappers.toVpsUpdate
import com.vpsguardian.app.data.local.entity.VpsEntity
import com.vpsguardian.app.data.local.entity.HistoryEntity
import com.vpsguardian.app.data.mapper.Mappers.toEntity as serviceToEntity
import kotlinx.coroutines.flow.first
import com.vpsguardian.app.data.remote.api.VpsAgentApi
import com.vpsguardian.app.data.remote.dto.RestartRequestDto
import com.vpsguardian.app.data.remote.dto.TokenRequest
import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.VpsServer
import com.vpsguardian.app.domain.repository.VpsRepository
import com.vpsguardian.app.security.AesEncryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpsRepositoryImpl @Inject constructor(
    private val vpsDao: VpsDao,
    private val serviceDao: ServiceDao,
    private val historyDao: HistoryDao,
    private val alertDao: AlertDao,
    private val aesEncryption: AesEncryption
) : VpsRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun getAllVps(): Flow<List<VpsServer>> =
        vpsDao.getAll().map { entities ->
            entities.map { entity ->
                entity.toDomain(decryptToken(entity.encryptedToken))
            }
        }

    override fun getVpsById(id: Long): Flow<VpsServer?> =
        vpsDao.getById(id).map { entity ->
            entity?.toDomain(decryptToken(entity.encryptedToken))
        }

    override suspend fun addVps(vps: VpsServer): Long {
        val encrypted = aesEncryption.encrypt(vps.token)
        return vpsDao.insert(vps.toEntity(encrypted))
    }

    override suspend fun updateVps(vps: VpsServer) {
        val encrypted = aesEncryption.encrypt(vps.token)
        vpsDao.update(vps.toEntity(encrypted))
    }

    override suspend fun deleteVps(id: Long) {
        serviceDao.deleteByVps(id)
        vpsDao.delete(id)
    }

    override suspend fun syncVps(id: Long): Result<VpsServer> = runCatching {
        val entity: VpsEntity = vpsDao.getById(id).first()
            ?: throw Exception("VPS não encontrada")

        val token = decryptToken(entity.encryptedToken)
        val api = createApi(entity.ip, entity.port, token)
        val inventory = api.getInventory()

        val updated = inventory.vps.toVpsUpdate(entity.toDomain(token))
        vpsDao.update(updated.toEntity(entity.encryptedToken))

        val services = inventory.services.map { it.serviceToEntity(id) }
        serviceDao.deleteByVps(id)
        serviceDao.insertAll(services)

        val bootCheck = api.checkBoot()
        if (bootCheck.rebootDetected) {
            historyDao.insert(
                HistoryEntity(
                    id = java.util.UUID.randomUUID().toString().take(8),
                    vpsId = id,
                    eventType = "reboot",
                    target = inventory.vps.hostname,
                    description = bootCheck.message
                )
            )
        }

        updated
    }

    override suspend fun syncAllVps(): Result<List<VpsServer>> = runCatching {
        val entities = vpsDao.getAll().first()
        entities.mapNotNull { entity ->
            syncVps(entity.id).getOrNull()
        }
    }

    private fun decryptToken(encrypted: String): String = try {
        aesEncryption.decrypt(encrypted)
    } catch (_: Exception) {
        encrypted
    }

    private fun createApi(ip: String, port: Int, token: String): VpsAgentApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val authRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${getAccessToken(ip, port, token)}")
                    .build()
                chain.proceed(authRequest)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://$ip:$port/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create()
    }

    private val tokenCache = mutableMapOf<String, String>()

    private suspend fun getAccessToken(ip: String, port: Int, token: String): String {
        val key = "$ip:$port"
        tokenCache[key]?.let { return it }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://$ip:$port/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create<VpsAgentApi>()
        val response = api.authenticate(TokenRequest(token))
        tokenCache[key] = response.accessToken
        return response.accessToken
    }
}

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val serviceDao: ServiceDao,
    private val vpsDao: VpsDao,
    private val aesEncryption: AesEncryption
) : com.vpsguardian.app.domain.repository.ServiceRepository {

    override fun getServicesByVps(vpsId: Long): Flow<List<Service>> =
        serviceDao.getByVps(vpsId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshServices(vpsId: Long): Result<List<Service>> = runCatching {
        emptyList()
    }

    override suspend fun restartService(vpsId: Long, serviceId: String, type: String): Result<String> =
        runCatching { "Restart solicitado" }

    override suspend fun startService(vpsId: Long, serviceId: String): Result<String> =
        runCatching { "Start solicitado" }

    override suspend fun stopService(vpsId: Long, serviceId: String): Result<String> =
        runCatching { "Stop solicitado" }

    override suspend fun getLogs(vpsId: Long, serviceId: String, lines: Int, search: String?): Result<List<String>> =
        runCatching { emptyList() }

    override suspend fun healthCheck(vpsId: Long, serviceId: String): Result<Boolean> =
        runCatching { true }
}
