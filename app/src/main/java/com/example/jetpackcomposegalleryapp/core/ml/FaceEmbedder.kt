package com.example.jetpackcomposegalleryapp.core.ml

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceEmbedder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var interpreter: Interpreter? = null

    private val modelFileName = "model.tflite"

    private val outputShape = intArrayOf(1, 512)

    /*
     * CPU-only inference for deterministic testing.
     *
     * We intentionally use ONE thread here.
     * Once the pipeline is proven stable, we can benchmark
     * multi-threaded CPU inference again.
     */
    private val numThreads: Int = 1

    init {
        loadModel()
    }

    private fun loadModelFile(
        context: Context,
        modelFileName: String
    ): ByteBuffer {

        val assetFileDescriptor =
            context.assets.openFd(modelFileName)

        val inputStream =
            assetFileDescriptor.createInputStream()

        val fileChannel =
            inputStream.channel

        val startOffset =
            assetFileDescriptor.startOffset

        val declaredLength =
            assetFileDescriptor.declaredLength

        val mappedBuffer =
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )

        inputStream.close()

        return mappedBuffer
    }

    private fun loadModel() {
        try {

            val modelBuffer =
                loadModelFile(
                    context,
                    modelFileName
                )

            val options =
                Interpreter.Options()

            /*
             * IMPORTANT:
             * No GPU delegate.
             *
             * The model now runs entirely on CPU.
             */
            options.setNumThreads(numThreads)

            interpreter =
                Interpreter(
                    modelBuffer,
                    options
                )

            interpreter?.allocateTensors()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateEmbedding(
        preprocessedFace: Bitmap
    ): FloatArray? {

        val currentInterpreter =
            interpreter ?: return null

        val inputBuffer =
            bitmapToFloatBuffer(
                preprocessedFace
            )

        val outputBuffer =
            Array(1) {
                FloatArray(
                    outputShape[1]
                )
            }

        currentInterpreter.run(
            inputBuffer,
            outputBuffer
        )

        return outputBuffer[0]
    }

    private fun bitmapToFloatBuffer(
        bitmap: Bitmap
    ): ByteBuffer {

        val width = 112
        val height = 112

        val bytesPerFloat = 4

        val bufferSize =
            width *
                    height *
                    3 *
                    bytesPerFloat

        val imgData =
            ByteBuffer.allocateDirect(
                bufferSize
            )

        imgData.order(
            ByteOrder.nativeOrder()
        )

        val intValues =
            IntArray(
                width * height
            )

        bitmap.getPixels(
            intValues,
            0,
            width,
            0,
            0,
            width,
            height
        )

        for (pixelValue in intValues) {

            val r =
                (pixelValue shr 16) and 0xFF

            val g =
                (pixelValue shr 8) and 0xFF

            val b =
                pixelValue and 0xFF

            /*
             * Keep the exact same normalization
             * as before. We are NOT changing preprocessing
             * in this experiment.
             */
            imgData.putFloat(
                (r - 127.5f) / 128.0f
            )

            imgData.putFloat(
                (g - 127.5f) / 128.0f
            )

            imgData.putFloat(
                (b - 127.5f) / 128.0f
            )
        }

        imgData.rewind()

        return imgData
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}