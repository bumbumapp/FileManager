package com.securefilemanager.app.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileToolsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun e(path: String, size: Long, modified: Long = 0) = ScanEntry(path, size, modified)

    @Test
    fun categorizesByExtension() {
        assertEquals(FileCategory.IMAGES, FileTools.categoryOf("A.JPG"))
        assertEquals(FileCategory.VIDEOS, FileTools.categoryOf("clip.mp4"))
        assertEquals(FileCategory.AUDIO, FileTools.categoryOf("a.flac"))
        assertEquals(FileCategory.DOCUMENTS, FileTools.categoryOf("r.pdf"))
        assertEquals(FileCategory.ARCHIVES, FileTools.categoryOf("x.zip"))
        assertEquals(FileCategory.APPS, FileTools.categoryOf("x.apk"))
        assertEquals(FileCategory.OTHER, FileTools.categoryOf("noext"))
        assertEquals(FileCategory.OTHER, FileTools.categoryOf("trailing."))
        assertEquals(FileCategory.OTHER, FileTools.categoryOf("weird.xyz"))
    }

    @Test
    fun summarizesAndSorts() {
        val list = listOf(e("/a/1.jpg", 10), e("/a/2.png", 20), e("/a/3.mp4", 100), e("/a/4.bin", 5))
        val s = FileTools.summarize(list)
        assertEquals(FileCategory.VIDEOS, s[0].category)
        assertEquals(100, s[0].size)
        assertEquals(FileCategory.IMAGES, s[1].category)
        assertEquals(2, s[1].count)
        assertEquals(30, s[1].size)
        assertEquals(3, s.size)
    }

    @Test
    fun picksLargestAndRecent() {
        val now = 100 * FileTools.DAY_MS
        val list = listOf(
            e("/a", 5, now - 1 * FileTools.DAY_MS),
            e("/b", 50, now - 10 * FileTools.DAY_MS),
            e("/c", 20, now - 2 * FileTools.DAY_MS)
        )
        assertEquals(listOf("/b", "/c"), FileTools.largest(list, 2).map { it.path })
        assertEquals(listOf("/b"), FileTools.largest(list, 5, minSize = 30).map { it.path })
        assertEquals(listOf("/a", "/c"), FileTools.recent(list, now, 7).map { it.path })
        assertEquals(listOf("/a"), FileTools.recent(list, now, 1).map { it.path })
    }

    @Test
    fun duplicateDetectionUsesContent() {
        val list = listOf(
            e("/x/a.txt", 10, 1), e("/x/b.txt", 10, 2), e("/x/c.txt", 10, 3),
            e("/x/empty1", 0), e("/x/empty2", 0), e("/x/solo", 99)
        )
        val hashes = mapOf("/x/a.txt" to "H1", "/x/b.txt" to "H1", "/x/c.txt" to "H2")
        val groups = FileTools.findDuplicates(list) { hashes[it.path] }
        assertEquals(1, groups.size)
        assertEquals(listOf("/x/a.txt", "/x/b.txt"), groups[0].files.map { it.path })
        assertEquals(10, groups[0].wasted)
        assertEquals(setOf("/x/b.txt"), FileTools.extras(groups))
        assertEquals(10, FileTools.wasted(groups))
    }

    @Test
    fun unreadableFilesAreIgnored() {
        val list = listOf(e("/a", 5), e("/b", 5))
        assertTrue(FileTools.findDuplicates(list) { null }.isEmpty())
    }

    @Test
    fun keepsOldestCopy() {
        val list = listOf(e("/new/long/path/x", 5, 50), e("/old/x", 5, 10))
        val g = FileTools.findDuplicates(list) { "same" }
        assertEquals("/old/x", g[0].files[0].path)
    }

    @Test
    fun guardsAgainstDeletingEveryCopy() {
        val g = listOf(DuplicateGroup(5, listOf(e("/a", 5), e("/b", 5))))
        assertFalse(FileTools.leavesNoCopy(g, setOf("/b")))
        assertTrue(FileTools.leavesNoCopy(g, setOf("/a", "/b")))
    }

    @Test
    fun scansRealFolderAndFindsRealDuplicates() {
        val root = tmp.newFolder("root")
        val sub = java.io.File(root, "sub").apply { mkdirs() }
        val hidden = java.io.File(root, ".hidden").apply { mkdirs() }
        val skipped = java.io.File(root, "skip").apply { mkdirs() }
        java.io.File(root, "one.txt").writeText("hello world")
        java.io.File(sub, "two.txt").writeText("hello world")
        java.io.File(sub, "three.txt").writeText("hello there!")
        java.io.File(hidden, "secret.txt").writeText("hello world")
        java.io.File(skipped, "s.txt").writeText("hello world")
        val entries = FileTools.scan(root, listOf(skipped.absolutePath))
        assertEquals(3, entries.size)
        val groups = FileTools.findDuplicates(entries) { FileTools.sha1(java.io.File(it.path)) }
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].files.size)
        assertNull(FileTools.sha1(java.io.File(root, "missing")))
        assertNotNull(FileTools.sha1(java.io.File(root, "one.txt")))
    }

    @Test
    fun scanStopsWhenCancelledOrCapped() {
        val root = tmp.newFolder("r2")
        repeat(5) { java.io.File(root, "f$it.txt").writeText("x$it") }
        assertEquals(2, FileTools.scan(root, maxFiles = 2).size)
        assertEquals(0, FileTools.scan(root, cancelled = { true }).size)
    }
}
