package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.UsageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DuplicateFinderTest {
    private val aloe = CosmeticItem(id = 1, name = "Hydra Genius Aloe Water", brand = "L'Oréal Paris", barcode = "3600523307593")

    @Test
    fun `same barcode is a duplicate whatever the name`() {
        assertTrue(DuplicateFinder.isSimilar(CosmeticItem(name = "Что-то", barcode = "3600523307593"), aloe))
    }

    @Test
    fun `similar names of the same brand match, ignoring accents, case and volume`() {
        val typed = CosmeticItem(name = "aloe water 70ml", brand = "LOREAL")
        assertTrue(DuplicateFinder.isSimilar(typed, aloe))
        val typed2 = CosmeticItem(name = "Genius aloe water 70ml", brand = "L'OREAL")
        assertTrue(DuplicateFinder.isSimilar(typed2, aloe))
    }

    @Test
    fun `different brand or different product is not a duplicate`() {
        assertFalse(DuplicateFinder.isSimilar(CosmeticItem(name = "Aloe Water", brand = "Garnier"), aloe))
        assertFalse(DuplicateFinder.isSimilar(CosmeticItem(name = "Revitalift Filler", brand = "L'Oréal"), aloe))
    }

    @Test
    fun `missing brand compares names only`() {
        assertTrue(DuplicateFinder.isSimilar(CosmeticItem(name = "Aloe Water Hydra"), aloe))
    }

    @Test
    fun `findSimilar skips the item itself`() {
        assertEquals(emptyList<CosmeticItem>(), DuplicateFinder.findSimilar(aloe, listOf(aloe)))
        assertEquals(listOf(aloe), DuplicateFinder.findSimilar(aloe.copy(id = 0), listOf(aloe)))
    }
}

class ExpiryRemindersTest {
    private val cream = CosmeticItem(id = 1, name = "Крем", openedOn = LocalDate.of(2026, 1, 10), periodAfterOpeningMonths = 6)

    @Test
    fun `reminds 30 days before and on the day`() {
        assertEquals(listOf(cream), ExpiryReminders.dueOn(listOf(cream), LocalDate.of(2026, 6, 10)))
        assertEquals(listOf(cream), ExpiryReminders.dueOn(listOf(cream), LocalDate.of(2026, 7, 10)))
        assertTrue(ExpiryReminders.dueOn(listOf(cream), LocalDate.of(2026, 6, 11)).isEmpty())
    }

    @Test
    fun `finished items are not reminded`() {
        assertTrue(ExpiryReminders.dueOn(listOf(cream.copy(usage = UsageState.FINISHED)), LocalDate.of(2026, 7, 10)).isEmpty())
    }
}
