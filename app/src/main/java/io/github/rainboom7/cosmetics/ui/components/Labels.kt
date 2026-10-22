package io.github.rainboom7.cosmetics.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.ItemBadge
import io.github.rainboom7.cosmetics.core.model.UsageState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

/** Error-colored badge on collection cards: "Истекает" / "Заканчивается". */
@Composable
fun BadgeLabel(badge: ItemBadge, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            stringResource(if (badge == ItemBadge.EXPIRING) R.string.badge_expiring else R.string.badge_running_low),
            color = MaterialTheme.colorScheme.onError,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
val MonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
val FullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** "Открыт 08.03 · до 03.2027", or null if the item was not opened. */
@Composable
fun openedLine(openedOn: LocalDate?, expiresOn: LocalDate?): String? {
    openedOn ?: return null
    val opened = stringResource(R.string.opened_on, openedOn.format(DayMonth))
    return if (expiresOn != null) opened + " · " + stringResource(R.string.until, expiresOn.format(MonthYear)) else opened
}
