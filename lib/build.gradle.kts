plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
   
    implementation("org.kohsuke:github-api:1.131")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
	implementation("org.jsoup:jsoup:1.13.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
