package com.haoyh.luckynumberview.lucknumber

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.haoyh.luckynumberview.R
import kotlin.math.absoluteValue

/**
 * @FileName: LuckNumberView
 * @Description: happy hash 选择数字及显示加载中奖号码动画 view
 * @Author: haoyanhui
 * @Date: 2020-07-24 16:42
 */
class LuckyNumberView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "hyh"

    companion object {

        const val strokeResId = R.mipmap.bg_animator

        val defNumResId = arrayOf(
            R.mipmap.bg_hphash_zero_def, R.mipmap.bg_hphash_one_def,
            R.mipmap.bg_hphash_two_def, R.mipmap.bg_hphash_three_def, R.mipmap.bg_hphash_four_def,
            R.mipmap.bg_hphash_five_def, R.mipmap.bg_hphash_six_def, R.mipmap.bg_hphash_seven_def,
            R.mipmap.bg_hphash_eight_def, R.mipmap.bg_hphash_nine_def
        )

        val checkedNumResId = arrayOf(
            R.mipmap.bg_hphash_zero_checked,
            R.mipmap.bg_hphash_one_checked,
            R.mipmap.bg_hphash_two_checked,
            R.mipmap.bg_hphash_three_checked,
            R.mipmap.bg_hphash_four_checked,
            R.mipmap.bg_hphash_five_checked,
            R.mipmap.bg_hphash_six_checked,
            R.mipmap.bg_hphash_seven_checked,
            R.mipmap.bg_hphash_eight_checked,
            R.mipmap.bg_hphash_nine_checked
        )

        val SECOND = 1000L
    }

    // item 点击事件接口
    interface OnItemClickListener {
        fun onItemClick(index: String)
    }

    // 显示中奖结果动画结束事件接口
    interface OnLuckNumberAnimationEndListener {
        fun onLuckNumberAnimationEnd()
    }

    private var mOnItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.mOnItemClickListener = listener
    }

    private var mOnLuckNumberAnimationEndListener: OnLuckNumberAnimationEndListener? = null

    fun setOnLuckNumberAnimationEndListener(listener: OnLuckNumberAnimationEndListener) {
        this.mOnLuckNumberAnimationEndListener = listener
    }

    // 存放数字 bitmap
    private var mDefBitmapList: MutableList<Bitmap>
    private var mCheckedBitmapList: MutableList<Bitmap>

    // 动画 bitmap
    private var mStorkBitmap: Bitmap

    // 画笔
    private var mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 每行的个数
    private val mRowCount = 5

    // 预留安全距离
    private var mSafeMargin = 5f

    // 水平边距，最左/最右 需要除以 2
    private var marginL = 5f

    // 垂直边距，最上/最下 需要除以 2
    private var marginT = 5f

    // 存储矩形的集合
    private var mNumberRects: MutableList<RectF>

    // 矩形的宽和高
    private var mRectW = 52f
    private var mRectH = 55f

    // 按下时点击的 item 项,确保一次点击是在同一个按钮上
    private var mLastClickIndex = ""

    // 动画类型
    var mAnimatorMode = AnimatorMode.MODE_TRANSLATION

    // 动画相关
    // JUMP 类型时
    private var mLuckSpinJumpLoadingAnimator: ValueAnimator? = null

    // 有中奖结果之后的动画
    private var mLuckSpinJumpResultAnimator: ValueAnimator? = null

    // 移动的index
    private var mCurrentMoveIndex = -1

    // TRANSLATION 类型时
    private var mLuckSpinTranslateLoadingAnimatorSet: AnimatorSet? = null

    // 移动图片隐藏 rectF
    private val mHiddenRectF = RectF(0f, 0f, 0f, 0f)
    private val mHiddenRect = Rect(0, 0, 0, 0)

    // 当前移动的坐标
    private var mCurrentRectF: RectF? = null

    // 当前选中的 数字
    var mChoosedItemList: MutableList<String> = mutableListOf()
        set(value) {
            field = value
            invalidate()
        }

    // 开始动画
    var startAnimation: Boolean = false
        set(value) {
            field = value
            if (value) {
                startMoveAnimation()
            }
        }

    // 中奖码
    var mLuckNumber: String = ""
        set(value) {
            field = value
            stopMoveAnimation()
        }

    // 隐藏滑块
    var mHiddenAnimatorView: Boolean = false
        set(value) {
            field = value
            if (value) {
                hiddenAnimatorView()
            }
        }

    init {
        mNumberRects = mutableListOf()

        // 初始化图片相关信息
        mStorkBitmap = BitmapFactory.decodeResource(resources, strokeResId)

        mDefBitmapList = mutableListOf()
        defNumResId.forEach {
            mDefBitmapList.add(BitmapFactory.decodeResource(resources, it))
        }

        mCheckedBitmapList = mutableListOf()
        checkedNumResId.forEach {
            mCheckedBitmapList.add(BitmapFactory.decodeResource(resources, it))
        }

        mRectW = dp2px(mRectW)
        mRectH = dp2px(mRectH)

        mSafeMargin = dp2px(mSafeMargin)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 计算水平、垂直边距
        marginL = (w.toFloat() - (mRowCount * mRectW) - mSafeMargin * 2) / mRowCount.toFloat()
        marginT = (h.toFloat() - (2f * mRectH) - mSafeMargin * 2) / 2f
        mNumberRects.clear()
        initRect()
    }

    // 画矩形
    private fun initRect() {
        var l: Float
        var t: Float
        var r: Float
        var b: Float
        var rectf: RectF?
        // 第一行
        for (i in 0 until mRowCount) {
            l = i * mRectW + marginL * i + marginL / 2 + mSafeMargin
            t = marginT / 2 + mSafeMargin
            r = i * mRectW + marginL * i + marginL / 2 + mRectW + mSafeMargin
            b = mRectH + marginT / 2 + mSafeMargin
            rectf = RectF(l, t, r, b)
            mNumberRects.add(rectf)
        }
        // 第二行
        for (i in 0 until mRowCount) {
            l = i * mRectW + marginL * i + marginL / 2 + mSafeMargin
            t = mRectH + marginT + mSafeMargin
            r = i * mRectW + marginL * i + marginL / 2 + mRectW + mSafeMargin
            b = mRectH * 2 + marginT / 2 + marginT + mSafeMargin
            rectf = RectF(l, t, r, b)
            mNumberRects.add(rectf)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            // 画数字
            drawNumberRects(it)
            // 画移动图片
            drawMoveImage(it)
        }
    }

    // 画 9 个数字
    private fun drawNumberRects(canvas: Canvas) {
        mNumberRects.forEachIndexed { index, rectF ->
            if (mChoosedItemList.contains(index.toString())) {
                canvas.drawBitmap(mCheckedBitmapList[index], rectF.left, rectF.top, mPaint)
            } else {
                canvas.drawBitmap(mDefBitmapList[index], rectF.left, rectF.top, mPaint)
            }
        }
    }

    // 画动画时平移的 bitmap
    private fun drawMoveImage(canvas: Canvas) {
        if (mAnimatorMode == AnimatorMode.MODE_JUMP) {
            if (mCurrentMoveIndex in 0..mNumberRects.size) {
                // 平移bitmap 同 数字bitmap 不同，所以需要重新计算坐标
                val left = mNumberRects[mCurrentMoveIndex].left - (mRectW - mStorkBitmap.width).absoluteValue.div(2)
                val top = mNumberRects[mCurrentMoveIndex].top - (mRectH - mStorkBitmap.height).absoluteValue.div(2)
                canvas.drawBitmap(mStorkBitmap, left, top, mPaint)
            }
        } else if (mAnimatorMode == AnimatorMode.MODE_TRANSLATION) {
            mCurrentRectF?.let {
                if (it != mHiddenRectF) {
                    val left = it.left - (mRectW - mStorkBitmap.width).absoluteValue.div(2)
                    val top = it.top - (mRectH - mStorkBitmap.height).absoluteValue.div(2)
                    canvas.drawBitmap(mStorkBitmap, left, top, mPaint)
                } else {
                    // 使移动图片不可见
                    canvas.drawBitmap(mStorkBitmap, Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(),
                        it.bottom.toInt()), mHiddenRect, mPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                mNumberRects.forEachIndexed { index, rectF ->
                    if (rectF.contains(event.x, event.y)) {
                        // 点击了某个按钮，返回 index
                        mLastClickIndex = index.toString()
                        Log.d(TAG, "down 点击了 $index")
                        return true
                    }
                }
                return true
            }
            if (it.action == MotionEvent.ACTION_UP) {
                mNumberRects.forEachIndexed { index, rectF ->
                    if (rectF.contains(event.x, event.y)) {
                        // 点击了某个按钮，返回 index
                        Log.d(TAG, "up 点击了 $index")
                        if (mLastClickIndex == index.toString()) {
                            // 点击监听回调
                            mOnItemClickListener?.onItemClick(mLastClickIndex)
                        }
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // 开始平移动画
    private fun startMoveAnimation() {
        if (mAnimatorMode == AnimatorMode.MODE_JUMP) {
            mLuckSpinJumpLoadingAnimator = ValueAnimator.ofInt(0, mNumberRects.size).apply {
                setInterpolatorAndDuration()
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val i: Int = (it.animatedValue) as Int
                    mCurrentMoveIndex = i % mNumberRects.size
                    invalidate()
                }
                start()
            }
        } else if (mAnimatorMode == AnimatorMode.MODE_TRANSLATION) {
            // 平移动画相当于画两条平行线，第一条线的起点、终点坐标对应 0、4 的坐标，第二条线的起点、终点坐标对应 5、9 的坐标
            // 从左到右的移动 第一条线
            val topLine = ValueAnimator.ofObject(RectFEvaluator(), mNumberRects[0], mNumberRects[4]).apply {
                setInterpolatorAndDuration()
                addUpdateListener {
                    mCurrentRectF = it.animatedValue as RectF
                    invalidate()
                }
            }

            // 从左到右的移动 第二条线
            val bottomLine = ValueAnimator.ofObject(RectFEvaluator(), mNumberRects[5], mNumberRects[9])
                .apply {
                    setInterpolatorAndDuration()
                    addUpdateListener {
                        mCurrentRectF = it.animatedValue as RectF
                        invalidate()
                    }
                }

            mLuckSpinTranslateLoadingAnimatorSet = AnimatorSet().apply {

                play(topLine).before(bottomLine)

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        // 实现重复执行该动画
                        start()
                    }
                })

                start()
            }
        }
    }

    // 暂停平移动画，并开启结果锁定中奖码的动画
    private fun stopMoveAnimation() {
        // 开始最后一轮动画，动画结束选中中奖数字
        if (mAnimatorMode == AnimatorMode.MODE_JUMP) {
            if (mLuckNumber.isEmpty()) {
                mLuckSpinJumpLoadingAnimator?.cancel()
                mOnLuckNumberAnimationEndListener?.onLuckNumberAnimationEnd()
            } else {
                mLuckSpinJumpResultAnimator = ValueAnimator.ofInt(mCurrentMoveIndex, mLuckNumber.toInt() + mNumberRects.size)
                    .apply {
                        setInterpolatorAndDuration()
                        removeAllUpdateListeners()
                        addUpdateListener {
                            val i: Int = (it.animatedValue) as Int
                            mCurrentMoveIndex = i % mNumberRects.size
                            invalidate()
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator?) {
                                super.onAnimationStart(animation)
                                mLuckSpinJumpLoadingAnimator?.cancel()
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                mOnLuckNumberAnimationEndListener?.onLuckNumberAnimationEnd()
                            }
                        })
                        start()
                    }
            }
        } else if (mAnimatorMode == AnimatorMode.MODE_TRANSLATION) {
            // 根据当前位置画到指定暂停的位置
            if (mLuckNumber.isEmpty()) {
                mLuckSpinTranslateLoadingAnimatorSet?.pause()
                mLuckSpinTranslateLoadingAnimatorSet?.removeAllListeners()
                mOnLuckNumberAnimationEndListener?.onLuckNumberAnimationEnd()
                hiddenAnimatorView()
            } else {
                // 继续画线
                // 起点是 mCurrentRectF 的坐标
                // 终点就是中奖码对应的坐标
                val endRectF = mNumberRects[mLuckNumber.toInt()]
                // 需要判断起点、终点位于哪儿条（第一/二）条线上
                // 仍然是画线，动画不需要重复
                mCurrentRectF?.let { currentRectF ->
                    // 第一条线的坐标，通过数字0同数字4的坐标可以得到
                    val topLineRectF = RectF(mNumberRects[0].left, mNumberRects[0].top, mNumberRects[4].right, mNumberRects[0].bottom)
                    // 第一条线的坐标，通过数字5同数字9的坐标可以得到
                    val bottomLineRectF = RectF(mNumberRects[5].left, mNumberRects[5].top, mNumberRects[9].right, mNumberRects[5].bottom)

                    val fullLine = mNumberRects[4].left - mNumberRects[0].left

                    var startInTop = false
                    var endInTop = false

                    if (topLineRectF.contains(currentRectF)) {
                        Log.d(TAG, "起点在 topLine")
                        startInTop = true
                    }

                    if (bottomLineRectF.contains(currentRectF)) {
                        Log.d(TAG, "起点在 bottomLine")
                        startInTop = false
                    }

                    if (topLineRectF.contains(endRectF)) {
                        Log.d(TAG, "终点在 topLine")
                        endInTop = true
                    }

                    if (bottomLineRectF.contains(endRectF)) {
                        Log.d(TAG, "终点在 bottomLine")
                        endInTop = false
                    }

                    // 第一条线的坐标：起点（mCurrentRectF） 到所在线的终点值
                    val topLineEnd: RectF
                    topLineEnd = if (startInTop) {
                        mNumberRects[4]
                    } else {
                        mNumberRects[9]
                    }
                    val topLineAnimator = ValueAnimator.ofObject(RectFEvaluator(), currentRectF, topLineEnd).apply {
                        interpolator = LinearInterpolator()
                        // 计算时间，起点到终点的距离占完整线的比例 * SECOND
                        duration = ((topLineEnd.left - currentRectF.left).div(fullLine) * SECOND).toLong()
                        addUpdateListener {
                            mCurrentRectF = it.animatedValue as RectF
                            invalidate()
                        }
                    }

                    // 第二条线（只有当起点、终点在同一条线上时才需要添加）
                    var middleLineAnimator: ValueAnimator? = null
                    var middleLineStart: RectF? = null
                    var middleLineEnd: RectF? = null
                    if (startInTop && endInTop) {
                        middleLineStart = mNumberRects[5]
                        middleLineEnd = mNumberRects[9]
                    }
                    if (!startInTop && !endInTop) {
                        middleLineStart = mNumberRects[0]
                        middleLineEnd = mNumberRects[4]
                    }
                    middleLineStart?.let { start ->
                        middleLineEnd?.let { end ->
                            middleLineAnimator = ValueAnimator.ofObject(RectFEvaluator(), start, end).apply {
                                setInterpolatorAndDuration()
                                addUpdateListener {
                                    mCurrentRectF = it.animatedValue as RectF
                                    invalidate()
                                }
                            }
                            Log.d(TAG, "需要画中间线")
                        }
                    }

                    // 第三条线的坐标：所在线的起点值到 终点（中奖码对应的坐标）
                    val bottomLineStart: RectF
                    bottomLineStart = if (endInTop) {
                        mNumberRects[0]
                    } else {
                        mNumberRects[5]
                    }
                    val bottomLineAnimator = ValueAnimator.ofObject(RectFEvaluator(), bottomLineStart, endRectF)
                        .apply {
                            interpolator = LinearInterpolator()
                            // 计算时间，起点到终点的距离占完整线的比例 * SECOND
                            duration = ((endRectF.left - bottomLineStart.left).div(fullLine) * SECOND).toLong()
                            addUpdateListener {
                                mCurrentRectF = it.animatedValue as RectF
                                invalidate()
                            }
                        }

                    AnimatorSet().apply {
                        topLineAnimator?.let {
                            if (null != middleLineAnimator) {
                                play(middleLineAnimator).after(topLineAnimator).before(bottomLineAnimator)
                            } else {
                                play(topLineAnimator).before(bottomLineAnimator)
                            }
                        }

                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator?) {
                                // 结束加载时动画
                                mLuckSpinTranslateLoadingAnimatorSet?.pause()
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                // 通知页面动画结束
                                mOnLuckNumberAnimationEndListener?.onLuckNumberAnimationEnd()
                            }
                        })
                        start()
                    }
                }
            }
        }
    }

    // 设置动画差值器和动画时长
    private fun ValueAnimator?.setInterpolatorAndDuration() {
        this?.let {
            interpolator = LinearInterpolator()
            duration = SECOND
        }
    }

    // 隐藏动画滑块
    private fun hiddenAnimatorView() {
        mCurrentRectF = mHiddenRectF
        invalidate()
    }

    fun onDestroy() {
        mLuckSpinJumpLoadingAnimator?.cancel()
        mLuckSpinJumpResultAnimator?.cancel()
    }

    private fun dp2px(dp: Float): Float {
        return (resources.displayMetrics.density * dp + 0.5).toFloat()
    }
}