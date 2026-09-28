package io.github.takusan23.akaridroid.ui.sheet.projectlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.data.RoundedListEndShape
import io.github.takusan23.akaridroid.ui.component.data.RoundedListInnerShape
import io.github.takusan23.akaridroid.ui.component.data.RoundedListTopShape
import io.github.takusan23.akaridroid.ui.sheet.bottomSheetPadding

private const val LandscapeToPortraitBlurUrl = "https://takusan.negitoro.dev/posts/akari_droid_tutorial_video_side_blur/"
private const val ChromakeyUrl = "https://takusan.negitoro.dev/posts/akari_droid_tutorial_chromakey/"
private const val ShortLikeVideoUrl = "https://takusan.negitoro.dev/posts/akari_droid_tutorial_youtube_de_yokumiru_short/"

/**
 * チュートリアルのリンク集ボトムシート
 *
 * @param onOpenUrl URL を開いてほしいときに呼ばれる
 */
@Composable
fun TutorialLinkBottomSheet(onOpenUrl: (String) -> Unit) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        Text(
            text = stringResource(id = R.string.project_list_bottomsheet_tutorial_title),
            color = MaterialTheme.colorScheme.primary
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.project_list_bottomsheet_tutorial_landscape_to_portrait_blur_title),
            description = stringResource(id = R.string.project_list_bottomsheet_tutorial_landscape_to_portrait_blur_description),
            iconResId = R.drawable.open_in_browser_24px,
            shape = RoundedListTopShape,
            onClick = { onOpenUrl(LandscapeToPortraitBlurUrl) }
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.project_list_bottomsheet_tutorial_chromakey_title),
            description = stringResource(id = R.string.project_list_bottomsheet_tutorial_chromakey_description),
            iconResId = R.drawable.open_in_browser_24px,
            shape = RoundedListInnerShape,
            onClick = { onOpenUrl(ChromakeyUrl) }
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.project_list_bottomsheet_tutorial_short_like_video_title),
            description = stringResource(id = R.string.project_list_bottomsheet_tutorial_short_like_video_description),
            iconResId = R.drawable.open_in_browser_24px,
            shape = RoundedListEndShape,
            onClick = { onOpenUrl(ShortLikeVideoUrl) }
        )
    }
}