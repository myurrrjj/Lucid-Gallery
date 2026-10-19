package com.example.jetpackcomposegalleryapp.presentation.editor

data class CropRect(
    val left : Float,
    val top : Float,
    val right : Float,
    val bottom: Float

)

data class PhotoEditState(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val sharpness: Float = 0f,
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,
    val cropRect: CropRect = CropRect(0f, 0f, 1f, 1f)
)
