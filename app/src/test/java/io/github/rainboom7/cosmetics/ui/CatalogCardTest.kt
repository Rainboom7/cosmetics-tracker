package io.github.rainboom7.cosmetics.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.collection.KnownBarcodes
import io.github.rainboom7.cosmetics.core.model.CatalogProduct
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.testing.Samples
import io.github.rainboom7.cosmetics.ui.catalogcard.CatalogCardScreen
import io.github.rainboom7.cosmetics.ui.catalogcard.CatalogCardUiState
import io.github.rainboom7.cosmetics.ui.theme.CosmeticsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class CatalogCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val detailed = detailedAloe

    companion object {
        val detailedAloe = Samples.aloe.copy(
        ingredients = "Aqua, Glycerin, Aloe Barbadensis Leaf Juice",
        details = CatalogProduct.Details(
            genericName = "Увлажняющая вода для лица",
            categories = listOf("en:non-food-products", "en:face-creams"),
            labels = listOf("en:vegan"),
            periodAfterOpening = "6 months",
            pageUrl = "https://world.openbeautyfacts.org/product/3600523307593",
            ),
        )
    }

    @Test
    fun cardShowsOnlyTheFactsTheCatalogHas() {
        var added: ItemStatus? = null
        var opened: String? = null
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                CatalogCardScreen(CatalogCardUiState(detailed), KnownBarcodes(), SnackbarHostState(), onBack = {}, onAdd = { added = it }, onOpenShop = { opened = it })
            }
        }
        compose.onNodeWithText("Hydra Genius Aloe Water").assertExists()
        compose.onNodeWithText("Увлажняющая вода для лица").assertExists()
        compose.onNodeWithText("Face creams").assertExists()
        compose.onNodeWithText("Vegan").assertExists()
        compose.onNodeWithText("6 months").assertExists()
        compose.onNodeWithText("3600523307593").assertExists()
        compose.onNodeWithText("Aqua, Glycerin, Aloe Barbadensis Leaf Juice").assertExists()
        // Not in the data, so no row for it.
        compose.onAllNodesWithText("Упаковка").assertCountEquals(0)
        compose.onAllNodesWithText("Магазины").assertCountEquals(0)

        compose.onNodeWithTag("card_wish").performScrollTo().performClick()
        assertEquals(ItemStatus.WISHLIST, added)

        compose.onNodeWithText("Подружка").performScrollTo().performClick()
        assertEquals("https://www.podrygka.ru/search/?q=L%27Or%C3%A9al%20Paris%20Hydra%20Genius%20Aloe%20Water", opened)
    }

    @Test
    fun bareProductHasNoEmptySections() {
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                CatalogCardScreen(CatalogCardUiState(Samples.mascara.copy(barcode = null)), KnownBarcodes(), SnackbarHostState(), onBack = {}, onAdd = {})
            }
        }
        compose.onNodeWithText("Volume Million Lashes").assertExists()
        compose.onAllNodesWithTag("card_specs").assertCountEquals(0)
        compose.onAllNodesWithText("Описание").assertCountEquals(0)
        compose.onAllNodesWithText("Состав").assertCountEquals(0)
    }
}
