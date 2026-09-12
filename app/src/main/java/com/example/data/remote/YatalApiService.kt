package com.example.data.remote

import com.example.data.model.BookDto
import com.example.data.model.BooksResponseDto
import com.example.data.model.CategoryDto
import com.example.data.model.QuotaDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface YatalApiService {

    @GET("api/categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("api/books")
    suspend fun getBooks(
        @Query("category") category: String = "ALL",
        @Query("q") query: String = "",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "default"
    ): BooksResponseDto

    @GET("api/book/{id}")
    suspend fun getBookDetail(
        @Path("id") id: Long
    ): BookDto

    @GET("api/quota")
    suspend fun getQuota(): QuotaDto

    companion object {
        private const val BASE_URL = "https://library.yatal.tech/"

        fun create(): YatalApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) YatalLibraryApp/1.0")
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YatalApiService::class.java)
        }
    }
}
