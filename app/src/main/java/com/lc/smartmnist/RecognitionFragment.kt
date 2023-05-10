package com.lc.smartmnist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.lc.smartmnist.commons.UtilsFunctions
import java.io.IOException
import java.security.SecureRandom

/**
 * @ClassName RecognitionFragment
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:24
 * @Version 1.0
 */
class RecognitionFragment : Fragment() {

    private val rand = SecureRandom()

    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var utilsFunctions: UtilsFunctions

    companion object {

        fun newInstance(): RecognitionFragment {
            return RecognitionFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            utilsFunctions = context as UtilsFunctions
        } catch (ignored: ClassCastException) {
            // Ignored exception
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recognition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieving or creating a ViewModel to allow data to survive configuration changes
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        imageView = requireView().findViewById(R.id.imageView)
        textView = requireView().findViewById(R.id.textView)
        val loadButton: Button = requireView().findViewById(R.id.loadBtn)
        loadButton.setOnClickListener { loadNewImage() }

        // Attempting to restore the data contained in the ViewModel (if the theme of the app is changed)
        if (sharedViewModel.getRecognitionImage() == null) {
            loadNewImage()
        } else {
            imageView.setImageDrawable(sharedViewModel.getRecognitionImage())
            textView.text = sharedViewModel.getRecognitionText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Saving imageView and textView before the Activity is killed
        sharedViewModel.setRecognitionImage(imageView.drawable)
        sharedViewModel.setRecognitionText(textView.text.toString())
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
        imageView.setImageBitmap(bitmap)

        // Recognising the digit on the image
        val result: String = utilsFunctions.digitRecognition(bitmap)
        textView.text = result
    }
}