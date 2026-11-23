// Test, game-test and JaCoCo harness, applied from addon.gradle.kts.

import java.math.BigDecimal

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Attaches the JaCoCo agent to a forked game.
 *
 * <p>
 * The `Test` tasks get the agent from the jacoco plugin; a `JavaExec` run task needs it as an explicit
 * `-javaagent` argument. The agent jar is declared as a `@Classpath` input so Gradle resolves it with the task (in
 * configuration-cache terms this is a task input, not a configuration-time resolution).
 * </p>
 */
abstract class JacocoAgentArgumentProvider : CommandLineArgumentProvider {

    @get:Classpath
    abstract val agent: ConfigurableFileCollection

    @get:Input
    abstract val destFile: Property<String>

    @get:Input
    abstract val includes: Property<String>

    override fun asArguments(): Iterable<String> {
        val jar = extractAgent()
        return listOf(
            "-javaagent:" + jar.absolutePath +
                "=destfile=" + destFile.get() +
                ",append=false,dumponexit=true" +
                ",includes=" + includes.get()
        )
    }

    /**
     * The `jacocoAgent` configuration resolves to the JaCoCo *distribution* jar, which has no `Premain-Class` of its
     * own: the real agent is `jacocoagent.jar` inside it. Gradle's own JaCoCo support unpacks it the same way.
     */
    private fun extractAgent(): java.io.File {
        val distribution = agent.files.single()
        val target = java.io.File(destFile.get()).parentFile.resolve("jacocoagent.jar")
        java.util.zip.ZipFile(distribution).use { zip ->
            val entry = zip.getEntry("jacocoagent.jar") ?: return distribution
            target.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
        return target
    }
}

/** Fails a game-test run when the JaCoCo execution data produced an empty report. */
abstract class VerifyGameTestReport : DefaultTask() {

    @get:InputFile
    @get:Optional
    abstract val executionData: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val reportXml: RegularFileProperty

    @TaskAction
    fun verify() {
        if (!executionData.get().asFile.exists()) {
            return
        }
        val xml = reportXml.get().asFile
        if (!xml.exists() || !xml.readText().contains("<counter")) {
            throw GradleException("gameTest.exec exists but jacocoGameTestReport.xml has no counters")
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// Unit tests + coverage
// ---------------------------------------------------------------------------------------------------------

apply(plugin = "jacoco")

project.extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = "0.8.16"
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = true
    }
    // The mod targets Java 8 bytecode; run the suite on the same JVM generation the mod supports. Mockito 4's
    // inline mock maker also behaves best there.
    javaLauncher.set(
        project.extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    )
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Tests need the compileOnly dependencies too: production classes implement/import compileOnly types (CalendarAPI),
// so a test cannot even name those classes without them on its classpath.
val mainSourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
configurations.named("testCompileClasspath") {
    extendsFrom(configurations.named("compileOnly").get())
}
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.named("compileOnly").get())
}

// Only the unit test run feeds the report. A gameTestServer run also writes jacoco/gameTest.exec (see below), but in
// a Forge 1.7.10 *dev* launch the LaunchClassLoader round-trips every class through ASM, so the class ids in that
// file cannot match the class files on disk. Merging it would only produce "Classes in bundle do not match with
// execution data" warnings and no data.
val jacocoExecFiles = project.files(project.layout.buildDirectory.file("jacoco/test.exec"))

fun jacocoClassDirs() = project.files(mainSourceSet.output.classesDirs).asFileTree.matching {
    // Exclude only the classes that need a GL context, an audio device, or a running client. Pure client-side
    // logic (fog math, weather state, aurora geometry, sound values, footstep maps) stays in the denominator.
    exclude(
        "com/mrfuzzihead/dynamicsurroundings/Tags.class",
        "com/mrfuzzihead/dynamicsurroundings/client/**/*Renderer*",
        "com/mrfuzzihead/dynamicsurroundings/client/**/*Handler*",
        "com/mrfuzzihead/dynamicsurroundings/client/gui/**",
        "com/mrfuzzihead/dynamicsurroundings/client/hud/**",
        "com/mrfuzzihead/dynamicsurroundings/client/waila/**",
        "com/mrfuzzihead/dynamicsurroundings/client/fx/particle/**",
        "com/mrfuzzihead/dynamicsurroundings/client/sound/cache/**",
        "com/mrfuzzihead/dynamicsurroundings/client/sound/SoundManager*",
        "com/mrfuzzihead/dynamicsurroundings/client/fog/BiomeFogRangeCalculator*",
        "com/mrfuzzihead/dynamicsurroundings/client/fog/BiomeFogColorCalculator*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/Footsteps*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/game/system/PFResourcePackDealer*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/game/system/PFIsolator*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/game/system/Association*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/game/system/UserConfigSoundPlayerWrapper*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/game/user/**",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/AcousticsLibrary*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/BasicAcoustic*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/ConfigOptions*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/DelayedAcoustic*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/EventSelectorAcoustics*",
        "com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/SimultaneousAcoustic*"
    )
}

tasks.named<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates HTML and XML JaCoCo coverage reports from the unit test run"
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(jacocoExecFiles)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails the build when unit test coverage drops below the configured floor"
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(jacocoExecFiles)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                // Global regression guard. The denominator now includes the testable client logic; only GL/audio
                // bound renderers, handlers, the config GUI and the biome grid scans are excluded above.
                minimum = BigDecimal("0.50")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.35")
            }
        }

        // Per-package floors stop a well-covered package from hiding a regression in another one.
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.util.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.85")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.70")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.data.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.50")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.28")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.weather.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.70")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.55")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.aurora.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.90")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.80")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.sound.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.65")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.45")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.fog.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.65")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.48")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.20")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.15")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.25")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.15")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.footsteps.parsers.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.45")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.40")
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("com.mrfuzzihead.dynamicsurroundings.client.footsteps.util.property.simple.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.90")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.90")
            }
        }
    }
}

// `check` runs the tests and enforces the floor, so CI catches coverage regressions.
tasks.named("check").configure {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

// ---------------------------------------------------------------------------------------------------------
// In-game test mod (never shipped)
// ---------------------------------------------------------------------------------------------------------

val gameTestSourceSet = project.extensions.getByType(SourceSetContainer::class.java).create("gameTest")

configurations.getByName(gameTestSourceSet.compileClasspathConfigurationName)
    .extendsFrom(configurations.getByName("compileClasspath"))
configurations.getByName(gameTestSourceSet.runtimeClasspathConfigurationName)
    .extendsFrom(configurations.getByName("runtimeClasspath"))
gameTestSourceSet.compileClasspath += mainSourceSet.output
gameTestSourceSet.compileClasspath += mainSourceSet.compileClasspath
gameTestSourceSet.runtimeClasspath += mainSourceSet.output
gameTestSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
// The game-test mod is plain Java 8. Leaving Jabel's compiler plugin on this source set's classpath makes javac
// auto-load it through ServiceLoader, which then needs ByteBuddy's agent (not present here).
gameTestSourceSet.compileClasspath = gameTestSourceSet.compileClasspath
    .filter { !it.name.contains("jabel") }

val isGameTestRun = project.hasProperty("gameTest") ||
    gradle.startParameter.taskNames.any { it.contains("gameTestServer") }
val isClientSmokeRun = gradle.startParameter.taskNames.any { it.contains("clientSmokeTest") }

if (isGameTestRun || isClientSmokeRun) {
    // Only our package is instrumented, and the file is dumped when the game calls System.exit (the game test's
    // exitJava with halt=false), so a crashed or hung run simply leaves no data behind.
    // The data is NOT merged into jacocoTestReport: a dev launch rewrites every class through ASM, so the class ids
    // cannot match the files on disk. It is produced so that a run without the dev transformers - a production
    // launcher - can be merged by pointing executionData at it.
    val jacocoProvider = objects.newInstance(JacocoAgentArgumentProvider::class.java)
    jacocoProvider.agent.from(configurations.named("jacocoAgent"))
    jacocoProvider.destFile.set(
        layout.buildDirectory.file("jacoco/gameTest.exec")
            .map { it.asFile.absolutePath }
    )
    jacocoProvider.includes.set("com.mrfuzzihead.dynamicsurroundings.*")

    tasks.withType<JavaExec>().configureEach {
        if (name.startsWith("runServer") || name.startsWith("runClient")) {
            classpath += gameTestSourceSet.output
            systemProperty("dynamicsurroundings.gameTest", "true")
            if (isClientSmokeRun) {
                systemProperty("dynamicsurroundings.clientSmoke", "true")
                providers.gradleProperty("expectedNearFogPercent")
                    .orNull
                    ?.let { systemProperty("dynamicsurroundings.expectedNearFogPercent", it) }
            }
            // A reused dev world may reference blocks from mods that are no longer installed; the game test is not
            // about that, so answer FML's "continue anyway?" prompt up front instead of blocking on stdin.
            systemProperty("fml.queryResult", "confirm")
            jvmArgumentProviders.add(jacocoProvider)
        }
    }
}

tasks.named("check").configure {
    dependsOn(gameTestSourceSet.classesTaskName)
}

tasks.register("clientSmokeTest") {
    group = "verification"
    description = "Launches the client, runs the flat-world smoke assertions, and exits with the result"
    dependsOn(gameTestSourceSet.classesTaskName)
    dependsOn(tasks.named("runClient"))
}

// ---------------------------------------------------------------------------------------------------------
// In-game coverage report
// ---------------------------------------------------------------------------------------------------------
// `gameTestServer` runs with the JaCoCo agent attached and writes build/jacoco/gameTest.exec. The classes in
// build/classes/java/main are the ones the game test loaded (our own classes are not ASM-transformed by FML), so
// they can be used directly for the report. This also avoids depending on an RFB class dump being present.

tasks.register<JacocoReport>("jacocoGameTestReport") {
    group = "verification"
    description = "Reports coverage from the in-game game test run, using the compiled classes"
    val execFile = project.layout.buildDirectory.file("jacoco/gameTest.exec").get().asFile
    // No `onlyIf`: a script closure cannot be stored in the configuration cache. An empty execution-data collection
    // makes JaCoCo skip the task instead, which is the same outcome.
    executionData.setFrom(
        project.files(execFile).filter { it.exists() }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(project.layout.buildDirectory.file("reports/jacoco/gameTest/jacocoGameTestReport.xml"))
        html.outputLocation.set(project.layout.buildDirectory.dir("reports/jacoco/gameTest/html"))
    }

    classDirectories.setFrom(
        project.files(mainSourceSet.output.classesDirs).asFileTree.matching {
            include("com/mrfuzzihead/dynamicsurroundings/**")
            exclude(
                "com/mrfuzzihead/dynamicsurroundings/Tags.class",
                "com/mrfuzzihead/dynamicsurroundings/client/**",
                "com/mrfuzzihead/dynamicsurroundings/gametest/**"
            )
        }
    )
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
}

val verifyGameTestReport = tasks.register<VerifyGameTestReport>("verifyGameTestReport") {
    group = "verification"
    description = "Fails when a game test run produced an empty JaCoCo report"
    dependsOn(tasks.named("jacocoGameTestReport"))
    executionData.set(project.layout.buildDirectory.file("jacoco/gameTest.exec"))
    reportXml.set(project.layout.buildDirectory.file("reports/jacoco/gameTest/jacocoGameTestReport.xml"))
}

tasks.register("gameTestServer") {
    group = "verification"
    description = "Boots a dedicated server, runs the in-game game test mod, and exits with its result"
    dependsOn(tasks.named("runServer"))
    finalizedBy(verifyGameTestReport)
}

tasks.named("check").configure {
    dependsOn(verifyGameTestReport)
}
