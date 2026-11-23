package com.mrfuzzihead.dynamicsurroundings.gametest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Test-only mod: a separate, non-shipped {@code @Mod} that asserts Dynamic Surroundings works against the real game.
 *
 * <p>
 * It lives in the {@code gameTest} source set and is only put on the classpath by {@code ./gradlew gameTestServer}
 * (or {@code -PgameTest}), so it never reaches the published jar. Run it with:
 * </p>
 *
 * <pre>
 * mkdir -p run/server &amp;&amp; echo "eula=true" &gt; run/server/eula.txt
 * ./gradlew gameTestServer
 * </pre>
 *
 * <p>
 * The assertions run on the server thread at {@link FMLServerStartedEvent}, where the overworld exists and block,
 * weather and world-time mutations are legal without a tick-loop hand-off. Everything that needs the real
 * registries, a live {@code World}, the mixins or the saved-data store is asserted here; pure logic is
 * covered by the JUnit suite in {@code src/test/java}.
 * </p>
 */
@Mod(
    modid = GameTestMod.MOD_ID,
    name = "Dynamic Surroundings Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:dynamicsurroundings")
public class GameTestMod {

    public static final String MOD_ID = "dynamicsurroundingsgametest";
    static final String OK_MARKER = "DS_GAMETEST_OK";
    static final String FAIL_MARKER = "DS_GAMETEST_FAILED";

    private boolean ran;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FMLCommonHandler.instance()
            .getSide() == Side.CLIENT && Boolean.getBoolean("dynamicsurroundings.clientSmoke")) {
            ClientSmokeTest.install();
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (ran || Boolean.getBoolean("dynamicsurroundings.clientSmoke")) return;
        ran = true;

        int failures;
        int checks;
        try {
            failures = ServerGameTests.runAll();
            checks = ServerGameTests.checkCount();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println(FAIL_MARKER + ": " + t);
            exit(1);
            return;
        }

        System.out.println("[DS-GAMETEST] " + (checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) {
            System.out.println(FAIL_MARKER + ": " + failures + " assertion(s) failed, see above");
            exit(1);
        } else {
            System.out.println(OK_MARKER);
            exit(0);
        }
    }

    private static void exit(int status) {
        FMLCommonHandler.instance()
            .exitJava(status, false);
    }
}
