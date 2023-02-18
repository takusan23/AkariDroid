# akari-core

![Maven Central](https://img.shields.io/maven-central/v/io.github.takusan23/akaricore)

↑ shields.io で作ったバッジ

前作った conecocore ( https://github.com/takusan23/Coneco/tree/master/conecocore ) から実装をパクってます。

# special thanks
moovブロックを先頭に移動する (ダウンロードしながら再生)
https://github.com/ypresto/qtfaststart-java

# わたし向け MavenCentral 公開手順

## local.properties に必要な値を書く

```properties
# Key Id Last 8 character
signing.keyId={鍵IDの最後8桁}
# Password
signing.password={秘密鍵のパスワード}
# Private key Base64
signing.key={Base64にした秘密鍵}
# Sonatype OSSRH UserName
ossrhUsername={Sonatype OSSRH のユーザー名}
# Sonatype OSSRH Password
ossrhPassword={Sonatype OSSRH のパスワード}
# Sonatype Staging Profile Id
sonatypeStagingProfileId={SonatypeステージングプロファイルID}
```

## バージョンをインクリメントする

`build.gradle.kts`の`version = "1.0.0-alpha01"`を +1 します。

## コマンドを叩く

`gradle :akari-core:publishToSonatype`

## Close と Release を行う
`Close` を押します。

![Imgur](https://imgur.com/pDPVunk.png)

終わったら `Release` を押します。これで MavenCentral に公開できます。