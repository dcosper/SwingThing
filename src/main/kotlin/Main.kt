import com.formdev.flatlaf.FlatDarkLaf
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.math.*
import kotlin.random.Random

data class Vec2(var x: Double = 0.0, var y: Double = 0.0) {
    constructor(point: Point): this() {
        this.x = point.x.toDouble()
        this.y = point.y.toDouble()
    }
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

class World(var window: JFrame, var objects: Array<Object>, private var camera: Vec2 = Vec2()) {
    init {
        for (obj in this.objects) {
            obj.addTo(this.window)
        }
    }

    fun setCameraPos(pos: Vec2) {
        this.camera = pos
    }

    fun update() {
        this.camera = this.objects[0].pos() * -1.0
        this.camera -= this.objects[0].size()/2.0
        this.camera += Vec2(window.width/2.0, window.height/2.0)
        for (obj in this.objects) {
            obj.update(this.camera)
        }
    }
}

var camera = Vec2()
fun move(button: JButton, x: Int, y: Int) {
    val pos = button.location
    button.setLocation(pos.x + x + camera.x.toInt(), pos.y + y + camera.y.toInt())
}

fun moveRandom(button: JButton, window: JFrame) {
    val maxX = window.width - button.width
    val maxY = window.height - button.height
    val x = Random.nextInt(maxX)
    val y = Random.nextInt(maxY)
    button.setLocation(x, y)
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

fun colliding(a: JButton, b: JButton): Boolean {
    return  a.x < b.x + b.width &&
            a.x + a.width > b.x &&
            a.y < b.y + b.height &&
            a.height + a.y > b.y
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
        window.defaultCloseOperation= WindowConstants.EXIT_ON_CLOSE
        window.title = "Cool thing"
        window.setSize(1200, 700)
        window.layout = null

        val rootPane = window.rootPane
        rootPane.setLocation(100, 100)

//        ground.setBounds(0, 0, window.width, 20)
//        ground.setLocation(0, window.height - 100)
//        window.add(ground)

        val player = Object("Player",
            Vec2(),
            Vec2(200.0, 200.0)
        )
        val realGround = Object(
            "Ground",
            Vec2(window.width * -0.5, (window.height - 100).toDouble()),
            Vec2(window.width.toDouble() * 2.0, 20.0)
        )
        val a = Object("Thing",
            Vec2(window.width - 150.0, window.height - 200.0),
            Vec2(120.0, 120.0)
        )
        val b = Object("Thing",
            Vec2(0.0, 0.0),
            Vec2(120.0, 220.0)
        )
        val world = World(window, arrayOf(player, realGround, a, b))


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
                                    val offset = obstacle.y() - (player.width() - 1.0 + player.y())
                                    player.move(Vec2(0.0, offset))
                                }
                                Side.Bottom -> {

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