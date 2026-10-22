package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.model.UsageState
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.DateTimeException
import java.time.LocalDate

/**
 * JSON backup of the collection, so the user can move to a new phone. Own photo URIs are kept
 * as is: they only resolve on the same device, catalog images resolve everywhere.
 * Enums are stored by name and unknown names fall back to defaults, so old backups keep working.
 */
object CollectionBackup {
    const val FORMAT_VERSION = 1

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun export(items: List<CosmeticItem>): String =
        json.encodeToString(BackupFile.serializer(), BackupFile(FORMAT_VERSION, items.map(BackupItem::from)))

    /** @throws IllegalArgumentException if the text is not a backup this version understands. */
    fun import(text: String): List<CosmeticItem> {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Not a cosmetics backup", e)
        }
        require(file.version <= FORMAT_VERSION) { "Backup version ${file.version} is newer than $FORMAT_VERSION" }
        return try {
            file.items.map(BackupItem::toItem)
        } catch (e: DateTimeException) {
            throw IllegalArgumentException("Bad date in backup", e)
        }
    }

    @Serializable
    private data class BackupFile(val version: Int, val items: List<BackupItem>)

    @Serializable
    private data class BackupItem(
        val name: String,
        val brand: String? = null,
        val category: String = Category.OTHER.name,
        val status: String = ItemStatus.OWNED.name,
        val barcode: String? = null,
        val shade: String? = null,
        val quantity: String? = null,
        val ingredients: String? = null,
        val catalogImageUrl: String? = null,
        val catalogSource: String? = null,
        val photoUris: List<String> = emptyList(),
        val notes: String = "",
        val usage: String = UsageState.NEW.name,
        val remainingPercent: Int = 100,
        val openedOn: String? = null,
        val periodAfterOpeningMonths: Int? = null,
        val wantBadly: Boolean = false,
        val rebuy: Boolean = false,
        val price: String? = null,
        val link: String? = null,
        val addedAtMillis: Long = 0,
    ) {
        fun toItem() = CosmeticItem(
            name = name,
            brand = brand,
            category = enumOrDefault(category, Category.OTHER),
            status = enumOrDefault(status, ItemStatus.OWNED),
            barcode = barcode,
            shade = shade,
            quantity = quantity,
            ingredients = ingredients,
            catalogImageUrl = catalogImageUrl,
            catalogSource = catalogSource,
            photoUris = photoUris,
            notes = notes,
            usage = enumOrDefault(usage, UsageState.NEW),
            remainingPercent = remainingPercent.coerceIn(0, 100),
            openedOn = openedOn?.let(LocalDate::parse),
            periodAfterOpeningMonths = periodAfterOpeningMonths?.takeIf { it > 0 },
            wantBadly = wantBadly,
            rebuy = rebuy,
            price = price,
            link = link,
            addedAtMillis = addedAtMillis,
        )

        companion object {
            fun from(item: CosmeticItem) = BackupItem(
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
                openedOn = item.openedOn?.toString(),
                periodAfterOpeningMonths = item.periodAfterOpeningMonths,
                wantBadly = item.wantBadly,
                rebuy = item.rebuy,
                price = item.price,
                link = item.link,
                addedAtMillis = item.addedAtMillis,
            )
        }
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
