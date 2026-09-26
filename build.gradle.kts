// Plain java/application plugins rather than the Spring Boot Gradle plugin:
// Spring is used as a library here, which keeps the build simple and avoids
// tying the example to a particular Boot plugin / Gradle version pairing.
subprojects {
    apply(plugin = "java")
    repositories { mavenCentral() }
    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }
    // Spring resolves @PathVariable / @RequestParam names by reflection,
    // which needs parameter names retained in the bytecode.
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }
}
