# processor

映像を加工するクラスがあったりする

- SilenceAudioProcessor
  - 指定した時間で無音の音声ファイルを作成します
- ConcatProcessor
    - 動画を結合する
- VideoCanvasProcessor
    - 動画 に Canvas を重ねた動画を作成する
- CanvasProcessor
    - Canvas だけで動画を作成する
- AudioMixingProcessor
    - 複数の音声を同時に重ねて音声を作成する
- CutProcessor
  - 動画を切り取る

## QtFastStart

`MediaMuxer`が作る`mp4`の`moovブロック`を先頭に移動するためのコードです。
以下の実装をお借りました、ありがとうございます！

https://github.com/ypresto/qtfaststart-java