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

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorScale(a : Int, b : Int) : Float  {
    val k : Float = scaleFactor()
    return (1 - k ) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorScale(a, b) * dir * scGap
fun Int.sf() : Float = 1f - 2 * this
fun Int.sjf() : Float = (this % 2).sf()
fun Int.mirror() : Float = 1f - this
fun Int.jMirror() : Float = (this % 2).mirror()

fun Canvas.drawMovingSquare(j : Int, size : Float, x: Float,  y : Float, paint : Paint) {

    save()
    translate(x, y)
    drawRect(RectF(0f, 0f, size, size), paint)
    restore()
}

fun Canvas.drawPMBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    val xGap : Float = (2 * size) / squares
    save()
    translate(w / 2, gap * (i + 1))
    rotate(rotDeg * sc2)
    var x : Float = -xGap
    var y : Float = xGap
    for (j in 0..(squares - 1)) {
        val sc1j : Float = sc1.divideScale(j, squares).divideScale(0, parts)
        val sc2j : Float = sc1.divideScale(j, squares).divideScale(1, parts)
        val xDiff = xGap * sc1j
        val yDiff = xGap * sc2j * j.sjf()
        save()
        translate(-size, 0f)
        drawMovingSquare(j, xGap, x + xDiff, y - yDiff, paint)
        restore()
        x += xDiff
        y -= yDiff
    }
    restore()
}

class PrevMovingBlocksView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN ->  {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, squares, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class PMBNode(var i : Int, val state : State = State()) {

        private var next : PMBNode? = null
        private var prev : PMBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = PMBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawPMBNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : PMBNode {
            var curr : PMBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class PrevMovingBlocks(var i : Int) {

        private var dir : Int = 1
        private val root : PMBNode = PMBNode(0)
        private var curr : PMBNode = root

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : PrevMovingBlocksView) {

        private val animator : Animator = Animator(view)
        private val pmb : PrevMovingBlocks = PrevMovingBlocks(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            pmb.draw(canvas, paint)
            animator.animate {
                pmb.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            pmb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : PrevMovingBlocksView {
            val view : PrevMovingBlocksView = PrevMovingBlocksView(activity)
            activity.setContentView(view)
            return view
        }
    }
}