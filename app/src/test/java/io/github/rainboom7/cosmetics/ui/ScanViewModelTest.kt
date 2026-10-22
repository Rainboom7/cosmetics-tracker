package io.github.rainboom7.cosmetics.ui

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.collection.InMemoryCollectionRepository
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.photo.PhotoRecognition
import io.github.rainboom7.cosmetics.core.photo.PhotoSearch
import io.github.rainboom7.cosmetics.core.photo.RecognizedLine
import io.github.rainboom7.cosmetics.testing.FakeCatalog
import io.github.rainboom7.cosmetics.testing.FakeRecognizer
import io.github.rainboom7.cosmetics.testing.MainDispatcherRule
import io.github.rainboom7.cosmetics.testing.Samples
import io.github.rainboom7.cosmetics.ui.scan.AddFlowStore
import io.github.rainboom7.cosmetics.ui.scan.DraftReason
import io.github.rainboom7.cosmetics.ui.scan.ScanEvent
import io.github.rainboom7.cosmetics.ui.scan.ScanMode
import io.github.rainboom7.cosmetics.ui.scan.ScanViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val catalog = FakeCatalog(listOf(Samples.aloe, Samples.mascara))
    private val recognizer = FakeRecognizer()
    private val repo = InMemoryCollectionRepository()
    private val service = CollectionService(repo)
    private val addFlow = AddFlowStore()
    private val photo = Uri.parse("file:///data/photos/1.jpg")

    private fun vm(mode: ScanMode = ScanMode.BARCODE, target: ItemStatus = ItemStatus.OWNED) =
        ScanViewModel(target, mode, catalog, PhotoSearch(catalog), recognizer, service, addFlow)

    @Test
    fun `known barcode shows one 100 percent candidate`() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onBarcode("3600523307593")

        val candidates = vm.state.value.candidates!!
        assertEquals(listOf(Samples.aloe), candidates.matches.map { it.product })
        assertEquals(100, candidates.matches.single().confidence)
        assertNull(candidates.photoUri)
    }

    @Test
    fun `invalid and repeated barcodes are ignored`() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onBarcode("12345")
        assertTrue(catalog.lookups.isEmpty())

        vm.onBarcode("3600523307593")
        vm.onBarcode("3600523307593") // sheet already open
        assertEquals(1, catalog.lookups.size)
    }

    @Test
    fun `unknown barcode opens the form with the barcode`() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onBarcode("4006381333931")

        assertEquals(ScanEvent.OpenForm, vm.events.first())
        val draft = addFlow.take()!!
        assertEquals(DraftReason.BARCODE_NOT_FOUND, draft.reason)
        assertEquals("4006381333931", draft.barcode)
    }

    @Test
    fun `offline barcode lookup still opens the form`() = runTest(main.dispatcher) {
        catalog.failing = true
        val vm = vm()
        vm.onBarcode("3600523307593")
        assertEquals(ScanEvent.OpenForm, vm.events.first())
        assertEquals(DraftReason.OFFLINE, addFlow.take()!!.reason)
    }

    @Test
    fun `package photo finds candidates by text, picking one attaches the photo`() = runTest(main.dispatcher) {
        recognizer.result = PhotoRecognition(lines = listOf(RecognizedLine("L'ORÉAL", 90), RecognizedLine("Aloe Water", 40)))
        val vm = vm(ScanMode.PHOTO)

        vm.onPhoto(photo)

        val candidates = vm.state.value.candidates!!
        assertEquals(Samples.aloe, candidates.matches.first().product)
        assertEquals(photo.toString(), candidates.photoUri)

        vm.pick(candidates.matches.first(), ItemStatus.WISHLIST)
        val event = vm.events.first() as ScanEvent.Added
        val saved = repo.get(event.result.id)!!
        assertEquals(ItemStatus.WISHLIST, saved.status)
        assertEquals(listOf(photo.toString()), saved.photoUris)
        assertEquals(Samples.aloe.imageUrl, saved.catalogImageUrl)
        assertNull(vm.state.value.candidates)
    }

    @Test
    fun `nothing fits sends photo and OCR prefill to the form`() = runTest(main.dispatcher) {
        recognizer.result = PhotoRecognition(lines = listOf(RecognizedLine("L'ORÉAL", 90), RecognizedLine("Aloe Water", 40), RecognizedLine("12M", 5)))
        val vm = vm(ScanMode.PHOTO, ItemStatus.WISHLIST)
        vm.onPhoto(photo)
        assertNotNull(vm.state.value.candidates)

        vm.nothingFits()

        assertEquals(ScanEvent.OpenForm, vm.events.first())
        val draft = addFlow.take()!!
        assertEquals(DraftReason.NO_MATCH, draft.reason)
        assertEquals(ItemStatus.WISHLIST, draft.target)
        assertEquals(photo.toString(), draft.photoUri)
        assertEquals("L'ORÉAL", draft.prefill.brand)
        assertEquals(12, draft.prefill.periodAfterOpeningMonths)
    }

    @Test
    fun `photo with nothing in the catalog goes straight to the form`() = runTest(main.dispatcher) {
        recognizer.result = PhotoRecognition(lines = listOf(RecognizedLine("Чистая линия", 50)))
        val vm = vm(ScanMode.PHOTO)
        vm.onPhoto(photo)
        assertEquals(ScanEvent.OpenForm, vm.events.first())
        assertEquals(photo.toString(), addFlow.take()!!.photoUri)
    }

    @Test
    fun `enter manually opens an empty form`() = runTest(main.dispatcher) {
        val vm = vm()
        vm.enterManually()
        assertEquals(ScanEvent.OpenForm, vm.events.first())
        assertEquals(DraftReason.MANUAL, addFlow.take()!!.reason)
    }
}
