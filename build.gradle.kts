import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.3.4.RELEASE"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
}

group = "org.github.mibo"
version = "0.1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
//	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework:spring-web")
	implementation("org.springframework.shell:spring-shell-starter:2.0.1.RELEASE")
	implementation("com.google.code.gson:gson:2.8.6")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
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
