package com.lc.smartmnist.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * @ClassName ImageUtils
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:59
 * @Version 1.0
 */
object ImageUtils {
    @JvmStatic
    fun readBitmapFromFile(filePath: String?, size: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()
        var inSampleSize = 1
        if (srcHeight > size || srcWidth > size) {
            inSampleSize = if (srcWidth < srcHeight) {
                Math.round(srcHeight / size)
            } else {
                Math.round(srcWidth / size)
            }
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeFile(filePath, options)
    }
}