import com.formdev.flatlaf.FlatDarkLaf
import java.awt.KeyboardFocusManager
import java.awt.Point
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

class Object(text: String, private var pos: Vec2 = Vec2(), private var size: Vec2 = Vec2()) {
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

    fun update(offset: Vec2) {
        button.location = Point(pos.x.toInt() + offset.x.toInt(), pos.y.toInt() + offset.y.toInt())
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

    fun update() {
        val focusedObject = this.objects[this.objectIndex]
        this.camera = focusedObject.pos() * -1.0
        this.camera -= focusedObject.size()/2.0
        this.camera += Vec2(window.width/2.0, window.height/2.0)
        for (obj in this.objects) {
            obj.update(this.camera)
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

fun applyFriction(xVel: Double, grounded: Boolean): Double {
    val friction = if (grounded) {
        2.0
    } else {
        0.5
    }

    return max(abs(xVel) - friction, 0.0) * sign(xVel)
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
        val world = World(window, arrayOf(player, realGround, ground2, a, b))
        world.setCameraFocus(0)

        window.isVisible = true

        var windowClosing = false
        window.addWindowStateListener {
            windowClosing = it.id == WindowEvent.WINDOW_CLOSING
        }

        var leftPressed = false
        var rightPressed = false
        var upPressed = false
        val vel = Vec2()
        val worker = object : SwingWorker<Int, Int>() {
            override fun doInBackground(): Int {
                while (true) {
                    Thread.sleep(16)

                    //val grounded = player.collidingWith(realGround) && newCollision(player, realGround) == Side.Top

                    if (leftPressed) {
                        vel.x = -10.0
                    } else if (rightPressed) {
                        vel.x = 10.0
                    }

                    player.move(vel)

                    var grounded = false
                    for (obstacle in world.objects) {
                        if (obstacle == player)
                            continue
                        val colliding = player.collidingWith(obstacle)
                        if (colliding) {
                            when (newCollision(player, obstacle)) {
                                Side.Top -> {
                                    grounded = true
                                    val offset = obstacle.y() - (player.height() - 1.0 + player.y())
                                    player.move(Vec2(0.0, offset))
                                }
                                Side.Bottom -> {
                                    val offset = obstacle.y() + obstacle.height() - player.y()
                                    player.move(Vec2(0.0, offset))
                                    vel.y = 0.0
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

                    vel.x = applyFriction(vel.x, grounded)

                    if (grounded) {
                        vel.y = if (upPressed) {-20.0} else {0.0}
                    } else {
                        vel.y += 1.0
                    }

                    world.update()
                    window.repaint()

//                    val distancePastBottom = (199 + player.pos.y) - realGround.pos.y
//
//                    if (distancePastBottom > 0) {
//                        player.move(Vec2(0.0, -distancePastBottom))
//                    }

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