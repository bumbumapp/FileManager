package com.securefilemanager.app.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.setupDialogStuff

class WritePermissionDialog(activity: Activity, val callback: () -> Unit) {
    var dialog: AlertDialog

    init {
        val layout = R.layout.dialog_write_permission
        val view = activity.layoutInflater.inflate(layout, null)

        val glide = Glide.with(activity)
        val crossFade = DrawableTransitionOptions.withCrossFade()
        glide.load(R.drawable.img_write_storage).transition(crossFade).into(view.write_permissions_dialog_image)
        glide.load(R.drawable.img_write_storage_sd).transition(crossFade).into(view.write_permissions_dialog_image_sd)

        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setOnCancelListener {
                BaseAbstractActivity.funAfterSAFPermission?.invoke(false)
                BaseAbstractActivity.funAfterSAFPermission = null
            }
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.confirm_storage_access_title)
            }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }
}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.view.View.write_permissions_dialog_image: android.widget.ImageView get() = findViewById(com.securefilemanager.app.R.id.write_permissions_dialog_image)
private val android.view.View.write_permissions_dialog_image_sd: android.widget.ImageView get() = findViewById(com.securefilemanager.app.R.id.write_permissions_dialog_image_sd)
