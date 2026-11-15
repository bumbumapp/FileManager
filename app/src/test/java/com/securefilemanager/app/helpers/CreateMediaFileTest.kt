package com.securefilemanager.app.helpers

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CreateMediaFileTest {
    private fun tmp(): File = kotlin.io.path.createTempDirectory("fm").toFile()

    @Test
    fun usesRequestedFolderWhenWritable() {
        val dir = tmp()
        val f = createMediaFileOrFallback(dir.path, ".jpg", null)
        assertNotNull(f)
        assertEquals(dir.path, f!!.parent)
    }

    @Test
    fun fallsBackInsteadOfThrowingWhenFolderIsNotWritable() {
        val fallback = tmp()
        val f = createMediaFileOrFallback("/nonexistent-dir/does/not/exist", ".mp4", fallback)
        assertNotNull(f)
        assertEquals(fallback.path, f!!.parent)
    }

    @Test
    fun returnsNullWhenNothingIsWritable() {
        assertNull(createMediaFileOrFallback("/nonexistent-dir/a", ".jpg", File("/nonexistent-dir/b")))
        assertNull(createMediaFileOrFallback("/nonexistent-dir/a", ".jpg", null))
    }
}
