/*
 * Copyright (c) 2021 Nikita A. Chegodaev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.0.1"

val kotlinVersion = "1.6.0"

plugins {
    kotlin("jvm") version "1.6.0"
    id("org.jetbrains.dokka") version "1.4.32"
    `java-library`
    jacoco
    maven
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

jacoco {
    toolVersion = "0.8.7"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    api ("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    api("org.xpathqs:web-selenium:0.1.3")
    api("org.xpathqs:web:0.1.3")
    api("org.xpathqs:driver:0.1.3")
    api("org.xpathqs:core:0.1.3")
    api("org.xpathqs:log:0.1.3")
    api("org.xpathqs:prop:0.2.2")
    api("org.xpathqs:cache:0.1")

    api("org.xpathqs:gwt:0.2.3")

    api("org.seleniumhq.selenium:selenium-remote-driver:3.141.59")
    api("org.testcontainers:selenium:1.16.0")

    api("org.testng:testng:6.14.3")
    api("com.beust:jcommander:1.72")
    api("org.apache-extras.beanshell:bsh:2.0b6")
    api("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
    api("io.qameta.allure:allure-testng:2.14.0")


    implementation("org.slf4j:slf4j-log4j12:1.7.29")

    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    api("org.openapitools:jackson-databind-nullable:0.2.2")

    api("io.konform:konform-jvm:0.3.0")
}

publishing {
    publications {
        beforeEvaluate {
            signing.sign(this@publications)
        }
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("XpathQS Framework TestNG")
                description.set("Framework for the interaction with drivers for the xpathqs-core")
                url.set("https://xpathqs.org/")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("nachg")
                        name.set("Nikita A. Chegodaev")
                        email.set("nikchgd@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/xpathqs/framework-testng.git")
                    developerConnection.set("scm:git:ssh://github.com/xpathqs/framework-testng.git")
                    url.set("https://xpathqs.org/")
                }
            }
            groupId = "org.xpathqs"
            artifactId = "framework-testng"

            from(components["java"])
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.property("ossrhUsername").toString()
                password = project.property("ossrhPassword").toString()
            }
        }
    }
}

/* signing {
     sign(publishing.publications["mavenJava"])
 }*/

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = true
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            samples.from("src/test/kotlin/org/xpathqs/frameworktestng", "src/main/kotlin/org/xpathqs/frameworktestng")
        }
    }
}