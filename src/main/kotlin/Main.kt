import com.formdev.flatlaf.FlatDarkLaf
import java.awt.Font
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.math.*

data class Vec2(var x: Double = 0.0, var y: Double = 0.0) {
    operator fun plus(other: Vec2): Vec2 {
        return Vec2(this.x + other.x, this.y + other.y)
    }
    operator fun minus(other: Vec2): Vec2 {
        return Vec2(this.x - other.x, this.y - other.y)
    }

    operator fun times(d: Double): Vec2 {
        return Vec2(this.x * d, this.y * d)
    }

    operator fun div(d: Double): Vec2 {
        return Vec2(this.x / d, this.y / d)
    }
}

interface GameObject {
    fun update(dt: Double, posOffset: Vec2)
}

class Object(text: String, private var pos: Vec2 = Vec2(), private var size: Vec2 = Vec2()) : GameObject {
    private val button = JButton(text)

    init {
        button.setBounds(pos.x.toInt(), pos.y.toInt(), size.x.toInt(), size.y.toInt())
    }

    fun addTo(window: JFrame) {
        window.add(button)
    }

    fun move(by: Vec2) {
        pos += by
    }

    override fun update(dt: Double, posOffset: Vec2) {
        button.location = Point(pos.x.toInt() + posOffset.x.toInt(), pos.y.toInt() + posOffset.y.toInt())
    }

    fun collidingWith(other: Object): Boolean {
        return  this.pos.x < other.pos.x + other.size.x &&
                this.pos.x + this.size.x > other.pos.x &&
                this.pos.y < other.pos.y + other.size.y &&
                this.size.y + this.pos.y > other.pos.y
    }

    fun x(): Double {
        return this.pos.x
    }

    fun y(): Double {
        return this.pos.y
    }

    fun pos(): Vec2 {
        return this.pos
    }

    fun width(): Double {
        return this.size.x
    }

    fun height(): Double {
        return this.size.y
    }

    fun size(): Vec2 {
        return this.size
    }
}

class World(private var window: JFrame, var objects: Array<Object>, private var camera: Vec2 = Vec2()) {
    private var objectIndex = 0
    init {
        for (obj in this.objects) {
            obj.addTo(this.window)
        }
    }

    fun setCameraFocus(objectIndex: Int) {
        this.objectIndex = objectIndex
    }

    fun update(dt: Double) {
        val focusedObject = this.objects[this.objectIndex]
        this.camera = focusedObject.pos() * -1.0
        this.camera -= focusedObject.size()/2.0
        this.camera += Vec2(window.width/2.0, window.height/2.0)
        for (obj in this.objects) {
            obj.update(dt, this.camera)
        }
    }
}

fun keyPressed(event: KeyEvent): Boolean {
    when (event.id) {
        KeyEvent.KEY_PRESSED -> return true
        KeyEvent.KEY_RELEASED -> return false
        KeyEvent.KEY_TYPED -> return true
    }
    return false
}

enum class Side {
    Top,
    Bottom,
    Left,
    Right
}

// https://stackoverflow.com/a/13349505
fun newCollision(a: Object, b: Object): Side {
    val aBottom = a.y() + a.height()
    val bBottom = b.y() + b.height()
    val aRight = a.x() + a.width()
    val bRight = b.x() + b.width()

    val bCollision = bBottom - a.y()
    val tCollision = aBottom - b.y()
    val lCollision = aRight - b.x()
    val rCollision = bRight - a.x()

    val collisions = arrayOf(bCollision, tCollision, lCollision, rCollision)
    return when (collisions.min()) {
        bCollision -> Side.Bottom
        tCollision -> Side.Top
        lCollision -> Side.Left
        rCollision -> Side.Right
        else -> {Side.Top}
    }
}

fun dtToFps(dt: Double): Double {
    return 1.0/dt
}

// Pixels/Second
const val SPEED = 600.0
const val JUMP_SPEED = -1200.0
const val GRAVITY = 3600.0
const val GROUND_FRICTION = 7200.0
const val AIR_FRICTION = 1800.0

fun applyFriction(dt: Double, xVel: Double, grounded: Boolean): Double {
    val friction = if (grounded) {
        GROUND_FRICTION
    } else {
        AIR_FRICTION
    } * dt

    return max(abs(xVel) - friction, 0.0) * sign(xVel)
}

fun main() {
    //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    UIManager.setLookAndFeel(FlatDarkLaf())

    JFrame().also {window ->
        window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        window.title = "Cool thing"
        window.setSize(1200, 700)
        window.layout = null

        val rootPane = window.rootPane
        rootPane.setLocation(100, 100)

        val player = Object("Player",
            Vec2(0.0, -200.0),
            Vec2(100.0, 100.0)
        )
        val realGround = Object(
            "Ground",
            Vec2(-800.0, 0.0),
            Vec2(1600.0, 300.0)
        )
        val ground2 = Object(
            "Ground",
            Vec2(800.0, 0.0),
            Vec2(1600.0, 300.0)
        )
        val a = Object("Thing",
            Vec2(300.0, -120.0),
            Vec2(120.0, 120.0)
        )
        val b = Object("Thing",
            Vec2(500.0, -220.0),
            Vec2(120.0, 220.0)
        )
        val ceiling = Object("Ceiling",
            Vec2(-700.0, -600.0),
            Vec2(1000.0, 300.0)
        )

        val slider = JSlider()
        slider.bounds = Rectangle(0, 0, 300, 20)
        window.add(slider)

        val world = World(window, arrayOf(player, realGround, ground2, a, b, ceiling))
        world.setCameraFocus(0)

        val fpsDisplay = JLabel("999 FPS")
        fpsDisplay.setBounds(0, 50, 200, 20)
        fpsDisplay.font = Font(fpsDisplay.font.name, fpsDisplay.font.style, 20)
        window.add(fpsDisplay)

        window.isVisible = true

        var windowClosing = false
        window.addWindowStateListener {
            windowClosing = it.id == WindowEvent.WINDOW_CLOSING
        }

        var leftPressed = false
        var rightPressed = false
        var upPressed = false
        val fpsHistory = Array(3000) {0}
        var frameCount = 0L
        val vel = Vec2()
        var lastFrameEnd = System.nanoTime()
        val worker = object : SwingWorker<Int, Int>() {
            override fun doInBackground(): Int {
                while (true) {
                    val thisFrameStart = System.nanoTime()
                    val dt = (thisFrameStart - lastFrameEnd).toDouble() / 1_000_000_000.0
                    lastFrameEnd = System.nanoTime()

                    if (dt > 5)
                        continue

                    val fps = dtToFps(dt).toInt()
                    val fpsHistoryIndex = (frameCount % 3000).toInt()
                    if (fpsHistoryIndex == 0) {
                        val fpsAverage = fpsHistory.average().toInt()
                        fpsDisplay.text = "$fpsAverage FPS"
                    }
                    fpsHistory[fpsHistoryIndex] = fps


                    if (leftPressed) {
                        vel.x = -SPEED
                    } else if (rightPressed) {
                        vel.x = SPEED
                    }

                    player.move(vel * dt)

                    var grounded = false
                    for (obstacle in world.objects) {
                        if (obstacle == player)
                            continue
                        val colliding = player.collidingWith(obstacle)
                        if (colliding) {
                            when (newCollision(player, obstacle)) {
                                Side.Top -> {
                                    if (vel.y >= 0) {
                                        grounded = true
                                        val offset = obstacle.y() - (player.height() + player.y())
                                        player.move(Vec2(0.0, offset))
                                    }
                                }
                                Side.Bottom -> {
                                    if (vel.y <= 0) {
                                        val offset = obstacle.y() + obstacle.height() - player.y()
                                        player.move(Vec2(0.0, offset))
                                        vel.y = 0.0
                                    }
                                }
                                Side.Left -> {
                                    val offset = obstacle.x() - (player.x() + player.width())
                                    player.move(Vec2(offset, 0.0))
                                    vel.x = 0.0
                                }
                                Side.Right -> {
                                    val offset = obstacle.x() + obstacle.width() - player.x()
                                    player.move(Vec2(offset, 0.0))
                                    vel.x = 0.0
                                }
                            }
                        }
                    }

                    vel.x = applyFriction(dt, vel.x, grounded)

                    if (grounded) {
                        vel.y = if (upPressed) {JUMP_SPEED} else {0.0}
                    } else {
                        vel.y += GRAVITY * dt
                    }

                    world.update(dt)
                    window.repaint()

                    frameCount += 1

                    if (windowClosing) {
                        break
                    }
                }
                return 0
            }
        }

        worker.execute()


        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher {
            val pressed = keyPressed(it)
            when (it.keyCode) {
                KeyEvent.VK_LEFT -> leftPressed = pressed
                KeyEvent.VK_RIGHT -> rightPressed = pressed
                KeyEvent.VK_UP -> upPressed = pressed
            }

            false
        }
    }
}