plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "io.github.rainboom7.cosmetics"
version = "1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// The Open Beauty Facts indexer: a JVM tool run in CI, not part of the library the app uses.
val indexer: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[indexer.implementationConfigurationName].extendsFrom(configurations.implementation.get())

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(indexer.output)
    testImplementation(libs.sqlite.jdbc)

    "indexerImplementation"(libs.sqlite.jdbc)
}

/**
 * ./gradlew -p core buildObfIndex -PobfExport=<url or .csv.gz path> -PobfIndexOut=<file.db>
 * Builds the search index the app bundles (see IndexSchema).
 */
tasks.register<JavaExec>("buildObfIndex") {
    group = "build"
    description = "Builds the Open Beauty Facts search index"
    classpath = indexer.runtimeClasspath
    mainClass.set("io.github.rainboom7.cosmetics.indexer.IndexerMainKt")
    args(
        providers.gradleProperty("obfExport").getOrElse("https://static.openbeautyfacts.org/data/en.openbeautyfacts.org.products.csv.gz"),
        providers.gradleProperty("obfIndexOut").getOrElse(layout.buildDirectory.file("obf-index/obf-index.db").get().asFile.path),
    )
}
