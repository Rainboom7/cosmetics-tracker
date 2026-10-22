package io.github.rainboom7.cosmetics.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rainboom7.cosmetics.CosmeticsApp
import io.github.rainboom7.cosmetics.MainActivity
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.testing.Samples
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The real app without background work, so the whole activity can run under Robolectric. */
class TestCosmeticsApp : CosmeticsApp() {
    override fun scheduleBackgroundWork() = Unit
}

/** Opens every tab of the real app: catches crashes in routes, ViewModel factories and Room queries. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = TestCosmeticsApp::class, qualifiers = "w411dp-h891dp")
class NavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 10_000)

    @Test
    fun allTabsOpen() {
        waitForText("Добавьте первое средство")

        compose.onNodeWithTag("tab_wishlist").performClick()
        waitForText("Здесь будет то, что хочется")

        compose.onNodeWithTag("tab_catalog").performClick()
        waitForText("Каталог Open Beauty Facts")

        compose.onNodeWithTag("tab_wishlist").performClick()
        waitForText("Здесь будет то, что хочется")

        compose.onNodeWithTag("tab_collection").performClick()
        waitForText("Добавьте первое средство")
    }

    @Test
    fun tabsOpenWithData() {
        val service = ApplicationProvider.getApplicationContext<CosmeticsApp>().container.collection
        runBlocking {
            val owned = service.addFromCatalog(Samples.aloe, ItemStatus.OWNED).id
            service.addFromCatalog(Samples.mascara, ItemStatus.WISHLIST)
            service.addFromCatalog(Samples.nivea, ItemStatus.WISHLIST)
            service.setWantBadly(service.addFromCatalog(Samples.nivea.copy(barcode = null, name = "Soft"), ItemStatus.WISHLIST).id, true)
            service.setUsage(owned, UsageState.OPENED)
            service.buyAgain(owned)
        }

        waitForText("Hydra Genius Aloe Water")
        compose.onNodeWithTag("tab_wishlist").performClick()
        waitForText("Volume Million Lashes")
        waitForText("Похожее уже есть у вас")

        compose.onNodeWithTag("tab_collection").performClick()
        waitForText("Hydra Genius Aloe Water")
        compose.onNodeWithTag("tab_wishlist").performClick()
        waitForText("Volume Million Lashes")
    }
}
