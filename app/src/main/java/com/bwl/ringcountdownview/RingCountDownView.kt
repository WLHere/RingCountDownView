package com.bwl.ringcountdownview

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup

private const val DEFAULT_TIME = 2000L
private const val STATE_INIT = 0
private const val STATE_STARTED = 1
private const val STATE_FINISHED = 2

/**
 * 圆环倒计时（可用于连击倒计时）
 * Created by baiwenlong on 2020/7/13
 */
class RingCountDownView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    ViewGroup(context, attrs, defStyleAttr) {

    private val mRingWidth: Int
    private val mRingSpace: Int
    private val mContentPadding: Int
    private var mTotalTime = DEFAULT_TIME
    private var mStartTime = 0L
    private var mState = STATE_INIT
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mListener: CountDownListener? = null

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    interface CountDownListener {
        fun onFinished()
    }

    init {
        // xfermode不支持硬件加速，所以需要关闭硬件加速
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setWillNotDraw(false)

        var ringWidth: Int
        var ringMargin: Int

        val defWidth = (context.resources.displayMetrics.density * 10 + 0.5f).toInt()
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.RingCountDownView)
            ringWidth = ta.getDimensionPixelSize(R.styleable.RingCountDownView_RingCountDownView_ring_width, defWidth)
            ringMargin = ta.getDimensionPixelSize(R.styleable.RingCountDownView_RingCountDownView_ring_margin, defWidth)
            ta.recycle()
        } else {
            ringWidth = defWidth
            ringMargin = defWidth
        }

        mRingWidth = ringWidth
        mRingSpace = ringMargin
        mContentPadding = mRingWidth + mRingSpace
    }

    fun start() {
        mState = STATE_STARTED
        mStartTime = SystemClock.elapsedRealtime()
        invalidate()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount <= 0) {
            return
        } else {
            val child = getChildAt(0)
            child.layout(
                mContentPadding,
                mContentPadding,
                (r - l) - mContentPadding,
                (b - t) - mContentPadding
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val child = getChildAt(0)
            val lp = child.layoutParams

            val width = when {
                lp.width >= 0 -> MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
                lp.width == LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(
                    MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.EXACTLY
                )
                else -> MeasureSpec.makeMeasureSpec(
                    MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.AT_MOST
                )
            }
            val height = when {
                lp.height >= 0 -> MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
                lp.height == LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(
                    MeasureSpec.getSize(heightMeasureSpec),
                    MeasureSpec.EXACTLY
                )
                else -> MeasureSpec.makeMeasureSpec(
                    MeasureSpec.getSize(heightMeasureSpec),
                    MeasureSpec.AT_MOST
                )
            }
            child.measure(width, height)
            val extra = mContentPadding * 2
            setMeasuredDimension(
                MeasureSpec.makeMeasureSpec(child.measuredWidth + extra, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(child.measuredHeight + extra, MeasureSpec.EXACTLY)
            )
        }
    }

    private var cachedBitmap: Bitmap? = null

    override fun draw(canvas: Canvas?) {
        // 绘制缺角
        if (canvas == null) {
            return
        }
        val startTime = System.currentTimeMillis()
        var tempBitmap = cachedBitmap
        if (tempBitmap == null || tempBitmap.width != canvas.width || tempBitmap.height != canvas.height) {
            tempBitmap?.apply {
                if (!isRecycled) {
                    recycle()
                }
            }
            tempBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            cachedBitmap = tempBitmap
        }

        val proxyCanvas = Canvas(tempBitmap!!)
        super.draw(proxyCanvas)
        val startAngle = -90f

        val sweepAngle = when (mState) {
            STATE_INIT -> 0f
            STATE_STARTED -> {
                val passedTime = (SystemClock.elapsedRealtime() - mStartTime).toFloat()
                if (passedTime < mTotalTime) {
                    -passedTime / mTotalTime * 360
                } else {
                    mState = STATE_FINISHED
                    notifyOnFinished()
                    -360f
                }
            }
            STATE_FINISHED -> -360f
            else -> 0f
        }

        // 绘制圆形和缺角区域
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f
        // 外环
        mPaint.xfermode = null
        canvas.drawCircle(centerX, centerY, centerX, mPaint)
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawCircle(centerX, centerY, centerX - mRingWidth, mPaint)

        // arc缺角
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawArc(
            RectF(-1f, -1f, canvas.width + 1f, canvas.height + 1f),
            startAngle,
            sweepAngle,
            true,
            mPaint
        )

        // 内圆
        mPaint.xfermode = null
        canvas.drawCircle(centerX, centerY, centerX - mContentPadding, mPaint)

        // 绘制原有图形
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(tempBitmap, 0f, 0f, mPaint)

        if (mState == STATE_STARTED) {
            invalidate()
        }

        val endTime = System.currentTimeMillis()
        Log.d("bwl", "time: ${endTime - startTime}ms")
    }

    private var cachedBitmapForChild: Bitmap? = null
    private var cachedBitmapForChildCut: Bitmap? = null

    override fun dispatchDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        var tempBitmap = cachedBitmapForChild
        if (tempBitmap == null || tempBitmap.width != canvas.width || tempBitmap.height != canvas.height) {
            tempBitmap?.apply {
                if (!isRecycled) {
                    recycle()
                }
            }
            tempBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            cachedBitmapForChild = tempBitmap
        }
        val proxyCanvas = Canvas(tempBitmap!!)

        var tempCutBitmap = cachedBitmapForChildCut
        if (tempCutBitmap == null || tempBitmap.width != canvas.width || tempBitmap.height != canvas.height) {
            tempCutBitmap?.apply {
                if (!isRecycled) {
                    recycle()
                }
            }
            tempCutBitmap =
                Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            cachedBitmapForChildCut = tempCutBitmap
        }
        val cutProxyCanvas = Canvas(tempCutBitmap!!)

        super.dispatchDraw(proxyCanvas)

        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f
        // 内圆
        mPaint.xfermode = null
        cutProxyCanvas.drawCircle(centerX, centerY, centerX - mContentPadding, mPaint)
        // 绘制原有图形
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        cutProxyCanvas.drawBitmap(tempBitmap, 0f, 0f, mPaint)

        mPaint.xfermode = null
        canvas.drawBitmap(tempCutBitmap, 0f, 0f, mPaint)
    }

    private fun notifyOnFinished() {
        post {
            mListener?.onFinished()
        }
    }

    fun setListener(listener: CountDownListener?) {
        mListener = listener
    }
}