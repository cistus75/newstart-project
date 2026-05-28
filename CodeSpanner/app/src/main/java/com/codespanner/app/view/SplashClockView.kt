package com.codespanner.app.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SplashClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var handAngle = -60f  // 시작 각도 (참고 이미지 기준)

    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG)

    private val paintOuterRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#4A5A10")
        strokeWidth = 3f
    }

    private val paintInnerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E2C00")
    }

    private val paintInnerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3A4A08")
        strokeWidth = 2f
    }

    private val paintTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3A4A08")
        strokeWidth = 1.5f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#C8FF00")
    }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener {
            handAngle = it.animatedValue as Float - 90f
            invalidate()
        }
    }

    init {
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val size = min(cx, cy)

        val outerRadius = size * 0.92f
        val innerRadius = size * 0.68f
        val dotRadius = size * 0.08f

        // 방사형 글로우
        paintGlow.shader = RadialGradient(
            cx, cy, outerRadius * 1.4f,
            intArrayOf(Color.parseColor("#28506000"), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, outerRadius * 1.4f, paintGlow)

        // 외부 링
        canvas.drawCircle(cx, cy, outerRadius, paintOuterRing)

        // 내부 채운 원
        canvas.drawCircle(cx, cy, innerRadius, paintInnerFill)
        canvas.drawCircle(cx, cy, innerRadius, paintInnerRing)

        // 눈금 (12개)
        for (i in 0 until 12) {
            val tickAngle = Math.toRadians((i * 30 - 90).toDouble())
            val tickOuter = innerRadius * 0.92f
            val tickInner = innerRadius * 0.75f
            canvas.drawLine(
                cx + (tickOuter * cos(tickAngle)).toFloat(),
                cy + (tickOuter * sin(tickAngle)).toFloat(),
                cx + (tickInner * cos(tickAngle)).toFloat(),
                cy + (tickInner * sin(tickAngle)).toFloat(),
                paintTick
            )
        }

        // 시계 바늘
        val handLength = innerRadius * 0.82f
        val rad = Math.toRadians(handAngle.toDouble())
        canvas.drawLine(
            cx, cy,
            cx + (handLength * cos(rad)).toFloat(),
            cy + (handLength * sin(rad)).toFloat(),
            paintHand
        )

        // 중심 점 (accent color)
        canvas.drawCircle(cx, cy, dotRadius, paintDot)
    }
}
