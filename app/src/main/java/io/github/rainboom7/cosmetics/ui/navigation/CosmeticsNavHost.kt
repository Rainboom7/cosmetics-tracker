package io.github.rainboom7.cosmetics.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.ui.about.AboutRoute
import io.github.rainboom7.cosmetics.ui.catalog.CatalogRoute
import io.github.rainboom7.cosmetics.ui.collection.CollectionRoute
import io.github.rainboom7.cosmetics.ui.detail.DetailRoute
import io.github.rainboom7.cosmetics.ui.form.AddFormRoute
import io.github.rainboom7.cosmetics.ui.scan.ScanMode
import io.github.rainboom7.cosmetics.ui.scan.ScanRoute
import io.github.rainboom7.cosmetics.ui.wishlist.WishlistRoute

/** Route strings, matching the design spec. Arguments are ids and enum names only. */
object Routes {
    const val COLLECTION = "collection"
    const val WISHLIST = "wishlist"
    const val CATALOG = "catalog?query={query}"
    const val PRODUCT = "product/{id}"
    const val ADD_SCAN = "add_scan?target={target}&mode={mode}"
    const val ADD_FORM = "add_form?target={target}&editId={editId}"
    const val ABOUT = "about"

    fun catalog(query: String = "") = "catalog?query=${android.net.Uri.encode(query)}"
    fun product(id: Long) = "product/$id"
    fun addScan(target: ItemStatus = ItemStatus.OWNED, mode: ScanMode = ScanMode.BARCODE) = "add_scan?target=${target.name}&mode=${mode.name}"
    fun addForm(target: ItemStatus = ItemStatus.OWNED, editId: Long? = null) = "add_form?target=${target.name}&editId=${editId ?: -1}"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector, val navigateTo: String)

private val Tabs = listOf(
    Tab(Routes.COLLECTION, R.string.tab_collection, Icons.Outlined.Spa, Icons.Filled.Spa, Routes.COLLECTION),
    Tab(Routes.WISHLIST, R.string.tab_wishlist, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite, Routes.WISHLIST),
    Tab(Routes.CATALOG, R.string.tab_catalog, Icons.Outlined.Search, Icons.Filled.Search, Routes.catalog()),
)

@Composable
fun CosmeticsNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = Tabs.any { it.route == currentRoute }

    // Insets: the bar handles the navigation bar; each screen handles the status bar itself.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tabs.forEach { tab ->
                        val selected = tab.route == currentRoute
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.navigateTo) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                            modifier = Modifier.testTag("tab_" + tab.route.substringBefore('?')),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val tabModifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding()
        NavHost(navController, startDestination = Routes.COLLECTION) {
            composable(Routes.COLLECTION) {
                Box(tabModifier) {
                    CollectionRoute(
                        onOpenItem = { navController.navigate(Routes.product(it)) },
                        onAdd = { navController.navigate(Routes.addScan()) },
                        onPhotoSearch = { navController.navigate(Routes.addScan(mode = ScanMode.PHOTO)) },
                        onAbout = { navController.navigate(Routes.ABOUT) },
                    )
                }
            }
            composable(Routes.WISHLIST) {
                Box(tabModifier) {
                    WishlistRoute(
                        onOpenItem = { navController.navigate(Routes.product(it)) },
                        onOpenCatalog = { navController.navigate(Routes.catalog()) },
                    )
                }
            }
            composable(Routes.CATALOG, arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })) { entry ->
                Box(tabModifier) {
                    CatalogRoute(
                        initialQuery = entry.arguments?.getString("query").orEmpty(),
                        onScanBarcode = { navController.navigate(Routes.addScan()) },
                        onPhotoSearch = { navController.navigate(Routes.addScan(mode = ScanMode.PHOTO)) },
                        onAddManually = { navController.navigate(Routes.addForm()) },
                    )
                }
            }
            composable(Routes.PRODUCT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("id")
                DetailRoute(
                    id = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.addForm(editId = it)) },
                )
            }
            composable(
                Routes.ADD_SCAN,
                arguments = listOf(
                    navArgument("target") { type = NavType.StringType; defaultValue = ItemStatus.OWNED.name },
                    navArgument("mode") { type = NavType.StringType; defaultValue = ScanMode.BARCODE.name },
                ),
            ) { entry ->
                val target = ItemStatus.valueOf(entry.arguments?.getString("target") ?: ItemStatus.OWNED.name)
                ScanRoute(
                    target = target,
                    initialMode = ScanMode.valueOf(entry.arguments?.getString("mode") ?: ScanMode.BARCODE.name),
                    onBack = { navController.popBackStack() },
                    onOpenForm = {
                        navController.navigate(Routes.addForm(target)) { popUpTo(Routes.ADD_SCAN) { inclusive = true } }
                    },
                    onAdded = { navController.popBackStack() },
                )
            }
            composable(
                Routes.ADD_FORM,
                arguments = listOf(
                    navArgument("target") { type = NavType.StringType; defaultValue = ItemStatus.OWNED.name },
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                val editId = entry.arguments?.getLong("editId")?.takeIf { it > 0 }
                AddFormRoute(
                    target = ItemStatus.valueOf(entry.arguments?.getString("target") ?: ItemStatus.OWNED.name),
                    editId = editId,
                    onClose = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.popBackStack()
                        if (editId == null) navController.navigate(Routes.product(id))
                    },
                    onOpenItem = { navController.navigate(Routes.product(it)) },
                )
            }
            composable(Routes.ABOUT) { AboutRoute(onBack = { navController.popBackStack() }) }
        }
    }
}
