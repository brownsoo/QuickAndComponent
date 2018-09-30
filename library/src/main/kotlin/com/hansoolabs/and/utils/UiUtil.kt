package com.hansoolabs.and.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
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

@Suppress("unused")
object UiUtil {

    @JvmStatic
    fun dp2px(dpSize: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpSize, metrics))
    }

    @JvmStatic
    fun sp2px(spSize: Float): Float {
        val metrics = Resources.getSystem().displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spSize, metrics)
    }

    @JvmStatic
    fun getDisplaySize(context: Context): Point {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)
        return point
    }

    @JvmStatic
    fun <T : Dialog> showDialog(dialog: T): T {
        if (!dialog.isShowing) {
            dialog.show()
        }
        return dialog
    }
    
    @JvmStatic
    fun toast(context: Context, @StringRes textResId: Int, time: Int = Toast.LENGTH_SHORT) =
        toast(context, context.getString(textResId), time)

    @SuppressLint("InflateParams")
    @JvmStatic
    fun toast(context: Context, text: String, time: Int = Toast.LENGTH_SHORT): Toast {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.and__toast, null, false)
        (layout.findViewById<View>(R.id.text) as TextView).text = text

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
                Arrays.asList(*editText.filters))
        curInputFilters.add(0, AlphaNumericInputFilter())
        val newInputFilters = curInputFilters.toTypedArray()
        editText.filters = newInputFilters
        return editText
    }


    class AlphaNumericInputFilter : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int,
                            dest: Spanned, dstart: Int, dend: Int): CharSequence? {

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
    fun encodeToBase64(image: Bitmap,
                       compressFormat: Bitmap.CompressFormat,
                       quality: Int): String {
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
            context.grantUriPermission(packageName, uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @JvmStatic
    fun revokeUriPermission(context: Context, uri: Uri) {
        context.revokeUriPermission(uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    @JvmStatic
    fun hideKeyboard(fragment: Fragment) {
        if (fragment.activity != null) {
            hideKeyboard(fragment.activity)
        }
    }
    
    @JvmStatic
    fun hideKeyboard(fragmentV4: androidx.fragment.app.Fragment) {
        fragmentV4.activity?.let {
            hideKeyboard(it)
        }
    }
    
    @JvmStatic
    fun hideKeyboard(activity: Activity) {
        val v = activity.window.currentFocus
        if (v != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }
    
    @JvmStatic
    fun isAvailable(activity: Activity?): Boolean {
        val valid = activity != null && !activity.isFinishing
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            valid && activity?.isDestroyed == false
        } else {
            valid
        }
    }
    
    @JvmStatic
    fun getScreenMetrics(activity: Activity): DisplayMetrics {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        return metrics
    }
    
    @JvmStatic
    fun getScreenInches(activity: Activity): Double {
        val dm = getScreenMetrics(activity)
        val x = Math.pow((dm.widthPixels / dm.xdpi).toDouble(), 2.0)
        val y = Math.pow((dm.heightPixels / dm.ydpi).toDouble(), 2.0)
        return Math.sqrt(x + y)
    }
}
