package io.github.rainboom7.cosmetics.ui

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.collection.WishlistFilter
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.testing.MainDispatcherRule
import io.github.rainboom7.cosmetics.ui.collection.CollectionViewModel
import io.github.rainboom7.cosmetics.ui.wishlist.WishlistViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CollectionAndWishlistViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
    private val repo = InMemoryCollectionRepository(
        listOf(
            CosmeticItem(id = 1, name = "Aloe Water", brand = "L'Oréal", category = Category.SKINCARE, addedAtMillis = 1,
                usage = UsageState.OPENED, openedOn = LocalDate.of(2026, 5, 1), periodAfterOpeningMonths = 6),
            CosmeticItem(id = 2, name = "Тушь", brand = "Maybelline", category = Category.MAKEUP, addedAtMillis = 2),
            CosmeticItem(id = 3, name = "Aloe Water", brand = "L'Oréal", status = ItemStatus.WISHLIST, addedAtMillis = 3),
            CosmeticItem(id = 4, name = "Помада", status = ItemStatus.WISHLIST, wantBadly = true, addedAtMillis = 4),
        ),
    )
    private val service = CollectionService(repo, clock)

    @Test
    fun `collection shows owned items, filters and counts expiring ones`() = runTest(main.dispatcher) {
        val vm = CollectionViewModel(service)
        backgroundScope.launch { vm.state.collect {} }

        val initial = vm.state.value
        assertFalse(initial.loading)
        assertEquals(listOf(2L, 1L), initial.items.map { it.id })
        assertEquals(2, initial.totalCount)
        assertEquals(1, initial.expiringSoon) // Aloe: opened 01.05 + 6 months = 01.11, within 30 days

        vm.setCategory(Category.MAKEUP)
        assertEquals(listOf(2L), vm.state.value.items.map { it.id })

        vm.setCategory(null)
        vm.setText("aloe")
        assertEquals(listOf(1L), vm.state.value.items.map { it.id })
        assertEquals(2, vm.state.value.totalCount)
    }

    @Test
    fun `wishlist marks items that are already owned and puts want-badly first`() = runTest(main.dispatcher) {
        val vm = WishlistViewModel(service)
        backgroundScope.launch { vm.state.collect {} }

        val rows = vm.state.value.rows
        assertEquals(listOf(4L, 3L), rows.map { it.item.id })
        assertEquals(listOf(false, true), rows.map { it.alreadyOwned })

        vm.setFilter(WishlistFilter.WANT_BADLY)
        assertEquals(listOf(4L), vm.state.value.rows.map { it.item.id })
    }

    @Test
    fun `bought moves to collection and undo restores it`() = runTest(main.dispatcher) {
        val vm = WishlistViewModel(service)
        backgroundScope.launch { vm.state.collect {} }

        vm.markBought(4)
        val before = vm.bought.first()
        assertEquals(ItemStatus.OWNED, repo.get(4)!!.status)
        assertTrue(vm.state.value.rows.none { it.item.id == 4L })

        vm.undo(before)
        assertEquals(ItemStatus.WISHLIST, repo.get(4)!!.status)
        assertTrue(repo.get(4)!!.wantBadly)
    }
}
