package io.github.rainboom7.cosmetics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.rainboom7.cosmetics.ui.theme.Sky

/**
 * Draws the cloud sky behind a header: the [Sky] gradient, two soft white puffs and, along the
 * bottom, a row of overlapping bumps in [pageColor], so the header ends in a cloud edge instead
 * of a straight line. Leave [edge] of bottom padding in the content for the bumps.
 */
fun Modifier.cloudSky(pageColor: Color, edge: Dp = 22.dp): Modifier = drawBehind {
    drawRect(Sky.brush)
    val unit = edge.toPx()
    drawPuff(Offset(size.width * 0.82f, unit * 1.6f), unit * 0.9f, Color.White.copy(alpha = 0.55f))
    drawPuff(Offset(size.width * 0.12f, size.height * 0.42f), unit * 0.6f, Color.White.copy(alpha = 0.4f))
    // The cloud edge: bumps of alternating size, centred on the bottom line.
    val step = unit * 1.7f
    var x = -step / 2
    var index = 0
    while (x < size.width + step) {
        val radius = if (index % 2 == 0) unit * 1.05f else unit * 0.8f
        drawCircle(pageColor, radius, Offset(x, size.height + radius * 0.25f))
        x += step
        index++
    }
    drawRect(pageColor, topLeft = Offset(0f, size.height - unit * 0.1f), size = Size(size.width, unit * 0.1f))
}

/** A small cartoon cloud: three circles on a rounded base. */
private fun DrawScope.drawPuff(center: Offset, unit: Float, color: Color) {
    drawRoundRect(color, Offset(center.x - unit * 1.6f, center.y), Size(unit * 3.2f, unit * 1.1f), CornerRadius(unit * 0.55f))
    drawCircle(color, unit * 0.75f, Offset(center.x - unit * 0.8f, center.y + unit * 0.3f))
    drawCircle(color, unit, Offset(center.x + unit * 0.1f, center.y))
    drawCircle(color, unit * 0.65f, Offset(center.x + unit * 0.95f, center.y + unit * 0.35f))
}

/**
 * Widens a child of a padded list by [horizontal] on each side, so a header inside a
 * LazyColumn or grid with side padding can still run edge to edge.
 */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

/** The soft pink drop shadow under white cards, so they float like clouds. */
fun Modifier.cloudShadow(shape: androidx.compose.ui.graphics.Shape, elevation: Dp = 6.dp): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = Color(0xFFCC3D72), spotColor = Color(0xFFCC3D72))

/** Section heading in the shop style: bold title, optional counter in a pink bubble. */
@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (count != null && count > 0) {
            Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

/** A round "story" bubble: gradient ring when [active], muted ring otherwise. */
@Composable
fun StoryRing(active: Boolean, selected: Boolean, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(size)
            .then(
                when {
                    selected -> Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    active -> Modifier.border(2.5.dp, Sky.ring, CircleShape)
                    else -> Modifier.border(1.5.dp, Sky.ringMuted, CircleShape)
                },
            )
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A white card floating on the page with a soft pink shadow: the v3 replacement for tinted sections. */
@Composable
fun CloudCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.large
    androidx.compose.material3.Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.cloudShadow(shape, 4.dp),
        content = content,
    )
}

/** A pink cartoon cloud with [content] in the middle, for empty states. */
@Composable
fun CloudBadge(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val color = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier
            .size(width = 150.dp, height = 104.dp)
            .drawBehind { drawPuff(Offset(size.width / 2, size.height * 0.48f), size.width / 3.4f, color) },
        contentAlignment = Alignment.Center,
    ) { content() }
}
