package com.securefilemanager.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/**
 * Regressions for targetSdk 34+: foreground services must declare a type (else
 * MissingForegroundServiceTypeException) and PendingIntents need a mutability flag
 * (else IllegalArgumentException on Android 12+).
 */
public class PlatformConfigTest {
    private static String read(String rel) throws Exception {
        File f = new File(rel);
        if (!f.exists()) f = new File("app", rel);
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void foregroundServicesDeclareTypes() throws Exception {
        String m = read("src/main/AndroidManifest.xml");
        assertTrue(m.contains("android:foregroundServiceType=\"dataSync\""));
        assertTrue(m.contains("android:foregroundServiceType=\"specialUse\""));
        assertTrue(m.contains("FOREGROUND_SERVICE_DATA_SYNC"));
        assertTrue(m.contains("FOREGROUND_SERVICE_SPECIAL_USE"));
    }

    @Test
    public void pendingIntentsAreImmutable() throws Exception {
        for (String svc : new String[]{"ZipManagerService.kt", "UnlockAppService.kt"}) {
            String src = read("src/main/kotlin/com/securefilemanager/app/services/" + svc);
            assertTrue(svc, src.contains("PendingIntent.FLAG_IMMUTABLE"));
            assertFalse(svc, src.matches("(?s).*PendingIntent\\.getService\\([^)]*,\\s*0\\s*\\).*"));
        }
    }
}
