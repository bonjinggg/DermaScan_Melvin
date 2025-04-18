package com.example.dermascanai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val instance: PsgcApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://psgc.gitlab.io/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PsgcApiService::class.java)
    }
}
