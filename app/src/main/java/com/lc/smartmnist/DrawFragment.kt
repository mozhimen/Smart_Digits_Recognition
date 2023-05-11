package com.lc.smartmnist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import com.lc.smartmnist.commons.UtilsFunctions
import com.lc.smartmnist.databinding.FragmentDrawBinding
import com.mozhimen.basick.elemk.fragment.bases.BaseFragmentVBVM

/**
 * @ClassName DrawFragment
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:21
 * @Version 1.0
 */
class DrawFragment : BaseFragmentVBVM<FragmentDrawBinding, SharedViewModel>() {
    private lateinit var utilsFunctions: UtilsFunctions

    companion object {
        fun newInstance(): DrawFragment {
            return DrawFragment()
        }
    }

    override fun bindViewVM(vb: FragmentDrawBinding) {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            utilsFunctions = context as UtilsFunctions
        } catch (ignored: ClassCastException) {
            // Ignored exception
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(savedInstanceState: Bundle?) {
        val eraseButton: Button = requireView().findViewById(R.id.eraseBtn)
        VB.drawView.setOnTouchListener { _, motionEvent ->
            // Recognising the digit on the drawing when the user lifts his finger from the screen
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                val scaledBitmap: Bitmap = VB.drawView.save()
                val result: String = utilsFunctions.digitRecognition(scaledBitmap)
                VB.textView.text = result
                // Indicating that the touch has been consumed
                return@setOnTouchListener true
            }
            false
        }
        eraseButton.setOnClickListener {
            // Erasing the drawing
            VB.drawView.erase()
            VB.textView.setText(R.string.unknown_digit)
        }

        // Attempting to restore the data contained in the ViewModel (if the theme of the app is changed)
        if (vm.getDrawText() != null) {
            VB.textView.text = vm.getDrawText()
            VB.drawView.path = vm.getDrawPath()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Saving textView and path before the Activity is killed
        vm.setDrawText(VB.textView.text.toString())
        vm.setDrawPath(VB.drawView.path)
    }
}