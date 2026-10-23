package com.securefilemanager.app.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.setupDialogStuff
import com.securefilemanager.app.extensions.value

class PasswordPromptDialog(
    val activity: AppCompatActivity,
    val title: String?,
    val callback: (password: CharArray) -> Unit
) {
    private val view = View.inflate(activity, R.layout.dialog_password_prompt, null)

    init {
        AlertDialog.Builder(activity).apply {
            setPositiveButton(R.string.ok, null)
            setNegativeButton(R.string.cancel, null)
            create().apply {
                activity.setupDialogStuff(
                    view,
                    this,
                    R.string.password_prompt_title,
                    title ?: "",
                    false
                ) {
                    val password = view.password
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        dismiss()
                        callback(password.value.toCharArray())
                    }
                }
            }
        }
    }

}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.view.View.password: com.google.android.material.textfield.TextInputEditText get() = findViewById(com.securefilemanager.app.R.id.password)
