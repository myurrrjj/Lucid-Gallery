package com.myurrrjj.chronicle.data.local.converters

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Converters {
    companion object{
        private const val FLOAT_SIZE_BYTES = 4
    }
    @TypeConverter
    fun fromFloatArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer
            .allocate(floatArray.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (float in floatArray) {
            byteBuffer.putFloat(float)
        }
        return byteBuffer.array()
    }

    @TypeConverter
    fun toFloatArray(byteArray: ByteArray): FloatArray {
        require(byteArray.size%FLOAT_SIZE_BYTES == 0){
            "Invalid FloatArray BLOB: ${byteArray.size} bytes"
        }

        val byteBuffer = ByteBuffer
            .wrap(byteArray)
            .order(ByteOrder.LITTLE_ENDIAN)

        val floatArray = FloatArray(byteArray.size /FLOAT_SIZE_BYTES)
        for (i in floatArray.indices) {
            floatArray[i] = byteBuffer.getFloat()
        }
        return floatArray
    }
}