plugins {
  java
}

repositories {
  jcenter()
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework:spring-web")
  implementation("org.springframework.shell:spring-shell-starter:2.0.1.RELEASE")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}
