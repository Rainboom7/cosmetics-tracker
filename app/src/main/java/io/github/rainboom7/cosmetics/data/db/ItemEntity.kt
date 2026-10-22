package io.github.rainboom7.cosmetics.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import java.time.LocalDate

/**
 * Room row for [CosmeticItem]. Enums are stored by name, dates as epoch days, photo URIs as a
 * JSON array (see [Converters]). Change the schema only together with a Room migration.
 */
@Entity(tableName = "items", indices = [Index("status"), Index("barcode")])
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String?,
    val category: String,
    val status: String,
    val barcode: String?,
    val shade: String?,
    val quantity: String?,
    val ingredients: String?,
    val catalogImageUrl: String?,
    val catalogSource: String?,
    val photoUris: List<String>,
    val notes: String,
    val usage: String,
    val remainingPercent: Int,
    val openedOn: LocalDate?,
    val periodAfterOpeningMonths: Int?,
    val wantBadly: Boolean,
    val rebuy: Boolean,
    val price: String?,
    val link: String?,
    val addedAtMillis: Long,
) {
    fun toModel() = CosmeticItem(
        id = id,
        name = name,
        brand = brand,
        category = Category.entries.firstOrNull { it.name == category } ?: Category.OTHER,
        status = ItemStatus.entries.firstOrNull { it.name == status } ?: ItemStatus.OWNED,
        barcode = barcode,
        shade = shade,
        quantity = quantity,
        ingredients = ingredients,
        catalogImageUrl = catalogImageUrl,
        catalogSource = catalogSource,
        photoUris = photoUris,
        notes = notes,
        usage = UsageState.entries.firstOrNull { it.name == usage } ?: UsageState.NEW,
        remainingPercent = remainingPercent.coerceIn(0, 100),
        openedOn = openedOn,
        periodAfterOpeningMonths = periodAfterOpeningMonths?.takeIf { it > 0 },
        wantBadly = wantBadly,
        rebuy = rebuy,
        price = price,
        link = link,
        addedAtMillis = addedAtMillis,
    )

    companion object {
        fun from(item: CosmeticItem) = ItemEntity(
            id = item.id,
            name = item.name,
            brand = item.brand,
            category = item.category.name,
            status = item.status.name,
            barcode = item.barcode,
            shade = item.shade,
            quantity = item.quantity,
            ingredients = item.ingredients,
            catalogImageUrl = item.catalogImageUrl,
            catalogSource = item.catalogSource,
            photoUris = item.photoUris,
            notes = item.notes,
            usage = item.usage.name,
            remainingPercent = item.remainingPercent,
            openedOn = item.openedOn,
            periodAfterOpeningMonths = item.periodAfterOpeningMonths,
            wantBadly = item.wantBadly,
            rebuy = item.rebuy,
            price = item.price,
            link = item.link,
            addedAtMillis = item.addedAtMillis,
        )
    }
}
