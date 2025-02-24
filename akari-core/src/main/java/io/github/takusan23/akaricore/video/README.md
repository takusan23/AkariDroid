# video パッケージ

動画から画像としてフレームを取り出す、画像をフレームとして動画を作る処理があります。

あかりどろいどでは graphics パッケージにある AkariGraphicsProcessor を使ってるので優先度低なクラスたちです。  
できれば AkariGraphicsProcessor を使った処理に移行すべき...かも。

# 用意されてる関数

- CanvasVideoProcessor
  - 画像のフレームから動画を作ります
  - Canvas で書いて動画を作る
  - special thanks
    - Android Open Source Project
- VideoFrameBitmapExtractor
  - 動画から画像としてフレームを取り出す
  - 動画の指定位置を Bitmap に
  - クロマキー機能があります。指定した色とそれに近しい色が透明になります。
