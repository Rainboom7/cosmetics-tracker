package io.github.rainboom7.cosmetics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.ui.theme.tone

/**
 * Product picture in a fixed box: category-tinted placeholder underneath, the photo cropped on
 * top. With [attribution] the catalog's required credit line is drawn in the corner.
 */
@Composable
fun ItemImage(
    model: String?,
    category: Category,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    attribution: Boolean = false,
) {
    val tone = category.tone()
    Box(modifier.background(tone.tone), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Spa, contentDescription = null, tint = tone.ink, modifier = Modifier.size(iconSize))
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (attribution && model != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            ) {
                Text(
                    stringResource(R.string.photo_attribution_obf),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
