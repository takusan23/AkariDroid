plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Maven Central に公開する際に利用
    `maven-publish`
    signing
}

// ライブラリ公開は Android でも言及するようになったので目を通すといいかも
// https://developer.android.com/build/publish-library/upload-library
// そのほか役に立ちそうなドキュメント
// https://docs.gradle.org/current/dsl/org.gradle.api.publish.maven.MavenPublication.html
// https://github.com/gradle-nexus/publish-plugin

// OSSRH にアップロードせずに成果物を確認する方法があります。ローカルに吐き出せばいい
// gradle :akari-core:publishToMavenLocal

android {
    namespace = "io.github.takusan23.akaricore"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

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

    // どうやら Android Gradle Plugin 側で sources.jar と javadoc.jar を作る機能が実装されたそう
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
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

// ライブラリのメタデータ
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "io.github.takusan23"
            artifactId = "akaricore"
            version = "4.1.1" // バージョンアップの際は CORE_RELEASE_NOTE.md もう更新

            // afterEvaluate しないとエラーなる
            afterEvaluate {
                from(components["release"])
            }

            pom {
                // ライブラリ情報
                name.set("akaricore")
                description.set("AkariDroid is Video editor app in Android. AkariDroid core library")
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

// 署名
signing {
    // ルート build.gradle.kts の extra を見に行く
    useInMemoryPgpKeys(
        rootProject.extra["signing.keyId"] as String,
        rootProject.extra["signing.key"] as String,
        rootProject.extra["signing.password"] as String,
    )
    sign(publishing.publications["release"])
}
