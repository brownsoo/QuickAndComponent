package com.hansoolabs.and.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import com.hansoolabs.and.utils.ImageUtil
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.utils.isContentUri
import com.hansoolabs.and.utils.isFileUri
import java.io.ByteArrayOutputStream
import java.io.File

enum class PermType {
    ImageSelect, ImageCapture
}

abstract class ImageTakeFragment: Fragment() {
    
    abstract val FileProviderAuthorityName: String

    private val permImageSelectLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        onPermissionResult(PermType.ImageSelect, it)
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImageSelected(uri)
    }

    private val permImageCaptureLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        onPermissionResult(PermType.ImageCapture, allGranted)
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        onImageCaptured(it)
    }

    // https://developer.android.com/training/data-storage/shared/photopicker?hl=ko
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        onImageSelected(uri)
    }

    private var photoFile: File? = null

    private fun isPhotoPickerAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * 이미지 선택 플로우 시작
     */
    fun startImageSelection() {
        val context = this.context ?: return
        if (!hasReadImagePermission(context)) {
            requestReadImagePermission()
        } else {
            if (isPhotoPickerAvailable()) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                selectImageLauncher.launch("image/*")
            }
        }
    }

    /**
     * 카메라 촬영 플로우 시작
     */
    fun startImageCapture() {
        val context = this.context ?: return
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(context, "카메라 기능을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasCameraPermission(context) || !hasWritePermission(context)) {
            val list = ArrayList<String>()
            if (!hasCameraPermission(context)) {
                list.add(Manifest.permission.CAMERA)
            }
            if (!hasWritePermission(context)) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            permImageCaptureLauncher.launch(list.toTypedArray())
        } else {
            photoFile = File(context.externalCacheDir, System.currentTimeMillis().toString() + ".jpg")
            val fileUri = FileProvider.getUriForFile(
                context,
                FileProviderAuthorityName,
                photoFile!!
            )
            captureImageLauncher.launch(fileUri)
        }
    }

    private fun onImageSelected(uri: Uri?) {
        val context = this.context ?: return
        if (uri == null) {
            return
        }
        if (uri.isContentUri()) {
            context.contentResolver.openInputStream(uri)?.let { input ->
                val bitmap: Bitmap = BitmapFactory.decodeStream(input)
                val dest = File(context.externalCacheDir, System.currentTimeMillis().toString() + ".jpg")
                if (!dest.exists()) {
                    dest.createNewFile()
                }
                val outStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
                dest.outputStream().use { output ->
                    output.write(outStream.toByteArray())
                }
                onImageSelected(dest)
                return
            }
        } else if (uri.isFileUri()) {
            val file = uri.toFile()
            if (ImageUtil.checkIfGif(file)) {
                Toast.makeText(requireContext(), "GIF 이미지는 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val resolved =  ImageUtil.getCorrectOrientationBitmap(context, file.path)
            onImageSelected(resolved)
            return
        }
        Toast.makeText(context, "파일 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 이미지 선택 후 파일로 제공
     */
    open fun onImageSelected(file: File) {
    }

    private fun onImageCaptured(success: Boolean) {
        val context = this.context ?: return
        if (!success) return
        val path = photoFile?.path ?: return
        val resolved = ImageUtil.getCorrectOrientationBitmap(context, path)
        onImageCaptured(resolved)
    }

    /**
     * 이미지 촬영 후 파일로 제공
     */
    open fun onImageCaptured(file: File) {

    }

    private fun onPermissionResult(type: PermType, granted: Boolean) {
        val context = this.context ?: return
        if (granted) {
            when(type) {
                PermType.ImageSelect -> {
                    startImageSelection()
                }
                PermType.ImageCapture -> {
                    startImageCapture()
                }
            }
        } else {
            when(type) {
                PermType.ImageSelect -> {
                    if (!hasReadImagePermission(context)) {
                        val message = "이미지를 가져오기 위해 사진 권한이 필요합니다.\n\n설정에서 앱 권한을 확인할 수 있습니다."
                        AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton("앱 설정 열기") { _, _ ->
                                UiUtil.startInstalledAppDetailSetting(context)
                            }
                            .setNegativeButton("취소", null)
                            .create()
                            .show()
                        return
                    }
                }
                PermType.ImageCapture -> {
                    if (!hasCameraPermission(context)) {
                        val message = "촬영을 위해 카메라 권한이 필요합니다.\n\n설정에서 앱 권한을 확인할 수 있습니다."
                        AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton("앱 설정 열기") { _, _ ->
                                UiUtil.startInstalledAppDetailSetting(context)
                            }
                            .setNegativeButton("취소", null)
                            .create()
                            .show()
                        return
                    }
                    if (!hasWritePermission(context)) {
                        val message = "사진을 임시 저장하기 위해 저장 권한이 필요합니다.\n\n설정에서 앱 권한을 확인할 수 있습니다."
                        AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton("앱 설정 열기") { _, _ ->
                                UiUtil.startInstalledAppDetailSetting(context)
                            }
                            .setNegativeButton("취소", null)
                            .create()
                            .show()
                        return
                    }
                }
            }
        }
    }

    private fun hasReadImagePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return PackageManager.PERMISSION_DENIED != ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES)
        }
        return PackageManager.PERMISSION_DENIED != ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun requestReadImagePermission() {
        permImageSelectLauncher.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )
    }

    private fun hasWritePermission(context: Context): Boolean {
        // If this permission is not allowlisted for an app that targets an API level
        // before Build.VERSION_CODES.Q this permission cannot be granted to apps.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        return PackageManager.PERMISSION_DENIED != ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun hasCameraPermission(context: Context): Boolean {
        return PackageManager.PERMISSION_DENIED != ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA)
    }
}