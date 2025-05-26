plugins {
    kotlin("jvm") version "2.1.10"
}

group = "me.snipz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.purpurmc.org/snapshots")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("org.purpurmc.purpur", "purpur-api", "1.21.3-R0.1-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    compileOnly("com.github.devoirr:snipzGUI:alpha")
    compileOnly("com.github.devoirr:snipzLocales:alpha-6")
    compileOnly("com.github.devoirr:snipzDatabase:alpha-8")
    compileOnly("com.github.devoirr:snipzEcoApi:alpha-3")
}


kotlin {
    jvmToolchain(21)
}