package com.securefilemanager.app.fragments.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.securefilemanager.app.R
import com.securefilemanager.app.fragments.settings.SettingsEncryptionFragment
import com.securefilemanager.app.helpers.htmlText

class IntroEncryptingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_encrypting, container, false)

        this.childFragmentManager
            .beginTransaction()
            .replace(R.id.settings_encryption, SettingsEncryptionFragment()).commit()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        info_text_1?.text = htmlText(this.getString(R.string.intro_encrypting_info_1))
        info_text_2?.text = htmlText(this.getString(R.string.intro_encrypting_info_2))
    }

    companion object {
        fun newInstance(): IntroEncryptingFragment {
            return IntroEncryptingFragment()
        }
    }
}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.app.Activity.info_text_1: android.widget.TextView get() = findViewById(com.securefilemanager.app.R.id.info_text_1)
private val androidx.fragment.app.Fragment.info_text_1: android.widget.TextView get() = requireView().findViewById(com.securefilemanager.app.R.id.info_text_1)
private val android.app.Dialog.info_text_1: android.widget.TextView get() = findViewById(com.securefilemanager.app.R.id.info_text_1)
private val android.app.Activity.info_text_2: android.widget.TextView get() = findViewById(com.securefilemanager.app.R.id.info_text_2)
private val androidx.fragment.app.Fragment.info_text_2: android.widget.TextView get() = requireView().findViewById(com.securefilemanager.app.R.id.info_text_2)
private val android.app.Dialog.info_text_2: android.widget.TextView get() = findViewById(com.securefilemanager.app.R.id.info_text_2)
