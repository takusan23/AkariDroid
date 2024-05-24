package io.github.takusan23.akaridroid.canvasrender.itemrender

import io.github.takusan23.akaridroid.RenderData

/**
 * 各動画フレームにフラグメントシェーダーで切り替えアニメーションを付ける。
 * フェードアウトとか。
 * 詳しくは[BaseShaderRender]で。
 */
class SwitchAnimationRender(
    private val switchAnimation: RenderData.CanvasItem.SwitchAnimation
) : BaseShaderRender() {
    override val layerIndex: Int
        get() = switchAnimation.layerIndex

    override val fragmentShader: String
        get() = when (switchAnimation.type) {
            RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.FADE_IN_OUT -> FRAGMENT_SHADER_FADE_IN_OUT
            RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.SLIDE -> FRAGMENT_SHADER_SLIDE
            RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.BLUR -> FRAGMENT_SHADER_BLUR
        }

    override val size: RenderData.Size
        get() = switchAnimation.size

    override val position: RenderData.Position
        get() = switchAnimation.position

    override val displayTime: RenderData.DisplayTime
        get() = switchAnimation.displayTime

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return switchAnimation == renderItem
    }

    // シェーダー集
    companion object {

// フラグメントシェーダーのテンプレ
        /*
precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;
uniform float f_time;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / v_resolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    // 色を出す
    vec4 color = texture2D(s_texture, uv);
    gl_FragColor = color;
}
         */

        /** フェードアウトしてフェードインするやつ */
        private const val FRAGMENT_SHADER_FADE_IN_OUT = """
precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;
uniform float f_time;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / v_resolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    // 色を出す
    vec4 color = texture2D(s_texture, uv);
    
    // f_time を 2 倍することで、0から2までの範囲にする
    // 0.5 までは、2倍した値を 1 から引けば 0.8  ... 0.0 となっていく
    // それ以降は、 -0.2 ... -1.0 とかになるので、abs で絶対値にする
    
    // 0.0 ~ 1.0 を、0.0 ~ 0.1 にしたあと、1.0 ~ 0.0 にするやつ
    // 0.5 で 1.0 になって、それ以降は減っていく
    float timeOutIn = abs(1.0 - (f_time * 2.0));
    color.rgb *= timeOutIn;
    
    gl_FragColor = color;
}
"""

        /** スライドするやつ */
        private const val FRAGMENT_SHADER_SLIDE = """precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;
uniform float f_time;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / v_resolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    
    if (uv.x < f_time) {
        // スライドせずに残るやつ
        vec4 color = texture2D(s_texture, uv);
        gl_FragColor = color;
    } else {
        // スライドするやつ。ずらす
        uv.x -= f_time;
        vec4 color = texture2D(s_texture, uv);
        gl_FragColor = color;
    }  
}
"""

        /**
         * ぼかし
         * thx!!!!!!!!
         * https://github.com/GameMakerDiscord/blur-shaders
         */
        private const val FRAGMENT_SHADER_BLUR = """precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;
uniform float f_time;

const int Quality = 8;
const int Directions = 16;
const float Pi = 6.28318530718; //pi * 2
const float Radius = 16.0; // ぼかし具合

void main()
{
    // 0.0 ~ 1.0 を、0.0 ~ 0.1 にしたあと、1.0 ~ 0.0 にするやつ
    // 0.5 で 1.0 になって、それ以降は減っていく
    float timeOutIn = abs(1.0 - (f_time * 2.0));

    // const Radius を時間で変化させる
    // わかりにくいので 2 倍する
    float radiusTime = Radius * ((1.0 - timeOutIn) * 2.0);

    vec2 v_vTexcoord = gl_FragCoord.xy / v_resolution.xy;
    v_vTexcoord = vec2(v_vTexcoord.x, 1.-v_vTexcoord.y);
    
    vec2 radius = radiusTime / v_resolution.xy;
    vec4 Color = texture2D( s_texture, v_vTexcoord);
    
    for( float d=0.0;d<Pi;d+=Pi/float(Directions) )
    {
        for( float i=1.0/float(Quality);i<=1.0;i+=1.0/float(Quality) )
        {
            Color += texture2D( s_texture, v_vTexcoord+vec2(cos(d),sin(d))*radius*i);
        }
    }
    Color /= float(Quality)*float(Directions)+1.0;
    gl_FragColor = Color;
}
"""

    }

}