# あかりどろいど
動画編集アプリ。大した機能はない。  
`MediaCodec`を叩いています。

// todo

# 開発者向け
お楽しみ項目

## あかりんく機能
外部連携機能があります。これを使うと、あかりどろいどでは作れない素材を、他のアプリで作って追加出来ます。  
詳しくは AKALINK_README.md へ

## ライブラリ
動画編集の、`MediaCodec`を代わりに叩いてちょっとだけ使いやすくしたものを`akari-core`ライブラリとして公開しています。  
詳しくは`akari-core/README.md`へ。

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
`akari-core`がライブラリの部分で、`app`が実際のアプリです。`akari-core`を取り込んで使ってます。