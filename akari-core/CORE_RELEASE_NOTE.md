# akari-core リリースノート
更新忘れたらごめん。リリースのバージョンは適当（あかりどろいど側のリリースに合わせてる）。

## 4.0.0 時代
あかりどろいど 3.x に対応します

### akaricore:4.1.0
破壊的変更は無いですが、変更点があります。

- GpuShaderImageProcessor 
  - シェーダーが GLSL の文法ミスでコンパイルできなかった場合、以下の例外を投げます
    - GlslSyntaxErrorException
- VideoFrameBitmapExtractor
  - 内部で使っている MediaParserKeyFrameTimeDetector で解析に失敗した際、例外を投げましたが、このクラスがなくてもシークが遅くなる程度なので、このクラス起因の例外（MediaParser）は投げられないようになりました。
    - 今のところ MP4 / Matroska (WebM も) / MPEG2-TS が対象
  - MPEG2-TS コンテナを MediaParserKeyFrameTimeDetector で受け付ける設定をしました。忘れてた

### akaricore:4.0.0
破壊的変更はないはず。

- GpuShaderImageProcessor を追加しました
  - OpenGL ES のフラグメントシェーダーで、好きな Bitmap に色を付けることが出来ます
  - OpenGL ES を使ってるので、GPU 側で画像編集するので速いはず？
- VideoFrameBitmapExtractor のフレーム取得が若干速くなりました
  - Android 11 以降、キーフレームの再生位置を取得できるため、連続していないフレームの取得が若干速くなっているはずです。
  - MediaParserKeyFrameTimeDetector クラス参照

## 3.0.0 時代
あかりどろいど 2.x に対応します

### akaricore:3.0.0
API の互換がない、アプリで 2.0.0 リリースしたのでこちらも。
alpha とか付けてたけど誰も使わんだろうし外すわ。

- VideoFrameBitmapExtractor にクロマキー機能追加。指定した色とそれに近しい色が透明になります
- AudioMixingProcessor が秒ではなくミリ秒単位で処理されるよう
- ReSamplingRateProcessor が AudioSonicProcessor へ。追加で再生速度を適用する関数がつきました。
- core の OpenGL ES 周りがちょっと共通化
- VideoFrameBitmapExtractor が一部動画ではやっぱり崩れたので修正

## 2.0.0 時代
- akaricore:2.0.0-alpha01
  - 2.0.0 試験的公開

## 1.0.0 時代
作り直そうと思ったので、1系はこれが最後

- akaricore:1.0.0-alpha02
  - 2023/03/09
  - VideoCanvasProcessor の修正

- akaricore:1.0.0-alpha02
  - 2023/03/07
  - 以下のクラスを追加
    - CanvasProcessor
    - ConcatProcessor
    - CutProcessor
    - SilenceAudioProcessor

- akaricore:1.0.0-alpha01
  - 2023/02/18
  - 以下のクラスを追加
    - VideoProcessor
    - AudioMixingProcessor
