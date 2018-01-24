package com.hansoolabs.and.utils

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.provider.Settings
import android.support.annotation.StringRes
import android.text.InputFilter
import android.text.Spanned
import android.util.Base64
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.hansoolabs.and.BuildConfig
import com.hansoolabs.and.R
import java.io.ByteArrayOutputStream
import java.util.*

object UiUtil {

    @JvmStatic
    fun constant(name: String) = BuildConfig.APPLICATION_ID + "." + name

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
    private val activityProgressDialog = WeakHashMap<Activity, ProgressDialog>()

    @JvmStatic
    fun showProgressDialog(activity: Activity,
                           title: String,
                           message: String,
                           cancelable: Boolean): ProgressDialog {

        var progressDialog: ProgressDialog?
        progressDialog = activityProgressDialog[activity]
        if (progressDialog == null) {
            synchronized(activityProgressDialog) {
                progressDialog = activityProgressDialog[activity]
                if (progressDialog == null) {
                    progressDialog = ProgressDialog(
                            activity, R.style.AndTheme_Dialog_Progress)
                    activityProgressDialog.put(activity, progressDialog)
                }
            }
        }

        return progressDialog!!.apply {
            setTitle(title)
            setMessage(message)
            setCancelable(cancelable)
            if (!isShowing) {
                show()
            }
        }
    }

    @JvmStatic
    fun hideProgressDialog(activity: Activity): Boolean {
        synchronized(activityProgressDialog) {
            val progressDialog = activityProgressDialog.remove(activity)
            if (progressDialog != null && progressDialog.isShowing) {
                progressDialog.dismiss()
                return true
            }
            return false
        }
    }

    @JvmStatic
    fun toast(context: Context, @StringRes textResId: Int, time: Int): Toast {
        return toast(context, context.getString(textResId), time)
    }

    @JvmStatic
    fun toast(context: Context, text: String, time: Int): Toast {
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
                        var handled: Boolean
                        try {
                            handled = action.invoke()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            handled = true
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

    @JvmStatic
    fun <T : Any, K : Any> changeListAs(original: MutableList<T>,
                                        to: List<K>,
                                        comparatorForPosition: (obj1: Any, obj2: Any) -> Boolean,
                                        comparatorForUpdate: (obj1: Any, obj2: Any) -> Boolean,
                                        converter: (obj: K) -> T) {
        val common = LongestCommonSubSequence.find(original, to, { obj1: T, obj2: K ->
            comparatorForPosition.invoke(obj1, obj2)
        })

        var commonIdx = 0
        val commonSize = common.size
        var originalIdx = 0
        while (originalIdx < original.size) {
            if (commonIdx < commonSize && comparatorForPosition.invoke(
                    original[originalIdx],
                    common[commonIdx])) {
                originalIdx++
                commonIdx++
            } else {
                original.removeAt(originalIdx)
            }
        }

        originalIdx = 0
        var toIdx = 0
        val toSize = to.size
        var item: K
        while (toIdx < toSize) {
            item = to[toIdx]
            if (originalIdx >= original.size) {
                original.add(originalIdx, converter.invoke(item))
            } else if (!comparatorForPosition.invoke(original[originalIdx], item)) {
                original.add(originalIdx, converter.invoke(item))
            } else if (!comparatorForUpdate.invoke(original[originalIdx], item)) {
                original[originalIdx] = converter.invoke(item)
            }
            toIdx++
            originalIdx++
        }
    }


    class AlphaNumericInputFilter : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int,
                            dest: Spanned, dstart: Int, dend: Int): CharSequence? {

            // Only keep characters that are alphanumeric
            val builder = StringBuilder()
            for (i in start..end - 1) {
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

}
