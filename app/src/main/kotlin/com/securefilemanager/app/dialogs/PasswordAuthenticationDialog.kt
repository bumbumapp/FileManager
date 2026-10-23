package com.securefilemanager.app.dialogs

import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.crypto.Password
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PasswordAuthenticationDialog(
    val activity: AppCompatActivity,
    val callbackPositive: (success: Boolean) -> Unit,
    private val callbackNegative: ((success: Boolean) -> Unit)? = null
) : View.OnKeyListener {
    private val view = View.inflate(activity, R.layout.dialog_password_authentication, null)
    private lateinit var passwordLayout: TextInputLayout

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        passwordLayout.error = null
        return false
    }

    init {
        val passwordAuthenticationDialog: PasswordAuthenticationDialog = this

        AlertDialog.Builder(activity).apply {
            setPositiveButton(R.string.ok, null)
            if (callbackNegative != null) {
                setNegativeButton(R.string.cancel, null)
            }
            create().apply {
                activity.setupDialogStuff(view, this, R.string.prompt_info_title, "", false) {
                    passwordLayout = view.password_layout
                    val password: EditText = view.password

                    passwordLayout.setOnKeyListener(passwordAuthenticationDialog)
                    showKeyboard(password)

                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (password.value.isEmpty()) {
                            invalidPasswordAction(passwordLayout)
                        } else {
                            progressbar.visibility = View.VISIBLE
                            val alertDialog: AlertDialog = this

                            GlobalScope.launch {
                                val passwordMatch = Password().verify(
                                    hash = activity.config.passwordHash,
                                    password = password.value.toByteArray()
                                )

                                activity.runOnUiThread {
                                    progressbar.visibility = View.GONE
                                    if (passwordMatch) {
                                        positiveCallback(alertDialog)
                                    } else {
                                        invalidPasswordAction(passwordLayout)
                                    }
                                }
                            }
                        }
                    }

                    if (callbackNegative != null) {
                        getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                            negativeCallback(this)
                        }
                    }
                }
            }
        }
    }

    private fun positiveCallback(alertDialog: AlertDialog) {
        alertDialog.dismiss()
        callbackPositive(true)
    }

    private fun negativeCallback(alertDialog: AlertDialog) {
        alertDialog.dismiss()
        callbackNegative?.invoke(true)
    }

    private fun invalidPasswordAction(passwordLayout: TextInputLayout) {
        passwordLayout.setError(activity, R.string.invalid_password)
    }

}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.app.Activity.password: com.google.android.material.textfield.TextInputEditText get() = findViewById(com.securefilemanager.app.R.id.password)
private val androidx.fragment.app.Fragment.password: com.google.android.material.textfield.TextInputEditText get() = requireView().findViewById(com.securefilemanager.app.R.id.password)
private val android.app.Dialog.password: com.google.android.material.textfield.TextInputEditText get() = findViewById(com.securefilemanager.app.R.id.password)
private val android.app.Activity.progressbar: android.widget.ProgressBar get() = findViewById(com.securefilemanager.app.R.id.progressbar)
private val androidx.fragment.app.Fragment.progressbar: android.widget.ProgressBar get() = requireView().findViewById(com.securefilemanager.app.R.id.progressbar)
private val android.app.Dialog.progressbar: android.widget.ProgressBar get() = findViewById(com.securefilemanager.app.R.id.progressbar)
private val android.view.View.password: com.google.android.material.textfield.TextInputEditText get() = findViewById(com.securefilemanager.app.R.id.password)
private val android.view.View.password_layout: com.google.android.material.textfield.TextInputLayout get() = findViewById(com.securefilemanager.app.R.id.password_layout)
private val android.view.View.progressbar: android.widget.ProgressBar get() = findViewById(com.securefilemanager.app.R.id.progressbar)
