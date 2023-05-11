package com.lc.smartmnist

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.lc.smartmnist.databinding.FragmentOcrBinding
import com.lc.smartmnist.utils.ImageUtils
import com.mozhimen.basick.elemk.fragment.bases.BaseFragmentVBVM
import com.mozhimen.basick.imagek.loader.exts.loadImageCircleComplex
import com.mozhimen.basick.manifestk.annors.AManifestKRequire
import com.mozhimen.basick.manifestk.cons.CPermission
import com.mozhimen.basick.manifestk.cons.CUseFeature
import com.mozhimen.basick.utilk.graphics.bitmap.UtilKBitmapDeal
import com.mozhimen.basick.utilk.graphics.bitmap.UtilKBitmapIO
import com.mozhimen.basick.utilk.java.io.file.UtilKFile
import com.mozhimen.basick.utilk.os.UtilKPath
import com.mozhimen.componentk.cameraxk.annors.ACameraXKFacing
import com.mozhimen.componentk.cameraxk.annors.ACameraXKFormat
import com.mozhimen.componentk.cameraxk.commons.ICameraXKCaptureListener
import com.mozhimen.componentk.cameraxk.mos.CameraXKConfig
import com.youdao.ocr.online.ImageOCRecognizer
import com.youdao.ocr.online.Line
import com.youdao.ocr.online.OCRListener
import com.youdao.ocr.online.OCRParameters
import com.youdao.ocr.online.OCRResult
import com.youdao.ocr.online.OcrErrorCode
import com.youdao.ocr.online.RecognizeLanguage
import com.youdao.ocr.online.Region
import com.youdao.ocr.online.Word
import com.youdao.sdk.app.EncryptHelper
import java.io.ByteArrayOutputStream

/**
 * @ClassName OcrFragment
 * @Description TODO
 * @Author kolin zhao & mozhimen
 * @Date 2023/5/10 23:37
 * @Version 1.0
 */
@AManifestKRequire(CPermission.CAMERA, CUseFeature.CAMERA, CUseFeature.CAMERA_AUTOFOCUS)
class OcrFragment : BaseFragmentVBVM<FragmentOcrBinding, SharedViewModel>() {
    private var tps: OCRParameters? = null
    private var handler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance(): OcrFragment {
            return OcrFragment()
        }
    }

    override fun bindViewVM(vb: FragmentOcrBinding) {

    }

    override fun initView(savedInstanceState: Bundle?) {
        initOcrParameter()
        initCamera()
    }

    private fun initCamera() {
        VB.cameraxkLayout.initCamera(this, CameraXKConfig(ACameraXKFormat.RGBA_8888, ACameraXKFacing.BACK))
        VB.cameraxkLayout.setCameraXKCaptureListener(_cameraXKCaptureListener)
        VB.cameraxkLayout.startCamera()
        VB.cameraCaptureButton.setOnClickListener {
            VB.cameraCaptureButton.isClickable = false
            VB.cameraxkLayout.takePicture()
        }
    }

    private val _cameraXKCaptureListener = object : ICameraXKCaptureListener {
        override fun onCaptureSuccess(bitmap: Bitmap, imageRotation: Int) {
            val bitmap = UtilKBitmapDeal.rotateBitmap(bitmap, imageRotation)
            VB.previewImg.loadImageCircleComplex(bitmap, R.color.transparent, R.color.transparent, crossFadeEnable = false)
            val file = UtilKBitmapIO.bitmap2JpegFile(bitmap, UtilKPath.Absolute.Internal.getCacheDir() + "/${UtilKFile.nowStr2FileName()}.jpg", quality = 50)
            file?.let {
                startRecognize(it.absolutePath)
            }
        }

        override fun onCaptureFail() {}
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

    private fun startRecognize(filePath: String) {
        val bitmap = ImageUtils.readBitmapFromFile(filePath, 768)

        //压缩位图
        var baos = ByteArrayOutputStream()
        var quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        var datas = baos.toByteArray()

        //?
        var bases64 = EncryptHelper.getBase64(datas)
        val count = bases64.length
        while (count > 1.4 * 1024 * 1024) {
            quality -= 10
            baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            datas = baos.toByteArray()
            bases64 = EncryptHelper.getBase64(datas)
        }
        val base64 = bases64

        //提示
        handler.post {
            Log.d(TAG, "开始识别...")
            Toast.makeText(requireContext(), "开始识别...", Toast.LENGTH_SHORT).show()
        }

        //OCR识别
        ImageOCRecognizer.getInstance(tps).recognize(base64, object : OCRListener {
            override fun onResult(result: OCRResult, input: String) {
                //识别成功
                handler.post {
                    val text = getResult(result)
                    Log.d(TAG, "TEXT = $text")
                    VB.textViewOcr.text = text
                }
                VB.cameraCaptureButton.isClickable = true
            }

            override fun onError(error: OcrErrorCode) {
                //识别失败
                Log.e(TAG, "onError: $error")
                VB.cameraCaptureButton.isClickable = true
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
}