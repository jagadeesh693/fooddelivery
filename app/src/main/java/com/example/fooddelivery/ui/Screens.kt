@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fooddelivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.fooddelivery.data.*
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import java.util.Locale

private fun money(v: Double) = String.format(Locale.US, "$%.2f", v)

// =====================================================================
// Root: onboarding -> auth -> main app
// =====================================================================
@Composable
fun FoodApp(vm: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val onboarded by vm.prefs.onboarded.collectAsState(initial = null)
    when {
        onboarded == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        onboarded == false -> OnboardingScreen { vm.finishOnboarding() }
        vm.user == null -> AuthScreen(vm)
        else -> MainScaffold(vm)
    }
}

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ShoppingCart, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Hungry? We deliver.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Browse meals, save favourites and order in a few taps.")
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
fun AuthScreen(vm: AppViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isLogin) "Welcome back" else "Create account",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            email, { email = it }, Modifier.fillMaxWidth(),
            label = { Text("Email") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        vm.authError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (isLogin) vm.login(email, password) else vm.signUp(email, password) },
            enabled = !vm.authBusy, modifier = Modifier.fillMaxWidth()
        ) {
            if (vm.authBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (isLogin) "Log in" else "Sign up")
        }
        TextButton(onClick = { isLogin = !isLogin; vm.authError = null }, Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (isLogin) "No account? Sign up" else "Have an account? Log in")
        }
    }
}

// =====================================================================
// Main scaffold with bottom navigation
// =====================================================================
private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Menu", Icons.Default.Home),
    Tab("favourites", "Favourites", Icons.Default.Favorite),
    Tab("cart", "Cart", Icons.Default.ShoppingCart),
    Tab("profile", "Profile", Icons.Default.Person)
)

@Composable
fun MainScaffold(vm: AppViewModel) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val cartCount = vm.cart.values.sumOf { it.qty }

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (t.route == "cart" && cartCount > 0)
                                    BadgedBox(badge = { Badge { Text("$cartCount") } }) { Icon(t.icon, null) }
                                else Icon(t.icon, null)
                            },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm) { nav.navigate("detail/$it") } }
            composable("favourites") { FavouritesScreen(vm) { nav.navigate("detail/$it") } }
            composable("cart") { CartScreen(vm) }
            composable("profile") { ProfileScreen(vm) }
            composable("detail/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
                DetailScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
        }
    }
}

// =====================================================================
// Home
// =====================================================================
@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val favIds = vm.favourites.collectAsState().value.map { it.id }.toSet()
    val autoRefresh by vm.prefs.autoRefresh.collectAsState(initial = false)
    var query by remember { mutableStateOf("") }

    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            delay(60_000)
            vm.loadMeals()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "What would you like to eat?",
            Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            query, { query = it },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search meals") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(query) })
        )
        LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.categories) { c ->
                FilterChip(
                    selected = c.strCategory == vm.selected && query.isBlank(),
                    onClick = { query = ""; vm.selectCategory(c.strCategory) },
                    label = { Text(c.strCategory) }
                )
            }
        }
        when {
            vm.loading -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(6) { ShimmerCard() }
            }
            vm.error != null -> Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(vm.error!!)
                Button(onClick = { vm.loadMeals() }) { Text("Retry") }
            }
            vm.meals.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No meals found") }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.meals, key = { it.idMeal }) { m ->
                    MealRow(m, m.idMeal in favIds, { vm.toggleFavourite(m) }, { vm.addToCart(m) }) { onOpen(m.idMeal) }
                }
            }
        }
    }
}

@Composable
fun ShimmerCard() {
    Row(
        Modifier.fillMaxWidth().shimmer().clip(RoundedCornerShape(12.dp)).background(Color.LightGray.copy(alpha = 0.4f)).padding(12.dp)
    ) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(160.dp).height(16.dp).background(Color.LightGray))
            Box(Modifier.width(60.dp).height(14.dp).background(Color.LightGray))
        }
    }
}

@Composable
fun MealRow(meal: Meal, isFav: Boolean, onFav: () -> Unit, onAdd: () -> Unit, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                meal.strMealThumb, meal.strMeal,
                Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.strMeal, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(money(meal.price()), color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onFav) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favourite", tint = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add to cart") }
            }
        }
    }
}

// =====================================================================
// Detail
// =====================================================================
@Composable
fun DetailScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    LaunchedEffect(id) { vm.loadDetail(id) }
    val meal = vm.detail
    val isFav = vm.favourites.collectAsState().value.any { it.id == id }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(meal?.strMeal ?: "Loading…", maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                if (meal != null) IconButton(onClick = { vm.toggleFavourite(meal) }) {
                    Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite")
                }
            }
        )
        if (meal == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                AsyncImage(
                    meal.strMealThumb, meal.strMeal,
                    Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                Text(meal.strMeal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${meal.strCategory.orEmpty()} • ${meal.strArea.orEmpty()}", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(money(meal.price()), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.addToCart(meal) }, Modifier.fillMaxWidth()) { Text("Add to cart") }
                Spacer(Modifier.height(16.dp))
                Text("About this dish", fontWeight = FontWeight.SemiBold)
                Text(meal.strInstructions.orEmpty())
            }
        }
    }
}

// =====================================================================
// Favourites (Room)
// =====================================================================
@Composable
fun FavouritesScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val favs by vm.favourites.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text("Favourites", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (favs.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No favourites yet. Tap the heart on a meal.") }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favs, key = { it.id }) { f ->
                    val meal = f.toMeal()
                    MealRow(meal, true, { vm.toggleFavourite(meal) }, { vm.addToCart(meal) }) { onOpen(f.id) }
                }
            }
        }
    }
}

// =====================================================================
// Cart / checkout
// =====================================================================
@Composable
fun CartScreen(vm: AppViewModel) {
    val items = vm.cart.values.toList()
    val subtotal = items.sumOf { it.price * it.qty }
    val delivery = if (items.isEmpty()) 0.0 else 2.99
    var address by remember { mutableStateOf("") }
    var showDone by remember { mutableStateOf(false) }

    if (showDone) AlertDialog(
        onDismissRequest = { showDone = false },
        confirmButton = { TextButton(onClick = { showDone = false }) { Text("OK") } },
        title = { Text("Order placed!") },
        text = { Text("Your food is on its way.") }
    )

    Column(Modifier.fillMaxSize()) {
        Text("Your cart", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Your cart is empty") }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(c.thumb, c.name, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                Text(money(c.price * c.qty), color = MaterialTheme.colorScheme.primary)
                            }
                            OutlinedButton(onClick = { vm.changeQty(c.id, -1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("−") }
                            Text("${c.qty}", Modifier.padding(horizontal = 10.dp))
                            OutlinedButton(onClick = { vm.changeQty(c.id, 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("+") }
                        }
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Delivery address") },
                    leadingIcon = { Icon(Icons.Default.Place, null) })
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Subtotal"); Text(money(subtotal)) }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Delivery"); Text(money(delivery)) }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Total", fontWeight = FontWeight.Bold); Text(money(subtotal + delivery), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.placeOrder(); address = ""; showDone = true },
                    enabled = address.isNotBlank(), modifier = Modifier.fillMaxWidth()
                ) { Text("Place order") }
            }
        }
    }
}

// =====================================================================
// Profile + settings (DataStore) + logout (Firebase)
// =====================================================================
@Composable
fun ProfileScreen(vm: AppViewModel) {
    val notifications by vm.prefs.notifications.collectAsState(initial = true)
    val autoRefresh by vm.prefs.autoRefresh.collectAsState(initial = false)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(vm.user?.email.orEmpty(), color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Order notifications")
            Switch(notifications, { vm.setNotifications(it) })
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Auto-refresh menu (every 60s)")
            Switch(autoRefresh, { vm.setAutoRefresh(it) })
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { vm.logout() }, Modifier.fillMaxWidth()) { Text("Log out") }
    }
}
