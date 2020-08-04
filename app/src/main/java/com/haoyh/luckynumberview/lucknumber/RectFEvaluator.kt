package com.haoyh.luckynumberview.lucknumber

import android.animation.TypeEvaluator
import android.graphics.RectF

/**
 * @FileName: RectFEvaluator
 * @Description: 作用描述
 * @Author: haoyanhui
 * @Date: 2020-07-27 12:57
 */
class RectFEvaluator : TypeEvaluator<RectF> {

    override fun evaluate(fraction: Float, startValue: RectF?, endValue: RectF?): RectF {
        val start = startValue!!
        val end = endValue!!
        val left = start.left + fraction * (end.left - start.left)
        val top = start.top + fraction * (end.top - start.top)
        val right = start.right + fraction * (end.right - start.right)
        val bottom = start.bottom + fraction * (end.bottom - start.bottom)
        return RectF(left, top, right, bottom)
    }
}