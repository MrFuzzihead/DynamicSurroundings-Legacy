package com.mrfuzzihead.dynamicsurroundings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

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
    public void aSourceBuildWithoutTagsFallsBackInsteadOfCrashing() {
        assertEquals(
            "0.0.0",
            VersionCheck.parseModVersion("026c435")
                .toString());
        assertEquals(
            "5.1.0",
            VersionCheck.parseModVersion("26.3-5.1.0-108-g6dafdc41")
                .toString());
    }

    @Test
    public void parsesGitDescribeAndGitHubTags() {
        assertEquals("5.2.0", new VersionCheck.SoftwareVersion("26.3-5.2.0-70-gc0d4f92f").toString());
        assertEquals(
            "5.2.0",
            VersionCheck.parseRemoteVersion("{\"tag_name\":\"26.3-5.2.0\"}", "1.7.10")
                .toString());
        assertEquals(
            "5.2.0",
            VersionCheck.parseRemoteVersion("1.7.10:5.2.0", "1.7.10")
                .toString());
        assertEquals(null, VersionCheck.parseRemoteVersion("{}", "1.7.10"));
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
    public void numericOrderingMatchesTheVersionFields() {
        Random random = new Random(0x5EED5EEDL);
        for (int i = 0; i < 200; i++) {
            int major = random.nextInt(5);
            int minor = random.nextInt(20);
            int revision = random.nextInt(50);
            VersionCheck.SoftwareVersion left = new VersionCheck.SoftwareVersion(major + "." + minor + "." + revision);
            VersionCheck.SoftwareVersion right = new VersionCheck.SoftwareVersion(
                major + "." + (minor + 1) + "." + revision);
            assertTrue(left.compareTo(right) < 0);
            assertTrue(right.compareTo(left) > 0);
            assertEquals(0, left.compareTo(new VersionCheck.SoftwareVersion(major + "." + minor + "." + revision)));
        }
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
