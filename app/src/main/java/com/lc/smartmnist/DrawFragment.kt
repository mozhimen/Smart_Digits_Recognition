package com.lc.smartmnist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.lc.smartmnist.commons.UtilsFunctions
import com.lc.smartmnist.widgets.DrawView

/**
 * @ClassName DrawFragment
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:21
 * @Version 1.0
 */
class DrawFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var drawView: DrawView
    private lateinit var textView: TextView
    private lateinit var utilsFunctions: UtilsFunctions

    companion object {
        fun newInstance(): DrawFragment {
            return DrawFragment()
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
        return inflater.inflate(R.layout.fragment_draw, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieving or creating a ViewModel to allow data to survive configuration changes
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        drawView = requireView().findViewById(R.id.drawView)
        textView = requireView().findViewById(R.id.textView)
        val eraseButton: Button = requireView().findViewById(R.id.eraseBtn)
        drawView.setOnTouchListener { _, motionEvent ->
            // Recognising the digit on the drawing when the user lifts his finger from the screen
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                val scaledBitmap: Bitmap = drawView.save()
                val result: String = utilsFunctions.digitRecognition(scaledBitmap)
                textView.text = result
                // Indicating that the touch has been consumed
                return@setOnTouchListener true
            }
            false
        }
        eraseButton.setOnClickListener {
            // Erasing the drawing
            drawView.erase()
            textView.setText(R.string.unknown_digit)
        }

        // Attempting to restore the data contained in the ViewModel (if the theme of the app is changed)
        if (sharedViewModel.getDrawText() != null) {
            textView.text = sharedViewModel.getDrawText()
            drawView.path = sharedViewModel.getDrawPath()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Saving textView and path before the Activity is killed
        sharedViewModel.setDrawText(textView.text.toString())
        sharedViewModel.setDrawPath(drawView.path)
    }
}