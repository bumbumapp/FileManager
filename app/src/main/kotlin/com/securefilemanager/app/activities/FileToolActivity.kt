package com.securefilemanager.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.deleteFiles
import com.securefilemanager.app.extensions.formatSize
import com.securefilemanager.app.extensions.tryOpenPathIntent
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.tools.CategoryTotal
import com.securefilemanager.app.tools.DuplicateGroup
import com.securefilemanager.app.tools.FileCategory
import com.securefilemanager.app.tools.FileTools
import com.securefilemanager.app.tools.ScanEntry
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.concurrent.thread

/** Storage analyzer, duplicate finder and recent files, one screen with three modes. */
class FileToolActivity : BaseAbstractActivity() {

    companion object {
        const val MODE_STORAGE = "storage"
        const val MODE_DUPLICATES = "duplicates"
        const val MODE_RECENT = "recent"
        private const val EXTRA_MODE = "mode"

        fun start(context: Context, mode: String) {
            context.startActivity(Intent(context, FileToolActivity::class.java).putExtra(EXTRA_MODE, mode))
        }
    }

    private sealed class Row {
        data class Header(val title: String, val subtitle: String, val fraction: Float?) : Row()
        data class FileRow(val entry: ScanEntry) : Row()
    }

    private lateinit var mode: String
    private lateinit var adapter: ToolAdapter
    private lateinit var summary: TextView
    private lateinit var empty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var actions: LinearLayout

    @Volatile
    private var cancelled = false
    private var all: List<ScanEntry> = emptyList()
    private var groups: List<DuplicateGroup> = emptyList()
    private val selected = HashSet<String>()
    private var recentDays = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_tool)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_STORAGE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(
            when (mode) {
                MODE_DUPLICATES -> R.string.tool_duplicates
                MODE_RECENT -> R.string.tool_recent
                else -> R.string.tool_storage
            }
        )
        summary = findViewById(R.id.tool_summary)
        empty = findViewById(R.id.tool_empty)
        progress = findViewById(R.id.tool_progress)
        actions = findViewById(R.id.tool_actions)
        adapter = ToolAdapter()
        findViewById<RecyclerView>(R.id.tool_list).apply {
            layoutManager = LinearLayoutManager(this@FileToolActivity)
            adapter = this@FileToolActivity.adapter
        }
        buildActions()
        startScan()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        cancelled = true
        super.onDestroy()
    }

    private fun root(): File = Environment.getExternalStorageDirectory()

    private fun buildActions() {
        actions.removeAllViews()
        when (mode) {
            MODE_RECENT -> {
                for (days in intArrayOf(1, 7, 30)) {
                    actions.addView(actionButton(getString(R.string.tool_days, days)) {
                        recentDays = days
                        showRecent()
                    })
                }
                actions.visibility = View.VISIBLE
            }
            MODE_DUPLICATES -> {
                actions.addView(actionButton(getString(R.string.tool_select_extras)) {
                    selected.clear()
                    selected.addAll(FileTools.extras(groups))
                    adapter.notifyDataSetChanged()
                    updateDuplicateSummary()
                })
                actions.addView(actionButton(getString(R.string.tool_delete_selected)) { confirmDeleteSelected() })
                actions.visibility = View.GONE
            }
            else -> actions.visibility = View.GONE
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button =
        Button(this, null, android.R.attr.buttonBarButtonStyle).apply {
            this.text = text
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }
        }

    private fun startScan() {
        progress.visibility = View.VISIBLE
        summary.text = getString(R.string.tool_scanning, 0)
        thread(name = "file-tool-scan") {
            val base = root()
            val skip = listOf("${base.absolutePath}/Android/data", "${base.absolutePath}/Android/obb")
            val found = FileTools.scan(base, skip, cancelled = { cancelled }) { n ->
                runOnUiThread { if (!cancelled) summary.text = getString(R.string.tool_scanning, n) }
            }
            if (cancelled) return@thread
            all = found
            if (mode == MODE_DUPLICATES) {
                val candidates = FileTools.sizeCandidates(found).flatten()
                var done = 0
                val result = FileTools.findDuplicates(found) {
                    if (cancelled) return@findDuplicates null
                    done++
                    if (done % 20 == 0) {
                        val d = done
                        runOnUiThread { if (!cancelled) summary.text = getString(R.string.tool_checking, d, candidates.size) }
                    }
                    FileTools.sha1(File(it.path))
                }
                groups = result
            }
            if (cancelled) return@thread
            runOnUiThread {
                if (cancelled) return@runOnUiThread
                progress.visibility = View.GONE
                when (mode) {
                    MODE_DUPLICATES -> showDuplicates()
                    MODE_RECENT -> showRecent()
                    else -> showStorage()
                }
            }
        }
    }

    private fun showStorage() {
        val rows = ArrayList<Row>()
        val stat = try {
            StatFs(root().absolutePath)
        } catch (e: Exception) {
            null
        }
        if (stat != null) {
            val total = stat.totalBytes
            val free = stat.availableBytes
            summary.text = getString(R.string.tool_storage_summary, (total - free).formatSize(), total.formatSize(), free.formatSize())
        } else {
            summary.text = getString(R.string.tool_storage)
        }
        val totals: List<CategoryTotal> = FileTools.summarize(all)
        val scanned = all.sumOf { it.size }.coerceAtLeast(1)
        for (t in totals) {
            rows.add(
                Row.Header(
                    categoryName(t.category),
                    getString(R.string.tool_category_line, t.count, t.size.formatSize()),
                    t.size.toFloat() / scanned
                )
            )
        }
        val big = FileTools.largest(all, 30)
        if (big.isNotEmpty()) {
            rows.add(Row.Header(getString(R.string.tool_largest_files), getString(R.string.tool_tap_open), null))
            big.forEach { rows.add(Row.FileRow(it)) }
        }
        setRows(rows, getString(R.string.tool_nothing_found))
    }

    private fun showRecent() {
        val list = FileTools.recent(all, System.currentTimeMillis(), recentDays)
        summary.text = getString(R.string.tool_recent_summary, list.size, recentDays)
        setRows(list.take(300).map { Row.FileRow(it) }, getString(R.string.tool_nothing_found))
    }

    private fun showDuplicates() {
        selected.clear()
        val rows = ArrayList<Row>()
        for (g in groups) {
            rows.add(Row.Header(getString(R.string.tool_group_title, g.files.size, g.size.formatSize()), "", null))
            g.files.forEach { rows.add(Row.FileRow(it)) }
        }
        actions.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        setRows(rows, getString(R.string.tool_no_duplicates))
        updateDuplicateSummary()
    }

    private fun updateDuplicateSummary() {
        if (groups.isEmpty()) {
            summary.text = getString(R.string.tool_no_duplicates)
            return
        }
        val selSize = groups.sumOf { g -> g.files.filter { it.path in selected }.sumOf { it.size } }
        summary.text = getString(
            R.string.tool_duplicates_summary, groups.size, FileTools.wasted(groups).formatSize(),
            selected.size, selSize.formatSize()
        )
    }

    private fun setRows(rows: List<Row>, emptyText: String) {
        adapter.rows = rows
        adapter.notifyDataSetChanged()
        empty.text = emptyText
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun categoryName(c: FileCategory): String = getString(
        when (c) {
            FileCategory.IMAGES -> R.string.tool_cat_images
            FileCategory.VIDEOS -> R.string.tool_cat_videos
            FileCategory.AUDIO -> R.string.tool_cat_audio
            FileCategory.DOCUMENTS -> R.string.tool_cat_documents
            FileCategory.ARCHIVES -> R.string.tool_cat_archives
            FileCategory.APPS -> R.string.tool_cat_apps
            FileCategory.OTHER -> R.string.tool_cat_other
        }
    )

    private fun confirmDeleteSelected() {
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.tool_nothing_selected, Toast.LENGTH_SHORT).show()
            return
        }
        if (FileTools.leavesNoCopy(groups, selected)) {
            Toast.makeText(this, R.string.tool_keep_one, Toast.LENGTH_LONG).show()
            return
        }
        val paths = selected.toList()
        val size = groups.sumOf { g -> g.files.filter { it.path in selected }.sumOf { it.size } }
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.tool_delete_confirm, paths.size, size.formatSize()))
            .setPositiveButton(R.string.tool_delete) { _, _ -> deletePaths(paths) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteOne(entry: ScanEntry) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.tool_delete_one_confirm, entry.name))
            .setPositiveButton(R.string.tool_delete) { _, _ -> deletePaths(listOf(entry.path)) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deletePaths(paths: List<String>) {
        val items = ArrayList<FileDirItem>()
        for (p in paths) {
            val f = File(p)
            items.add(FileDirItem(p, f.name, false, 0, f.length(), f.lastModified()))
        }
        deleteFiles(items) {
            // Re-check the disk: some deletes can fail, the list must show what is really left.
            val remaining = HashSet<String>()
            for (p in paths) if (File(p).exists()) remaining.add(p)
            val gone = paths.filter { it !in remaining }
            all = all.filter { it.path !in gone }
            selected.removeAll(gone.toSet())
            groups = groups.map { g -> DuplicateGroup(g.size, g.files.filter { it.path !in gone }) }.filter { it.files.size > 1 }
            when (mode) {
                MODE_DUPLICATES -> showDuplicates()
                MODE_RECENT -> showRecent()
                else -> showStorage()
            }
            Toast.makeText(this, getString(R.string.tool_deleted, gone.size), Toast.LENGTH_SHORT).show()
        }
    }

    private inner class ToolAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var rows: List<Row> = emptyList()

        override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) 0 else 1
        override fun getItemCount() = rows.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                object : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_tool_header, parent, false)) {}
            } else {
                object : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_tool_file, parent, false)) {}
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val v = holder.itemView
            when (val row = rows[position]) {
                is Row.Header -> {
                    v.findViewById<TextView>(R.id.header_title).text = row.title
                    val sub = v.findViewById<TextView>(R.id.header_subtitle)
                    sub.text = row.subtitle
                    sub.visibility = if (row.subtitle.isEmpty()) View.GONE else View.VISIBLE
                    val bar = v.findViewById<ProgressBar>(R.id.header_bar)
                    bar.visibility = if (row.fraction == null) View.GONE else View.VISIBLE
                    if (row.fraction != null) bar.progress = (row.fraction * 1000).toInt()
                }
                is Row.FileRow -> {
                    val e = row.entry
                    v.findViewById<TextView>(R.id.file_name).text = e.name
                    v.findViewById<TextView>(R.id.file_path).text =
                        e.parent.removePrefix(root().absolutePath).ifEmpty { "/" } + "  " +
                            DateFormat.getDateInstance(DateFormat.SHORT).format(Date(e.modified))
                    v.findViewById<TextView>(R.id.file_size).text = e.size.formatSize()
                    val check = v.findViewById<CheckBox>(R.id.file_check)
                    if (mode == MODE_DUPLICATES) {
                        check.visibility = View.VISIBLE
                        check.setOnCheckedChangeListener(null)
                        check.isChecked = e.path in selected
                        check.setOnCheckedChangeListener { _, on ->
                            if (on) selected.add(e.path) else selected.remove(e.path)
                            updateDuplicateSummary()
                        }
                    } else {
                        check.visibility = View.GONE
                    }
                    v.setOnClickListener { tryOpenPathIntent(e.path, false) }
                    v.setOnLongClickListener {
                        confirmDeleteOne(e)
                        true
                    }
                }
            }
        }
    }
}
