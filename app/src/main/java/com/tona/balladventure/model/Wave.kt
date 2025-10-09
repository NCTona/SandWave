package com.tona.balladventure.model

import kotlin.math.PI
import kotlin.random.Random

data class Wave(
    val baseY: Float,
    val baselineAmp: Float,
    val baselineLambda: Float,
    val baselinePhase: Float,
    val mainAmp: Float,
    val mainLambda: Float,
    val mainPhase: Float,
    val subAmp1: Float,
    val subLambda1: Float,
    val subPhase1: Float,
    val subAmp2: Float,
    val subLambda2: Float,
    val subPhase2: Float,
    val minWaveY: Float,
    val maxWaveY: Float,
    val waveGaps: List<Pair<Float, Float>>
) {
    companion object {
        fun random(canvasWidth: Float, canvasHeight: Float): Wave {
            val baseY = canvasHeight / 1.5f
            val baselineAmp = Random.nextFloat() * 40f + 30f
            val baselineLambda = canvasWidth * (Random.nextFloat() + 2.5f)
            val baselinePhase = Random.nextFloat() * (2 * PI).toFloat()

            val mainAmp = Random.nextFloat() * 50f + 60f
            val mainLambda = (canvasWidth / 2f) + Random.nextFloat() * (canvasWidth * 2f / 3f - canvasWidth / 2f)
            val mainPhase = Random.nextFloat() * (2 * PI).toFloat()

            val subAmp1 = mainAmp * (0.35f + Random.nextFloat() * 0.25f)
            val subLambda1 = mainLambda * (0.55f + Random.nextFloat() * 0.25f)
            val subPhase1 = Random.nextFloat() * (2 * PI).toFloat()

            val subAmp2 = mainAmp * (0.20f + Random.nextFloat() * 0.20f)
            val subLambda2 = mainLambda * (0.33f + Random.nextFloat() * 0.20f)
            val subPhase2 = Random.nextFloat() * (2 * PI).toFloat()

            val minWaveY = canvasHeight * 0.4f
            val maxWaveY = canvasHeight * 0.6f

            val waveGaps = buildList {
                var currentX = canvasWidth * 2f
                repeat(5) {
                    val width = Random.nextFloat() * 8000f + 2000f
                    val start = currentX + Random.nextFloat() * 300f
                    val end = start + width
                    add(start to end)
                    currentX = end + canvasWidth + Random.nextFloat() * (canvasWidth * 0.5f)
                }
            }

            return Wave(
                baseY, baselineAmp, baselineLambda, baselinePhase,
                mainAmp, mainLambda, mainPhase,
                subAmp1, subLambda1, subPhase1,
                subAmp2, subLambda2, subPhase2,
                minWaveY, maxWaveY, waveGaps
            )
        }
    }
}
