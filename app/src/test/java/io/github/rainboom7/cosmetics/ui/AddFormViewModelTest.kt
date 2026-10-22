package io.github.rainboom7.cosmetics.ui

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.core.photo.FormPrefill
import io.github.rainboom7.cosmetics.testing.MainDispatcherRule
import io.github.rainboom7.cosmetics.ui.form.AddFormViewModel
import io.github.rainboom7.cosmetics.ui.scan.AddDraft
import io.github.rainboom7.cosmetics.ui.scan.AddFlowStore
import io.github.rainboom7.cosmetics.ui.scan.DraftReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AddFormViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
    private val existing = CosmeticItem(id = 1, name = "Aloe Water", brand = "L'Oréal", photoUris = listOf("file:///old.jpg"))
    private val repo = InMemoryCollectionRepository(listOf(existing))
    private val service = CollectionService(repo, clock)
    private val addFlow = AddFlowStore()

    @Test
    fun `save needs a name and a photo`() = runTest(main.dispatcher) {
        val vm = AddFormViewModel(service, addFlow, ItemStatus.OWNED, editId = null)
        backgroundScope.launch { vm.state.collect {} }

        assertFalse(vm.state.value.canSave)
        vm.update { it.copy(name = "Крем") }
        assertFalse(vm.state.value.canSave)
        vm.update { it.copy(photoUri = "file:///p.jpg") }
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `draft prefills the form and saving creates an opened item`() = runTest(main.dispatcher) {
        addFlow.put(
            AddDraft(
                DraftReason.BARCODE_NOT_FOUND,
                ItemStatus.OWNED,
                photoUri = "file:///pack.jpg",
                barcode = "4006381333931",
                prefill = FormPrefill(brand = "NATURA SIBERICA", name = "Face cream", category = Category.SKINCARE, periodAfterOpeningMonths = 12),
            ),
        )
        val vm = AddFormViewModel(service, addFlow, ItemStatus.OWNED, editId = null)
        backgroundScope.launch { vm.state.collect {} }

        val state = vm.state.value
        assertEquals(DraftReason.BARCODE_NOT_FOUND, state.reason)
        assertEquals("NATURA SIBERICA", state.fields.brand)
        assertEquals(LocalDate.of(2026, 10, 22), state.fields.openedOn)
        assertTrue(state.canSave)

        vm.save(ItemStatus.OWNED)
        val id = vm.saved.first()
        val item = repo.get(id)!!
        assertEquals("Face cream", item.name)
        assertEquals("4006381333931", item.barcode)
        assertEquals(listOf("file:///pack.jpg"), item.photoUris)
        assertEquals(UsageState.OPENED, item.usage)
        assertEquals(12, item.periodAfterOpeningMonths)
        assertNull(addFlow.take())
    }

    @Test
    fun `saving to the wishlist drops the opened date and keeps wish fields`() = runTest(main.dispatcher) {
        val vm = AddFormViewModel(service, addFlow, ItemStatus.OWNED, editId = null)
        backgroundScope.launch { vm.state.collect {} }
        vm.update { it.copy(name = "Помада", photoUri = "file:///p.jpg", openedOn = LocalDate.of(2026, 1, 1), wantBadly = true, price = " 990 ₽ ") }

        vm.save(ItemStatus.WISHLIST)

        val item = repo.get(vm.saved.first())!!
        assertEquals(ItemStatus.WISHLIST, item.status)
        assertNull(item.openedOn)
        assertTrue(item.wantBadly)
        assertEquals("990 ₽", item.price)
    }

    @Test
    fun `similar items are hinted while typing`() = runTest(main.dispatcher) {
        val vm = AddFormViewModel(service, addFlow, ItemStatus.OWNED, editId = null)
        backgroundScope.launch { vm.state.collect {} }

        assertTrue(vm.state.value.similar.isEmpty())
        vm.update { it.copy(brand = "LOREAL", name = "aloe water") }
        assertEquals(listOf(existing), vm.state.value.similar)
    }

    @Test
    fun `editing loads the item, does not flag itself as duplicate and keeps the id`() = runTest(main.dispatcher) {
        val vm = AddFormViewModel(service, addFlow, ItemStatus.OWNED, editId = 1)
        backgroundScope.launch { vm.state.collect {} }

        assertEquals("Aloe Water", vm.state.value.fields.name)
        assertTrue(vm.state.value.similar.isEmpty())

        vm.update { it.copy(name = "Aloe Water Gel", photoUri = "file:///new.jpg") }
        vm.save(ItemStatus.OWNED)

        assertEquals(1L, vm.saved.first())
        val item = repo.get(1)!!
        assertEquals("Aloe Water Gel", item.name)
        assertEquals(listOf("file:///new.jpg", "file:///old.jpg"), item.photoUris)
    }
}
