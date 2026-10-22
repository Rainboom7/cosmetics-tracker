package io.github.rainboom7.cosmetics.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.model.ItemStatus
import io.github.rainboom7.cosmetics.core.photo.PhotoMatch
import io.github.rainboom7.cosmetics.data.photo.PhotoStorage
import io.github.rainboom7.cosmetics.ui.components.ItemImage
import io.github.rainboom7.cosmetics.ui.components.appViewModel
import kotlinx.coroutines.launch

private val CameraBackground = Color(0xFF1A1113)

@Composable
fun ScanRoute(
    target: ItemStatus,
    initialMode: ScanMode,
    onBack: () -> Unit,
    onOpenForm: () -> Unit,
    onAdded: () -> Unit,
    onSearchByName: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = appViewModel(key = "scan") {
        ScanViewModel(target, initialMode, it.catalog, it.photoSearch, it.recognizer, it.collection, it.addFlow)
    }
    val photoStorage = remember { (context.applicationContext as io.github.rainboom7.cosmetics.CosmeticsApp).container.photoStorage }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.OpenForm -> onOpenForm()
                is ScanEvent.Added -> {
                    val text = when (event.result) {
                        is CollectionService.AddResult.AlreadyThere -> R.string.already_there
                        is CollectionService.AddResult.MovedFromWishlist -> R.string.moved_from_wishlist
                        is CollectionService.AddResult.Added ->
                            if (event.status == ItemStatus.OWNED) R.string.added_to_collection else R.string.added_to_wishlist
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    onAdded()
                }
            }
        }
    }

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) requestPermission.launch(Manifest.permission.CAMERA) }

    val scope = rememberCoroutineScope()
    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.setMode(ScanMode.PHOTO)
            scope.launch { viewModel.onPhoto(Uri.parse(photoStorage.importFrom(uri))) }
        }
    }

    Box(Modifier.fillMaxSize().background(CameraBackground)) {
        if (granted) {
            CameraView(state.mode, photoStorage, viewModel)
        } else {
            NoCameraPermission(onAllow = { requestPermission.launch(Manifest.permission.CAMERA) })
        }
        ScanOverlay(
            state = state,
            onBack = onBack,
            onMode = viewModel::setMode,
            onGallery = { pickFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onManual = viewModel::enterManually,
            onSearchByName = onSearchByName,
        )
        if (state.busy) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
    }
    state.candidates?.let { candidates ->
        CandidatesSheet(
            matches = candidates.matches,
            onPick = viewModel::pick,
            onNothingFits = viewModel::nothingFits,
            onDismiss = viewModel::dismissCandidates,
        )
    }
}

@Composable
private fun CameraView(mode: ScanMode, photoStorage: PhotoStorage, viewModel: ScanViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val analysis = remember {
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    }
    val analyzer = remember { BarcodeAnalyzer(viewModel::onBarcode) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            provider = future.get().also { p ->
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                p.unbindAll()
                camera = runCatching {
                    p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
                }.getOrNull()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            analyzer.close()
        }
    }
    DisposableEffect(mode) {
        if (mode == ScanMode.BARCODE) analysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) else analysis.clearAnalyzer()
        onDispose { analysis.clearAnalyzer() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        // Viewfinder frame: wide for barcodes, square-ish for packages.
        val frame = if (mode == ScanMode.BARCODE) Modifier.size(300.dp, 180.dp) else Modifier.size(280.dp, 360.dp)
        Box(frame.align(Alignment.Center).border(BorderStroke(3.dp, MaterialTheme.colorScheme.primaryContainer), RoundedCornerShape(24.dp)))
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { torch = !torch; camera?.cameraControl?.enableTorch(torch) }) {
                Icon(if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, contentDescription = stringResource(R.string.flash), tint = Color.White)
            }
            if (mode == ScanMode.PHOTO) {
                Surface(
                    onClick = {
                        val file = photoStorage.newPhotoFile()
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) = viewModel.onPhoto(Uri.fromFile(file))
                                override fun onError(exception: ImageCaptureException) = viewModel.onCaptureFailed()
                            },
                        )
                    },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp).testTag("shutter"),
                ) {}
            }
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ScanOverlay(
    state: ScanUiState,
    onBack: () -> Unit,
    onMode: (ScanMode) -> Unit,
    onGallery: () -> Unit,
    onManual: () -> Unit,
    onSearchByName: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
        }
        Text(
            stringResource(if (state.mode == ScanMode.BARCODE) R.string.scan_hint_barcode else R.string.scan_hint_photo),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        if (state.failed) {
            Text(stringResource(R.string.capture_failed), color = MaterialTheme.colorScheme.errorContainer, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ScanMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { onMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ScanMode.entries.size),
                    label = { Text(stringResource(if (mode == ScanMode.BARCODE) R.string.mode_barcode else R.string.mode_photo), color = Color.White) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGallery) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = stringResource(R.string.from_gallery), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSearchByName, modifier = Modifier.testTag("search_by_name")) {
                Text(stringResource(R.string.search_by_name), color = Color.White)
            }
            TextButton(onClick = onManual) { Text(stringResource(R.string.enter_manually), color = Color.White) }
        }
    }
}

@Composable
private fun NoCameraPermission(onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.camera_permission_title), color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.camera_permission_body),
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onAllow) { Text(stringResource(R.string.action_allow)) }
    }
}

@Composable
fun CandidatesSheet(
    matches: List<PhotoMatch>,
    onPick: (PhotoMatch, ItemStatus) -> Unit,
    onNothingFits: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        CandidatesContent(matches, onPick, onNothingFits)
    }
}

@Composable
fun CandidatesContent(
    matches: List<PhotoMatch>,
    onPick: (PhotoMatch, ItemStatus) -> Unit,
    onNothingFits: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.is_it_this), style = MaterialTheme.typography.titleLarge)
        val best = matches.first()
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ItemImage(best.product.imageUrl, best.product.category, best.product.name, Modifier.size(88.dp).clip(MaterialTheme.shapes.medium))
                    Column(Modifier.weight(1f)) {
                        best.product.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium) }
                        Text(best.product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.match_percent, best.confidence), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onPick(best, ItemStatus.OWNED) }, modifier = Modifier.weight(1f).testTag("pick_owned")) { Text(stringResource(R.string.action_to_collection)) }
                    OutlinedButton(onClick = { onPick(best, ItemStatus.WISHLIST) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_to_wishlist)) }
                }
            }
        }
        matches.drop(1).forEach { match ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemImage(match.product.imageUrl, match.product.category, match.product.name, Modifier.size(56.dp).clip(MaterialTheme.shapes.medium), iconSize = 24.dp)
                Column(Modifier.weight(1f)) {
                    match.product.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium) }
                    Text(match.product.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.match_percent, match.confidence), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { onPick(match, ItemStatus.OWNED) }) { Text(stringResource(R.string.action_choose)) }
            }
        }
        TextButton(onClick = onNothingFits, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.nothing_fits)) }
    }
}
