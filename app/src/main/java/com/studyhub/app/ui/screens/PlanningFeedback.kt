package com.studyhub.app.ui.screens

import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R

fun planningFeedback(view: View, success: Boolean, successText: Int) {
    if (ViewCompat.isAttachedToWindow(view)) {
        Snackbar.make(view, if (success) successText else R.string.planning_save_failed, Snackbar.LENGTH_LONG).show()
    }
}
