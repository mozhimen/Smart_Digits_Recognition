package com.lc.smartmnist

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationBarView
import com.lc.smartmnist.commons.UtilsFunctions
import com.lc.smartmnist.databinding.ActivityMainBinding
import com.mozhimen.basick.elemk.activity.bases.BaseActivityVB
import com.mozhimen.basick.elemk.activity.bases.BaseActivityVBVM
import com.mozhimen.basick.manifestk.cons.CPermission
import com.mozhimen.basick.manifestk.permission.ManifestKPermission
import com.mozhimen.basick.manifestk.permission.annors.APermissionCheck
import com.mozhimen.basick.utilk.content.activity.UtilKLaunchActivity
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@APermissionCheck(CPermission.CAMERA)
open class MainActivity : BaseActivityVBVM<ActivityMainBinding, SharedViewModel>(), UtilsFunctions {
    private val items = arrayOf("Light", "Dark", "Auto (Based on System)")
    private var module: Module? = null
    private var dialog: AlertDialog? = null
    private lateinit var recognitionFragment: RecognitionFragment
    private lateinit var drawFragment: DrawFragment
    private lateinit var ocrFragment: OcrFragment
    override fun bindViewVM(vb: ActivityMainBinding) {

    }

    override fun initData(savedInstanceState: Bundle?) {
        ManifestKPermission.initPermissions(this) {
            if (it) {
                super.initData(savedInstanceState)
            } else {
                UtilKLaunchActivity.startSettingAppDetails(this)
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        // Configuring the Top app bar
        val topAppBar = findViewById<MaterialToolbar>(R.id.top_app_bar)
        topAppBar.setOnMenuItemClickListener { item: MenuItem ->
            // Displaying the AlertDialog when the settings button is clicked
            if (item.itemId == R.id.action_settings) {
                dialog!!.show()
                return@setOnMenuItemClickListener true
            }
            false
        }

        // Modifying Status Bar and Navigation Bar colors to match app colors
        window.statusBarColor = SurfaceColors.SURFACE_0.getColor(this)
        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)

        // Retrieving or creating a ViewModel to allow data to survive configuration changes
        vm = ViewModelProvider(this)[SharedViewModel::class.java]
        module = vm.getModule()
        if (module == null) {
            try {
                // Loading serialized TorchScript module from file packaged into app Android asset (app/src/model/assets/modelMNIST_ts_lite.ptl)
                module = LiteModuleLoader.load(assetFilePath(this, "modelMNIST_ts_lite.ptl"))
                vm.setModule(module)
            } catch (e: IOException) {
                Log.e("IOException", "Error reading assets (module)", e)
                finish()
            }
        }

        // Creating the settings AlertDialog and re-displaying it when the theme is changed
        dialog = createSettingsDialog()
        if (vm.getDialogState()) {
            dialog!!.show()
        }

        // Creating Fragments at first launch or retrieving them when changing theme
        if (savedInstanceState == null) {
            recognitionFragment = RecognitionFragment.newInstance()
            drawFragment = DrawFragment.newInstance()
            ocrFragment = OcrFragment.newInstance()
            supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .add(R.id.fragmentContainerView, recognitionFragment, "RecognitionFragment")
                .add(R.id.fragmentContainerView, drawFragment, "DrawFragment")
                .add(R.id.fragmentContainerView, ocrFragment, "OcrFragment")
                .hide(drawFragment)
                .hide(ocrFragment)
                .commit()
        } else {
            recognitionFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                "recognitionFragment"
            ) as RecognitionFragment
            drawFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                "drawFragment"
            ) as DrawFragment
            ocrFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                "ocrFragment"
            ) as OcrFragment
        }

        // Configuring the NavigationBarView to display the proper Fragment
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            val buttonId = item.itemId
            if (buttonId == R.id.recognition && !recognitionFragment.isVisible) {
                supportFragmentManager.beginTransaction()
                    .show(recognitionFragment)
                    .hide(drawFragment)
                    .hide(ocrFragment)
                    .commit()
                return@setOnItemSelectedListener true
            } else if (buttonId == R.id.draw && !drawFragment.isVisible) {
                supportFragmentManager.beginTransaction()
                    .show(drawFragment)
                    .hide(recognitionFragment)
                    .hide(ocrFragment)
                    .commit()
                return@setOnItemSelectedListener true
            } else if (buttonId == R.id.ocr && !ocrFragment.isVisible) {
                supportFragmentManager.beginTransaction()
                    .show(ocrFragment)
                    .hide(drawFragment)
                    .hide(recognitionFragment)
                    .commit()
                return@setOnItemSelectedListener true
            }
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Applying the chosen theme when (re)starting the app
        val sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val mode = sharedPreferences.getInt("mode", 1)
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Saving both Fragments
        supportFragmentManager.putFragment(outState, "recognitionFragment", recognitionFragment)
        supportFragmentManager.putFragment(outState, "drawFragment", drawFragment)
        supportFragmentManager.putFragment(outState, "ocrFragment", ocrFragment)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Saving the dialog state before the Activity is killed
        vm.setDialogState(dialog!!.isShowing)
        dialog!!.dismiss()
    }

    override fun digitRecognition(bitmap: Bitmap): String {
        // Preparing input tensor
        var inputTensor: Tensor? = null
        try {
            inputTensor = UtilsFunctions.bitmapToFloat32Tensor(bitmap)
        } catch (e: Exception) {
            Log.e("Exception", "Error bitmapToFloat32Tensor()", e)
            finish()
        }

        // Running the model
        val outputTensor: Tensor = module!!.forward(IValue.from(inputTensor)).toTensor()

        // Getting tensor content as Java array of floats
        val scores: FloatArray = outputTensor.dataAsFloatArray

        // Searching for the index with maximum score
        var maxScore = -Float.MAX_VALUE
        var maxScoreIdx = -1
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxScoreIdx = i
            }
        }
        return "Recognised Digit: $maxScoreIdx"
    }

    private fun setAppTheme(mode: Int, editor: SharedPreferences.Editor) {
        // Setting the default night mode and saving it in the SharedPreferences
        AppCompatDelegate.setDefaultNightMode(mode)
        editor.putInt("mode", mode)
    }

    private fun createSettingsDialog(): AlertDialog {
        val sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Building the settings AlertDialog
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Select app theme")
        val checkedTheme = sharedPreferences.getInt("checkedTheme", 0)
        builder.setSingleChoiceItems(
            items,
            checkedTheme
        ) { _: DialogInterface?, i: Int ->
            when (i) {
                0 -> setAppTheme(AppCompatDelegate.MODE_NIGHT_NO, editor)
                1 -> setAppTheme(AppCompatDelegate.MODE_NIGHT_YES, editor)
                2 -> setAppTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, editor)
                else -> {}
            }
            editor.putInt("checkedTheme", i)
            editor.apply()
        }
        builder.setPositiveButton(
            "Close"
        ) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
        return builder.create()
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }
}