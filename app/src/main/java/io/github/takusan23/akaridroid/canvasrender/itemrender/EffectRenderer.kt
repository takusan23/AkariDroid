package io.github.takusan23.akaridroid.canvasrender.itemrender

import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaridroid.RenderData

/** エフェクトをかける。モザイクとか、モノクロとか、2値化とか */
class EffectRenderer(private val effect: RenderData.CanvasItem.Effect) : BaseShaderRenderer() {

    override val layerIndex: Int
        get() = effect.layerIndex

    override val fragmentShader: String
        get() = when (effect.effectType) {
            RenderData.CanvasItem.Effect.EffectType.MOSAIC -> FRAGMENT_SHADER_MOSAIC
            RenderData.CanvasItem.Effect.EffectType.MONOCHROME -> FRAGMENT_SHADER_MONOCHROME
            RenderData.CanvasItem.Effect.EffectType.THRESHOLD -> FRAGMENT_SHADER_THRESHOLD
            RenderData.CanvasItem.Effect.EffectType.BLUR -> FRAGMENT_SHADER_BLUR
        }

    override val size: RenderData.Size
        get() = effect.size

    override val position: RenderData.Position
        get() = effect.position

    override val displayTime: RenderData.DisplayTime
        get() = effect.displayTime

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return effect == renderItem
    }

    companion object {

        /**
         * モザイク
         * https://qiita.com/edo_m18/items/d166653ac0dccbc607dc
         *
         * uniform 変数は[GpuShaderImageProcessor]参照。
         */
        const val FRAGMENT_SHADER_MOSAIC = """#version 100
precision mediump float;

uniform sampler2D sVideoFrameTexture;
uniform vec2 vResolution;
uniform vec4 vCropLocation;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / vResolution.xy;
    // 範囲内
    if (vCropLocation[0] < uv.x && vCropLocation[1] > uv.x && vCropLocation[2] < uv.y && vCropLocation[3] > uv.y) {
        // モザイクしてみる
        uv = floor(uv * 15.0) / 15.0;
    }
    // 色を出す
    vec4 color = texture2D(sVideoFrameTexture, uv);
    gl_FragColor = color;
}
"""

        /** おやすみモノクローム */
        private const val FRAGMENT_SHADER_MONOCHROME = """#version 100
precision mediump float;

uniform sampler2D sVideoFrameTexture;
uniform vec2 vResolution;
uniform vec4 vCropLocation;

// 3.0 で割るだと、あんまりきれいなモノクロにならない
// monochromeScale を 3.0 の代わりに使う
// https://wgld.org/d/webgl/w053.html
const float redScale = 0.298912;
const float greenScale = 0.586611;
const float blueScale = 0.114478;
const vec3 monochromeScale = vec3(redScale, greenScale, blueScale);

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / vResolution.xy;
    // 範囲内
    if (vCropLocation[0] < uv.x && vCropLocation[1] > uv.x && vCropLocation[2] < uv.y && vCropLocation[3] > uv.y) {
        // 色を出す
        vec4 color = texture2D(sVideoFrameTexture, uv);
        // 対象ピクセルのRGB値を加算
        float sum = dot(color.rgb, monochromeScale);
        gl_FragColor = vec4(vec3(sum), color.a);
    } else {
        gl_FragColor = texture2D(sVideoFrameTexture, uv);
    }
}
"""

        /** 2値化 */
        private const val FRAGMENT_SHADER_THRESHOLD = """#version 100
precision mediump float;

uniform sampler2D sVideoFrameTexture;
uniform vec2 vResolution;
uniform vec4 vCropLocation;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / vResolution.xy;
    // 範囲内
    if (vCropLocation[0] < uv.x && vCropLocation[1] > uv.x && vCropLocation[2] < uv.y && vCropLocation[3] > uv.y) {
        // 色を出す
        vec4 color = texture2D(sVideoFrameTexture, uv);
        // rgb の平均を出す
        float avg = (color.r + color.g + color.b) / 3.0;
        // 0.5 以上と以下で色を分ける
        if (avg <= 0.5) {
            gl_FragColor = vec4(vec3(1.0), 1.0);
        } else {
            gl_FragColor = vec4(vec3(0.0), 1.0);
        }
    } else {
        gl_FragColor = texture2D(sVideoFrameTexture, uv);
    }
}
"""

        /**
         * ぼかし
         * thx!!!!!!!!
         * https://github.com/GameMakerDiscord/blur-shaders
         */
        private const val FRAGMENT_SHADER_BLUR = """#version 100
precision mediump float;

uniform sampler2D sVideoFrameTexture;
uniform vec2 vResolution;
uniform vec4 vCropLocation;

const int Quality = 8;
const int Directions = 16;
const float Pi = 6.28318530718; //pi * 2
const float Radius = 32.0; // ぼかし具合

void main()
{
    vec2 v_vTexcoord = gl_FragCoord.xy / vResolution.xy;
    
    // 範囲内
    if (vCropLocation[0] < v_vTexcoord.x && vCropLocation[1] > v_vTexcoord.x && vCropLocation[2] < v_vTexcoord.y && vCropLocation[3] > v_vTexcoord.y) {
        vec2 radius = Radius / vResolution.xy;
        vec4 Color = texture2D( sVideoFrameTexture, v_vTexcoord);
        
        for( float d=0.0;d<Pi;d+=Pi/float(Directions) )
        {
            for( float i=1.0/float(Quality);i<=1.0;i+=1.0/float(Quality) )
            {
                Color += texture2D( sVideoFrameTexture, v_vTexcoord+vec2(cos(d),sin(d))*radius*i);
            }
        }
        Color /= float(Quality)*float(Directions)+1.0;
        gl_FragColor = Color;
    } else {
        gl_FragColor = texture2D(sVideoFrameTexture, v_vTexcoord);
    }
}
"""

    }
}