package im.fdx.v2ex.utils.extensions

import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import im.fdx.v2ex.R
import org.jetbrains.anko.forEachChild

/**
 * Created by fdx on 2017/6/14.
 * fdx will maintain it
 */


/**
 * 在中间位置显示"没有内容"信息
 */
fun FrameLayout.showNoContent(boolean: Boolean = true) {
    if (!boolean) {
        this.forEachChild { view ->
            when {
                view.tag == "no" -> removeView(view)
            }
        }
    } else {
        var b = false
        this.forEachChild { view ->
            when {
                view.tag == "no" -> b = true
            }
        }
        if (!b) {
            val child = TextView(this.context)
            child.tag = "no"
            child.text = "没有内容"
            child.setTextColor(ContextCompat.getColor(context, R.color.hint))
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.CENTER_HORIZONTAL
            params.topMargin = 120.dp2px()
            this.addView(child, -1, params)
        }

    }
}


/**
 * 为每个Activity设置基本toolbar，简化代码
 */
fun AppCompatActivity.setUpToolbar(title: String? = "") {

    val toolbar: Toolbar = findViewById(R.id.toolbar)
    toolbar.title = title
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { onBackPressed() }
}