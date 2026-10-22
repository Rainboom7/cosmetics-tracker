package io.github.rainboom7.cosmetics.core.usage

import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/** One month of the calendar screen. */
data class MonthUsage(
    val month: YearMonth,
    /** Owned items used on each day of [month]; days without marks are absent. */
    val days: Map<LocalDate, List<CosmeticItem>> = emptyMap(),
    /** Days in a row with at least one mark, ending today (or yesterday, if today has none yet). */
    val streak: Int = 0,
    /** Up to three items used on the most days this month, with their day counts. */
    val mostUsed: List<Pair<CosmeticItem, Int>> = emptyList(),
    /** Opened items not used for [UsageCalendar.FORGOTTEN_AFTER_DAYS] days: "Давно не пользовались". */
    val forgotten: List<CosmeticItem> = emptyList(),
) {
    val activeDays: Int get() = days.size
}

/** The usage line on an item's card. */
data class ItemUsage(
    val lastUsed: LocalDate? = null,
    val usedToday: Boolean = false,
    /** Days used in the last 30 days, today included. */
    val last30Days: Int = 0,
    val total: Int = 0,
)

/** Business rules of the usage calendar ("Календарь использования"). */
class UsageCalendar(
    private val log: UsageLogRepository,
    private val collection: CollectionService,
) {
    fun observeMonth(month: YearMonth): Flow<MonthUsage> =
        combine(log.observeAll(), collection.observe(ItemStatus.OWNED)) { marks, owned ->
            buildMonth(month, marks, owned, collection.today())
        }

    fun observeItem(itemId: Long): Flow<ItemUsage> = log.observeForItem(itemId).map { itemUsage(it, collection.today()) }

    /**
     * Marks or unmarks [itemId] as used on [date]. Future days are ignored. Using a new, unopened
     * item opens it on that day, which starts its PAO countdown.
     */
    suspend fun setUsed(itemId: Long, date: LocalDate, used: Boolean) {
        if (date.isAfter(collection.today())) return
        val mark = UsageMark(itemId, date)
        if (!used) {
            log.remove(mark)
            return
        }
        log.add(mark)
        if (collection.get(itemId)?.usage == UsageState.NEW) {
            collection.setUsage(itemId, UsageState.OPENED, openedOn = date)
        }
    }

    /** "Использовала сегодня" button: flips today's mark. Returns whether it is now marked. */
    suspend fun toggleToday(itemId: Long): Boolean {
        val today = collection.today()
        val used = today !in log.observeForItem(itemId).first()
        setUsed(itemId, today, used)
        return used
    }

    companion object {
        const val FORGOTTEN_AFTER_DAYS = 30L

        /** Statuses that mean "I know I'm not using it": such items are never called forgotten. */
        private val SHELVED = setOf(ShelfStatus.NOT_USING, ShelfStatus.GIVE_AWAY)

        fun buildMonth(month: YearMonth, marks: List<UsageMark>, owned: List<CosmeticItem>, today: LocalDate): MonthUsage {
            val byId = owned.associateBy { it.id }
            val known = marks.filter { it.itemId in byId }
            val inMonth = known.filter { YearMonth.from(it.date) == month }
            val days = inMonth.groupBy({ it.date }, { byId.getValue(it.itemId) })
                .mapValues { (_, items) -> items.distinctBy { it.id }.sortedBy { it.name.lowercase() } }
            val mostUsed = inMonth.groupingBy { it.itemId }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<Long, Int>> { it.value }.thenBy { byId.getValue(it.key).name.lowercase() })
                .take(3)
                .map { byId.getValue(it.key) to it.value }
            return MonthUsage(
                month = month,
                days = days,
                streak = streak(known.mapTo(HashSet()) { it.date }, today),
                mostUsed = mostUsed,
                forgotten = forgotten(known, owned, today),
            )
        }

        fun streak(days: Set<LocalDate>, today: LocalDate): Int {
            var day = if (today in days) today else today.minusDays(1)
            var count = 0
            while (day in days) {
                count++
                day = day.minusDays(1)
            }
            return count
        }

        /**
         * Opened items whose last use (or opening, if never marked) was [FORGOTTEN_AFTER_DAYS] or
         * more days ago, oldest first. Empty until the user has marked anything at all, so people
         * who don't keep the calendar aren't nagged.
         */
        fun forgotten(marks: List<UsageMark>, owned: List<CosmeticItem>, today: LocalDate): List<CosmeticItem> {
            if (marks.isEmpty()) return emptyList()
            val lastUse = marks.groupBy { it.itemId }.mapValues { (_, list) -> list.maxOf { it.date } }
            val cutoff = today.minusDays(FORGOTTEN_AFTER_DAYS)
            return owned
                .filter { it.usage == UsageState.OPENED && it.effectiveShelfStatus() !in SHELVED }
                .mapNotNull { item -> (lastUse[item.id] ?: item.openedOn)?.let { item to it } }
                .filter { (_, last) -> !last.isAfter(cutoff) }
                .sortedBy { (_, last) -> last }
                .map { it.first }
        }

        fun itemUsage(daysNewestFirst: List<LocalDate>, today: LocalDate): ItemUsage {
            val monthAgo = today.minusDays(29)
            return ItemUsage(
                lastUsed = daysNewestFirst.firstOrNull(),
                usedToday = today in daysNewestFirst,
                last30Days = daysNewestFirst.count { !it.isBefore(monthAgo) && !it.isAfter(today) },
                total = daysNewestFirst.size,
            )
        }
    }
}
