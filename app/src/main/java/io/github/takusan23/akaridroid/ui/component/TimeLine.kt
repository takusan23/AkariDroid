package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.groupByLane
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * タイムラインの拡大縮小用
 *
 * @param msWidthPx 1 ミリ秒をどれだけの幅で表すか
 */
@JvmInline
private value class TimeLineMillisecondsWidthPx(val msWidthPx: Int) {

    /** 1 ミリ秒をどれだけの幅で表すか。[dp]版 */
    val Long.msToWidthDp: Dp
        // Pixel to Dp
        @Composable
        get() = with(LocalDensity.current) { msToWidth.toDp() }

    /** 1 ミリ秒をどれだけの幅で表すか。[Int]版。 */
    val Long.msToWidth: Int
        get() = (this / msWidthPx).toInt()

    /** 幅や位置は何 ミリ秒を表しているか */
    val Int.widthToMs: Long
        get() = (this * msWidthPx).toLong()

    /** 幅や位置は何 ミリ秒を表しているか */
    val Float.widthToMs: Long
        get() = (this * msWidthPx).toLong()

}

/** タイムラインの拡大、縮小を CompositionLocal で提供する、バケツリレーを回避する */
private val LocalTimeLineMillisecondsWidthPx = compositionLocalOf { TimeLineMillisecondsWidthPx(msWidthPx = 20) }

/**
 * タイムラインのアイテム移動中に、隣接するアイテムの隣に置きたい場合に、磁石のように勝手にくっつく機能がある。
 * 磁石モード。これのしきい値
 */
private const val MAGNET_THRESHOLD_MOVE = 50

/** 磁石モード。長さ調整版。[MAGNET_THRESHOLD_MOVE]よりも大きくしないとズレて使いにくかった */
private const val MAGNET_THRESHOLD_DURATION_CHANGE = 200

/**
 * 磁石モードのくっつく位置と ID
 *
 * @param id [TimeLineData.Item.id]と同じ
 * @param positionMs くっつける位置
 */
private data class MagnetPosition(
    val id: Long,
    val positionMs: Long
)

/**
 * タイムラインのアイテムが移動したときに貰えるデータ
 * [startRect]、[stopRect]はレーンの判定だけに使ってください。時間をこの[IntRect]から出すと多分正確じゃない（丸められる）
 *
 * @param id [TimeLineData.Item.id]と同じ
 * @param startRect 移動前の位置
 * @param stopRect 移動後の位置
 * @param positionMs 移動後の再生開始位置
 */
private data class TimeLineItemComponentDragAndDropData(
    val id: Long,
    val startRect: IntRect,
    val stopRect: IntRect,
    val positionMs: Long
)

/**
 * タイムライン
 *
 * @param msWidthPx 1 ミリ秒をどれだけの幅で表すか
 * @param currentPositionMs プレビューの再生位置。ラムダで値を取るため遅延読み取りが可能です。これにより、高速に値が変化しても再コンポジションされません。
 * @param durationMs 動画の時間
 */
@Composable
fun TimeLine(
    modifier: Modifier = Modifier,
    timeLineData: TimeLineData = TimeLineData(
        durationMs = 30_000,
        laneCount = 5,
        itemList = listOf(
            TimeLineData.Item(id = 1, laneIndex = 0, startMs = 0, stopMs = 10_000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 2, laneIndex = 0, startMs = 10_000, stopMs = 20_000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 3, laneIndex = 1, startMs = 1000, stopMs = 2000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 4, laneIndex = 2, startMs = 0, stopMs = 2000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 5, laneIndex = 3, startMs = 1000, stopMs = 1500, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 6, laneIndex = 4, startMs = 10_000, stopMs = 11_000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
        )
    ),
    currentPositionMs: () -> Long,
    durationMs: Long,
    msWidthPx: Int,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Boolean,
    onSeek: (positionMs: Long) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit, // TODO これ TimeLineData.Item 全部のパラメーターは要らないわ。
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    // タイムラインの拡大縮小
    val millisecondsWidthPx = remember(msWidthPx) { TimeLineMillisecondsWidthPx(msWidthPx) }

    // はみ出しているタイムラインの LayoutCoordinates
    // タイムラインのレーンや、タイムラインのアイテムの座標を出すのに必要
    val timelineScrollableAreaCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    // millisecondsWidthPx を LocalTimeLineMillisecondsWidthPx で提供する
    CompositionLocalProvider(value = LocalTimeLineMillisecondsWidthPx provides millisecondsWidthPx) {
        Box(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {

            // 横に長ーいタイムラインを作る
            // 画面外にはみ出すので requiredSize
            RequiredSizeTimeLine(
                modifier = Modifier
                    // タイムラインにあるアイテムの座標出すのに使う
                    .onGloballyPositioned { timelineScrollableAreaCoordinates.value = it },
                timeLineData = timeLineData,
                onSeek = onSeek,
                timeLineScrollableAreaCoordinates = timelineScrollableAreaCoordinates.value,
                currentPositionMs = currentPositionMs,
                onEdit = onEdit,
                onCut = onCut,
                onDelete = onDelete,
                onDuplicate = onDuplicate,
                onDurationChange = onDurationChange,
                onDragAndDropRequest = onDragAndDropRequest,
            )

            // タイムラインの縦の棒。タイムラインに重ねて使う
            // matchParentSize で親 Box の大きさに合わせる
            OverlayTimeLineComponents(
                modifier = Modifier.matchParentSize(),
                durationMs = { durationMs },
                currentPositionMs = currentPositionMs
            )
        }
    }
}

/**
 * タイムラインの縦の棒。タイムラインに重ねるコンポーネント
 *
 * @param modifier [Modifier]
 * @param durationMs 動画時間
 * @param currentPositionMs 再生位置
 */
@Composable
private fun OverlayTimeLineComponents(
    modifier: Modifier = Modifier,
    durationMs: () -> Long,
    currentPositionMs: () -> Long
) {
    Box(modifier) {
        // 動画の長さ
        TimeLinePositionBar(
            modifier = Modifier.fillMaxHeight(),
            color = Color.Blue,
            positionMs = durationMs
        )

        // 再生位置
        TimeLinePositionBar(
            modifier = Modifier.fillMaxHeight(),
            color = Color.Red,
            positionMs = currentPositionMs
        )
    }
}

/**
 * 必要なサイズ分あるタイムライン。
 * 横と縦に長いので、親はスクロールできるようにする必要があります。
 *
 * @param modifier [Modifier]
 * @param timeLineData タイムラインのデータ
 * @param onSeek シークがリクエストされた
 * @param timeLineScrollableAreaCoordinates このコンポーネントのサイズ
 * @param currentPositionMs 再生位置
 * @param onDragAndDropRequest [TimeLineItemComponentDragAndDropData]参照。ドラッグアンドドロップで移動先に移動してよいかを返します
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDuplicate メニューで複製を押した
 * @param onDurationChange 長さ調整がリクエストされた
 */
@Composable
private fun RequiredSizeTimeLine(
    modifier: Modifier = Modifier,
    timeLineData: TimeLineData,
    onSeek: (positionMs: Long) -> Unit,
    timeLineScrollableAreaCoordinates: LayoutCoordinates?,
    currentPositionMs: () -> Long,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Boolean
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current

    // 一番遅い時間
    val maxDurationMs = remember(timeLineData) { maxOf(timeLineData.itemList.maxOfOrNull { it.stopMs } ?: 0, timeLineData.durationMs) }
    // レーンコンポーネントの位置と番号
    val timeLineLaneIndexRectMap = remember { mutableStateMapOf<Int, IntRect>() }
    // レーンとレーンのアイテムの Map
    val laneItemMap = remember(timeLineData) { timeLineData.groupByLane() }
    // 磁石モード用に、くっつく位置。最初と最後
    // +1 は、例えば2秒で終わった後に2秒に始めると、重なってしまうため
    val magnetPositionList = remember(timeLineData) {
        timeLineData.itemList
            .map { item -> listOf(MagnetPosition(item.id, item.startMs), MagnetPosition(item.id, item.stopMs + 1)) }
            .flatten()
    }

    Column(
        modifier = modifier
            .requiredWidth(with(msWidthPx) { maxDurationMs.msToWidthDp })
            // 再生位置の移動。タイムラインの棒を移動させる
            .pointerInput(msWidthPx) {
                detectTapGestures {
                    val seekMs = with(msWidthPx) { it.x.toInt().widthToMs }
                    onSeek(seekMs)
                }
            },
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        // 時間を表示するやつ
        TimeLineTopTimeLabel(
            durationMs = maxDurationMs,
            // TODO もっといい感じにする
            stepMs = if (10 < msWidthPx.msWidthPx) {
                10_000
            } else {
                2_000
            }
        )

        HorizontalDivider()

        if (timeLineScrollableAreaCoordinates != null) {

            // タイムラインのアイテム
            // レーンの数だけ
            laneItemMap.forEach { (laneIndex, itemList) ->
                TimeLineLane(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .onGloballyPositioned {
                            timeLineLaneIndexRectMap[laneIndex] = timeLineScrollableAreaCoordinates
                                .localBoundingBoxOf(it)
                                .roundToIntRect()
                        },
                    laneIndex = laneIndex,
                    laneItemList = itemList,
                    currentPositionMs = currentPositionMs,
                    onEdit = onEdit,
                    onCut = onCut,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onDurationChange = onDurationChange,
                    timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                    magnetPositionList = magnetPositionList,
                    onDragAndDropRequest = { dragAndDropData ->
                        val (id, start, stop, positionMs) = dragAndDropData
                        // ドラッグアンドドロップで移動先に移動してよいか
                        // 移動元、移動先のレーン番号を取得
                        val fromLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(start.center) }.keys.firstOrNull() ?: return@TimeLineLane false
                        val toLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(stop.center) }.keys.firstOrNull() ?: return@TimeLineLane false

                        // 移動先の TimeLineItemData を作る
                        val request = TimeLineData.DragAndDropRequest(
                            id = id,
                            dragAndDroppedStartMs = positionMs,
                            dragAndDroppedLaneIndex = toLaneIndex
                        )

                        // 渡して処理させる
                        onDragAndDropRequest(request)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * タイムラインのレーンです。回転寿司。
 * Box にしてオフセットで調整しています。
 *
 * 画面外にも描画するのでなんとかしたい所存。
 *
 * @param modifier [Modifier]
 * @param laneIndex レーン番号
 * @param laneItemList タイムラインに表示するアイテムの配列[TimeLineData.Item]
 * @param currentPositionMs 現在の再生位置（赤いバーがある位置）
 * @param timeLineScrollableAreaCoordinates 横に長いタイムラインを表示しているコンポーネントの[LayoutCoordinates]
 * @param onDragAndDropRequest [TimeLineItemComponentDragAndDropData]参照。ドラッグアンドドロップで移動先に移動してよいかを返します
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDuplicate メニューで複製を押した
 * @param onDurationChange 長さ調整がリクエストされた
 */
@Composable
private fun TimeLineLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineData.Item>,
    laneIndex: Int,
    currentPositionMs: () -> Long,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    magnetPositionList: List<MagnetPosition>,
    onDragAndDropRequest: (TimeLineItemComponentDragAndDropData) -> Boolean = { false },
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    Box(modifier = modifier) {

        Text(
            text = (laneIndex + 1).toString(),
            modifier = Modifier
                .padding(start = 10.dp)
                .alpha(0.5f)
                .align(Alignment.CenterStart),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )

        laneItemList.forEach { timeLineItemData ->
            TimeLineItem(
                timeLineItemData = timeLineItemData,
                currentPositionMs = currentPositionMs,
                onDragAndDropRequest = onDragAndDropRequest,
                timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                magnetPositionList = magnetPositionList,
                onEdit = { onEdit(timeLineItemData) },
                onCut = { onCut(timeLineItemData) },
                onDelete = { onDelete(timeLineItemData) },
                onDuplicate = { onDuplicate(timeLineItemData) },
                onDurationChange = onDurationChange
            )
        }
    }
}

/**
 * タイムラインに表示するアイテム
 *
 * @param modifier [Modifier]
 * @param timeLineItemData タイムラインのデータ[TimeLineData.Item]
 * @param currentPositionMs 現在の再生位置（赤いバーがある位置）
 * @param timeLineScrollableAreaCoordinates 横に長いタイムラインを表示しているコンポーネントの[LayoutCoordinates]
 * @param magnetPositionList [MagnetPosition]の配列。
 * @param onDragAndDropRequest ドラッグアンドドロップで指を離したら呼ばれます。引数は[TimeLineItemComponentDragAndDropData]参照。返り値はドラッグアンドドロップが成功したかです。移動先が空いていない等は false
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete 削除を押した
 * @param onDuplicate 複製を押した
 * @param onDurationChange 長さ調整がリクエストされた。長さ調整つまみを離したら呼ばれる。
 */
@Composable
private fun TimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineData.Item,
    currentPositionMs: () -> Long,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    magnetPositionList: List<MagnetPosition>,
    onDragAndDropRequest: (TimeLineItemComponentDragAndDropData) -> Boolean = { false },
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    // 移動開始、磁石モード発動時にフィードバックを
    val haptic = LocalHapticFeedback.current
    // 拡大縮小
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
    // アイテムが移動中かどうか
    val isDragging = remember { mutableStateOf(false) }
    // アイテムの位置
    val latestGlobalRect = remember { mutableStateOf<IntRect?>(null) }
    // アイテムの移動中位置。アイテムが変化したら作り直す
    val draggingOffset = remember(timeLineItemData, msWidthPx) {
        mutableStateOf(IntOffset(with(msWidthPx) { timeLineItemData.startMs.msToWidth }, 0))
    }
    // メニューを表示するか
    val isVisibleMenu = remember { mutableStateOf(false) }
    // タイムラインのアイテムの表示時間。長さ調整出来るように State で持っている。
    val durationMs = remember(timeLineItemData) { mutableLongStateOf(timeLineItemData.durationMs) }
    // 磁石モード用に、くっつける位置。
    // 自分自身は除く必要あり。あと再生位置も欲しい
    val magnetPositionMsList = remember(magnetPositionList, currentPositionMs()) {
        magnetPositionList
            .filter { it.id != timeLineItemData.id }
            .map { it.positionMs } + currentPositionMs()
    }

    Surface(
        modifier = modifier
            .offset {
                // 移動中の場合はそのオフセット
                // 移動中じゃない場合は再生開始位置分オフセットでずらす
                if (isDragging.value) {
                    draggingOffset.value
                } else {
                    IntOffset(with(msWidthPx) { timeLineItemData.startMs.msToWidth }, 0)
                }
            }
            .width(with(msWidthPx) { durationMs.longValue.msToWidthDp })
            .fillMaxHeight()
            .onGloballyPositioned {
                // timeLineScrollableAreaCoordinates の理由は TimeLine() コンポーネント参照
                latestGlobalRect.value = timeLineScrollableAreaCoordinates
                    .localBoundingBoxOf(it)
                    .roundToIntRect()
            }
            .pointerInput(timeLineItemData, msWidthPx, magnetPositionMsList) {
                // アイテム移動のシステム
                // データが変化したら pointerInput も再起動するようにキーに入れる
                var startRect: IntRect? = null
                // LocalTimeLineMillisecondsWidthPx を経由すると、おそらく時間の細かい部分が丸められてしまう
                // これがドラッグアンドドロップなら問題ないが、磁石モードの場合、細かい部分が丸められた結果、ギリギリ重ならないのに移動が却下される可能性がある
                // それを回避するため、onDrag = { } の段階で変数に格納する
                var latestMagnetPositionMs: Long? = null

                detectDragGestures(
                    onDragStart = {
                        // ドラッグアンドドロップ開始時。フラグを立てて開始位置を入れておく
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isDragging.value = true
                        startRect = latestGlobalRect.value
                    },
                    onDrag = { change, dragAmount ->
                        // 移動中
                        change.consume()
                        val x = (draggingOffset.value.x + dragAmount.x).toInt()
                        val y = (draggingOffset.value.y + dragAmount.y).toInt()

                        // 磁石モード発動するか
                        val magnetPositionMsOrNull = magnetPositionMsList.firstOrNull { magnetPositionMs -> abs(magnetPositionMs - with(msWidthPx) { x.widthToMs }) < MAGNET_THRESHOLD_MOVE }
                        if (magnetPositionMsOrNull != null) {
                            // 差があるときだけにして連続対策
                            if (latestMagnetPositionMs != magnetPositionMsOrNull) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            draggingOffset.value = IntOffset(with(msWidthPx) { magnetPositionMsOrNull.msToWidth }, y)
                            latestMagnetPositionMs = magnetPositionMsOrNull
                        } else {
                            draggingOffset.value = IntOffset(x, y)
                            latestMagnetPositionMs = null
                        }
                    },
                    onDragEnd = {
                        // 移動終了
                        isDragging.value = false
                        // ドラッグアンドドロップが終わった後の位置に対応する時間を出す
                        // 磁石モードでくっついた場合は latestMagnetPositionMs が Null 以外
                        val nonnullLatestGlobalRect = latestGlobalRect.value ?: return@detectDragGestures
                        val stopMsInDroppedPos = latestMagnetPositionMs ?: with(msWidthPx) { nonnullLatestGlobalRect.left.widthToMs }
                        // 移動できるか判定を上のコンポーネントでやる
                        val isAccept = onDragAndDropRequest(
                            TimeLineItemComponentDragAndDropData(
                                id = timeLineItemData.id,
                                startRect = startRect ?: return@detectDragGestures,
                                stopRect = nonnullLatestGlobalRect,
                                positionMs = stopMsInDroppedPos
                            )
                        )
                        // 出来ない場合は戻す
                        if (!isAccept) {
                            draggingOffset.value = IntOffset(with(msWidthPx) { timeLineItemData.startMs.msToWidth }, 0)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        onClick = { isVisibleMenu.value = true }
    ) {
        Box(modifier = Modifier.height(IntrinsicSize.Max)) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    modifier = Modifier.padding(5.dp),
                    painter = painterResource(id = timeLineItemData.iconResId),
                    contentDescription = null
                )
                Text(
                    text = timeLineItemData.label,
                    maxLines = 1
                )
            }

            // 長さ調整できる場合は、それ用のつまみを出す
            if (timeLineItemData.isChangeDuration) {
                DurationChangeHandle(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .pointerInput(timeLineItemData, msWidthPx, magnetPositionMsList) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // 動かした位置に対応した時間を出す
                                    val widthToMs = durationMs.longValue + with(msWidthPx) { dragAmount.x.widthToMs }
                                    // 磁石モード。開始位置を加味して探す必要がある
                                    // startMs を引くことで、durationMs として使えるように
                                    val magnetPositionMsOrNull = magnetPositionMsList
                                        .firstOrNull { magnetPositionMs -> abs(magnetPositionMs - (timeLineItemData.startMs + widthToMs)) < MAGNET_THRESHOLD_DURATION_CHANGE }
                                        ?.let { nonnullLong -> nonnullLong - timeLineItemData.startMs }

                                    // 前回と違うときのみ Haptic
                                    if (magnetPositionMsOrNull != null && durationMs.longValue != magnetPositionMsOrNull) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        durationMs.longValue = magnetPositionMsOrNull
                                    } else {
                                        durationMs.longValue = widthToMs
                                    }
                                },
                                onDragEnd = {
                                    onDurationChange(
                                        TimeLineData.DurationChangeRequest(
                                            id = timeLineItemData.id,
                                            newDurationMs = durationMs.longValue
                                        )
                                    )
                                }
                            )
                        }
                )
            }
        }

        // ドロップダウンメニュー
        // ボトムシートを出すとか
        TimeLineItemContextMenu(
            isVisibleMenu = isVisibleMenu.value,
            isEnableCut = currentPositionMs() in timeLineItemData.timeRange,
            onDismissRequest = { isVisibleMenu.value = false },
            onEdit = onEdit,
            onCut = onCut,
            onDelete = onDelete,
            onDuplicate = onDuplicate
        )
    }
}

/**
 * アイテムを押したときのメニューです。
 * 値の編集、この位置で分割など。
 *
 * @param isVisibleMenu 表示する場合は true
 * @param isEnableCut この位置で分割が利用できるか。赤いバーがタイムラインのアイテムに重なってないのに表示されるのはあれ
 * @param onDismissRequest 非表示にして欲しいときに呼ばれる
 * @param onEdit 値の編集を押した
 * @param onCut 分割を押した
 * @param onDelete 削除を押した
 * @param onDuplicate 複製を押した
 */
@Composable
private fun TimeLineItemContextMenu(
    isVisibleMenu: Boolean,
    isEnableCut: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    DropdownMenu(
        expanded = isVisibleMenu,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_edit)) },
            onClick = {
                onEdit()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_edit_24px), contentDescription = null) }
        )
        if (isEnableCut) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.timeline_context_menu_cut)) },
                onClick = {
                    onCut()
                    onDismissRequest()
                },
                leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_cut_24px), contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_duplicate)) },
            onClick = {
                onDuplicate()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_content_copy_24px), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_delete)) },
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) }
        )
    }
}

/** タイムラインの一番上に表示する時間 */
@Composable
private fun TimeLineTopTimeLabel(
    modifier: Modifier = Modifier,
    durationMs: Long,
    stepMs: Long = 10_000 // 10 秒間隔
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    val labelList = remember(durationMs, stepMs) {
        // 最後まで
        ((0 until durationMs step stepMs) + durationMs).toList()
    }

    Box(modifier = modifier) {
        labelList.forEach { timeMs ->
            val textMeasure = rememberTextMeasurer()
            val timeText = simpleDateFormat.format(timeMs)

            Text(
                modifier = Modifier
                    .offset { IntOffset(-textMeasure.measure(timeText).size.width / 2, 0) }
                    .offset { IntOffset(with(msWidthPx) { timeMs.msToWidth }, 0) },
                text = timeText
            )
        }
    }
}

/**
 * タイムラインで現在位置とか、最後の位置を表示するためのバー
 *
 * @param modifier [Modifier]
 * @param positionMs バーを表示したい位置
 */
@Composable
private fun TimeLinePositionBar(
    modifier: Modifier = Modifier,
    color: Color,
    positionMs: () -> Long
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current

    Box(
        modifier = modifier
            .width(2.dp)
            .offset { IntOffset(with(msWidthPx) { positionMs().msToWidth }, 0) }
            .background(color)
    )
}

/**
 * 長さ調整用のつまみ
 * Modifier に pointerInput いれて使ってね
 *
 * @param modifier [Modifier]
 */
@Composable
private fun DurationChangeHandle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
