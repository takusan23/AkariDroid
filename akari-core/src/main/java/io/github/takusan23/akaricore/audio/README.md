# audio パッケージ

音声の加工ができます。  
流れとしては、

- `AudioEncodeDecodeProcessor.decode`で音声ファイルをデコードする
  - 圧縮されているので、戻します
  - PCM ファイルが取得できます
- `AudioVolumeProcessor`、`AudioSonicProcessor`とかで加工をします
  - 引数は PCM ファイル
- `AudioEncodeDecodeProcessor.encode`で音声ファイルをエンコードします
  - PCM ファイルを mp4 ( aac ) に出来ます

# めも
PCM ファイルは以下になるのを期待してます

- 量子化ビット数（ビット深度）
  - 16bit
- チャンネル数
  - 2
- サンプリングレート
  - 48000

# 用意されてる関数

- AkariCoreAudioProperties
  - サンプリングレートとかの定数置き場
- AudioEncodeDecodeProcessor
  - 音声ファイルのエンコード、デコード
- AudioMixingProcessor
  - 複数の PCM のバイト配列を合成したり同時に再生するような PCM を吐き出す
- AudioVolumeProcessor
  - PCM の音量を調整する
- AudioSonicProcessor
  - サンプリングレート変換器
  - 再生速度の変更も追加
  - special thanks
    - https://github.com/waywardgeek/sonic
