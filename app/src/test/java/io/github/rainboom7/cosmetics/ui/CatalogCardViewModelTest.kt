package io.github.rainboom7.cosmetics.ui

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.testing.FakeCatalog
import io.github.rainboom7.cosmetics.testing.MainDispatcherRule
import io.github.rainboom7.cosmetics.testing.Samples
import io.github.rainboom7.cosmetics.ui.catalog.CatalogMessage
import io.github.rainboom7.cosmetics.ui.catalogcard.CatalogCardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogCardViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val detailed = CatalogCardTest.detailedAloe

    @Test
    fun viewModelFetchesDetailsByBarcodeAndKeepsSearchDataOffline() = runTest(main.dispatcher) {
        val catalog = FakeCatalog(listOf(detailed))
        val service = CollectionService(InMemoryCollectionRepository())
        val vm = CatalogCardViewModel(Samples.aloe, catalog, service)
        runCurrent()
        assertEquals(listOf(Samples.aloe.barcode), catalog.lookups)
        assertEquals("6 months", vm.state.value.product?.details?.periodAfterOpening)
        assertEquals(detailed.ingredients, vm.state.value.product?.ingredients)
        assertFalse(vm.state.value.loadingDetails)

        catalog.failing = true
        val offline = CatalogCardViewModel(Samples.nivea, catalog, service)
        runCurrent()
        assertEquals(Samples.nivea, offline.state.value.product)
        assertFalse(offline.state.value.loadingDetails)

        vm.add(ItemStatus.OWNED)
        runCurrent()
        assertEquals(CatalogMessage.ADDED_COLLECTION, vm.events.first())
    }
}
