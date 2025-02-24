# あかりどろいど
動画編集アプリ。大した機能はない。  
`MediaCodec`や`OpenGL ES`などを利用した動画編集アプリです

<p align="center">
<img width="200" src="https://imgur.com/HbHsgpE.jpg">
<img width="200" src="https://imgur.com/1LTvNu2.jpg">
<img width="200" src="https://imgur.com/tGLrTW9.jpg">
<img width="200" src="https://imgur.com/5xnemwZ.jpg">
</p>

# ダウンロード
https://play.google.com/store/apps/details?id=io.github.takusan23.akaridroid

# できること

- 動画 / 音声 / テキスト / 画像 を動画に追加
  - 動画は性能が許す限り同時に表示させることが出来ます
  - テキストのフォントは自前のを取り込むことで変更できます
- 切り替え時のアニメーション / エフェクト 機能
  - GLSL が書ける場合は自分でエフェクトを作ることも出来ます
- 動画 / 音声 の再生速度変更
- 動画のクロマキー機能
- 10 ビット HDR 動画の編集機能
  - 今のところ `HLG (BT.2020 + HLG)`形式のみです
  - `HDR10 / HDR10+`は考え中...
- 上級者向けのエンコード設定
  - `コーデック`、`コンテナ`、`フレームレート`などが好きに決められます
- プロジェクトのエクスポート、インポート機能（他の端末に持ち出す機能）

# 動画編集 チュートリアル

https://takusan.negitoro.dev/posts/akari_droid_tutorial_video_side_blur/

# 開発者向け
お楽しみ項目

## あかりんく機能
外部連携機能があります。これを使うと、あかりどろいどでは作れない素材を、他のアプリで作って追加出来ます。  
詳しくは AKALINK_README.md へ

## あかりどろいど ビルド方法
それなりの性能のパソコンがあればいいはず。

- 最新の`Android Studio`を入れる
- ソースコードを取得する
  - `git clone`する
  - `git`無いならソースコードを`zip`でダウンロードして解凍していいはず
- `Android Studio`でこのプロジェクトを開く
- しばらく待つ（ライブラリのダウンロードとかするので時間がかかる）
- 端末を繋いで、実行ボタンを押す
  - エミュレータは`MediaCodec`（動画・音声エンコーダー、デコーダー）が動くか分かんないので実機がいいと思います

## ソースコードの構成
`app`モジュールと、`akari-core`モジュールで出来ています。  

![Imgur](https://i.imgur.com/3hplNnZ.png)

`akari-core`が`MediaCodec`と`OpenGL ES`を使って映像、音声の作成を担当しているライブラリで、それを`app`が取り込んでエンコードやプレビューの表示で使っています。  
`app`が実際のアプリで、`Jetpack Compose`で`UI`が作られています。

## ライブラリ
動画編集の、`MediaCodec`を代わりに叩いてちょっとだけ使いやすくしたものを`akari-core`ライブラリとして`MavenCentral`へ公開しています。  
私が他に作っているアプリでこれを参照したくて公開しているので、多分使いやすくないです。

詳しくは`akari-core/README.md`へ。