package io.github.rainboom7.cosmetics.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.Condition
import io.github.rainboom7.cosmetics.core.model.ItemBadge
import io.github.rainboom7.cosmetics.core.model.ShelfStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.ui.theme.tone
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun categoryLabel(category: Category): String = stringResource(
    when (category) {
        Category.SKINCARE -> R.string.category_skincare
        Category.MAKEUP -> R.string.category_makeup
        Category.HAIR -> R.string.category_hair
        Category.BODY -> R.string.category_body
        Category.FRAGRANCE -> R.string.category_fragrance
        Category.NAILS -> R.string.category_nails
        Category.OTHER -> R.string.category_other
    },
)

@Composable
fun usageLabel(usage: UsageState): String = stringResource(
    when (usage) {
        UsageState.NEW -> R.string.usage_new
        UsageState.OPENED -> R.string.usage_opened
        UsageState.FINISHED -> R.string.usage_finished
    },
)

@Composable
fun conditionLabel(condition: Condition): String = stringResource(
    when (condition) {
        Condition.NEW -> R.string.usage_new
        Condition.OPENED -> R.string.usage_opened
        Condition.TRYING -> R.string.shelf_trying
        Condition.STOCKED -> R.string.shelf_stocked
        Condition.RUNNING_LOW -> R.string.shelf_running_low
        Condition.NOT_USING -> R.string.shelf_not_using
        Condition.GIVE_AWAY -> R.string.shelf_give_away
        Condition.FINISHED -> R.string.usage_finished
    },
)

@Composable
fun shelfLabel(status: ShelfStatus): String = stringResource(
    when (status) {
        ShelfStatus.STOCKED -> R.string.shelf_stocked
        ShelfStatus.RUNNING_LOW -> R.string.shelf_running_low
        ShelfStatus.TRYING -> R.string.shelf_trying
        ShelfStatus.NOT_USING -> R.string.shelf_not_using
        ShelfStatus.GIVE_AWAY -> R.string.shelf_give_away
    },
)

/**
 * Warning pill on collection cards: "Истекает" in soft red, "Заканчивается" in peach.
 * Pastel containers instead of solid error red keep the grid calm.
 */
@Composable
fun BadgeLabel(badge: ItemBadge, modifier: Modifier = Modifier) {
    val expiring = badge == ItemBadge.EXPIRING
    val (container, ink) = if (expiring) {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    } else {
        ShelfStatus.RUNNING_LOW.tone().let { it.tone to it.ink }
    }
    Surface(color = container, contentColor = ink, shape = CircleShape, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                if (expiring) Icons.Outlined.HourglassBottom else Icons.Outlined.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Text(
                stringResource(if (expiring) R.string.badge_expiring else R.string.badge_running_low),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Small pastel pill with the item's status, e.g. on cards and in lists. */
@Composable
fun StatusPill(status: ShelfStatus, modifier: Modifier = Modifier) {
    val tone = status.tone()
    Surface(color = tone.tone, contentColor = tone.ink, shape = CircleShape, modifier = modifier) {
        Text(
            shelfLabel(status),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Pastel pill with an owned item's [Condition], on collection cards. */
@Composable
fun ConditionPill(condition: Condition, modifier: Modifier = Modifier) {
    val tone = condition.tone()
    Surface(color = tone.tone, contentColor = tone.ink, shape = CircleShape, modifier = modifier) {
        Text(
            conditionLabel(condition),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
val MonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
val FullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

val Russian: Locale = Locale.forLanguageTag("ru")

/** "22 октября" (genitive month, as in a sentence). */
val DayMonthWords: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Russian)

/** "Октябрь 2026" (nominative month, for the calendar header). */
fun monthTitle(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, Russian).replaceFirstChar { it.titlecase(Russian) } + " " + month.year

/** "Открыт 08.03 · до 03.2027", or null if the item was not opened. */
@Composable
fun openedLine(openedOn: LocalDate?, expiresOn: LocalDate?): String? {
    openedOn ?: return null
    val opened = stringResource(R.string.opened_on, openedOn.format(DayMonth))
    return if (expiresOn != null) opened + " · " + stringResource(R.string.until, expiresOn.format(MonthYear)) else opened
}
