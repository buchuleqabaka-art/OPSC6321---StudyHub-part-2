package com.studyhub.app.engagement

import android.content.res.ColorStateList
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.studyhub.app.R

fun LinearLayout.label(value: String, size: Float = 16f) {
    addView(TextView(context).apply { text = value; textSize = size; setTextColor(context.getColor(R.color.ink)); setPadding(0, 12, 0, 12) })
}
fun LinearLayout.action(value: String, enabled: Boolean = true, onClick: () -> Unit) {
    addView(MaterialButton(context).apply { text = value; isEnabled = enabled; setOnClickListener { onClick() } })
}
fun LinearLayout.bar(value: Int, total: Int, description: String, color: Int) {
    label(description)
    addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = total.coerceAtLeast(1); progress = value; progressTintList = ColorStateList.valueOf(color)
        contentDescription = description
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (20 * resources.displayMetrics.density).toInt())
    })
}
