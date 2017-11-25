package com.movesense.mds.sampleapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class CanvasView(ctx: Context, attr: AttributeSet) : View(ctx, attr) {
    private lateinit var mBitmap: Bitmap
    lateinit var mCanvas: Canvas

    init {

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
    }

}