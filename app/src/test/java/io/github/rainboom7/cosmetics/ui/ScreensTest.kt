package io.github.rainboom7.cosmetics.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.collection.CollectionQuery
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.photo.PhotoMatch
import io.github.rainboom7.cosmetics.testing.Samples
import io.github.rainboom7.cosmetics.ui.collection.CollectionScreen
import io.github.rainboom7.cosmetics.ui.collection.CollectionUiState
import io.github.rainboom7.cosmetics.ui.form.AddFormScreen
import io.github.rainboom7.cosmetics.ui.form.FormFields
import io.github.rainboom7.cosmetics.ui.form.FormUiState
import io.github.rainboom7.cosmetics.ui.scan.CandidatesContent
import io.github.rainboom7.cosmetics.ui.scan.DraftReason
import io.github.rainboom7.cosmetics.ui.theme.CosmeticsTheme
import io.github.rainboom7.cosmetics.ui.wishlist.WishRow
import io.github.rainboom7.cosmetics.ui.wishlist.WishlistScreen
import io.github.rainboom7.cosmetics.ui.wishlist.WishlistUiState
import io.github.rainboom7.cosmetics.core.collection.WishlistFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Screen-level UI tests on the JVM (Robolectric): stateless screens fed with fixed state. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun collectionShowsCardsBadgesAndBanner() {
        val items = listOf(
            CosmeticItem(id = 1, name = "Aloe Water", brand = "L'Oréal", usage = UsageState.OPENED, remainingPercent = 80,
                openedOn = LocalDate.of(2026, 5, 1), periodAfterOpeningMonths = 6),
            CosmeticItem(id = 2, name = "Тушь", usage = UsageState.OPENED, remainingPercent = 20),
        )
        var opened = -1L
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                CollectionScreen(
                    state = CollectionUiState(loading = false, items = items, totalCount = 2, expiringSoon = 1, today = today),
                    onTextChange = {}, onCategory = {}, onOpenItem = { opened = it }, onAdd = {}, onPhotoSearch = {}, onAbout = {},
                )
            }
        }

        compose.onNodeWithText("Aloe Water").assertIsDisplayed()
        compose.onNodeWithText("L'ORÉAL").assertIsDisplayed()
        compose.onNodeWithText("Истекает").assertIsDisplayed()
        compose.onNodeWithText("Заканчивается").assertIsDisplayed()
        compose.onNodeWithText("Открыт 01.05 · до 11.2026").assertIsDisplayed()
        compose.onNodeWithTag("expiring_banner").assertIsDisplayed()
        compose.onNodeWithText("Все · 2").assertIsDisplayed()

        compose.onNodeWithText("Тушь").performClick()
        assertEquals(2L, opened)
    }

    @Test
    fun emptyCollectionInvitesToAdd() {
        var added = false
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                CollectionScreen(CollectionUiState(loading = false, query = CollectionQuery()), {}, {}, {}, onAdd = { added = true }, {}, {})
            }
        }
        compose.onNodeWithText("Добавьте первое средство").assertIsDisplayed()
        compose.onNodeWithTag("expiring_banner").assertDoesNotExist()
        compose.onAllNodesWithTag("item_card").assertCountEquals(0)
        compose.onAllNodesWithText("Добавить")[0].performClick()
        assertEquals(true, added)
    }

    @Test
    fun wishlistBoughtButtonAndSimilarHint() {
        var bought = -1L
        val rows = listOf(
            WishRow(CosmeticItem(id = 7, name = "Помада", status = ItemStatus.WISHLIST, wantBadly = true, price = "990 ₽"), alreadyOwned = false),
            WishRow(CosmeticItem(id = 8, name = "Aloe Water", status = ItemStatus.WISHLIST), alreadyOwned = true),
        )
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                WishlistScreen(WishlistUiState(false, WishlistFilter.ALL, rows, 2), SnackbarHostState(), {}, { bought = it }, {}, {})
            }
        }
        compose.onNodeWithText("Похожее уже есть у вас").assertIsDisplayed()
        compose.onNodeWithText("990 ₽").assertIsDisplayed()
        compose.onAllNodesWithTag("bought_button")[0].performClick()
        assertEquals(7L, bought)
    }

    @Test
    fun formSaveButtonsFollowRequirements() {
        val incomplete = FormUiState(fields = FormFields(name = "Крем"), reason = DraftReason.BARCODE_NOT_FOUND, barcode = "4006381333931")
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                AddFormScreen(incomplete, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("reason_banner").assertIsDisplayed()
        compose.onNodeWithText("Штрихкод: 4006381333931").assertIsDisplayed()
        compose.onNodeWithTag("save_collection").assertIsNotEnabled()
        compose.onNodeWithTag("save_wishlist").assertIsNotEnabled()
    }

    @Test
    fun formWithPhotoAndNameCanSave() {
        var savedTo: ItemStatus? = null
        val complete = FormUiState(fields = FormFields(name = "Крем", photoUri = "file:///p.jpg"))
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                AddFormScreen(complete, {}, { savedTo = it }, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("reason_banner").assertDoesNotExist()
        compose.onNodeWithTag("save_collection").assertIsEnabled().performClick()
        assertEquals(ItemStatus.OWNED, savedTo)
    }

    @Test
    fun candidatesSheetShowsConfidenceAndPicks() {
        var picked: Pair<PhotoMatch, ItemStatus>? = null
        var nothing = false
        val matches = listOf(PhotoMatch(Samples.aloe, 83), PhotoMatch(Samples.mascara, 33))
        compose.setContent {
            CosmeticsTheme(dynamicColor = false) {
                CandidatesContent(matches, onPick = { m, s -> picked = m to s }, onNothingFits = { nothing = true })
            }
        }
        compose.onNodeWithText("Это оно?").assertIsDisplayed()
        compose.onNodeWithText("Совпадение 83%").assertIsDisplayed()
        compose.onNodeWithText("Совпадение 33%").assertIsDisplayed()
        compose.onNodeWithTag("pick_owned").performClick()
        assertEquals(matches[0] to ItemStatus.OWNED, picked)
        compose.onNodeWithText("Ничего не подходит — заполнить по фото").performClick()
        assertEquals(true, nothing)
    }
}
