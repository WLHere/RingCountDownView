package com.bwl.ringcountdownview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
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
    private val mOutCirclePath = Path()
    private val mInnerCirclePath = Path()
    private val mSpaceCirclePath = Path()
    private val mSweepAnglePath = Path()
    private var mCenterX = 0f
    private var mCenterY = 0f
    private var mRadius = 0f
    private var mListener: CountDownListener? = null

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    interface CountDownListener {
        fun onFinished()
    }

    init {
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

    override fun draw(canvas: Canvas?) {
        // 绘制缺角
        if (canvas == null) {
            super.draw(canvas)
            return
        }
        val startTime = System.currentTimeMillis()
        mCenterX = canvas.width / 2f
        mCenterY = canvas.height / 2f
        mRadius = mCenterX

        val sweepAngle = when (mState) {
            STATE_INIT -> 0f
            STATE_STARTED -> {
                val passedTime = (SystemClock.elapsedRealtime() - mStartTime).toFloat()
                if (passedTime < mTotalTime) {
                    passedTime / mTotalTime * 360
                } else {
                    mState = STATE_FINISHED
                    notifyOnFinished()
                    360f
                }
            }
            STATE_FINISHED -> 360f
            else -> 0f
        }

        mOutCirclePath.reset()
        mOutCirclePath.addCircle(mCenterX, mCenterY, mRadius, Path.Direction.CW)
        mInnerCirclePath.reset()
        mInnerCirclePath.addCircle(mCenterX, mCenterY, mRadius - mRingWidth - mRingSpace, Path.Direction.CW)
        mSpaceCirclePath.reset()
        mSpaceCirclePath.addCircle(mCenterX, mCenterY, mRadius - mRingWidth, Path.Direction.CW)
        mSweepAnglePath.reset()
        mSweepAnglePath.moveTo(mCenterX, mCenterY)
        mSweepAnglePath.lineTo(mCenterX, 0f)
        mSweepAnglePath.arcTo(RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()), 270f, -sweepAngle)
        mSweepAnglePath.close()



        mOutCirclePath.let {
            it.op(mSpaceCirclePath, Path.Op.DIFFERENCE)
            it.op(mSweepAnglePath, Path.Op.DIFFERENCE)
            it.op(mInnerCirclePath, Path.Op.UNION)
        }


        canvas.save()
        canvas.clipPath(mOutCirclePath)
        super.draw(canvas)
        canvas.restore()

        if (mState == STATE_STARTED) {
            invalidate()
        }

        val endTime = System.currentTimeMillis()
        Log.d("bwl", "${endTime - startTime}ms");
    }

    override fun dispatchDraw(canvas: Canvas?) {
        if (canvas == null) {
            super.draw(canvas)
            return
        }
        canvas.save()
        canvas.clipPath(mInnerCirclePath)
        super.dispatchDraw(canvas)
        canvas.restore()
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