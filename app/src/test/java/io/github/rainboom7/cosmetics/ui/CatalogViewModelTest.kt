package io.github.rainboom7.cosmetics.ui

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.testing.FakeCatalog
import io.github.rainboom7.cosmetics.testing.MainDispatcherRule
import io.github.rainboom7.cosmetics.testing.Samples
import io.github.rainboom7.cosmetics.ui.catalog.CatalogMessage
import io.github.rainboom7.cosmetics.ui.catalog.CatalogViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val catalog = FakeCatalog(listOf(Samples.aloe, Samples.mascara, Samples.nivea))
    private val repo = InMemoryCollectionRepository()
    private val service = CollectionService(repo)

    @Test
    fun `typing is debounced into one search`() = runTest(main.dispatcher) {
        val vm = CatalogViewModel(catalog, service)
        runCurrent()

        vm.onQueryChange("o")
        advanceTimeBy(100)
        vm.onQueryChange("oré")
        advanceTimeBy(100)
        vm.onQueryChange("oréal")
        advanceTimeBy(CatalogViewModel.DEBOUNCE_MS + 1)
        runCurrent()

        assertEquals(listOf("oréal" to 1), catalog.searches)
        assertEquals(listOf(Samples.aloe, Samples.mascara), vm.state.value.results)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `short typed queries and repeats do not hit the catalog`() = runTest(main.dispatcher) {
        val vm = CatalogViewModel(catalog, service)
        runCurrent()

        vm.onQueryChange("ni")
        advanceTimeBy(CatalogViewModel.DEBOUNCE_MS + 1)
        runCurrent()
        assertEquals(emptyList<Pair<String, Int>>(), catalog.searches)

        vm.onQueryChange("nivea")
        advanceTimeBy(CatalogViewModel.DEBOUNCE_MS + 1)
        vm.onQueryChange("nive")
        advanceTimeBy(CatalogViewModel.DEBOUNCE_MS + 1)
        vm.onQueryChange("nivea")
        advanceTimeBy(CatalogViewModel.DEBOUNCE_MS + 1)
        runCurrent()

        assertEquals(listOf("nivea" to 1, "nive" to 1), catalog.searches)
        assertEquals(listOf(Samples.nivea), vm.state.value.results)
    }

    @Test
    fun `initial query searches right away`() = runTest(main.dispatcher) {
        val vm = CatalogViewModel(catalog, service, initialQuery = "nivea")
        runCurrent()
        assertEquals(listOf(Samples.nivea), vm.state.value.results)
    }

    @Test
    fun `filters apply to loaded results`() = runTest(main.dispatcher) {
        val vm = CatalogViewModel(catalog, service, initialQuery = "l'oréal")
        runCurrent()

        vm.setCategory(Category.MAKEUP)
        assertEquals(listOf(Samples.mascara), vm.state.value.visible)
        vm.setCategory(null)
        vm.toggleWithPhoto()
        assertEquals(listOf(Samples.aloe), vm.state.value.visible)
        assertEquals(listOf("L'Oréal Paris"), vm.state.value.brands)
    }

    @Test
    fun `nothing found offers manual add, errors offer retry`() = runTest(main.dispatcher) {
        val vm = CatalogViewModel(catalog, service, initialQuery = "чистая линия")
        runCurrent()
        assertTrue(vm.state.value.nothingFound)

        catalog.failing = true
        vm.retry()
        runCurrent()
        assertTrue(vm.state.value.error)
        assertFalse(vm.state.value.nothingFound)

        catalog.failing = false
        vm.onQueryChange("nivea")
        advanceTimeBy(CatalogViewModel.DEBOUNCE_MS + 1)
        runCurrent()
        assertFalse(vm.state.value.error)
        assertEquals(listOf(Samples.nivea), vm.state.value.results)
    }

    @Test
    fun `load more appends the next page`() = runTest(main.dispatcher) {
        catalog.pageSize = 1
        val vm = CatalogViewModel(catalog, service, initialQuery = "l'oréal")
        runCurrent()
        assertTrue(vm.state.value.hasMore)

        vm.loadMore()
        runCurrent()

        assertEquals(listOf(Samples.aloe, Samples.mascara), vm.state.value.results)
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun `adding reports the outcome and marks known barcodes`() = runTest(main.dispatcher) {
        val vm = CatalogViewModel(catalog, service)
        backgroundScope.launch { vm.known.collect {} }
        runCurrent()

        vm.add(Samples.aloe, ItemStatus.WISHLIST)
        runCurrent()
        assertEquals(CatalogMessage.ADDED_WISHLIST, vm.events.first())

        vm.add(Samples.aloe, ItemStatus.OWNED)
        runCurrent()
        assertEquals(CatalogMessage.MOVED_FROM_WISHLIST, vm.events.first())
        assertEquals(setOf(Samples.aloe.barcode), vm.known.value.owned)

        vm.add(Samples.aloe, ItemStatus.OWNED)
        runCurrent()
        assertEquals(CatalogMessage.ALREADY_THERE, vm.events.first())
    }
}
