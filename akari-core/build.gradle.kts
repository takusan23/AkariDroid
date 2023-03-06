plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // ドキュメント生成
    id("org.jetbrains.dokka")
    // Maven Central に公開する際に利用
    `maven-publish`
    signing
}

android {
    namespace = "io.github.takusan23.akaricore"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// ライブラリ
dependencies {

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutine)
    testImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutine.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// 署名
signing {
    // ルート build.gradle.kts の extra を見に行く
    useInMemoryPgpKeys(
        rootProject.extra["signing.keyId"] as String,
        rootProject.extra["signing.key"] as String,
        rootProject.extra["signing.password"] as String,
    )
    sign(publishing.publications)
}

// ソースコードを提供する
val androidSourcesJar = tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

// JavaDoc を生成する
tasks.dokkaJavadoc {
    outputDirectory.set(File(buildDir, "dokkaJavadoc"))
}
val javadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

artifacts {
    archives(androidSourcesJar)
    archives(javadocJar)
}

// ライブラリのメタデータ
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "io.github.takusan23"
                artifactId = "akaricore"
                version = "1.0.0-alpha02"
                if (project.plugins.hasPlugin("com.android.library")) {
                    from(components["release"])
                } else {
                    from(components["java"])
                }
                artifact(androidSourcesJar)
                artifact(javadocJar)
                pom {
                    // ライブラリ情報
                    name.set("akaricore")
                    description.set("AkariDroid core library")
                    url.set("https://github.com/takusan23/AkariDroid/")
                    // ライセンス
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://github.com/takusan23/AkariDroid/blob/master/LICENSE")
                        }
                    }
                    // 開発者
                    developers {
                        developer {
                            id.set("takusan_23")
                            name.set("takusan_23")
                            url.set("https://takusan.negitoro.dev/")
                        }
                    }
                    // git
                    scm {
                        connection.set("scm:git:github.com/takusan23/AkariDroid")
                        developerConnection.set("scm:git:ssh://github.com/takusan23/AkariDroid")
                        url.set("https://github.com/takusan23/AkariDroid")
                    }
                }
            }
        }
    }
}