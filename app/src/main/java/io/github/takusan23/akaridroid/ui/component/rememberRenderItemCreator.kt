package io.github.takusan23.akaridroid.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData

/** [AddRenderItemMenuResult]からメニューを探す */
fun AddRenderItemMenuResult.toMenu() = when (this) {
    AddRenderItemMenuResult.AkaLink -> AddRenderItemMenu.AkaLink
    is AddRenderItemMenuResult.Audio -> AddRenderItemMenu.Audio
    is AddRenderItemMenuResult.Image -> AddRenderItemMenu.Image
    AddRenderItemMenuResult.Shape -> AddRenderItemMenu.Shape
    AddRenderItemMenuResult.Text -> AddRenderItemMenu.Text
    is AddRenderItemMenuResult.Video -> AddRenderItemMenu.Video
}

/**
 * メニュー一覧
 *
 * @param label なまえ
 * @param description せつめい
 * @param iconResId あいこん
 */
enum class AddRenderItemMenu(
    val label: String,
    val description: String,
    val iconResId: Int
) {
    /** テキスト */
    Text(
        "テキスト",
        "文字を追加します。フォントを変えたい場合は設定で取り込めます。",
        R.drawable.ic_outline_text_fields_24
    ),

    /** 画像 */
    Image(
        "画像",
        "画像を追加します。",
        R.drawable.ic_outline_add_photo_alternate_24px
    ),

    /** 動画 */
    Video(
        "動画",
        "動画を追加します。性能が許す限りタイムラインに追加できるはず？",
        R.drawable.ic_outlined_movie_24px
    ),

    /** 音声 */
    Audio(
        "音声",
        "音声を追加します。",
        R.drawable.ic_outline_audiotrack_24
    ),

    /** 図形 */
    Shape(
        "図形",
        "図形を追加します。背景とかにどうぞ。",
        R.drawable.ic_outline_category_24
    ),

    /** あかりんく */
    AkaLink(
        "あかりんく",
        "外部アプリで素材を作成し、タイムラインに追加できる機能です。対応しているアプリが必要です。",
        R.drawable.akari_droid_icon
    )
}

/** [rememberRenderItemCreator]の結果 */
sealed interface AddRenderItemMenuResult {

    /** 追加のステップ無しで追加ができる。 */
    sealed interface Addable

    /** ボトムシートを出す必要がある。ぶっちゃけあかりんく用。sealed class まじで kotlin の中でも好きな機能の上位にいるわ */
    sealed interface BottomSheetOpenable {

        /** 遷移先 */
        val bottomSheet: VideoEditorBottomSheetRouteRequestData
    }

    /** テキストの追加 */
    data object Text : AddRenderItemMenuResult, Addable

    /** 図形の追加 */
    data object Shape : AddRenderItemMenuResult, Addable

    /** フォトピッカーで選んだ画像の追加 */
    data class Image(val uri: Uri) : AddRenderItemMenuResult, Addable

    /** フォトピッカーで選んだ動画の追加 */
    data class Video(val uri: Uri) : AddRenderItemMenuResult, Addable

    /** ファイルピッカーで選んだ音声の追加 */
    data class Audio(val uri: Uri) : AddRenderItemMenuResult, Addable

    /** あかりんくの開始。こいつは別のボトムシートを出す必要があるので [BottomSheetOpenable] を継承してます */
    data object AkaLink : AddRenderItemMenuResult, BottomSheetOpenable {
        override val bottomSheet: VideoEditorBottomSheetRouteRequestData
            get() = VideoEditorBottomSheetRouteRequestData.OpenAkaLink
    }
}

/**
 * タイムラインにアイテム（テキスト、画像）を追加するボタンが2箇所あるので、まとめた。以下2つの箇所から使われる。
 * [io.github.takusan23.akaridroid.ui.bottomsheet.AddRenderItemBottomSheet]、[FloatingAddRenderItemBar]
 *
 * 基本的には [RenderItemCreator.create] を使って呼び出して [onResult] で受け取る。
 *
 * @param onResult 結果
 */
@Composable
fun rememberRenderItemCreator(onResult: (AddRenderItemMenuResult) -> Unit): RenderItemCreator {
    val creator = remember { RenderItemCreator(onResult) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.also { creator.receiveFile(uri) } }
    )
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.also { creator.receiveFile(uri) } }
    )

    LaunchedEffect(key1 = Unit) {
        creator.setRequestCallback(
            onStartPhotoPicker = { isImageOnly ->
                photoPicker.launch(PickVisualMediaRequest(if (isImageOnly) ActivityResultContracts.PickVisualMedia.ImageOnly else ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onStartFilePicker = { mimeType ->
                filePicker.launch(arrayOf(mimeType))
            }
        )
    }

    return creator
}

/** 詳しくは [rememberRenderItemCreator] で */
@Stable
class RenderItemCreator(private val onResult: (AddRenderItemMenuResult) -> Unit) {

    private var onStartPhotoPicker: ((isImageOnly: Boolean) -> Unit)? = null
    private var onStartFilePicker: ((mimeType: String) -> Unit)? = null

    /** [create] で何を呼び出したか */
    private var latestMenu: AddRenderItemMenu? = null

    /** Activity Result API の用意ができたら呼び出す */
    /* private */ fun setRequestCallback(
        onStartPhotoPicker: (isImageOnly: Boolean) -> Unit,
        onStartFilePicker: (mimeType: String) -> Unit
    ) {
        this@RenderItemCreator.onStartPhotoPicker = onStartPhotoPicker
        this@RenderItemCreator.onStartFilePicker = onStartFilePicker
    }

    /** フォトピッカーや　StorageAccessFramework のコールバックで呼び出す */
    /* private */ fun receiveFile(uri: Uri?) {
        // 何もしないで戻ってきたら uri が null
        if (uri == null) {
            latestMenu = null
            return
        }

        val result = when (latestMenu) {
            AddRenderItemMenu.Text -> null
            AddRenderItemMenu.Image -> AddRenderItemMenuResult.Image(uri)
            AddRenderItemMenu.Video -> AddRenderItemMenuResult.Video(uri)
            AddRenderItemMenu.Audio -> AddRenderItemMenuResult.Audio(uri)
            AddRenderItemMenu.Shape -> null
            AddRenderItemMenu.AkaLink -> null
            null -> null
        } ?: return
        onResult(result)
    }

    /** RenderItem の作成をする */
    fun create(menu: AddRenderItemMenu) {
        // 後で使うので
        latestMenu = menu

        // テキストとかは即時返せる。画像とかは選ばないといけないので
        when (menu) {
            AddRenderItemMenu.Text -> onResult(AddRenderItemMenuResult.Text)
            AddRenderItemMenu.Image -> onStartPhotoPicker?.invoke(true)
            AddRenderItemMenu.Video -> onStartPhotoPicker?.invoke(false)
            AddRenderItemMenu.Audio -> onStartFilePicker?.invoke("audio/*")
            AddRenderItemMenu.Shape -> onResult(AddRenderItemMenuResult.Shape)
            AddRenderItemMenu.AkaLink -> onResult(AddRenderItemMenuResult.AkaLink)
        }
    }

}