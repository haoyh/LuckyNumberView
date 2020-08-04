package com.haoyh.luckynumberview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.haoyh.luckynumberview.lucknumber.AnimatorMode
import com.haoyh.luckynumberview.lucknumber.LuckyNumberView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var mMode = AnimatorMode.MODE_TRANSLATION
    private val mChoosedNumList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rg_change_mode?.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.rb_slide) {
                mMode = AnimatorMode.MODE_TRANSLATION
            } else if (checkedId == R.id.rb_jump) {
                mMode = AnimatorMode.MODE_JUMP
            }
            lucknumberview?.mAnimatorMode = mMode
        }

        lucknumberview?.apply {
            setOnItemClickListener(object : LuckyNumberView.OnItemClickListener {
                override fun onItemClick(index: String) {
                    if (mChoosedNumList.contains(index)) {
                        mChoosedNumList.remove(index)
                    } else {
                        mChoosedNumList.add(index)
                    }

                    mChoosedItemList = mChoosedNumList

                    tv_choosed_num?.text = "已选中数字：${mChoosedNumList}"
                }
            })

            setOnLuckNumberAnimationEndListener(object : LuckyNumberView.OnLuckNumberAnimationEndListener {
                override fun onLuckNumberAnimationEnd() {
                    Toast.makeText(this@MainActivity, "动画结束", Toast.LENGTH_SHORT).show()
                    // 两秒之后隐藏滑块
                    GlobalScope.launch(Dispatchers.IO) {
                        Thread.sleep(1000 * 2)
                        withContext(Dispatchers.Main) {
                            mHiddenAnimatorView = true
                        }
                    }
                }
            })
        }

        tv_start?.setOnClickListener {
            lucknumberview?.startAnimation = true
        }

        tv_stop?.setOnClickListener {
            val resultStrList = listOf("", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
            val luckNum = resultStrList[Random.nextInt(11)]
            Log.d("hyh", "luckNum = $luckNum")
            lucknumberview?.mLuckNumber = luckNum
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        lucknumberview?.onDestroy()
    }
}