package io.github.rainboom7.cosmetics.ui.scan

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.github.rainboom7.cosmetics.data.photo.MlKitPhotoRecognizer

/** Live CameraX analyzer: ML Kit barcode scanning on every frame, no shutter needed. */
class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(MlKitPhotoRecognizer.PRODUCT_BARCODES)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes -> barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcode) }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() = scanner.close()
}
