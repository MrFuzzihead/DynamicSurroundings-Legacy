// Extra build configuration for the GTNH/RFG build.
//
// The test harness lives here: JaCoCo coverage, the Java 8 unit-test launcher, the in-game `gameTest` source set
// and the `gameTestServer` task. It mirrors the setup in MrFuzzihead/PeripheralsPlusPlus.

apply(from = rootProject.file("gradle/test-harness.gradle.kts"))
