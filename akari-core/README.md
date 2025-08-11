# akari-core

![Maven Central](https://img.shields.io/maven-central/v/io.github.takusan23/akaricore) ← shields.io で作ったバッジ。最新バージョンはこの通りです。

`あかりどろいど`で利用している、動画編集のコアな部分をライブラリとして切り出してみました。`MavenCentral`から取ってくることが出来ます。  
コアな部分と言ってもほんとに`MediaCodec`を代わりに叩くくらいのコアな部分なのですが。。。  
やる気が無くなったらやめる。  

`2.0.0`で全面的に作り直したので互換性はなくなった。

# なんとなく MavenCentral に公開したけど

更新するかは分からんしまだ不十分すぎる。。

```kotlin
implementation("io.github.takusan23:akaricore:5.0.1")
```

# 何ができるの
- `MediaCodec`を利用した映像、音声のデコード（`audio`、`muxer`、`graphics`パッケージ）
- `OpenGL ES`を利用して高速に描画する（`graphics`パッケージ）
  - カメラや動画のフレームを描画する機能
  - `Canvas`で描いた内容を`OpenGL ES`へ転写する機能
- `Canvas`から動画を作る機能、動画フレームをできる限り高速で取り出す機能（`video`パッケージ）
  - あかりどろいどでは`graphics`パッケージに取って代わられたので使ってません

例えば、動画プレイヤーの映像を`OpenGL ES`で使えるようにして、エフェクトを適用したり、上から`Canvas`を重ねたりしたあと、動画ファイルに保存（もしくは画面に表示）が出来ます。  
これで動画編集が出来ているわけですね。

![Imgur](https://i.imgur.com/SYryBpw.png)

# 使ってるところ
使ってる実装を見るのが早そう

- あかりどろいど
  - https://github.com/takusan23/AkariDroid/blob/master/app/src/main/java/io/github/takusan23/akaridroid/canvasrender/VideoTrackRenderer.kt
  - https://github.com/takusan23/AkariDroid/blob/master/app/src/main/java/io/github/takusan23/akaridroid/audiorender/AudioRender.kt
  - https://github.com/takusan23/AkariDroid/blob/master/app/src/main/java/io/github/takusan23/akaridroid/encoder/AkariCoreEncoder.kt
- ちがうアプリ
  - https://github.com/takusan23/DougaUnDroid/blob/master/app/src/main/java/io/github/takusan23/dougaundroid/processor/VideoProcessor.kt
  - https://github.com/takusan23/DougaUnDroid/blob/master/app/src/main/java/io/github/takusan23/dougaundroid/processor/AudioProcessor.kt
  - https://github.com/takusan23/KomaDroid/blob/master/app/src/main/java/io/github/takusan23/komadroid/KomaDroidCameraManager.kt

# つかいかた
本当に`MediaCodec`を代わりに叩くくらいの関数しか無い。  
今度真面目に書く

## 共通

- AkariCoreInputOutput
  - これ以降の関数のファイル入出力を担当するクラス。
    - Java の File
    - Android の Uri
    - Java の ByteArray
  - 一部は対応していない。Java の File が確実

## 動画用

- AkariVideoEncoder
  - `MediaCodec`を使ったエンコーダー
  - 後述する AkariGraphicsProcessor で生成したフレームをエンコードするのに使ったり
- AkariVideoDecoder
  - `MediaCodec`を使ったデコーダー
  - 後述する SurfaceTexture で動画フレームを使いたい時にこれでデコードする
- AkariGraphicsProcessor
  - `OpenGL ES`で動画のフレームを生成するやつ
  - Canvas や SurfaceTexture を描画する
- AkariGraphicsSurfaceTexture
  - `Android`の`SurfaceTexture`を使いやすくしたもの
  - 動画の映像トラックや、カメラ映像を AkariGraphicsProcessor (OpenGL ES) で描画する

## 音声用

- AudioEncodeDecodeProcessor
  - 音声のデコード、エンコードをする
  - AAC を PCM にする
  - PCM を AAC にする
- AudioMixingProcessor
  - 音声を重ねる
  - PCM データを重ねます
- AudioMonoToStereoProcessor
  - ステレオにする
- AudioVolumeProcessor
  - 音量調整
- ReSaplingRateProcessor
  - Sonic.java を利用したサンプリングレート変換

# special thanks

- moovブロックを先頭に移動する (mp4ファイルをストリーミング可能にする)
    - https://github.com/ypresto/qtfaststart-java
- Android Open Source Project の CTS
    - エンコード部分は大体ここのを参考にしてます
    - https://cs.android.com/android/platform/superproject/+/master:cts/tests/tests/media/codec/src/android/media/codec/cts/DecodeEditEncodeTest.java
- サンプリングレート変換
    - https://github.com/waywardgeek/sonic

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

--- waywardgeek/sonic ---

Sonic library
Copyright 2010, 2011
Bill Cox
This file is part of the Sonic Library.

This file is licensed under the Apache 2.0 license.

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

`GitHub Actions`の`publish-library-maven-central.yml`を利用することで、ライブラリを`MavenCentral`
までアップロードしてくれます。  
手動実行ボタンを押してしばらく待ちます。  
あとは一番最後の手順を踏みます。

## GitHub Actions を利用しない

ローカルでも公開できます。

### CentralPortal ユーザートークンを取得する
https://central.sonatype.com/account

↑を元に、ユーザートークンを発行してください。  
私はすでにやってるのでやらなくて良いはず。

### local.properties に必要な値を書く

```properties
# Key Id Last 8 character
signing.keyId={鍵IDの最後8桁}
# Password
signing.password={秘密鍵のパスワード}
# Private key Base64
signing.key={Base64にした秘密鍵}
# Sonatype Central Portal UserToken UserName
centralPortalUsername={Central Portal で生成したユーザー名}
# Sonatype Central Portal UserToken Password
centralPortalPassword={Central Portal で生成したパスワード}
```

#### コマンドを叩く

`gradle :akari-core:publishToSonatype closeSonatypeStagingRepository`

## Close と Release を行う

https://central.sonatype.com/publishing/deployments  
へアクセスしログインした後、`Staging Repositories`を押します。

`Close` を押します。

![Imgur](https://imgur.com/pDPVunk.png)

終わったら `Release` を押します。これで MavenCentral に公開できます。
