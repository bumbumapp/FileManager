package com.securefilemanager.app.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.config
import com.securefilemanager.app.extensions.setupDialogStuff
import com.securefilemanager.app.helpers.*

class ChangeSortingDialog(
    val activity: BaseAbstractActivity,
    val path: String = "",
    val callback: () -> Unit
) {
    private var currSorting = 0
    private var config = activity.config
    private var view = View.inflate(activity, R.layout.dialog_change_sorting, null)

    init {
        view.sorting_dialog_use_for_this_folder.isChecked = config.hasCustomSorting(path)

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.sort_by)
            }

        currSorting = config.getFolderSorting(path)
        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        val sortBtn = when {
            currSorting and SORT_BY_SIZE != 0 -> sortingRadio.sorting_dialog_radio_size
            currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingRadio.sorting_dialog_radio_last_modified
            currSorting and SORT_BY_EXTENSION != 0 -> sortingRadio.sorting_dialog_radio_extension
            else -> sortingRadio.sorting_dialog_radio_name
        }
        sortBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.sorting_dialog_radio_order
        var orderBtn = orderRadio.sorting_dialog_radio_ascending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = orderRadio.sorting_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_name -> SORT_BY_NAME
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            R.id.sorting_dialog_radio_last_modified -> SORT_BY_DATE_MODIFIED
            else -> SORT_BY_EXTENSION
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (view.sorting_dialog_use_for_this_folder.isChecked) {
            config.saveCustomSorting(path, sorting)
        } else {
            config.removeCustomSorting(path)
            config.sorting = sorting
        }
        callback()
    }
}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.view.View.sorting_dialog_radio_ascending: com.google.android.material.radiobutton.MaterialRadioButton get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_ascending)
private val android.view.View.sorting_dialog_radio_descending: com.google.android.material.radiobutton.MaterialRadioButton get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_descending)
private val android.view.View.sorting_dialog_radio_extension: com.google.android.material.radiobutton.MaterialRadioButton get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_extension)
private val android.view.View.sorting_dialog_radio_last_modified: com.google.android.material.radiobutton.MaterialRadioButton get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_last_modified)
private val android.view.View.sorting_dialog_radio_name: com.google.android.material.radiobutton.MaterialRadioButton get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_name)
private val android.view.View.sorting_dialog_radio_order: android.widget.RadioGroup get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_order)
private val android.view.View.sorting_dialog_radio_size: com.google.android.material.radiobutton.MaterialRadioButton get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_size)
private val android.view.View.sorting_dialog_radio_sorting: android.widget.RadioGroup get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_radio_sorting)
private val android.view.View.sorting_dialog_use_for_this_folder: com.google.android.material.checkbox.MaterialCheckBox get() = findViewById(com.securefilemanager.app.R.id.sorting_dialog_use_for_this_folder)
