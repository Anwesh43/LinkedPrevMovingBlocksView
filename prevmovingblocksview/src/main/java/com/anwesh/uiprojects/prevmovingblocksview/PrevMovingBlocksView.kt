package com.anwesh.uiprojects.prevmovingblocksview

/**
 * Created by anweshmishra on 09/04/19.
 */


import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Color
import android.content.Context

val nodes : Int = 5
val squares : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")
val rotDeg : Float = 180f
val parts : Int = 2