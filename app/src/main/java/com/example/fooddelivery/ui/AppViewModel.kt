package com.example.fooddelivery.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fooddelivery.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).favouriteDao()
    val prefs = Prefs(app)
    private val auth = FirebaseAuth.getInstance()

    // ---- auth ----
    var user by mutableStateOf(auth.currentUser)
    var authError by mutableStateOf<String?>(null)
    var authBusy by mutableStateOf(false)

    // ---- menu ----
    var categories by mutableStateOf<List<Category>>(emptyList())
    var selected by mutableStateOf("Seafood")
    var meals by mutableStateOf<List<Meal>>(emptyList())
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var detail by mutableStateOf<Meal?>(null)

    // ---- cart / favourites ----
    var cart by mutableStateOf<Map<String, CartItem>>(emptyMap())
    val favourites = dao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadCategories()
        loadMeals()
    }

    // ---------- auth ----------
    fun login(email: String, password: String) = authCall(email, password) {
        auth.signInWithEmailAndPassword(email.trim(), password)
    }

    fun signUp(email: String, password: String) = authCall(email, password) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
    }

    private fun authCall(
        email: String,
        password: String,
        call: () -> com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>
    ) {
        if (email.isBlank() || password.length < 6) {
            authError = "Enter an email and a password of at least 6 characters."
            return
        }
        authBusy = true
        authError = null
        call().addOnCompleteListener { task ->
            authBusy = false
            if (task.isSuccessful) user = auth.currentUser
            else authError = task.exception?.localizedMessage ?: "Authentication failed"
        }
    }

    fun logout() {
        auth.signOut()
        user = null
    }

    // ---------- menu ----------
    fun loadCategories() = viewModelScope.launch {
        try { categories = Network.api.categories().categories.orEmpty() } catch (_: Exception) {}
    }

    fun loadMeals() = viewModelScope.launch {
        loading = true
        error = null
        try {
            meals = Network.api.byCategory(selected).meals.orEmpty()
        } catch (_: Exception) {
            error = "Could not load the menu. Check your internet connection."
        }
        loading = false
    }

    fun selectCategory(name: String) {
        selected = name
        loadMeals()
    }

    fun search(query: String) {
        if (query.isBlank()) { loadMeals(); return }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                meals = Network.api.search(query.trim()).meals.orEmpty()
            } catch (_: Exception) {
                error = "Search failed. Check your internet connection."
            }
            loading = false
        }
    }

    fun loadDetail(id: String) {
        detail = null
        viewModelScope.launch {
            try { detail = Network.api.lookup(id).meals?.firstOrNull() } catch (_: Exception) {}
        }
    }

    // ---------- cart ----------
    fun addToCart(m: Meal) {
        val cur = cart[m.idMeal]
        val item = cur?.copy(qty = cur.qty + 1)
            ?: CartItem(m.idMeal, m.strMeal, m.strMealThumb.orEmpty(), m.price(), 1)
        cart = cart + (m.idMeal to item)
    }

    fun changeQty(id: String, delta: Int) {
        val cur = cart[id] ?: return
        val q = cur.qty + delta
        cart = if (q <= 0) cart - id else cart + (id to cur.copy(qty = q))
    }

    fun placeOrder() { cart = emptyMap() }

    // ---------- favourites ----------
    fun toggleFavourite(m: Meal) = viewModelScope.launch {
        if (favourites.value.any { it.id == m.idMeal }) dao.deleteById(m.idMeal)
        else dao.insert(FavouriteEntity(m.idMeal, m.strMeal, m.strMealThumb.orEmpty(), m.price()))
    }

    // ---------- preferences ----------
    fun finishOnboarding() = viewModelScope.launch { prefs.setOnboarded(true) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { prefs.setNotifications(v) }
    fun setAutoRefresh(v: Boolean) = viewModelScope.launch { prefs.setAutoRefresh(v) }
}
