
package com.myurrrjj.chronicle.trial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.myurrrjj.chronicle.core.ml.FaceCropper
import com.myurrrjj.chronicle.core.ml.FaceEmbedder
import com.myurrrjj.chronicle.core.ml.MlKitFaceDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt

data class TestFace(
    val person: String,
    val name: String,
    val bitmap: Bitmap,
    val crop: Bitmap?,
    val embedding: FloatArray?,
    val rawFaceCount: Int,
    val validFaceCount: Int,
    val rejectionReasons: List<String> = emptyList(),
    val error: String? = null
)

data class SimilarityPair(
    val left: String,
    val right: String,
    val similarity: Float
)

private val TEST_THRESHOLDS = listOf(
    0.50f,
    0.55f,
    0.60f,
    0.65f,
    0.70f,
    0.75f,
    0.80f,
    0.85f
)

@HiltViewModel
class DirtyTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val faceDetector: MlKitFaceDetector,
    private val faceEmbedder: FaceEmbedder
) : ViewModel() {

    var isRunning by mutableStateOf(false)
        private set

    var status by mutableStateOf(
        "Ready. Put test photos in assets/face_data/person_a and person_b."
    )
        private set

    var results by mutableStateOf<List<TestFace>>(emptyList())
        private set

    fun runTest() {
        if (isRunning) return

        viewModelScope.launch {
            isRunning = true
            status = "Loading test photos..."

            results = withContext(Dispatchers.Default) {
                val personA = loadAndProcess("person_a", "A")
                val personB = loadAndProcess("person_b", "B")
                personA + personB
            }

            val successful =
                results.count { it.embedding != null }

            val rawFaces =
                results.sumOf { it.rawFaceCount }

            val validFaces =
                results.sumOf { it.validFaceCount }

            status =
                "Finished. $successful/${results.size} embeddings. " +
                        "ML Kit found $rawFaces faces; " +
                        "$validFaces passed validation."

            isRunning = false
        }
    }

    private suspend fun loadAndProcess(
        folder: String,
        personLabel: String
    ): List<TestFace> {

        val names = withContext(Dispatchers.IO) {
            context.assets.list("face_data/$folder")
                ?.filter {
                    it.endsWith(".jpg", true) ||
                            it.endsWith(".jpeg", true) ||
                            it.endsWith(".png", true) ||
                            it.endsWith(".webp", true)
                }
                ?.sorted()
                .orEmpty()
        }

        return names.mapIndexed { index, fileName ->
            val label = "$personLabel${index + 1}"

            processOne(
                folder = folder,
                fileName = fileName,
                personLabel = personLabel,
                label = label
            )
        }
    }

    private suspend fun processOne(
        folder: String,
        fileName: String,
        personLabel: String,
        label: String
    ): TestFace {

        var original: Bitmap? = null

        return try {
            original = withContext(Dispatchers.IO) {
                loadOrientedBitmap(
                    "face_data/$folder/$fileName"
                )
            } ?: return failed(
                personLabel,
                label,
                fileName,
                "Could not decode image."
            )

            val diagnostics =
                faceDetector.detectFacesWithDiagnostics(original)

            // Diagnostic-only image: draw every ML Kit detection box
            // so false positives are immediately visible.
            val diagnosticBitmap =
                drawDetectionBoxes(
                    original,
                    diagnostics
                )

            val rawFaceCount =
                diagnostics.size

            val accepted =
                diagnostics.filter { it.accepted }

            val validFaceCount =
                accepted.size

            if (accepted.size != 1) {

                val reasons =
                    diagnostics
                        .filter { !it.accepted }
                        .flatMap {
                            it.rejectionReasons
                        }
                        .distinct()

                val error =
                    when {
                        rawFaceCount == 0 ->
                            "ML Kit detected no face."

                        accepted.isEmpty() &&
                                reasons.isNotEmpty() ->
                            "Detected face(s), but validation rejected them."

                        accepted.size > 1 ->
                            "Expected 1 accepted face, " +
                                    "found $validFaceCount."

                        else ->
                            "Could not select exactly one accepted face."
                    }

                return TestFace(
                    person = personLabel,
                    name = fileName,
                    bitmap = diagnosticBitmap,
                    crop = null,
                    embedding = null,
                    rawFaceCount = rawFaceCount,
                    validFaceCount = validFaceCount,
                    rejectionReasons = reasons,
                    error = error
                )
            }

            val face =
                accepted.single().face

            val crop =
                FaceCropper.cropFace(
                    original,
                    face
                ) ?: return TestFace(
                    person = personLabel,
                    name = fileName,
                    bitmap = diagnosticBitmap,
                    crop = null,
                    embedding = null,
                    rawFaceCount = rawFaceCount,
                    validFaceCount = validFaceCount,
                    error = "Face crop failed."
                )

            val preprocessed =
                FaceCropper.preprocessForEmbedding(crop)

            val embedding =
                faceEmbedder.generateEmbedding(
                    preprocessed
                )

            TestFace(
                person = personLabel,
                name = fileName,
                bitmap = diagnosticBitmap,
                crop = preprocessed,
                embedding = embedding,
                rawFaceCount = rawFaceCount,
                validFaceCount = validFaceCount,
                error =
                    if (embedding == null) {
                        "Embedding generation failed."
                    } else {
                        null
                    }
            )

        } catch (e: Exception) {

            TestFace(
                person = personLabel,
                name = fileName,
                bitmap =
                    original
                        ?: Bitmap.createBitmap(
                            1,
                            1,
                            Bitmap.Config.ARGB_8888
                        ),
                crop = null,
                embedding = null,
                rawFaceCount = 0,
                validFaceCount = 0,
                error =
                    "${e::class.simpleName}: " +
                            (e.message ?: "unknown error")
            )
        }
    }


    private fun loadOrientedBitmap(
        assetPath: String
    ): Bitmap? {

        val bytes =
            context.assets.open(assetPath).use {
                it.readBytes()
            }

        val bitmap =
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            ) ?: return null

        val orientation =
            ExifInterface(
                ByteArrayInputStream(bytes)
            ).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

        val matrix = Matrix()

        when (orientation) {

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(1f, -1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(-90f)
            }

            ExifInterface.ORIENTATION_NORMAL,
            ExifInterface.ORIENTATION_UNDEFINED -> {
                return bitmap
            }
        }

        return try {

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            ).also { corrected ->

                if (corrected !== bitmap) {
                    bitmap.recycle()
                }
            }

        } catch (e: Exception) {

            bitmap.recycle()
            null
        }
    }

    private fun drawDetectionBoxes(
        original: Bitmap,
        diagnostics: List<com.myurrrjj.chronicle.core.ml.FaceDetectionDiagnostic>
    ): Bitmap {

        val output =
            original.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(output)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (original.width * 0.006f).coerceAtLeast(4f)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textSize = (original.width * 0.035f).coerceAtLeast(28f)
            typeface = Typeface.DEFAULT_BOLD
        }

        diagnostics.forEachIndexed { index, diagnostic ->
            val box = diagnostic.face.boundingBox

            strokePaint.color =
                if (diagnostic.accepted) Color.GREEN else Color.RED

            textPaint.color = strokePaint.color

            canvas.drawRect(box, strokePaint)

            val label =
                if (diagnostic.accepted) {
                    "#${index + 1} ACCEPTED"
                } else {
                    "#${index + 1} REJECTED"
                }

            val textX = box.left.toFloat().coerceAtLeast(4f)
            val textY =
                (box.top - 8f).coerceAtLeast(textPaint.textSize)

            canvas.drawText(
                label,
                textX,
                textY,
                textPaint
            )
        }

        return output
    }

    private fun failed(
        person: String,
        label: String,
        fileName: String,
        error: String
    ) =
        TestFace(
            person = person,
            name = fileName,
            bitmap =
                Bitmap.createBitmap(
                    1,
                    1,
                    Bitmap.Config.ARGB_8888
                ),
            crop = null,
            embedding = null,
            rawFaceCount = 0,
            validFaceCount = 0,
            error = error
        )

    fun samePersonPairs(): List<SimilarityPair> =
        pairsFor { a, b ->
            a.person == b.person
        }

    fun differentPersonPairs(): List<SimilarityPair> =
        pairsFor { a, b ->
            a.person != b.person
        }

    private fun pairsFor(
        condition: (TestFace, TestFace) -> Boolean
    ): List<SimilarityPair> {

        val usable =
            results.filter {
                it.embedding != null
            }

        val pairs =
            mutableListOf<SimilarityPair>()

        for (i in 0 until usable.size) {

            for (j in i + 1 until usable.size) {

                val a = usable[i]
                val b = usable[j]

                if (condition(a, b)) {

                    pairs += SimilarityPair(
                        left =
                            "${a.person}/${a.name}",

                        right =
                            "${b.person}/${b.name}",

                        similarity =
                            cosineSimilarity(
                                a.embedding!!,
                                b.embedding!!
                            )
                    )
                }
            }
        }

        return pairs.sortedByDescending {
            it.similarity
        }
    }

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray
    ): Float {

        require(
            a.size == b.size
        ) {
            "Embedding sizes differ: " +
                    "${a.size} vs ${b.size}"
        }

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (
            normA == 0f ||
            normB == 0f
        ) {
            0f
        } else {
            dot /
                    (
                            sqrt(normA) *
                                    sqrt(normB)
                            )
        }
    }

    override fun onCleared() {
        /*
         * FaceEmbedder and MlKitFaceDetector are @Singletons.
         * Hilt owns their lifecycle.
         */
        super.onCleared()
    }
}

@Composable
fun DirtyTestScreen(
    viewModel: DirtyTestViewModel =
        hiltViewModel()
) {

    val results =
        viewModel.results

    val samePairs =
        viewModel.samePersonPairs()

    val differentPairs =
        viewModel.differentPersonPairs()

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
                .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {

            Text(
                text = "Face Embedding Lab",
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text =
                    "Detector + embedding experiment",
                style =
                    MaterialTheme.typography.bodyMedium
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Button(
                onClick =
                    viewModel::runTest,

                enabled =
                    !viewModel.isRunning,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                if (viewModel.isRunning) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(18.dp),

                        strokeWidth = 2.dp
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text("Running...")

                } else {

                    Text("Run Face Test")
                }
            }

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text =
                    viewModel.status,

                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        if (results.isNotEmpty()) {

            item {

                SectionTitle(
                    "1. Per-photo pipeline"
                )
            }

            items(
                items = results,

                key = {
                    "${it.person}/${it.name}"
                }
            ) { result ->

                PhotoResultCard(result)
            }

            item {

                SectionTitle(
                    "2. Same-person similarities"
                )

                PairSummary(
                    title =
                        "Pairs where the folder says they are the same person",

                    pairs =
                        samePairs
                )
            }

            item {

                SectionTitle(
                    "3. Different-person similarities"
                )

                PairSummary(
                    title =
                        "Pairs where the folders say they are different people",

                    pairs =
                        differentPairs
                )
            }

            item {

                SectionTitle(
                    "4. Threshold sweep"
                )

                ThresholdSweep(
                    samePairs =
                        samePairs,

                    differentPairs =
                        differentPairs
                )
            }
        }
    }
}

@Composable
private fun PhotoResultCard(
    result: TestFace
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Image(
                bitmap =
                    result.bitmap.asImageBitmap(),

                contentDescription =
                    result.name,

                modifier =
                    Modifier.size(72.dp)
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "${result.person} — ${result.name}",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "ML Kit faces: " +
                            result.rawFaceCount
                )

                Text(
                    "Accepted faces: " +
                            result.validFaceCount
                )

                if (result.embedding != null) {

                    Text(
                        "Embedding: ✓ " +
                                "${result.embedding.size} values"
                    )

                } else {

                    Text(
                        "Embedding: ✗"
                    )
                }

                result.error?.let {

                    Text(
                        text = it,

                        color =
                            MaterialTheme.colorScheme.error,

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                if (
                    result.rejectionReasons.isNotEmpty()
                ) {

                    Spacer(
                        Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Rejected because: " +
                                    result.rejectionReasons.joinToString(
                                        separator = "; "
                                    ),

                        color =
                            MaterialTheme.colorScheme.error,

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }

            result.crop?.let {

                Image(
                    bitmap =
                        it.asImageBitmap(),

                    contentDescription =
                        "112x112 face crop",

                    modifier =
                        Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
private fun PairSummary(
    title: String,
    pairs: List<SimilarityPair>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Text(
                title,
                style =
                    MaterialTheme.typography.bodyMedium
            )

            if (pairs.isEmpty()) {

                Text(
                    "No usable pairs yet.",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                return@Column
            }

            Spacer(
                Modifier.height(8.dp)
            )

            pairs.forEach { pair ->

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text =
                            "${pair.left} ↔ ${pair.right}",

                        modifier =
                            Modifier.weight(1f),

                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text =
                            formatSimilarity(
                                pair.similarity
                            ),

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.height(4.dp)
                )
            }

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        vertical = 6.dp
                    )
            )

            Text(
                "count: ${pairs.size}"
            )

            Text(
                "min: ${
                    formatSimilarity(
                        pairs.minOf {
                            it.similarity
                        }
                    )
                }"
            )

            Text(
                "max: ${
                    formatSimilarity(
                        pairs.maxOf {
                            it.similarity
                        }
                    )
                }"
            )

            Text(
                "mean: ${
                    formatSimilarity(
                        pairs
                            .map {
                                it.similarity
                            }
                            .average()
                            .toFloat()
                    )
                }"
            )

            Text(
                "median: ${
                    formatSimilarity(
                        median(
                            pairs.map {
                                it.similarity
                            }
                        )
                    )
                }"
            )
        }
    }
}

@Composable
private fun ThresholdSweep(
    samePairs: List<SimilarityPair>,
    differentPairs: List<SimilarityPair>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Text(
                "Same-person vs different-person pairs",
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Threshold",
                    modifier =
                        Modifier.weight(1f),

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Same",
                    modifier =
                        Modifier.weight(1f),

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Different",
                    modifier =
                        Modifier.weight(1f),

                    fontWeight =
                        FontWeight.Bold
                )
            }

            TEST_THRESHOLDS.forEach { threshold ->

                val sameAbove =
                    samePairs.count {
                        it.similarity >= threshold
                    }

                val differentAbove =
                    differentPairs.count {
                        it.similarity >= threshold
                    }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 3.dp
                            )
                ) {

                    Text(
                        formatSimilarity(
                            threshold
                        ),

                        modifier =
                            Modifier.weight(1f)
                    )

                    Text(
                        "$sameAbove / ${samePairs.size}",

                        modifier =
                            Modifier.weight(1f)
                    )

                    Text(
                        "$differentAbove / ${differentPairs.size}",

                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                "This is measurement only. " +
                        "We are not selecting a new " +
                        "production threshold yet.",

                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text = text,

        style =
            MaterialTheme.typography.titleMedium,

        fontWeight =
            FontWeight.Bold
    )
}

private fun median(
    values: List<Float>
): Float {

    if (values.isEmpty()) {
        return 0f
    }

    val sorted =
        values.sorted()

    val middle =
        sorted.size / 2

    return if (sorted.size % 2 == 0) {

        (
                sorted[middle - 1] +
                        sorted[middle]
                ) / 2f

    } else {

        sorted[middle]
    }
}

private fun formatSimilarity(
    value: Float
): String =
    String.format(
        Locale.US,
        "%.4f",
        value
    )
