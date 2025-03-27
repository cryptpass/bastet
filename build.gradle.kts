import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar

logging.captureStandardOutput(LogLevel.INFO)

plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlin.jvm") version "2.1.10" // kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.spring") version "2.1.10" // kotlin("plugin.spring") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    idea
    id("org.graalvm.buildtools.native") version "0.10.3"
    id("org.jetbrains.kotlin.plugin.jpa") version "2.1.0" // kotlin("plugin.jpa") version "2.1.0"
    id("org.hibernate.orm") version "6.6.11.Final"
}

group = "com.arpanrec"
version = getVersions()

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains:annotations")
    implementation("org.bouncycastle:bcpg-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.pgpainless:pgpainless-core:1.6.7")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    implementation("org.hibernate.orm:hibernate-community-dialects")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.dbeaver.jdbc:com.dbeaver.jdbc.driver.libsql:1.0.2")

    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }

    implementation("org.springframework.boot:spring-boot-starter-security") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    implementation("org.springframework.boot:spring-boot-starter-actuator") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    // Log4j2 org.springframework.boot:spring-boot-starter-log4j2 and module replacement not needed because it's
    // excluded from spring-boot-starter but still it's here for clarity
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    modules {
        module("org.springframework.boot:spring-boot-starter-logging") {
            replacedBy("org.springframework.boot:spring-boot-starter-log4j2", "Use Log4j2 instead of Logback")
        }
    }
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.18.0")
    implementation("org.slf4j:jcl-over-slf4j")
    implementation("org.slf4j:jul-to-slf4j")
    implementation("org.slf4j:log4j-over-slf4j")
    implementation("org.slf4j:osgi-over-slf4j:2.1.0-alpha1")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    testImplementation("org.springframework.security:spring-security-test") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    testImplementation("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

graalvmNative {
    binaries {
        all {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    jvmToolchain(21)
}

sourceSets {
    main {
        java { srcDirs("src/main/java") }
        kotlin { srcDirs("src/main/kotlin") }
    }
    test {
        java { srcDirs("src/test/java") }
        kotlin { srcDirs("src/test/kotlin") }
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

configure<IdeaModel> {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks {
    getByName<Jar>("jar") {
        enabled = true
        archiveAppendix.set("original")
        manifest {
            attributes(
                "Implementation-Title" to getMainClassName(),
                "Implementation-Version" to getVersions()
            )
        }
    }
    getByName<BootJar>("bootJar") {
        enabled = true
        mainClass = getMainClassName()
        archiveAppendix.set("boot")
    }
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        systemProperty("spring.profiles.active", "test")
        reports {
            html.required = true
            junitXml.required = true
        }
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

fun getMainClassName(): String {
    return "com.arpanrec.bastet.Application"
}

fun getVersions(): String {

    val file = File("BASTET_VERSION")
    if (!file.exists()) {
        throw StopActionException("BASTET_VERSION file should be present on the root of the project")
    }
    val versionFromFile: String = file.readText().trim()
    if (versionFromFile.isEmpty()) {
        throw StopActionException("BASTET_VERSION file is empty")
    }
    return versionFromFile
}
