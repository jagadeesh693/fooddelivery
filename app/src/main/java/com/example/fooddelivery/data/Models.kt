package com.example.fooddelivery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class CategoriesResponse(val categories: List<Category>?)
data class Category(val idCategory: String, val strCategory: String, val strCategoryThumb: String?)

data class MealsResponse(val meals: List<Meal>?)
data class Meal(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String? = null,
    val strCategory: String? = null,
    val strArea: String? = null,
    val strInstructions: String? = null
)

/** TheMealDB has no prices, so we derive a stable demo price from the meal id. */
fun Meal.price(): Double = 5.0 + ((idMeal.toIntOrNull() ?: 0) % 10) + 0.99

data class CartItem(
    val id: String,
    val name: String,
    val thumb: String,
    val price: Double,
    val qty: Int
)

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumb: String,
    val price: Double
)

fun FavouriteEntity.toMeal() = Meal(idMeal = id, strMeal = name, strMealThumb = thumb)
