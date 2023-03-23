package com.hansoolabs.and.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtil {
    fun getCorrectOrientationBitmap(context: Context, path: String): File {
        val bitmap = BitmapFactory.decodeFile(path)
        val exifInterface = ExifInterface(path)
        val orientation = Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)!!)
        
        // 회전 정보를 얻어와서 이미지가 회전되어 있다면, 실제 방향으로 이미지를 회전시킨다.
        var degree = 0
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
        }
        return if (degree > 0) {
            val matrix = Matrix()
            matrix.setRotate(degree.toFloat())
            val rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            val outputDir = context.cacheDir
            val outputFile = File.createTempFile("prefix", "extension", outputDir)
            try {
                outputFile.createNewFile()
                val out = FileOutputStream(outputFile)
                rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rotateBitmap.recycle()
            outputFile
        } else {
            bitmap.recycle()
            File(path)
        }
    }
    
    fun checkIfGif(file: File): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(file)
                val drawable = ImageDecoder.decodeDrawable(source)
                if (drawable is AnimatedImageDrawable) {
                    return true
                }
            } else {
                val movie = Movie.decodeStream(file.inputStream())
                return movie != null
            }
        } catch (e: Throwable) {
            Log.e("BaseUtil", "checkIfGif", e)
        }
        return false
    }
}