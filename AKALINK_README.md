# あかりんく 機能
これは、外部の対応したアプリと連携する機能です。  
外部のアプリで動画編集で使いたい素材を作成し、それを取り込むことが出来ます。

この機能を使うと、あかりどろいどでは作れない素材（`QRコードの画像`、`TTS の音声`）を別のアプリで作って、取り込むことが出来ます。  
`Android`の`Intent`の仕組みを使った、昔からある方法です。  
（キーボードのマッシュルームとかもこれと同じはず。やばい懐かしすぎる）

# 対応している形式
以下のファイルを受け取ることが出来ます

- 画像
- 音声
- 動画

# サンプルアプリ
// TODO つくる

# 仕組み
あかりんくを始めると、あかりどろいどはある`Intent`を投げます。（`action = "io.github.takusan23.akaridroid.ACTION_START_AKALINK""`）  
それを、対応したアプリが受け取ります。受け取ると`Activity`が開きますね。  

後はデータを作成して、`Intent`に入っている`Uri`に書き込みます。  
書き込みが終わったら`MIME-Type`と名前を指定して`setResult`すればよいです。

この後くわしく説明します。

# 作り方
// TODO 画像とか貼ってわかりやすくする

## Android Studio で適当なプロジェクトを作ります
題目どおりです。`Jetpack Compose`を使うかどうかはお任せします。  
連携画面で使いたい場合は入れよう。

`Java`で書くことも出来ますが、`Jetpack Compose`使えないし辛そう。

## AndroidManifest.xml を編集する
今回は、`MainActivity`を連携した時に起動する画面とします。  
他にも連携用の画面を作りたい場合は、`android:name`の部分を変えてください。

あかりんくの開始ボタンを押して開けるようにするためには（あかりんく対応と認識させるためには）、`<intent-filter>`に書き足す必要があります。  
以下参照。

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:label="@string/app_name">
    
    <!-- ホーム画面アプリから起動 -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- あかりんく 連携 -->
    <intent-filter>
        <action android:name="io.github.takusan23.akaridroid.ACTION_START_AKALINK" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="*/*" />
    </intent-filter>
</activity>
```

## MainActivity を開く
`あかりんく起動`かどうかは以下のような条件分岐で確認できます。  
`Uri`をもらって、追加したいデータを`OutputStream`へ書き込み、`MIME-Type`とファイル名を`Intent`に入れて、あかりどろいどへ返してあげてください。

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // あかりんく起動かどうかは Intent#getAction で判定できます
        if (intent.action == "io.github.takusan23.akaridroid.ACTION_START_AKALINK") {

            // Uri が貰えます
            val uri = intent.data!!
            // 書き込みをしたい場合は OutputStream を開くことで出来ます
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                // バイナリデータ（画像、音声、動画）を書き込む
                outputStream.write()
            }

            // あかりどろいどは MIME-Type を知らないので、ここで教えてください
            val resultIntent = Intent()
            // これは必須です。書き込んだバイナリデータの MIME-Type を
            resultIntent.type = "image/png"
            // これは任意です。ファイルの名前を変えたい場合（タイムライン上の表示）
            resultIntent.putExtra(Intent.EXTRA_TITLE, "")

            // あかりどろいどにお戻しする。finish を呼んで画面を閉じます
            setResult(Activity.RESULT_OK, resultIntent)
            finish()

        } else {
            // あかりんく起動ではない
        }

    }
}
```