// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    // akaricore ライブラリ公開で使う
    alias(libs.plugins.gradle.nexus.publish.plugin)
}

tasks.register("clean") {
    doFirst {
        delete(rootProject.buildDir)
    }
}

// io.github.gradle-nexus.publish-plugin で利用
group = "io.github.takusan23"

// ライブラリ署名情報がなくてもビルドできるようにする
extra["signing.keyId"] = ""
extra["signing.password"] = ""
extra["signing.key"] = ""
extra["ossrhUsername"] = ""
extra["ossrhPassword"] = ""
extra["sonatypeStagingProfileId"] = ""

// 署名情報を読み出す。開発環境では local.properties に署名情報を置いている。
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    // 読み出して、extra へ格納する
    val properties = java.util.Properties().apply {
        load(secretPropsFile.inputStream())
    }
    properties.forEach { name, value -> extra[name as String] = value }
} else {
    // システム環境変数から読み出す。CI/CD 用
    extra["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    extra["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
    extra["sonatypeStagingProfileId"] = System.getenv("SONATYPE_STAGING_PROFILE_ID")
    extra["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    extra["signing.password"] = System.getenv("SIGNING_PASSWORD")
    extra["signing.key"] = System.getenv("SIGNING_KEY")
}

// Sonatype OSSRH リポジトリ情報
nexusPublishing.repositories.sonatype {
    username.set(extra["ossrhUsername"] as String)
    password.set(extra["ossrhPassword"] as String)
    nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
    snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
}