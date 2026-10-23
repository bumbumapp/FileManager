package com.securefilemanager.app.fragments.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.securefilemanager.app.R
import com.securefilemanager.app.helpers.htmlText

class IntroHidingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_intro_hiding, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        info_text?.text = htmlText(this.getString(R.string.intro_hiding_info))
    }

    companion object {
        fun newInstance(): IntroHidingFragment {
            return IntroHidingFragment()
        }
    }
}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.app.Activity.info_text: android.widget.TextView get() = findViewById(com.securefilemanager.app.R.id.info_text)
private val androidx.fragment.app.Fragment.info_text: android.widget.TextView get() = requireView().findViewById(com.securefilemanager.app.R.id.info_text)
private val android.app.Dialog.info_text: android.widget.TextView get() = findViewById(com.securefilemanager.app.R.id.info_text)
