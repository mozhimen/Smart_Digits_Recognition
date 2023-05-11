package com.lc.smartmnist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import com.lc.smartmnist.commons.UtilsFunctions
import com.lc.smartmnist.databinding.FragmentRecognitionBinding
import com.mozhimen.basick.elemk.fragment.bases.BaseFragmentVBVM
import java.io.IOException
import java.security.SecureRandom

/**
 * @ClassName RecognitionFragment
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:24
 * @Version 1.0
 */
class RecognitionFragment : BaseFragmentVBVM<FragmentRecognitionBinding, SharedViewModel>() {

    private val rand = SecureRandom()

    private lateinit var utilsFunctions: UtilsFunctions

    companion object {

        fun newInstance(): RecognitionFragment {
            return RecognitionFragment()
        }
    }

    override fun bindViewVM(vb: FragmentRecognitionBinding) {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            utilsFunctions = context as UtilsFunctions
        } catch (ignored: ClassCastException) {
            // Ignored exception
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        VB.loadBtn.setOnClickListener { loadNewImage() }

        // Attempting to restore the data contained in the ViewModel (if the theme of the app is changed)
        if (vm.getRecognitionImage() == null) {
            loadNewImage()
        } else {
            VB.imageView.setImageDrawable(vm.getRecognitionImage())
            VB.textView.text = vm.getRecognitionText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Saving imageView and textView before the Activity is killed
        vm.setRecognitionImage(VB.imageView.drawable)
        vm.setRecognitionText(VB.textView.text.toString())
    }

    private fun loadNewImage() {
        // Creating bitmap from img packaged into app Android asset (app/src/main/assets/img/img_?.jpg)
        var bitmap: Bitmap? = null
        try {
            val randomInt = rand.nextInt(350) + 1
            bitmap = BitmapFactory.decodeStream(
                requireActivity().assets.open("img/img_$randomInt.jpg")
            )
        } catch (e: IOException) {
            Log.e("IOException", "Error reading assets (image)", e)
            requireActivity().finish()
        }

        // Showing image on UI
        VB.imageView.setImageBitmap(bitmap)

        // Recognising the digit on the image
        val result: String = utilsFunctions.digitRecognition(bitmap)
        VB.textView.text = result
    }
}