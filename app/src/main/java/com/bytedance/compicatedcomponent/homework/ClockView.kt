package com.bytedance.compicatedcomponent.homework

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 *  author : neo
 *  time   : 2021/10/25
 *  desc   :
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val FULL_ANGLE = 360

        private const val CUSTOM_ALPHA = 140
        private const val FULL_ALPHA = 255

        private const val POINTER_TYPE_SECOND = 2
        private const val POINTER_TYPE_MINUTES = 1
        private const val POINTER_TYPE_HOURS = 0

        private const val DEFAULT_PRIMARY_COLOR: Int = Color.WHITE
        private const val DEFAULT_SECONDARY_COLOR: Int = Color.LTGRAY

        private const val DEFAULT_DEGREE_STROKE_WIDTH = 0.010f

        private const val RIGHT_ANGLE = 90

        private const val UNIT_DEGREE = (6 * Math.PI / 180).toFloat() // 一个小格的度数

        private const val SWITCH_HOUR = 0
        private const val SWITCH_MINUTE = 1
        private const val SWITCH_SECOND = 2
    }

    private var panelRadius = 200.0f // 表盘半径

    private var hourPointerLength = 0f // 指针长度

    private var minutePointerLength = 0f
    private var secondPointerLength = 0f

    private var resultWidth = 0
    private  var centerX: Int = 0
    private  var centerY: Int = 0
    private  var radius: Int = 0

    private var degreesColor = 0

    private val needlePaint: Paint
    private val canvas: Canvas
    init {
        degreesColor = DEFAULT_PRIMARY_COLOR
        needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        needlePaint.style = Paint.Style.FILL_AND_STROKE
        needlePaint.strokeCap = Paint.Cap.ROUND
        canvas = Canvas()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size: Int
        val width = measuredWidth
        val height = measuredHeight
        val widthWithoutPadding = width - paddingLeft - paddingRight
        val heightWithoutPadding = height - paddingTop - paddingBottom
        size = if (widthWithoutPadding > heightWithoutPadding) {
            heightWithoutPadding
        } else {
            widthWithoutPadding
        }
        setMeasuredDimension(size + paddingLeft + paddingRight, size + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        resultWidth = if (height > width) width else height
        val halfWidth = resultWidth / 2
        centerX = halfWidth
        centerY = halfWidth
        radius = halfWidth
        panelRadius = radius.toFloat()
        hourPointerLength = panelRadius - 400
        minutePointerLength = panelRadius - 250
        secondPointerLength = panelRadius - 150
        drawDegrees(canvas)
        drawHoursValues(canvas)
        drawNeedles(canvas)

        // todo 1: 每一秒刷新一次，让指针动起来
        if(flushThread == null) {
            flushThread = FlushThread(canvas)
            flushThread!!.start()
        }
    }

    inner class FlushThread(canvas: Canvas): Thread() {
        override fun run() {
            super.run()
            while(true) {
                sleep(1000)
                invalidate()
            }
        }
    }

    private var flushThread: FlushThread? = null

    private fun drawDegrees(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = resultWidth * DEFAULT_DEGREE_STROKE_WIDTH
            color = degreesColor
        }
        val rPadded: Int = centerX - (resultWidth * 0.01f).toInt()
        val rEnd: Int = centerX - (resultWidth * 0.05f).toInt()
        var i = 0
        while (i < FULL_ANGLE) {
            if (i % RIGHT_ANGLE != 0 && i % 15 != 0) {
                paint.alpha = CUSTOM_ALPHA
            } else {
                paint.alpha = FULL_ALPHA
            }
            val startX = (centerX + rPadded * cos(Math.toRadians(i.toDouble())))
            val startY = (centerX - rPadded * sin(Math.toRadians(i.toDouble())))
            val stopX = (centerX + rEnd * cos(Math.toRadians(i.toDouble())))
            val stopY = (centerX - rEnd * sin(Math.toRadians(i.toDouble())))
            canvas.drawLine(
                startX.toFloat(),
                startY.toFloat(),
                stopX.toFloat(),
                stopY.toFloat(),
                paint
            )
            i += 6
        }
    }

    /**
     * Draw Hour Text Values, such as 1 2 3 ...
     *
     * @param canvas
     */
    private fun drawHoursValues(canvas: Canvas) {
        // Default Color:
        // - hoursValuesColor

    }

    /**
     * Draw hours, minutes needles
     * Draw progress that indicates hours needle disposition.
     *
     * @param canvas
     */
    private fun drawNeedles(canvas: Canvas) {
        val calendar: Calendar = Calendar.getInstance()
        val now: Date = calendar.time
        val nowHours: Int = now.hours
        val nowMinutes: Int = now.minutes
        val nowSeconds: Int = now.seconds
        // 画秒针
        drawPointer(canvas, POINTER_TYPE_SECOND, nowSeconds + rotateValueOfSecond)
        // 画分针
        // todo 2: 画分针
        drawPointer(canvas, POINTER_TYPE_MINUTES, nowMinutes + rotateValueOfMinute)
        // 画时针
        val part = nowMinutes / 12
        drawPointer(canvas, POINTER_TYPE_HOURS, 5 * nowHours + part + rotateValueOfHour)
    }


    private fun drawPointer(canvas: Canvas, pointerType: Int, value: Int) {
        val degree: Float
        var pointerHeadXY = FloatArray(2)
        needlePaint.strokeWidth = resultWidth * DEFAULT_DEGREE_STROKE_WIDTH
        var value = (value + 60) % 60
        when (pointerType) {
            POINTER_TYPE_HOURS -> {
                degree = value * UNIT_DEGREE
                needlePaint.color = Color.WHITE
                pointerHeadXY = getPointerHeadXY(hourPointerLength, degree)
            }
            POINTER_TYPE_MINUTES -> {
                degree = value * UNIT_DEGREE
                needlePaint.color = Color.GRAY
                pointerHeadXY = getPointerHeadXY(minutePointerLength, degree)
               // Log.d("minute", "${value}")
            }
            POINTER_TYPE_SECOND -> {
                degree = value * UNIT_DEGREE
                needlePaint.color = Color.GREEN
                pointerHeadXY = getPointerHeadXY(secondPointerLength, degree)
            }
        }
        canvas.drawLine(
            centerX.toFloat(), centerY.toFloat(),
            pointerHeadXY[0], pointerHeadXY[1], needlePaint
        )
    }

    private fun getPointerHeadXY(pointerLength: Float, degree: Float): FloatArray {
        val xy = FloatArray(2)
        xy[0] = centerX + pointerLength * sin(degree)
        xy[1] = centerY - pointerLength * cos(degree)
        return xy
    }

    var flag:Int = 0
    var x1: Float = 0.0F
    var y1: Float = 0.0F
    var rotateValueOfSecond: Int = 0
    var rotateValueOfMinute: Int = 0
    var rotateValueOfHour: Int = 0

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when(event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                x1 = event.getX()
                y1 = event.getY()
                var distance: Float = (x1 - centerX) * (x1 - centerX) + (y1 - centerY) * (y1 - centerY)
                if(distance < hourPointerLength * hourPointerLength) {
                    flag = SWITCH_HOUR
                } else if(distance < minutePointerLength * minutePointerLength) {
                    flag = SWITCH_MINUTE
                } else {
                    flag = SWITCH_SECOND
                }
            }
            MotionEvent.ACTION_UP -> {
                var radian: Float = angle(x1, y1, event.getX(), event.getY())
                when(flag) {
                    SWITCH_SECOND -> {
                        rotateValueOfSecond += (radian / UNIT_DEGREE).toInt()
                        rotateValueOfSecond = (rotateValueOfSecond + 60) % 60
                    }
                    SWITCH_MINUTE -> {
                        rotateValueOfMinute += (radian / UNIT_DEGREE).toInt()
                        rotateValueOfMinute = (rotateValueOfMinute + 60) % 60
                        // Log.d("rotateValueMinute", "${rotateValueOfMinute}")
                    }
                    SWITCH_HOUR -> {
                        rotateValueOfHour +=  (radian / UNIT_DEGREE).toInt()
                        rotateValueOfHour = (rotateValueOfHour + 60) % 60
                    }
                }
            }
        }

        return true
    }

    private fun angle(x1: Float, x2: Float, y1: Float, y2: Float): Float {
        var dx1 = x1 - centerX
        var dy1 = y1 - centerY
        var dx2 = x2 - centerX
        var dy2 = y2 - centerY

        // 计算三边的平方
        var ab2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
        var oa2 = dx1 * dx1 + dy1 * dy1;
        var ob2 = dx2 * dx2 + dy2 * dy2;

        // 根据两向量的叉乘来判断顺逆时针
        var isClockwise: Boolean  = ((x1 - centerX) * (y2 - centerY) - (y1 - centerY) * (x2 - centerX)) > 0

        // 根据余弦定理计算旋转角的余弦值
        var cosDegree: Float = (oa2 + ob2 - ab2) / (2 * sqrt(oa2) * sqrt(ob2))

        // 异常处理，因为算出来会有误差绝对值可能会超过一，所以需要处理一下
        if (cosDegree > 1) {
            cosDegree = 1.0F
        } else if (cosDegree < -1) {
            cosDegree = -1.0F
        }

        // 计算弧度
        var radian: Float = acos(cosDegree)
        // Log.d("radian", "${radian}")
        // 顺时针为正，逆时针为负
        if(isClockwise) {
            return radian
        } else {
            return -1 * radian
        }
    }
}

