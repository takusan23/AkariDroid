# graphics パッケージ
`OpenGL ES`を利用して高速に映像フレームを生成するためのクラスたち。  
ざっくり紹介

- `OpenGL`の複雑さを隠ぺいして描画のための口を用意した`AkariGraphicsProcessor`
- `AkariGraphicsProcessor`を録画するための`MediaCodec`をいい感じにした`AkariVideoEncoder`
- `OpenGL ES`でカメラ映像、動画のフレームをテクスチャとして使える`SurfaceTexture`をいい感じにした`AkariGraphicsSurfaceTexture`
- `AkariGraphicsSurfaceTexture`へ動画のフレームを提供するための動画デコーダー。`AkariVideoDecoder`