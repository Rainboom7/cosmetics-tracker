package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import java.time.LocalDate

/**
 * Which items deserve a notification today: a daily job calls [dueOn] and notifies about the
 * result, so each item is announced exactly twice: [WARN_DAYS] before expiry and on the day.
 */
object ExpiryReminders {
    const val WARN_DAYS = 30L

    fun dueOn(items: List<CosmeticItem>, today: LocalDate): List<CosmeticItem> = items.filter { item ->
        val expires = item.expiresOn()
        item.status == ItemStatus.OWNED &&
            item.usage != UsageState.FINISHED &&
            expires != null &&
            (expires == today || expires.minusDays(WARN_DAYS) == today)
    }
}
