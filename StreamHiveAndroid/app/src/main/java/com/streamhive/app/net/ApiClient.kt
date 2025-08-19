package com.streamhive.app.net

import android.content.Context
import com.squareup.moshi.Moshi
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

object ApiClient {
    lateinit var appContext: Context
        private set

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    val raw: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val moshi = Moshi.Builder().build()
    private val retrofit by lazy {
        val baseUrl = BuildConfigHolder.baseUrl
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(raw)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: Api by lazy { retrofit.create(Api::class.java) }

    fun init(context: Context, baseUrl: String) {
        appContext = context.applicationContext
        BuildConfigHolder.baseUrl = baseUrl
    }

    fun putPresigned(url: String, body: RequestBody) {
        val request = Request.Builder().url(url).put(body).build()
        raw.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed with ${'$'}{response.code}")
            }
        }
    }
}

object BuildConfigHolder {
    // Note: Retrofit requires baseUrl to end with '/'
    var baseUrl: String = "http://10.0.2.2:8080/"
}

interface Api {
    @GET("v1/files")
    suspend fun listFiles(): ListFilesResponse

    @POST("v1/uploads/presign")
    suspend fun requestUpload(@Body body: Map<String, Any?>): PresignUploadResponse

    @POST("v1/uploads/commit")
    suspend fun commitUpload(@Body body: Map<String, Any?>): CommitUploadResponse

    @GET("v1/files/presign-get")
    suspend fun presignGet(@Query("key") key: String): PresignGetResponse
}

data class FileItem(
    val id: String,
    val name: String,
    val size: Long,
    val mime: String,
    val key: String,
    val url: String,
    val createdAt: Long
)

data class ListFilesResponse(val files: List<FileItem>)
data class PresignUploadResponse(val uploadUrl: String, val uploadId: String)
data class CommitUploadResponse(val ok: Boolean, val item: FileItem)
data class PresignGetResponse(val url: String, val expiresIn: Int)

