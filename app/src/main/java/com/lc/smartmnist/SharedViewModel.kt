package com.lc.smartmnist

import android.graphics.Path
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import com.mozhimen.basick.elemk.viewmodel.bases.BaseViewModel
import org.pytorch.Module

/**
 * @ClassName SharedViewModel
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:47
 * @Version 1.0
 */
class SharedViewModel : BaseViewModel() {
    // App
    private var module: Module? = null
    private var dialogState = false

    // Recognition Fragment
    private var recognitionImage: Drawable? = null
    private var recognitionText: String? = null

    // Draw Fragment
    private var drawText: String? = null
    private var drawPath: Path? = null

    fun getModule(): Module? {
        return module
    }

    fun setModule(module: Module?) {
        this.module = module
    }

    fun getDialogState(): Boolean {
        return dialogState
    }

    fun setDialogState(dialogState: Boolean) {
        this.dialogState = dialogState
    }

    fun getRecognitionImage(): Drawable? {
        return recognitionImage
    }

    fun setRecognitionImage(recognitionImage: Drawable?) {
        this.recognitionImage = recognitionImage
    }

    fun getRecognitionText(): String? {
        return recognitionText
    }

    fun setRecognitionText(recognitionText: String?) {
        this.recognitionText = recognitionText
    }

    fun getDrawText(): String? {
        return drawText
    }

    fun setDrawText(drawText: String?) {
        this.drawText = drawText
    }

    fun getDrawPath(): Path? {
        return drawPath
    }

    fun setDrawPath(drawPath: Path?) {
        this.drawPath = drawPath
    }
}