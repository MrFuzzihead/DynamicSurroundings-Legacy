package com.mrfuzzihead.dynamicsurroundings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IChatComponent.Serializer;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public final class VersionCheck implements Runnable {

    private static final int VERSION_CHECK_RETRIES = 3;
    private static final int VERSION_CHECK_INTERVAL = 10000;
    public static final VersionCheck.SoftwareVersion modVersion = parseModVersion(Module.VERSION);
    public static VersionCheck.SoftwareVersion currentVersion = new VersionCheck.SoftwareVersion();
    public static VersionCheck.UpdateStatus status = VersionCheck.UpdateStatus.UNKNOWN;
    private static final String mcVersion = resolveMinecraftVersion();
    private static final String RELEASES_URL = "https://github.com/MrFuzzihead/DynamicSurroundings-Legacy/releases/latest";

    private VersionCheck() {}

    static SoftwareVersion parseModVersion(String version) {
        try {
            return new SoftwareVersion(version);
        } catch (IllegalArgumentException e) {
            // Source builds without a reachable git tag have a commit hash as their version. Keep the mod usable;
            // the online check will simply treat that build as older than any published release.
            return new SoftwareVersion("0.0.0");
        }
    }

    private static String resolveMinecraftVersion() {
        try {
            return Loader.instance()
                .getMinecraftModContainer()
                .getVersion();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    public static void register() {
        if (ModOptions.enableVersionChecking) {
            VersionCheck test = new VersionCheck();
            FMLCommonHandler.instance()
                .bus()
                .register(test);
            Thread thread = new Thread(test, "dynamicsurroundings-version-check");
            thread.setDaemon(true);
            thread.start();
        }
    }

    @SubscribeEvent
    public void playerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayer && status == VersionCheck.UpdateStatus.OUTDATED) {
            String msg = StatCollector.translateToLocalFormatted(
                "msg.NewVersionAvailable.dynamicsurroundings",
                new Object[] { "Dynamic Surroundings", currentVersion, RELEASES_URL });
            IChatComponent component = Serializer.func_150699_a(msg);
            event.player.addChatMessage(component);
        }
    }

    static SoftwareVersion parseRemoteVersion(String body, String minecraftVersion) {
        if (body == null || body.trim()
            .isEmpty()) {
            return null;
        }

        String trimmed = body.trim();
        if (trimmed.charAt(0) == '{') {
            JsonObject json = new JsonParser().parse(trimmed)
                .getAsJsonObject();
            if (!json.has("tag_name")) {
                return null;
            }

            return new SoftwareVersion(
                json.get("tag_name")
                    .getAsString());
        }

        for (String line : trimmed.split("\r?\n")) {
            String[] tokens = line.split(":");
            if (tokens.length >= 2 && minecraftVersion.matches(tokens[0])) {
                return new SoftwareVersion(tokens[1]);
            }
        }

        return null;
    }

    private static void versionCheck() {
        HttpURLConnection conn = null;
        try {
            String location = ModOptions.versionCheckUrl;

            for (; location != null && !location.isEmpty(); location = conn.getHeaderField("Coordinates")) {
                URL url = new URL(location);
                if (conn != null) {
                    conn.disconnect();
                }

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(VERSION_CHECK_INTERVAL);
                conn.setReadTimeout(VERSION_CHECK_INTERVAL);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; ru; rv:1.9.0.11) Gecko/2009060215 Firefox/3.0.11 (.NET CLR 3.5.30729)");
                conn.connect();
            }

            if (conn == null) {
                throw new IllegalStateException("No version-check URL");
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line)
                        .append('\n');
                }
            }

            SoftwareVersion remote = parseRemoteVersion(body.toString(), mcVersion);
            if (remote == null) {
                throw new IllegalStateException("No matching version in remote response");
            }

            currentVersion = remote;
            status = VersionCheck.UpdateStatus.CURRENT;
            if (modVersion.compareTo(currentVersion) < 0) {
                status = VersionCheck.UpdateStatus.OUTDATED;
            }
        } catch (Exception e) {
            ModLog.warn("Unable to read remote version data");
            ModLog.catching(e);
            status = VersionCheck.UpdateStatus.COMM_ERROR;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public void run() {
        int count = 0;
        ModLog.info("Checking for newer mod version");

        try {
            do {
                if (count > 0) {
                    ModLog.info("Awaiting attempt %d", count);
                    Thread.sleep(VERSION_CHECK_INTERVAL);
                }

                versionCheck();
            } while (++count < VERSION_CHECK_RETRIES && status == VersionCheck.UpdateStatus.COMM_ERROR);
        } catch (InterruptedException var3) {
            Thread.currentThread()
                .interrupt();
            ModLog.warn("Version check interrupted");
        }

        switch (status) {
            case COMM_ERROR:
                ModLog.warn("Version check failed");
                break;
            case CURRENT:
                ModLog.info(
                    "Dynamic Surroundings version [%s] is the same or newer than the current version [%s]",
                    modVersion,
                    currentVersion);
                break;
            case OUTDATED:
                ModLog.warn(
                    "Using outdated version [" + modVersion
                        + "] for Minecraft "
                        + mcVersion
                        + ". Consider updating to "
                        + currentVersion
                        + ".");
                break;
            case UNKNOWN:
                ModLog.warn("Unknown version check status!");
        }
    }

    public static class SoftwareVersion implements Comparable<VersionCheck.SoftwareVersion> {

        public final int major;
        public final int minor;
        public final int revision;
        public final int patch;
        public final boolean isAlpha;
        public final boolean isBeta;

        public SoftwareVersion() {
            this.major = 0;
            this.minor = 0;
            this.revision = 0;
            this.patch = 0;
            this.isAlpha = false;
            this.isBeta = false;
        }

        public SoftwareVersion(String versionString) {
            if (versionString == null || versionString.isEmpty()) {
                throw new IllegalArgumentException("Version string is required");
            }

            if (versionString.charAt(0) == '@') {
                this.major = 0;
                this.minor = 0;
                this.revision = 0;
                this.patch = 0;
                this.isAlpha = false;
                this.isBeta = false;
                return;
            }

            this.isAlpha = StringUtils.containsIgnoreCase(versionString, "ALPHA");
            if (this.isAlpha) {
                versionString = StringUtils.remove(versionString, "ALPHA");
            }

            this.isBeta = StringUtils.containsIgnoreCase(versionString, "BETA");
            if (this.isBeta) {
                versionString = StringUtils.remove(versionString, "BETA");
            }

            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+))?")
                .matcher(versionString);
            if (!matcher.find()) {
                throw new IllegalArgumentException("Expected major.minor.revision: " + versionString);
            }

            this.major = Integer.parseInt(matcher.group(1));
            this.minor = Integer.parseInt(matcher.group(2));
            this.revision = Integer.parseInt(matcher.group(3));
            this.patch = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.major)
                .append('.')
                .append(this.minor)
                .append('.')
                .append(this.revision);
            if (this.patch != 0) {
                builder.append('.')
                    .append(this.patch);
            }

            if (this.isAlpha) {
                builder.append("ALPHA");
            }

            if (this.isBeta) {
                builder.append("BETA");
            }

            return builder.toString();
        }

        public int compareTo(VersionCheck.SoftwareVersion obj) {
            if (this.major != obj.major) {
                return this.major - obj.major;
            } else if (this.minor != obj.minor) {
                return this.minor - obj.minor;
            } else if (this.revision != obj.revision) {
                return this.revision - obj.revision;
            } else if (this.patch != obj.patch) {
                return this.patch - obj.patch;
            } else if (this.isAlpha != obj.isAlpha) {
                return this.isAlpha ? -1 : 1;
            } else if (this.isBeta != obj.isBeta) {
                return this.isBeta ? -1 : 1;
            } else {
                return 0;
            }
        }
    }

    public static enum UpdateStatus {
        UNKNOWN,
        CURRENT,
        OUTDATED,
        COMM_ERROR;
    }
}
