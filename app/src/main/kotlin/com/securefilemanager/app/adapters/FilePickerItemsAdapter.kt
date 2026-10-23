package com.securefilemanager.app.adapters

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.formatSize
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.views.MyRecyclerView

class FilePickerItemsAdapter(
    activity: BaseAbstractActivity,
    fileDirItems: List<FileDirItem>,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : ItemAbstractAdapter(activity, fileDirItems, recyclerView, null, itemClick) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        createViewHolder(R.layout.filepicker_list_item, parent)

    override fun setupView(view: View, fileDirItem: FileDirItem) {
        view.apply {
            list_item_name.text = fileDirItem.name
            list_item_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            list_item_details.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            super.setFileIcon(list_item_icon, fileDirItem)

            if (fileDirItem.isDirectory) {
                list_item_details.text = getChildrenCnt(fileDirItem)
            } else {
                list_item_details.text = fileDirItem.size.formatSize()
            }
        }
    }

}

// Replaces the removed kotlin-android-extensions synthetic view accessors.
private val android.view.View.list_item_details: com.google.android.material.textview.MaterialTextView get() = findViewById(com.securefilemanager.app.R.id.list_item_details)
private val android.view.View.list_item_icon: android.widget.ImageView get() = findViewById(com.securefilemanager.app.R.id.list_item_icon)
private val android.view.View.list_item_name: com.google.android.material.textview.MaterialTextView get() = findViewById(com.securefilemanager.app.R.id.list_item_name)
