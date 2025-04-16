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
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("/Users/romanrudoy/Developer/snipzEconomy/snipzApi.jar"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}


kotlin {
    jvmToolchain(21)
}