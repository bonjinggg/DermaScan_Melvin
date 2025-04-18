package com.example.dermascanai

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface PsgcApiService {
    @GET("provinces/")
    fun getProvinces(): Call<List<LocationModel>>

    @GET("provinces/{provinceCode}/cities-municipalities/")
    fun getCities(@Path("provinceCode") provinceCode: String): Call<List<LocationModel>>

    @GET("cities-municipalities/{cityCode}/barangays/")
    fun getBarangays(@Path("cityCode") cityCode: String): Call<List<LocationModel>>
}
