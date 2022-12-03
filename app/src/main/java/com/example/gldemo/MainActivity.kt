package com.example.gldemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.gldemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.surfaceView.setEGLContextClientVersion(3)
        mBinding.surfaceView.setEGLConfigChooser(8,8,8,8,8,8)
        mBinding.surfaceView.setRenderer(RoundedCornerRender())
    }
}