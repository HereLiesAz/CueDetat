package com.hereliesaz.cuedetat.ui.hatemode

import android.content.Context
import android.graphics.BlurMaskFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import de.chaffic.dynamics.Body
import de.chaffic.dynamics.World
import de.chaffic.geometry.Polygon
import de.chaffic.math.Vec2
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.random.Random

@Composable
fun HaterState(
    modifier: Modifier = Modifier,
    viewModel: HaterViewModel = viewModel()
) {
    val ballState by viewModel.ballState
    val answer by viewModel.answer

    // --- Physics and View State ---
    val world = remember { World(gravity = Vec2(0, 0)) }
    var dieBody by remember { mutableStateOf<Body?>(null) }
    var triangleVertices by remember { mutableStateOf(emptyArray<Vec2>()) }
    val wallBodies = remember { mutableStateListOf<Body>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var tick by remember { mutableStateOf(0L) }
    var staticLayout by remember { mutableStateOf<StaticLayout?>(null) }


    val textPaint = remember {
        TextPaint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
        }
    }
    val density = LocalDensity.current

    // --- Animation Values ---
    val dieAnimationProgress by animateFloatAsState(
        targetValue = if (ballState == TriangleState.SUBMERGING) 0f else 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "Die Visibility"
    )

    // --- Sensor Management ---
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensorDataManager = SensorDataManager { viewModel.onShake() }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sensorManager.registerListener(
                    sensorDataManager,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                sensorManager.unregisterListener(sensorDataManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(sensorDataManager)
        }
    }

    // --- Physics Loop ---
    LaunchedEffect(Unit) {
        while (true) {
            world.step((1f / 60f).toDouble())
            tick = System.nanoTime()
            delay(1000L / 60L)
        }
    }

    // --- Die Resizing and Text Layout Logic ---
    LaunchedEffect(answer, density, canvasSize.width) {
        // Don't try to measure if we don't have a canvas width yet.
        if (canvasSize.width == 0f) return@LaunchedEffect

        // 1. Configure the paint for the current density
        textPaint.textSize = with(density) { 16.sp.toPx() }

        // 2. Create a StaticLayout to correctly measure multi-line text.
        // The width is constrained to be slightly less than the canvas width to avoid edge cases.
        val layoutWidth = (canvasSize.width * 0.7f).toInt()
        val createdLayout =
            StaticLayout.Builder.obtain(answer, 0, answer.length, textPaint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
        staticLayout = createdLayout

        // 3. Get the actual measured width and height from the layout.
        val textWidth = createdLayout.width.toFloat()
        val textHeight = createdLayout.height.toFloat()

        // 4. Calculate triangle size based on the measured text block.
        val padding = with(density) { 30.dp.toPx() }
        val triangleHeight = textHeight + padding
        val sideLength = (triangleHeight / (kotlin.math.sqrt(3.0) / 2.0)).toFloat()
        val triangleWidth = max(textWidth + padding, sideLength)

        val halfHeight = triangleHeight / 2
        val halfWidth = triangleWidth / 2

        // 5. Define new vertices and recreate the physics body.
        val newVertices = arrayOf(
            Vec2(0, -halfHeight),
            Vec2(-halfWidth, halfHeight),
            Vec2(halfWidth, halfHeight)
        )
        triangleVertices = newVertices

        dieBody?.let { world.destroyBody(it) }
        dieBody = world.createBody(
            type = BodyType.Dynamic,
            position = Vec2(Random.nextFloat() * 50 - 25, Random.nextFloat() * 50 - 25),
            angle = Random.nextFloat() * 2 * PI.toFloat(),
            collider = Polygon(*newVertices),
            friction = 0.6f,
            density = 1.0f
        ).apply {
            linearDamping = 5.0f
            angularDamping = 7.0f
        }
    }

    // --- Screen Boundary Wall Creation ---
    LaunchedEffect(canvasSize) {
        if (canvasSize == Size.Zero) return@LaunchedEffect

        wallBodies.forEach { world.destroyBody(it) }
        wallBodies.clear()

        val width = canvasSize.width
        val height = canvasSize.height
        val thickness = 100f
        val halfW = width / 2
        val halfH = height / 2

        wallBodies.add(
            world.createBody(
                BodyType.Static,
                position = Vec2(0, -halfH - thickness / 2),
                collider = Polygon.fromBox(width, thickness)
            )
        )
        wallBodies.add(
            world.createBody(
                BodyType.Static,
                position = Vec2(0, halfH + thickness / 2),
                collider = Polygon.fromBox(width, thickness)
            )
        )
        wallBodies.add(
            world.createBody(
                BodyType.Static,
                position = Vec2(-halfW - thickness / 2, 0),
                collider = Polygon.fromBox(thickness, height)
            )
        )
        wallBodies.add(
            world.createBody(
                BodyType.Static,
                position = Vec2(halfW + thickness / 2, 0),
                collider = Polygon.fromBox(thickness, height)
            )
        )
    }


    Box(modifier = modifier.fillMaxSize()) {
        val glowPaint = remember {
            Paint().asFrameworkPaint().apply {
                color = Color(0x993366FF).toArgb()
                maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
            }
        }
        val trianglePaint = remember { Paint().apply { color = Color(0xFF3366FF) } }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dieBody) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            dieBody?.applyLinearImpulse(
                                Vec2(dragAmount.x, dragAmount.y) * 0.05f,
                                dieBody!!.position
                            )
                            change.consume()
                        }
                    )
                }
        ) {
            tick
            if (canvasSize != size) {
                canvasSize = size
            }

            val centerX = size.width / 2
            val centerY = size.height / 2

            drawRect(color = Color(0xFF050A1D))

            if (dieBody != null && triangleVertices.isNotEmpty() && staticLayout != null) {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(centerX, centerY)

                    val dieX = dieBody!!.position.x
                    val dieY = dieBody!!.position.y
                    val dieAngle = dieBody!!.angle * (180f / Math.PI.toFloat())

                    canvas.translate(dieX, dieY)
                    canvas.rotate(dieAngle)
                    canvas.scale(dieAnimationProgress, dieAnimationProgress)

                    val trianglePath = Path().apply {
                        moveTo(triangleVertices[0].x, triangleVertices[0].y)
                        lineTo(triangleVertices[1].x, triangleVertices[1].y)
                        lineTo(triangleVertices[2].x, triangleVertices[2].y)
                        close()
                    }

                    textPaint.alpha = (255 * dieAnimationProgress).toInt()

                    canvas.nativeCanvas.drawPath(trianglePath.asAndroidPath(), glowPaint)
                    canvas.drawPath(path = trianglePath, paint = trianglePaint)

                    // Draw the multi-line text using the StaticLayout
                    canvas.save()
                    // Translate to center the text block within the triangle
                    canvas.translate(-staticLayout!!.width / 2f, -staticLayout!!.height / 2f)
                    staticLayout!!.draw(canvas.nativeCanvas)
                    canvas.restore()

                    canvas.restore()
                }
            }
        }
    }
}
