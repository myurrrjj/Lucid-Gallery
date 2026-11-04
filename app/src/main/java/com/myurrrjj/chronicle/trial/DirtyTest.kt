package com.myurrrjj.chronicle.trial

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myurrrjj.chronicle.core.ml.FaceCropper
import com.myurrrjj.chronicle.core.ml.FaceEmbedder
import com.myurrrjj.chronicle.core.ml.FaceQualityAssessor
import com.myurrrjj.chronicle.core.ml.FaceQualityMetrics
import com.myurrrjj.chronicle.core.ml.MlKitFaceDetector
import com.myurrrjj.chronicle.core.util.BitmapLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt

enum class FaceSampleCondition(val title: String) {
    SHARP_REFERENCE("Sharp frontal"), SHARP_PROFILE("Sharp profile"), MILD_MOTION_BLUR("Mild motion blur"), HEAVY_BLUR(
        "Heavy blur"
    ),
    SMALL_OR_DISTANT("Small / distant face"), LOW_LIGHT("Low light / shadows"), OVEREXPOSED("Overexposed"), PARTIAL_OCCLUSION(
        "Partially occluded"
    ),
    MULTI_FACE_CONTROL("Multi-face control"), NO_FACE_CONTROL("No-face control")
}

data class SelectedTestPhoto(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val personLabel: String?,
    val condition: FaceSampleCondition
)

data class TestFaceSample(
    val faceIndex: Int,
    val identityLabel: String?,
    val accepted: Boolean,
    val rejectionReasons: List<String>,
    val qualityMetrics: FaceQualityMetrics?,
    val embedding: FloatArray?,
    val embeddingError: String? = null
)

data class TestPhotoResult(
    val photo: SelectedTestPhoto,
    val preview: Bitmap?,
    val rawFaceCount: Int,
    val faceSamples: List<TestFaceSample>,
    val error: String? = null
)

data class SimilarityPair(
    val left: String, val right: String, val similarity: Float
)

private data class IdentityEmbedding(
    val identity: String, val displayName: String, val embedding: FloatArray
)

private val TEST_THRESHOLDS = listOf(
    0.50f, 0.55f, 0.60f, 0.65f, 0.70f, 0.75f, 0.80f, 0.85f
)

@HiltViewModel
class DirtyTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val faceDetector: MlKitFaceDetector,
    private val faceEmbedder: FaceEmbedder
) : ViewModel() {

    var isRunning by mutableStateOf(false)
        private set

    var status by mutableStateOf("Select photographs to build your test dataset.")
        private set

    var selectedPhotos by mutableStateOf<List<SelectedTestPhoto>>(emptyList())
        private set

    var results by mutableStateOf<List<TestPhotoResult>>(emptyList())
        private set

    fun addSelectedPhotos(
        uris: List<Uri>, personLabel: String?, condition: FaceSampleCondition
    ) {
        val normalizedLabel = personLabel?.trim()?.takeIf { it.isNotEmpty() }

        val existingUris = selectedPhotos.map { it.uri }.toSet()
        val newUris = uris.distinct().filterNot { it in existingUris }

        newUris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
        }

        val additions = newUris.map { uri ->
            SelectedTestPhoto(
                id = uri.toString(),
                uri = uri,
                displayName = resolveDisplayName(uri),
                personLabel = normalizedLabel,
                condition = condition
            )
        }

        selectedPhotos = selectedPhotos + additions
        status = "${selectedPhotos.size} photographs selected."
    }

    fun removeSelectedPhoto(photoId: String) {
        val photo = selectedPhotos.find { it.id == photoId } ?: return

        releasePhotoPermission(photo.uri)
        selectedPhotos = selectedPhotos.filterNot { it.id == photoId }
        clearResults()
        status = "${selectedPhotos.size} photographs selected."
    }

    fun clearSelectedPhotos() {
        selectedPhotos.forEach {
            releasePhotoPermission(it.uri)
        }

        selectedPhotos = emptyList()
        clearResults()
        status = "Dataset cleared."
    }

    fun runTest() {
        if (isRunning) return

        if (selectedPhotos.isEmpty()) {
            status = "Select at least one photograph."
            return
        }

        viewModelScope.launch {
            isRunning = true
            clearResults()
            status = "Starting face analysis..."

            try {
                val photosToProcess = selectedPhotos.toList()

                for ((index, photo) in photosToProcess.withIndex()) {
                    status =
                        "Processing ${index + 1} of ${photosToProcess.size}: ${photo.displayName}"

                    val result = processPhoto(photo)

                    results = results + result
                }

                val allFaces = results.flatMap { it.faceSamples }
                val detected = results.sumOf { it.rawFaceCount }
                val accepted = allFaces.count { it.accepted }
                val embedded = allFaces.count { it.embedding != null }
                val comparable = identityEmbeddings().size

                status = buildString {
                    appendLine("Analysis complete.")
                    appendLine("Photographs: ${results.size}")
                    appendLine("Detected faces: $detected")
                    appendLine("Accepted faces: $accepted")
                    appendLine("Generated embeddings: $embedded")
                    append("Identity-comparable embeddings: $comparable")
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = "Analysis failed: ${e.message ?: e::class.simpleName}"
            } finally {
                isRunning = false
            }
        }
    }

    private suspend fun processPhoto(
        photo: SelectedTestPhoto
    ): TestPhotoResult {
        var original: Bitmap? = null
        var preview: Bitmap? = null

        try {
            original = BitmapLoader.loadBitmap(
                contentResolver = context.contentResolver, uriString = photo.uri.toString()
            ) ?: return TestPhotoResult(
                photo = photo,
                preview = null,
                rawFaceCount = 0,
                faceSamples = emptyList(),
                error = "Could not decode photograph or access its URI."
            )

            val bitmap = original

            val diagnostics = faceDetector.detectFacesWithDiagnostics(bitmap)

            preview = withContext(Dispatchers.Default) {
                drawDetectionPreview(bitmap, diagnostics)
            }

            val identityIsUnambiguous =
                photo.personLabel != null && diagnostics.size == 1 && diagnostics.single().accepted

            val samples = diagnostics.mapIndexed { index, diagnostic ->

                val face = diagnostic.face
                val landmarkCount = listOf(
                    com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE,
                    com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE,
                    com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE,
                    com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT,
                    com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT
                ).count {
                    face.getLandmark(it) != null
                }

                val qualityMetrics = withContext(Dispatchers.Default) {
                    FaceQualityAssessor.assess(
                        bitmap = bitmap,
                        faceBounds = face.boundingBox,
                        alignmentLandmarkCount = landmarkCount
                    )
                }

                if (!diagnostic.accepted) {
                    TestFaceSample(
                        faceIndex = index + 1,
                        identityLabel = null,
                        accepted = false,
                        rejectionReasons = diagnostic.rejectionReasons,
                        qualityMetrics = qualityMetrics,
                        embedding = null
                    )
                } else {
                    generateFaceSample(
                        bitmap = bitmap,
                        face = face,
                        faceIndex = index + 1,
                        identityLabel = if (identityIsUnambiguous) {
                            photo.personLabel
                        } else {
                            null
                        },
                        qualityMetrics = qualityMetrics
                    )
                }
            }

            val result = TestPhotoResult(
                photo = photo,
                preview = preview,
                rawFaceCount = diagnostics.size,
                faceSamples = samples
            )

            preview = null

            return result

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            preview?.recycle()

            return TestPhotoResult(
                photo = photo,
                preview = null,
                rawFaceCount = 0,
                faceSamples = emptyList(),
                error = "${e::class.simpleName}: ${e.message ?: "Unknown error"}"
            )
        } finally {
            original?.recycle()
        }
    }

    private suspend fun generateFaceSample(
        bitmap: Bitmap,
        face: com.google.mlkit.vision.face.Face,
        faceIndex: Int,
        identityLabel: String?,
        qualityMetrics: FaceQualityMetrics?
    ): TestFaceSample {
        var cropped: Bitmap? = null
        var preprocessed: Bitmap? = null

        try {
            cropped = FaceCropper.cropFace(
                originalBitmap = bitmap, face = face, margin = 0.2f
            ) ?: return TestFaceSample(
                faceIndex = faceIndex,
                identityLabel = null,
                accepted = true,
                rejectionReasons = emptyList(),
                qualityMetrics = qualityMetrics,
                embedding = null,
                embeddingError = "Face cropping failed."
            )

            preprocessed = FaceCropper.preprocessForEmbedding(cropped)

            val embedding = withContext(Dispatchers.Default) {
                faceEmbedder.generateEmbedding(preprocessed)
            }

            return TestFaceSample(
                faceIndex = faceIndex,
                identityLabel = if (embedding != null) identityLabel else null,
                accepted = true,
                rejectionReasons = emptyList(),
                qualityMetrics = qualityMetrics,
                embedding = embedding,
                embeddingError = if (embedding == null) {
                    "Embedding generation failed."
                } else {
                    null
                }
            )

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return TestFaceSample(
                faceIndex = faceIndex,
                identityLabel = null,
                accepted = true,
                rejectionReasons = emptyList(),
                qualityMetrics = qualityMetrics,
                embedding = null,
                embeddingError = "${e::class.simpleName}: ${e.message}"
            )
        } finally {
            if (preprocessed != null && preprocessed !== cropped) {
                preprocessed.recycle()
            }

            cropped?.recycle()
        }
    }

    private fun drawDetectionPreview(
        original: Bitmap, diagnostics: List<com.myurrrjj.chronicle.core.ml.FaceDetectionDiagnostic>
    ): Bitmap {
        val maxDimension = 384f
        val scale = minOf(
            1f, maxDimension / maxOf(original.width, original.height)
        )

        val width = (original.width * scale).toInt().coerceAtLeast(1)
        val height = (original.height * scale).toInt().coerceAtLeast(1)

        val output = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)
        val scaleX = width.toFloat() / original.width
        val scaleY = height.toFloat() / original.height

        canvas.drawBitmap(
            original,
            null,
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        diagnostics.forEachIndexed { index, diagnostic ->
            val bounds = diagnostic.face.boundingBox

            boxPaint.color = if (diagnostic.accepted) {
                Color.GREEN
            } else {
                Color.RED
            }

            textPaint.color = boxPaint.color

            val left = bounds.left * scaleX
            val top = bounds.top * scaleY
            val right = bounds.right * scaleX
            val bottom = bounds.bottom * scaleY

            canvas.drawRect(left, top, right, bottom, boxPaint)

            canvas.drawText(
                "#${index + 1}", left.coerceAtLeast(2f), (top - 4f).coerceAtLeast(12f), textPaint
            )
        }

        return output
    }

    private fun identityEmbeddings(): List<IdentityEmbedding> {
        return results.flatMap { result ->
            result.faceSamples.mapNotNull { sample ->
                val identity = sample.identityLabel ?: return@mapNotNull null
                val embedding = sample.embedding ?: return@mapNotNull null

                IdentityEmbedding(
                    identity = identity,
                    displayName = "${result.photo.displayName} / Face ${sample.faceIndex}",
                    embedding = embedding
                )
            }
        }
    }

    fun samePersonPairs(): List<SimilarityPair> {
        return calculatePairs { left, right ->
            left.identity == right.identity
        }
    }

    fun differentPersonPairs(): List<SimilarityPair> {
        return calculatePairs { left, right ->
            left.identity != right.identity
        }
    }

    private fun calculatePairs(
        condition: (IdentityEmbedding, IdentityEmbedding) -> Boolean
    ): List<SimilarityPair> {
        val embeddings = identityEmbeddings()
        val pairs = mutableListOf<SimilarityPair>()

        for (i in embeddings.indices) {
            for (j in i + 1 until embeddings.size) {
                val left = embeddings[i]
                val right = embeddings[j]

                if (condition(left, right)) {
                    pairs += SimilarityPair(
                        left = "${left.identity}/${left.displayName}",
                        right = "${right.identity}/${right.displayName}",
                        similarity = cosineSimilarity(
                            left.embedding, right.embedding
                        )
                    )
                }
            }
        }

        return pairs.sortedByDescending { it.similarity }
    }

    private fun cosineSimilarity(
        a: FloatArray, b: FloatArray
    ): Float {
        require(a.size == b.size) {
            "Embedding sizes differ: ${a.size} vs ${b.size}"
        }

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            require(a[i].isFinite() && b[i].isFinite()) {
                "Embedding contains a non-finite value."
            }

            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        if (normA <= 0.0 || normB <= 0.0) return 0f

        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    private fun resolveDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                    if (index >= 0) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } ?: uri.lastPathSegment ?: "Selected photograph"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Selected photograph"
        }
    }

    private fun releasePhotoPermission(uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun clearResults() {
        results.forEach { result ->
            result.preview?.takeUnless { it.isRecycled }?.recycle()
        }

        results = emptyList()
    }

    override fun onCleared() {
        clearResults()

        selectedPhotos.forEach {
            releasePhotoPermission(it.uri)
        }

        super.onCleared()
    }
}

@Composable
fun DirtyTestScreen(
    viewModel: DirtyTestViewModel = hiltViewModel()
) {
    var identityLabel by rememberSaveable {
        mutableStateOf("")
    }

    var qualityOnly by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedCondition by rememberSaveable {
        mutableStateOf(FaceSampleCondition.SHARP_REFERENCE.name)
    }

    var conditionMenuExpanded by remember {
        mutableStateOf(false)
    }

    val condition = FaceSampleCondition.valueOf(selectedCondition)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = 50
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedPhotos(
                uris = uris,
                personLabel = if (qualityOnly) null else identityLabel.trim(),
                condition = condition
            )
        }
    }

    val selectedPhotos = viewModel.selectedPhotos
    val results = viewModel.results
    val samePairs = viewModel.samePersonPairs()
    val differentPairs = viewModel.differentPersonPairs()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Face Quality Lab",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Build a labelled dataset using photographs from your gallery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = identityLabel,
                    onValueChange = { identityLabel = it },
                    label = { Text("Identity label") },
                    placeholder = { Text("Person A") },
                    enabled = !qualityOnly,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = qualityOnly, onCheckedChange = { qualityOnly = it })

                    Text(
                        text = "Quality-only control (no identity)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = {
                            conditionMenuExpanded = true
                        }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Condition: ${condition.title}")
                    }

                    DropdownMenu(
                        expanded = conditionMenuExpanded, onDismissRequest = {
                            conditionMenuExpanded = false
                        }) {
                        FaceSampleCondition.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.title) }, onClick = {
                                selectedCondition = option.name
                                conditionMenuExpanded = false
                            })
                        }
                    }
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    enabled = qualityOnly || identityLabel.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select photos from gallery")
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected photographs (${selectedPhotos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = viewModel::clearSelectedPhotos,
                        enabled = selectedPhotos.isNotEmpty() && !viewModel.isRunning
                    ) {
                        Text("Clear all")
                    }
                }
            }
        }

        items(
            items = selectedPhotos, key = { "selected_${it.id}" }) { photo ->
            SelectedPhotoCard(
                photo = photo, onRemove = {
                    viewModel.removeSelectedPhoto(photo.id)
                })
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::runTest,
                    enabled = selectedPhotos.isNotEmpty() && !viewModel.isRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Processing...")
                    } else {
                        Text("Run face analysis")
                    }
                }

                Text(
                    text = viewModel.status, style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (results.isNotEmpty()) {
            item {
                SectionTitle("1. Per-photo detection and quality")
            }

            items(
                items = results, key = { "result_${it.photo.id}" }) { result ->
                PhotoResultCard(result)
            }

            item {
                SectionTitle("2. Same-person similarities")
                PairSummary(
                    title = "Pairs belonging to the same labelled identity", pairs = samePairs
                )
            }

            item {
                SectionTitle("3. Different-person similarities")
                PairSummary(
                    title = "Pairs belonging to different labelled identities",
                    pairs = differentPairs
                )
            }

            item {
                SectionTitle("4. Threshold sweep")
                ThresholdSweep(
                    samePairs = samePairs, differentPairs = differentPairs
                )
            }
        }
    }
}

@Composable
private fun SelectedPhotoCard(
    photo: SelectedTestPhoto, onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = photo.displayName, fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Identity: ${photo.personLabel ?: "None"}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Condition: ${photo.condition.title}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(onClick = onRemove) {
                Text("Remove")
            }
        }
    }
}

@Composable
private fun PhotoResultCard(
    result: TestPhotoResult
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = result.photo.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Identity: ${result.photo.personLabel ?: "Unlabelled"}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Condition: ${result.photo.condition.title}",
                style = MaterialTheme.typography.bodySmall
            )

            result.preview?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Detected faces",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Text("ML Kit detections: ${result.rawFaceCount}")

            if (result.faceSamples.isEmpty()) {
                Text(
                    text = result.error ?: "No faces detected.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            result.faceSamples.forEach { sample ->
                HorizontalDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Face ${sample.faceIndex}: ${
                            if (sample.accepted) "BASIC PASS" else "BASIC REJECT"
                        }", fontWeight = FontWeight.Bold, color = if (sample.accepted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )

                    sample.qualityMetrics?.let { metrics ->
                        Text(
                            text = "Dimensions: ${metrics.faceWidthPx} × ${metrics.faceHeightPx}px",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Laplacian variance: ${
                                formatMetric(metrics.laplacianVariance)
                            }", style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Mean luminance: ${
                                formatMetric(metrics.meanLuminance)
                            }", style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Luminance SD: ${
                                formatMetric(metrics.luminanceStdDev)
                            }", style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Alignment landmarks: ${metrics.alignmentLandmarkCount}/5",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (metrics.wasClippedToImage) {
                            Text(
                                text = "Face bounding box clipped to image boundaries.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (sample.rejectionReasons.isNotEmpty()) {
                        Text(
                            text = "Rejection: ${sample.rejectionReasons.joinToString("; ")}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = when {
                            sample.embedding != null -> "Embedding: ${sample.embedding.size} values"
                            sample.embeddingError != null -> "Embedding: ${sample.embeddingError}"
                            else -> "Embedding: not generated"
                        }, style = MaterialTheme.typography.bodySmall
                    )

                    if (sample.identityLabel != null) {
                        Text(
                            text = "Included in identity comparisons: ${sample.identityLabel}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairSummary(
    title: String, pairs: List<SimilarityPair>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title, fontWeight = FontWeight.Bold
            )

            if (pairs.isEmpty()) {
                Text(
                    text = "No comparable identity pairs yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                pairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${pair.left} ↔ ${pair.right}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = formatMetric(pair.similarity.toDouble()),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider()

                Text("Pair count: ${pairs.size}")
                Text("Minimum: ${formatMetric(pairs.minOf { it.similarity }.toDouble())}")
                Text("Maximum: ${formatMetric(pairs.maxOf { it.similarity }.toDouble())}")
                Text("Mean: ${formatMetric(pairs.map { it.similarity }.average())}")
                Text(
                    "Median: ${formatMetric(median(pairs.map { it.similarity.toDouble() }))}"
                )
            }
        }
    }
}

@Composable
private fun ThresholdSweep(
    samePairs: List<SimilarityPair>, differentPairs: List<SimilarityPair>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Same-person vs different-person pairs", fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Threshold", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold
                )
                Text(
                    "Same", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold
                )
                Text(
                    "Different", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold
                )
            }

            TEST_THRESHOLDS.forEach { threshold ->
                val sameAbove = samePairs.count {
                    it.similarity >= threshold
                }

                val differentAbove = differentPairs.count {
                    it.similarity >= threshold
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatMetric(threshold.toDouble()), modifier = Modifier.weight(1f)
                    )

                    Text(
                        "$sameAbove / ${samePairs.size}", modifier = Modifier.weight(1f)
                    )

                    Text(
                        "$differentAbove / ${differentPairs.size}", modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "Diagnostic measurements only. No production threshold is selected by this test.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String
) {
    Text(
        text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
    )
}

private fun formatMetric(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

private fun median(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0

    val sorted = values.sorted()
    val middle = sorted.size / 2

    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle]
    }
}
