import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    jacoco
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

group = "com.shop"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 4 split-jar starters
    implementation("org.springframework.boot:spring-boot-starter-webmvc:_")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:_")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:_")
    implementation("org.springframework.boot:spring-boot-starter-security:_")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6:_")
    implementation("org.springframework.boot:spring-boot-starter-validation:_")
    implementation("org.springframework.boot:spring-boot-starter-mail:_")
    implementation("org.springframework.boot:spring-boot-docker-compose:_")
    implementation("tools.jackson.module:jackson-module-kotlin:_")
    implementation("org.jetbrains.kotlin:kotlin-reflect:_")
    runtimeOnly("org.postgresql:postgresql:_")
    implementation("io.minio:minio:_")
    implementation("com.stripe:stripe-java:_")
    implementation("com.itextpdf:kernel:_")
    implementation("com.itextpdf:io:_")
    implementation("com.itextpdf:layout:_")

    kapt("org.springframework.boot:spring-boot-configuration-processor:_")

    // Test — Spring Boot 4 split-jar test starters
    testImplementation("org.springframework.boot:spring-boot-starter-test:_")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test:_")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:_")
    testImplementation("org.springframework.boot:spring-boot-jpa-test:_")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:_")
    testImplementation("org.springframework.security:spring-security-test:_")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:_")
    testImplementation("org.testcontainers:testcontainers-postgresql:_")
    testImplementation("org.mockito.kotlin:mockito-kotlin:_")
    testImplementation("com.tngtech.archunit:archunit-junit5:_")
}

dependencyManagement {
    imports {
        // Not a plain Gradle dependency — Spring's dependencyManagement.imports
        // DSL isn't something refreshVersions' `:_` placeholder can intercept,
        // so this stays pinned explicitly.
        mavenBom("org.testcontainers:testcontainers-bom:2.0.5")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.15"
}

val jacocoCoverageExclusions =
    listOf(
        "com/shop/ShopApplicationKt.class",
    )

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoCoverageExclusions) } }),
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoCoverageExclusions) } }),
    )
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
