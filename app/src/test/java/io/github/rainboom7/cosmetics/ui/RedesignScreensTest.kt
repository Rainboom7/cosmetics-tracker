package io.github.rainboom7.cosmetics.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.Condition
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.usage.ItemUsage
import io.github.rainboom7.cosmetics.core.usage.MonthUsage
import io.github.rainboom7.cosmetics.ui.calendar.CalendarScreen
import io.github.rainboom7.cosmetics.ui.calendar.CalendarUiState
import io.github.rainboom7.cosmetics.ui.collection.CollectionScreen
import io.github.rainboom7.cosmetics.ui.collection.CollectionUiState
import io.github.rainboom7.cosmetics.ui.detail.DetailScreen
import io.github.rainboom7.cosmetics.ui.theme.CosmeticsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/** Screens added or reworked in the pastel redesign: statuses, favorites, usage calendar. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "ru-w411dp-h891dp")
class RedesignScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val cream = CosmeticItem(id = 1, name = "Крем", brand = "La Roche", usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 9, 1), favorite = true)
    private val serum = CosmeticItem(id = 2, name = "Сыворотка", shelfStatus = ShelfStatus.TRYING)

    @Test
    fun collectionShowsStatusChipsAndPills() {
        var shelf: ShelfStatus? = null
        var favorites = false
        var category: Category? = null
        var heart: Long? = null
        var usedToday: Long? = null
        compose.setContent {
            CosmeticsTheme {
                CollectionScreen(
                    state = CollectionUiState(
                        loading = false, items = listOf(cream, serum), totalCount = 2, today = today,
                        shelfCounts = mapOf(ShelfStatus.TRYING to 1), favoriteCount = 1,
                    ),
                    onTextChange = {}, onCategory = { category = it }, onOpenItem = {}, onAdd = {}, onPhotoSearch = {}, onAbout = {},
                    onShelf = { shelf = it }, onFavoritesOnly = { favorites = true },
                    onToggleFavorite = { heart = it }, onToggleUsedToday = { usedToday = it },
                )
            }
        }
        compose.onNodeWithTag("status_chips").assertIsDisplayed()
        compose.onNodeWithContentDescription("Пробую, 1").assertIsDisplayed() // the story circle
        compose.onNodeWithTag("story_TRYING").performClick()
        assertEquals(ShelfStatus.TRYING, shelf)
        compose.onNodeWithTag("story_FAVORITES").performClick()
        assertEquals(true, favorites)
        compose.onNodeWithTag("category_MAKEUP").performClick()
        assertEquals(Category.MAKEUP, category)

        compose.onNode(hasTestTag("item_card") and hasText("Пробую")).performScrollTo().assertIsDisplayed() // pill on the card
        compose.onAllNodesWithTag("card_today")[0].performScrollTo().performClick()
        assertEquals(1L, usedToday)
        compose.onAllNodesWithTag("card_heart")[1].performScrollTo().performClick()
        assertEquals(2L, heart)
    }

    @Test
    fun detailHasConditionChipsAndMarksDays() {
        var condition: Condition? = null
        var marked = false
        var favorite = false
        compose.setContent {
            CosmeticsTheme {
                DetailScreen(
                    item = cream, today = today, onBack = {}, onEdit = {}, onDelete = {}, onToggleWantBadly = {},
                    onCondition = { condition = it }, onRemaining = {}, onOpenedOn = {}, onAddPhoto = {}, onBuyAgain = {}, onBought = {},
                    usage = ItemUsage(lastUsed = LocalDate.of(2026, 10, 20), last30Days = 3, total = 3),
                    onToggleFavorite = { favorite = true }, onToggleUsedToday = { marked = true },
                )
            }
        }
        // The top buttons scroll away with the photo, so press the heart first.
        compose.onNodeWithTag("favorite_button", useUnmergedTree = true).performClick()
        assertEquals(true, favorite)
        compose.onNodeWithText("Последний раз: 20 октября").assertExists()
        compose.onNodeWithTag("condition_chips").performScrollTo()
        compose.onNodeWithText("Открыто").assertIsDisplayed() // the current condition
        compose.onNodeWithText("Есть запас").performClick()
        assertEquals(Condition.STOCKED, condition)
        compose.onNodeWithTag("used_other_day").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("used_today").performScrollTo().performClick()
        assertEquals(true, marked)
    }

    @Test
    fun calendarShowsMonthDayItemsAndForgotten() {
        var selected: LocalDate? = null
        var removed: Pair<Long, Boolean>? = null
        val state = CalendarUiState(
            loading = false,
            today = today,
            selected = today,
            usage = MonthUsage(
                month = YearMonth.of(2026, 10),
                days = mapOf(today to listOf(cream)),
                streak = 4,
                forgotten = listOf(serum),
            ),
            markable = listOf(cream, serum),
            empties = 2,
        )
        compose.setContent {
            CosmeticsTheme {
                CalendarScreen(state, {}, {}, onSelect = { selected = it }, onSetUsed = { id, used -> removed = id to used }, onOpenItem = {})
            }
        }
        compose.onNodeWithText("Октябрь 2026").assertIsDisplayed()
        compose.onNodeWithText("дня подряд").assertIsDisplayed()
        compose.onNodeWithTag("streak").assertIsDisplayed()
        compose.onNodeWithText("пустые баночки за год").assertIsDisplayed()
        compose.onNodeWithTag("selected_day").assertIsDisplayed()
        compose.onNodeWithText("22 октября").assertIsDisplayed()

        compose.onNodeWithTag("day_15").performClick()
        assertEquals(LocalDate.of(2026, 10, 15), selected)

        compose.onNodeWithTag("calendar").performScrollToNode(hasTestTag("forgotten_item"))
        compose.onNodeWithText("Давно не пользовались").assertIsDisplayed()
        compose.onNodeWithTag("calendar").performScrollToNode(hasTestTag("day_item"))
        compose.onNodeWithTag("day_item").assertIsDisplayed()
    }
}
