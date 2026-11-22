package com.mrfuzzihead.dynamicsurroundings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionCheckTest {

    @Test
    public void parsesAndFormatsSoftwareVersions() {
        assertEquals("1.2.3", new VersionCheck.SoftwareVersion("1.2.3").toString());
        assertEquals("1.2.3.4", new VersionCheck.SoftwareVersion("1.2.3.4").toString());
        assertEquals("1.2.3ALPHA", new VersionCheck.SoftwareVersion("1.2.3ALPHA").toString());
        assertEquals("1.2.3BETA", new VersionCheck.SoftwareVersion("1.2.3BETA").toString());
    }

    @Test
    public void prereleasesSortBeforeTheRelease() {
        VersionCheck.SoftwareVersion alpha = new VersionCheck.SoftwareVersion("1.2.3ALPHA");
        VersionCheck.SoftwareVersion beta = new VersionCheck.SoftwareVersion("1.2.3BETA");
        VersionCheck.SoftwareVersion release = new VersionCheck.SoftwareVersion("1.2.3");

        assertTrue(alpha.compareTo(beta) < 0);
        assertTrue(beta.compareTo(release) < 0);
        assertTrue(release.compareTo(alpha) > 0);
    }

    @Test
    public void malformedVersionsAreRejected() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("1.2");
        assertInvalid("not-a-version");
    }

    private static void assertInvalid(String value) {
        try {
            new VersionCheck.SoftwareVersion(value);
        } catch (IllegalArgumentException expected) {
            return;
        }

        throw new AssertionError("expected IllegalArgumentException for " + value);
    }
}
