# akari-core

![Maven Central](https://img.shields.io/maven-central/v/io.github.takusan23/akaricore)

↑ shields.io で作ったバッジ。最新バージョンは↑です。

前作った conecocore ( https://github.com/takusan23/Coneco/tree/master/conecocore ) から実装をパクってます。

# なんとなく MavenCentral に公開したけど
更新するかは分からんしまだ不十分すぎる

```kotlin
implementation("io.github.takusan23:akaricore:1.0.0-alpha01")
```

https://takusan.negitoro.dev/posts/android_add_canvas_text_to_video/

# つかいかた
`ExampleInstrumentedTest.kt`を見てください。  
`processor`パッケージ内のユーティリティ関数が利用可能です（`VideoCanvasProcessor`など）。詳しくは README で。

![image](https://user-images.githubusercontent.com/32033405/222954361-c1efe7a4-60ad-4e05-b83b-2969cdf0faf1.png)

# special thanks
- moovブロックを先頭に移動する (mp4ファイルをストリーミング可能にする)
  - https://github.com/ypresto/qtfaststart-java
- Android Open Source Project の CTS 
  - エンコード部分は大体ここのを参考にしてます
  - https://cs.android.com/android/platform/superproject/+/master:cts/tests/tests/media/codec/src/android/media/codec/cts/DecodeEditEncodeTest.java

# らいせんす

```
--- akari-core ---

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

--- ypresto/qtfaststart-java ---

The MIT License (MIT)

Copyright (c) 2014 Yuya Tanaka

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

--- Android Open Source Project ---

Copyright (C) 2013 The Android Open Source Project
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

# わたし向け MavenCentral 公開手順

## バージョンをインクリメントする

`build.gradle.kts`の`version = "1.0.0-alpha01"`を +1 します。

## 更新内容を書く

CORE_RELEASE_NORE.md を書く

## GitHub Actions を利用する
`GitHub Actions`の`publish-library-maven-central.yml`を利用することで、ライブラリを`MavenCentral`までアップロードしてくれます。  
手動実行ボタンを押してしばらく待ちます。  
あとは一番最後の手順を踏みます。

## GitHub Actions を利用しない
ローカルでも公開できます。

### local.properties に必要な値を書く

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

#### コマンドを叩く

`gradle :akari-core:publishToSonatype`

## Close と Release を行う
https://s01.oss.sonatype.org/  
へアクセスしログインした後、`Staging Repositories`を押します。

`Close` を押します。

![Imgur](https://imgur.com/pDPVunk.png)

終わったら `Release` を押します。これで MavenCentral に公開できます。
