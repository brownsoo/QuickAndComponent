@file:Suppress("MemberVisibilityCanBePrivate")

package com.hansoolabs.and.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.hansoolabs.and.R
import com.hansoolabs.and.utils.dp2px

/**
 * Simple Item View
 * Created by brownsoo on 2017. 10. 13..
 */

class SettingItemView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {

    val iconView: AppCompatImageView
    val titleView: TextView
    val descriptionView: TextView
    val accessoryView: TextView
    val tailIconView: AppCompatImageView
    val accessoryContainer: LinearLayout
    val switchView: MaterialSwitch
    private val divider: View

    var title: CharSequence? = null
        set(value) {
            field = value
            titleView.text = value
        }
    var description: CharSequence? = null
        set(value) {
            field = value
            descriptionView.text = value
            descriptionView.visibility = if (value != null) View.VISIBLE else View.GONE
        }

    var accessory: CharSequence? = null
        set(value) {
            field = value
            accessoryView.text = value
            accessoryView.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var switchVisible: Boolean = false
        set(value) {
            field = value
            switchView.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

    var isChecked: Boolean = false
        set(value) {
            field = value
            switchView.isChecked = value
        }

    var titleColor: Int = Color.parseColor("#0d131b")
        set(value) {
            field = value
            titleView.setTextColor(value)
        }
    var descriptionColor: Int = Color.parseColor("#0d131b")
        set(value) {
            field = value
            descriptionView.setTextColor(value)
        }

    var dividerColor: Int =  Color.parseColor("#b3b8c5")
        set(value) {
            field = value
            divider.setBackgroundColor(value)
        }

    var accessoryColor: Int = Color.parseColor("#0d131b")
        set(value) {
            field = value
            accessoryView.setTextColor(value)
        }

    init {

        View.inflate(context, R.layout.and__setting_item_view, this)
        iconView = findViewById(R.id.icon)
        titleView = findViewById(R.id.title)
        descriptionView = findViewById(R.id.desc)
        accessoryView = findViewById(R.id.accessory_tv)
        accessoryContainer = findViewById(R.id.accessory_container)
        tailIconView = findViewById(R.id.tail_icon)
        switchView = findViewById(R.id.tail_switch)
        divider = findViewById(R.id.split_line)


        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SettingItemView, defStyle, 0)
        setIcon(a.getResourceId(R.styleable.SettingItemView_itemIcon, -1))
        setTailIcon(a.getResourceId(R.styleable.SettingItemView_itemTailIcon, -1))
        title = a.getString(R.styleable.SettingItemView_itemTitle)
        description = a.getString(R.styleable.SettingItemView_itemDescription)
        accessory = a.getString(R.styleable.SettingItemView_itemAccessory)
        switchVisible = a.getBoolean(R.styleable.SettingItemView_itemSwitch, false)
        isChecked = a.getBoolean(R.styleable.SettingItemView_itemChecked, false)
        a.getColor(R.styleable.SettingItemView_titleColor, -1).let {
            if (it >= 0) titleColor = it
        }
        a.getColor(R.styleable.SettingItemView_descriptionColor, -1).let {
            if (it >= 0) descriptionColor = it
        }
        a.getColor(R.styleable.SettingItemView_dividerColor, -1).let {
            if (it >= 0) dividerColor = it
        }
        a.getDimension(R.styleable.SettingItemView_dividerWidth, 1.dp2px().toFloat()).let {
            divider.layoutParams.height = it.toInt()
        }
        
        a.getColor(R.styleable.SettingItemView_accessoryColor, -1).let {
            if (it >= 0) {
                accessoryColor = it
            }
        }

        if (a.hasValue(R.styleable.SettingItemView_iconTint)) {
            val colorList = a.getColorStateList(R.styleable.SettingItemView_iconTint)
            setIconTint(colorList)
        }

        val itemWrap = findViewById<LinearLayout>(R.id.item_wrap)
        itemWrap.setPadding(
            a.getDimension(R.styleable.SettingItemView_itemPaddingStart, 16.dp2px().toFloat()).toInt(),
            a.getDimension(R.styleable.SettingItemView_itemPaddingTop, 8.dp2px().toFloat()).toInt(),
            a.getDimension(R.styleable.SettingItemView_itemPaddingEnd, 16.dp2px().toFloat()).toInt(),
            a.getDimension(R.styleable.SettingItemView_itemPaddingBottom, 8.dp2px().toFloat()).toInt()
        )

        a.getDimension(R.styleable.SettingItemView_dividerPadding, 16.dp2px().toFloat()).let { padding ->
            (divider.layoutParams as? MarginLayoutParams)?.let { params ->
                params.marginStart = padding.toInt()
            }
        }

        a.recycle()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        switchView.isEnabled = enabled
        accessoryContainer.isEnabled = enabled
    }

    fun setIconTint(colorList: ColorStateList?) {
        ImageViewCompat.setImageTintList(iconView, colorList)
        ImageViewCompat.setImageTintList(tailIconView, colorList)
    }

    fun setIconVisible(visible: Boolean) {
        iconView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setIcon(resId: Int) {
        setIconVisible(resId > 0)
        if (resId > 0) {
            iconView.setImageDrawable(ContextCompat.getDrawable(context, resId))
        }
    }

    fun setTailIconVisible(visible: Boolean) {
        tailIconView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setTailIcon(resId: Int) {
        setTailIconVisible(resId > 0)
        if (resId > 0) {
            tailIconView.setImageDrawable(ContextCompat.getDrawable(context, resId))
        }
    }

    fun addAccessoryView(view: View) {
        accessoryContainer.addView(view)
    }

    fun addAccessoryView(view: View, at: Int) {
        accessoryContainer.addView(view, at)
    }
}