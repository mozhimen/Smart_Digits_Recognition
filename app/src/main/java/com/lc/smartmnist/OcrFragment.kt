package com.lc.smartmnist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import com.lc.smartmnist.utils.ImageUtils
import com.youdao.ocr.online.*
import com.youdao.sdk.app.EncryptHelper
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @ClassName OcrFragment
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:37
 * @Version 1.0
 */
class OcrFragment : Fragment() {
    private var mContext: Context? = null
    private var outputDirectory: File? = null
    private var tps: OCRParameters? = null
    private lateinit var imageCapture: ImageCapture
    private lateinit var container: View
    private lateinit var textView: TextView
    private lateinit var cameraCaptureButton: Button
    private lateinit var cameraExecutor: ExecutorService

    private var handler = Handler()

    companion object {
        const val TAG = "OcrFragment"
        fun newInstance(): OcrFragment {
            return OcrFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ocr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view
        mContext = requireContext().applicationContext

        initOcrParameter()

        // 设置拍照按钮监听
        cameraCaptureButton = view.findViewById(R.id.camera_capture_button)
        textView = view.findViewById(R.id.text_view_ocr)
        cameraCaptureButton.setOnClickListener { takePhoto() }

        // 设置照片等保存的位置
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun initOcrParameter() {
        //OCR识别
        tps = OCRParameters.Builder()
            .source("CloudAMQP")
            .timeout(100000)
            .lanType(RecognizeLanguage.AUTO.code)
            .build()
        //默认按行识别，支持自动、中英繁、日韩、拉丁、印地语，其中自动识别不支持印地语识别，其他都可以
        // 当采用按字识别时，识别语言支持中英和英文识别，其中"zh-en"为中英识别，"en"参数表示只识别英文。若为纯英文识别，"zh-en"的识别效果不如"en"，请妥善选择
    }

    /**
     * 核心
     * @param filePath
     */
    private fun startRecognize(filePath: String) {
        val bitmap: Bitmap = ImageUtils.readBitmapFromFile(filePath, 768)

        //压缩位图
        var byteArrayOutputStream = ByteArrayOutputStream()
        var quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        var datas = byteArrayOutputStream.toByteArray()

        //?
        var bases64: String = EncryptHelper.getBase64(datas)
        val count = bases64.length
        while (count > 1.4 * 1024 * 1024) {
            quality -= 10
            byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            datas = byteArrayOutputStream.toByteArray()
            bases64 = EncryptHelper.getBase64(datas)
        }
        val base64 = bases64

        //提示
        handler.post {
            Log.d(TAG, "开始识别...")
            Toast.makeText(mContext, "开始识别...", Toast.LENGTH_SHORT).show()
        }

        //OCR识别
        ImageOCRecognizer.getInstance(tps).recognize(base64, object : OCRListener {
            override fun onResult(result: OCRResult, input: String?) {
                //识别成功
                handler.post {
                    val text = getResult(result)
                    Log.d(TAG, "TEXT = $text")
                    textView.text = text
                }
            }

            override fun onError(error: OcrErrorCode?) {
                //识别失败
            }
        })
    }

    private fun getResult(result: OCRResult): String {
        val sb = StringBuilder()
        var i = 1
        //按文本识别
        val regions: List<Region> = result.regions
        for (region in regions) {
            val lines: List<Line> = region.lines
            for (line in lines) {
                sb.append("文本" + i++ + "： ")
                val words: List<Word> = line.words
                for (word in words) {
                    sb.append(word.text).append(" ")
                }
                sb.append("\n")
            }
        }
        var text = sb.toString()
        if (!TextUtils.isEmpty(text)) {
            text = text.substring(0, text.length - 2)
        }
        return text
    }

    private fun takePhoto() {
        // 确保imageCapture 已经被实例化, 否则程序将可能崩溃
        if (this::imageCapture.isInitialized) {
            // 创建带时间戳的输出文件以保存图片，带时间戳是为了保证文件名唯一
            val photoFile = File(outputDirectory, SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis()) + ".png")

            // 创建 output option 对象，用以指定照片的输出方式
            val outputFileOptions: ImageCapture.OutputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // 执行takePicture（拍照）方法
            imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    // 保存照片时的回调
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        startRecognize(photoFile.toString())
                        val msg = "照片捕获成功! $savedUri"
                        // Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: " + exception.message)
                    }
                })
        }
    }


    private fun startCamera() {
        // 将Camera的生命周期和Activity绑定在一起（设定生命周期所有者），这样就不用手动控制相机的启动和关闭。
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener({
            try {
                // 将你的相机和当前生命周期的所有者绑定所需的对象
                val processCameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // 创建一个Preview 实例，并设置该实例的 surface 提供者（provider）。
                val viewFinder: PreviewView =
                    container.findViewById<View>(R.id.viewFinder) as PreviewView
                val preview: Preview = Preview.Builder().build()
                preview.setSurfaceProvider(viewFinder.surfaceProvider)

                // 选择后置摄像头作为默认摄像头
                val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // 创建拍照所需的实例
                imageCapture = ImageCapture.Builder().build()

                // 设置预览帧分析
                val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder().build()
                imageAnalysis.setAnalyzer(cameraExecutor, MyAnalyzer())

                // 重新绑定用例前先解绑
                processCameraProvider.unbindAll()

                // 绑定用例至相机
                processCameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "用例绑定失败！$e")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun getOutputDirectory(): File? {
        val mediaDir = File(requireContext().externalMediaDirs[0], getString(R.string.app_name))
        val isExist = mediaDir.exists() || mediaDir.mkdir()
        return if (isExist) mediaDir else null
    }

    private class MyAnalyzer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(image: ImageProxy) {
            //Log.d(TAG, "Image's stamp is " + Objects.requireNonNull(image.getImage()).getTimestamp());
            image.close()
        }
    }
}