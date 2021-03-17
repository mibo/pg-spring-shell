import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("pg-spring-shell.java-common-conventions")

	id("org.springframework.boot") version "2.3.4.RELEASE"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
}

group = "org.github.mibo"
//name = "github-shell"
version = "0.1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

dependencies {
	// all dependencies declared in `pg-spring-shell.java-common-conventions.gradle.kts`
}

tasks.withType<Jar> {
	manifest.attributes["Build-Version"] = project.version
	manifest.attributes["Build-Date"] = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
	archiveFileName.set("pg-spring-shell.jar")
	println("Find assembled jar (v${project.version}) at: ${archiveFile.get()}")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}
