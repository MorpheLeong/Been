plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.bugchecking.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    defaultConfig {
        applicationId = "com.bugchecking.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

base {
    archivesName.set("Been")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("org.maplibre.gl:android-sdk:11.11.0")
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation("com.google.zxing:core:3.5.4")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val verifyPassportUiTokens = tasks.register("verifyPassportUiTokens") {
    group = "verification"
    description = "Reject raw UI colors, local shapes, theme-shape bypasses, and border widths outside PassportDesign.kt."
    val uiSources = fileTree("src/main/java") {
        include("**/*.kt")
        exclude("**/PassportDesign.kt")
    }
    inputs.files(uiSources)
    doLast {
        val forbidden = linkedMapOf(
            "raw color" to Regex("""Color\(0x[0-9A-Fa-f]+"""),
            "local rounded corner" to Regex("""RoundedCornerShape\("""),
            "theme shape bypass" to Regex("""shape\s*=\s*MaterialTheme\.shapes"""),
            "raw border width" to Regex("""BorderStroke\(\s*\d"""),
        )
        val violations = buildList {
            uiSources.files.sortedBy { it.path }.forEach { source ->
                source.readLines().forEachIndexed { index, line ->
                    forbidden.forEach { (name, pattern) ->
                        if (pattern.containsMatchIn(line)) {
                            add("${source.relativeTo(projectDir)}:${index + 1}: $name: ${line.trim()}")
                        }
                    }
                }
            }
        }
        check(violations.isEmpty()) {
            "UI token guard failed. Move values into PassportDesign.kt or use PassportTokens:\n" +
                violations.joinToString("\n")
        }
    }
}

tasks.named("check") {
    dependsOn(verifyPassportUiTokens)
}
