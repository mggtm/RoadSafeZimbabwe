
package com.roadsafe.zimbabwe

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RoadAI(context: Context) {

    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets.open("roadsafe_model.tflite").use {
            it.readBytes()
        }

        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        interpreter = Interpreter(modelBuffer)
    }

    fun classify(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        val input = ByteBuffer.allocateDirect(224 * 224 * 3 * 4)
            .order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resized.getPixel(x, y)

                input.putFloat(
                    BitmapColor.red(pixel) / 255f
                )
                input.putFloat(
                    BitmapColor.green(pixel) / 255f
                )
                input.putFloat(
                    BitmapColor.blue(pixel) / 255f
                )
            }
        }

        input.rewind()

        val output = Array(1) { FloatArray(1001) }
        interpreter.run(input, output)

        val index = output[0].indices.maxByOrNull {
            output[0][it]
        } ?: 0

        return "ImageNet class $index"
    }

    fun close() {
        interpreter.close()
    }
}

private object BitmapColor {
    fun red(pixel: Int) = android.graphics.Color.red(pixel)
    fun green(pixel: Int) = android.graphics.Color.green(pixel)
    fun blue(pixel: Int) = android.graphics.Color.blue(pixel)
}
