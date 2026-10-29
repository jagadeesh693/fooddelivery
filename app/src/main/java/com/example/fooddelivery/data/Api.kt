package com.example.fooddelivery.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApi {
    @GET("categories.php") suspend fun categories(): CategoriesResponse
    @GET("filter.php") suspend fun byCategory(@Query("c") category: String): MealsResponse
    @GET("search.php") suspend fun search(@Query("s") query: String): MealsResponse
    @GET("lookup.php") suspend fun lookup(@Query("i") id: String): MealsResponse
}

object Network {
    val api: MealApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.themealdb.com/api/json/v1/1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MealApi::class.java)
    }
}
