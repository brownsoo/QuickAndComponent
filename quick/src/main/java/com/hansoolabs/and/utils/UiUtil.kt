package com.hansoolabs.and.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.Settings
import androidx.annotation.StringRes
import android.text.InputFilter
import android.text.Spanned
import android.util.Base64
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.hansoolabs.and.R
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.roundToInt

@Suppress("unused")
object UiUtil {

    @JvmStatic
    fun dp2px(dpSize: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpSize, metrics).roundToInt()
    }

    @JvmStatic
    fun sp2px(spSize: Float): Float {
        val metrics = Resources.getSystem().displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spSize, metrics)
    }

    @JvmStatic
    fun getDisplaySize(context: Context): Point? {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rect = manager.currentWindowMetrics.bounds
            return Point(rect.width(), rect.height())
        } else {
            @Suppress("DEPRECATION")
            manager.defaultDisplay?.let { display ->
                val point = Point()
                display.getSize(point)
                return point
            }
        }
        return null
    }

    @JvmStatic
    fun <T : Dialog> showDialog(dialog: T): T {
        if (!dialog.isShowing) {
            dialog.show()
        }
        return dialog
    }

    @JvmStatic
    fun toastShort(context: Context?, @StringRes textResId: Int): Toast {
        if (context == null) return Toast.makeText(context, textResId, Toast.LENGTH_SHORT)
        return toast(context, context.getString(textResId), Toast.LENGTH_SHORT)
    }

    @JvmStatic
    fun toastLong(context: Context?, @StringRes textResId: Int): Toast {
        if (context == null) return Toast.makeText(context, textResId, Toast.LENGTH_LONG)
        return toast(context, context.getString(textResId), Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun toast(context: Context, @StringRes textResId: Int, time: Int = Toast.LENGTH_SHORT): Toast {
        return toast(context, context.getString(textResId), time)
    }

    @SuppressLint("InflateParams")
    @JvmStatic
    fun toast(context: Context, text: String, time: Int = Toast.LENGTH_SHORT): Toast {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.and__toast, null, false)
        (layout.findViewById<View>(R.id.toast_text) as? TextView)?.text = text

        return Toast(context.applicationContext).apply {
            setGravity(Gravity.BOTTOM, 0, dp2px(100f))
            duration = time
            view = layout
        }
    }

    @JvmStatic
    fun doOnGlobalLayout(view: View, action: (() -> Boolean)): Boolean {
        val viewTreeObserver = view.viewTreeObserver
        if (!viewTreeObserver.isAlive) {
            return false
        }
        viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val handled: Boolean
                    handled = try {
                        action.invoke()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        true
                    }

                    if (handled) {
                        val observer = view.viewTreeObserver
                        if (observer.isAlive) {
                            observer.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            })
        return true
    }

    @JvmStatic
    fun allowOnlyAlphaNumeric(editText: EditText): EditText {
        val curInputFilters = ArrayList(
            Arrays.asList(*editText.filters)
        )
        curInputFilters.add(0, AlphaNumericInputFilter())
        val newInputFilters = curInputFilters.toTypedArray()
        editText.filters = newInputFilters
        return editText
    }


    class AlphaNumericInputFilter : InputFilter {
        override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: Spanned, dstart: Int, dend: Int
        ): CharSequence? {

            // Only keep characters that are alphanumeric
            val builder = StringBuilder()
            for (i in start until end) {
                val c = source[i]
                if (Character.isLetterOrDigit(c)) {
                    builder.append(c)
                }
            }

            // If all characters are valid, return null, otherwise only return the filtered characters
            val allCharactersValid = builder.length == end - start
            return if (allCharactersValid) null else builder.toString()
        }
    }

    @JvmStatic
    fun startInstalledAppDetailSetting(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("package:" + context.packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        context.startActivity(intent)
    }

    @JvmStatic
    fun startGspSetting(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        context.startActivity(intent)
    }

    @JvmStatic
    fun encodeToBase64(
        image: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        quality: Int
    ): String {
        val byteArrayOS = ByteArrayOutputStream()
        image.compress(compressFormat, quality, byteArrayOS)
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT)
    }

    @JvmStatic
    fun decodeBase64(input: String): Bitmap {
        val decodedBytes = Base64.decode(input, 0)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    @JvmStatic
    fun grantUriPermission(context: Context, intent: Intent, uri: Uri) {
        val resInfoList = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName, uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    @JvmStatic
    fun revokeUriPermission(context: Context, uri: Uri) {
        context.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    @JvmStatic
    fun showKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    @JvmStatic
    fun hideKeyboard(context: Context?, view: View, hiddenComplete: ((Boolean) -> Unit)? = null) {
        if (context == null) {
            hiddenComplete?.invoke(false)
            return
        }
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val hidden =
            imm.hideSoftInputFromWindow(view.windowToken, 0, object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
//                If non-null, this will be called by the IME when
//                * it has processed your request to tell you what it has done.  The result
//                * code you receive may be either {@link #RESULT_UNCHANGED_SHOWN},
//                * {@link #RESULT_UNCHANGED_HIDDEN}, {@link #RESULT_SHOWN}, or
//                * {@link #RESULT_HIDDEN}.
                    when (resultCode) {
                        InputMethodManager.RESULT_UNCHANGED_SHOWN -> {
                            HLog.d("Quick", "hideSoftInputFromWindow", "RESULT_UNCHANGED_SHOWN")
                            hiddenComplete?.invoke(false)
                        }
                        InputMethodManager.RESULT_UNCHANGED_HIDDEN -> {
                            HLog.d("Quick", "hideSoftInputFromWindow", "RESULT_UNCHANGED_HIDDEN")
                            hiddenComplete?.invoke(true)
                        }
                        InputMethodManager.RESULT_SHOWN -> {
                            HLog.d("Quick", "hideSoftInputFromWindow", "RESULT_SHOWN")
                            hiddenComplete?.invoke(false)
                        }
                        InputMethodManager.RESULT_HIDDEN -> {
                            HLog.d("Quick", "hideSoftInputFromWindow", "RESULT_HIDDEN")
                            hiddenComplete?.invoke(true)
                        }
                        else -> {
                            hiddenComplete?.invoke(false)
                        }
                    }
                }
            })

        HLog.d("Quick", "hideSoftInputFromWindow", "flag 0 failed")
        if (!hidden) {
            val success =
                imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
            hiddenComplete?.invoke(success)
        } else {
            hiddenComplete?.invoke(true)
        }
    }

    @JvmStatic
    fun hideKeyboard(
        fragmentV4: androidx.fragment.app.Fragment,
        hiddenComplete: ((Boolean) -> Unit)? = null
    ) {
        fragmentV4.activity?.let {
            it.window?.decorView?.let { view ->
                hideKeyboard(it, view, hiddenComplete)
            } ?: kotlin.run {
                hideKeyboard(it, hiddenComplete)
            }
        }
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity, hiddenComplete: ((Boolean) -> Unit)? = null) {
        val v = activity.window.currentFocus
        if (v != null) {
            hideKeyboard(activity, v, hiddenComplete)
        } else {
            hideKeyboard(activity, View(activity), hiddenComplete)
        }
    }
}
