package com.securefilemanager.app.tools

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/** One file found by the scanner. */
data class ScanEntry(val path: String, val size: Long, val modified: Long) {
    val name: String get() = path.substring(path.lastIndexOf('/') + 1)
    val parent: String get() = path.substringBeforeLast('/', "")
}

enum class FileCategory { IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APPS, OTHER }

data class CategoryTotal(val category: FileCategory, val count: Int, val size: Long)

data class DuplicateGroup(val size: Long, val files: List<ScanEntry>) {
    /** Space that would be freed by keeping one copy. */
    val wasted: Long get() = size * (files.size - 1)
}

/** Pure file statistics: no Android classes, unit tested. */
object FileTools {

    const val DAY_MS = 24L * 60 * 60 * 1000

    private val images = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tif", "tiff", "raw", "dng")
    private val videos = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv", "m4v", "ts")
    private val audio = setOf("mp3", "wav", "wma", "m4a", "aac", "flac", "ogg", "opus", "amr", "mid")
    private val documents = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv", "epub", "md", "html", "xml", "json")
    private val archives = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "jar")
    private val apps = setOf("apk", "xapk", "apks", "aab")

    fun categoryOf(name: String): FileCategory {
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.length - 1) return FileCategory.OTHER
        val ext = name.substring(dot + 1).lowercase(Locale.ROOT)
        return when (ext) {
            in images -> FileCategory.IMAGES
            in videos -> FileCategory.VIDEOS
            in audio -> FileCategory.AUDIO
            in documents -> FileCategory.DOCUMENTS
            in archives -> FileCategory.ARCHIVES
            in apps -> FileCategory.APPS
            else -> FileCategory.OTHER
        }
    }

    /** Totals per category, biggest first; categories without files are left out. */
    fun summarize(entries: List<ScanEntry>): List<CategoryTotal> {
        val counts = HashMap<FileCategory, Int>()
        val sizes = HashMap<FileCategory, Long>()
        for (e in entries) {
            val c = categoryOf(e.name)
            counts[c] = (counts[c] ?: 0) + 1
            sizes[c] = (sizes[c] ?: 0L) + e.size
        }
        return counts.keys
            .map { CategoryTotal(it, counts.getValue(it), sizes.getValue(it)) }
            .sortedByDescending { it.size }
    }

    fun largest(entries: List<ScanEntry>, limit: Int, minSize: Long = 0): List<ScanEntry> =
        entries.filter { it.size >= minSize }.sortedByDescending { it.size }.take(limit)

    fun recent(entries: List<ScanEntry>, nowMs: Long, days: Int): List<ScanEntry> {
        val from = nowMs - days * DAY_MS
        return entries.filter { it.modified >= from && it.modified <= nowMs + DAY_MS }
            .sortedByDescending { it.modified }
    }

    /** Files that share a size with at least one other file; empty files never count as duplicates. */
    fun sizeCandidates(entries: List<ScanEntry>): List<List<ScanEntry>> =
        entries.filter { it.size > 0 }
            .groupBy { it.size }
            .values
            .filter { it.size > 1 }

    /**
     * Groups really identical files. [hash] returns a content fingerprint, or null when a file cannot be
     * read (it is then ignored). Groups are sorted by wasted space.
     */
    fun findDuplicates(entries: List<ScanEntry>, hash: (ScanEntry) -> String?): List<DuplicateGroup> {
        val groups = ArrayList<DuplicateGroup>()
        for (candidates in sizeCandidates(entries)) {
            val byHash = HashMap<String, MutableList<ScanEntry>>()
            for (e in candidates) {
                val h = hash(e) ?: continue
                byHash.getOrPut(h) { ArrayList() }.add(e)
            }
            for (same in byHash.values) {
                if (same.size > 1) {
                    groups.add(DuplicateGroup(same[0].size, same.sortedBy { keepOrder(it) }))
                }
            }
        }
        return groups.sortedByDescending { it.wasted }
    }

    /** Lower is kept first: oldest file, then shortest path. */
    private fun keepOrder(e: ScanEntry): Long = e.modified * 1000 + minOf(e.path.length, 999)

    /** In each group keep the first (the original) and select the others. */
    fun extras(groups: List<DuplicateGroup>): Set<String> =
        groups.flatMap { it.files.drop(1) }.map { it.path }.toSet()

    fun wasted(groups: List<DuplicateGroup>): Long = groups.sumOf { it.wasted }

    /** Files that would be deleted but leave a group with no copy at all: guards against selecting every copy. */
    fun leavesNoCopy(groups: List<DuplicateGroup>, selected: Set<String>): Boolean =
        groups.any { g -> g.files.all { it.path in selected } }

    /** SHA-1 of the file: first 64 KB quick check then the full content. Null if unreadable. */
    fun sha1(file: File): String? = try {
        val md = MessageDigest.getInstance("SHA-1")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }

    /**
     * Walks [root] and lists files. Hidden folders and the given skip paths are not entered; stops after
     * [maxFiles]. [cancelled] is polled so the screen can stop a running scan.
     */
    fun scan(
        root: File,
        skipPrefixes: List<String> = emptyList(),
        maxFiles: Int = 300_000,
        cancelled: () -> Boolean = { false },
        progress: (Int) -> Unit = {}
    ): List<ScanEntry> {
        val out = ArrayList<ScanEntry>()
        val stack = ArrayDeque<File>()
        stack.addLast(root)
        while (stack.isNotEmpty() && out.size < maxFiles && !cancelled()) {
            val dir = stack.removeLast()
            val children = dir.listFiles() ?: continue
            for (f in children) {
                if (cancelled()) break
                if (f.name.startsWith(".")) continue
                val abs = f.absolutePath
                if (skipPrefixes.any { abs == it || abs.startsWith("$it/") }) continue
                if (f.isDirectory) {
                    stack.addLast(f)
                } else if (f.isFile) {
                    out.add(ScanEntry(abs, f.length(), f.lastModified()))
                    if (out.size >= maxFiles) break
                }
            }
            progress(out.size)
        }
        return out
    }
}
