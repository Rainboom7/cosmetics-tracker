package io.github.rainboom7.cosmetics.data.photo

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.rainboom7.cosmetics.core.photo.PhotoRecognition
import io.github.rainboom7.cosmetics.core.photo.RecognizedLine
import kotlinx.coroutines.tasks.await

/** Reads barcodes and text from a photo. An interface so ViewModel tests can fake ML Kit. */
interface PhotoRecognizer {
    suspend fun recognize(imageUri: Uri): PhotoRecognition
}

/**
 * On-device ML Kit: barcode scanning plus Latin text recognition. Both models are bundled in the
 * APK, so this works offline and nothing leaves the phone. Cyrillic text is not supported by
 * ML Kit's recognizers; brand names on packages are usually Latin anyway.
 */
class MlKitPhotoRecognizer(private val context: Context) : PhotoRecognizer {
    private val barcodeScanner: BarcodeScanner by lazy { BarcodeScanning.getClient(PRODUCT_BARCODES) }
    private val textRecognizer: TextRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(imageUri: Uri): PhotoRecognition {
        val image = InputImage.fromFilePath(context, imageUri)
        val barcodes = barcodeScanner.process(image).await().mapNotNull { it.rawValue }
        val text = textRecognizer.process(image).await()
        val lines = text.textBlocks.flatMap { block -> block.lines }.map { line ->
            RecognizedLine(line.text, line.boundingBox?.height() ?: 0)
        }
        return PhotoRecognition(barcodes, lines)
    }

    companion object {
        /** The formats printed on retail products. */
        val PRODUCT_BARCODES: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .build()
    }
}
