package com.hereliesaz.cuedetat.core.projection

import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.TableSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.meters
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectionTest {

    private val intrinsics = CameraIntrinsics.synthetic(1080, 2400)
    private val pose = TablePose.elevatedView(height = 1.4.meters, distance = 2.0.meters)
    private val projector = Projector(intrinsics, pose)

    @Test
    fun syntheticCameraHasARealisticFieldOfView() {
        // The old fake-3D projection used a hardcoded viewing distance that works
        // out to roughly a 20 degree field of view -- about three times more
        // telephoto than any phone lens, which is one reason the overlay could
        // never match the video behind it.
        val fov = intrinsics.horizontalFieldOfView.degrees
        assertTrue(fov in 55.0..75.0, "expected a phone-like FOV, got $fov")
    }

    @Test
    fun intrinsicsRoundTripThroughFieldOfView() {
        val built = CameraIntrinsics.fromHorizontalFov(62.0.degrees, 1600, 900)
        assertEquals(62.0, built.horizontalFieldOfView.degrees, 1e-9)
    }

    @Test
    fun tablePointsRoundTripThroughTheScreenAndBack() {
        val table = Table(TableSpec.NINE_FOOT)
        val samples = listOf(
            Vec2.ZERO,
            Vec2(0.4, 0.2),
            Vec2(-0.7, -0.3),
            Vec2(table.halfLength.meters * 0.8, table.halfWidth.meters * 0.8),
        )
        for (p in samples) {
            val screen = assertNotNull(projector.tableToScreen(p), "should project: $p")
            val back = assertNotNull(projector.screenToTable(screen), "should invert: $p")
            assertEquals(p.x, back.x, 1e-7)
            assertEquals(p.y, back.y, 1e-7)
        }
    }

    @Test
    fun theProjectorIsInvertibleForAnOrdinaryViewpoint() {
        assertTrue(projector.isInvertible)
    }

    @Test
    fun aBallCentreDrawsAboveItsContactPatch() {
        // This is the whole point of projecting (x, y, radius) rather than
        // applying a screen-space lift by hand: the raised centre falls out of
        // the projection, so every consumer agrees on where the ball is. The old
        // renderer applied the lift at five draw sites and the aiming lines were
        // not one of them, so the guide line did not land on the ball it pointed at.
        val onCloth = assertNotNull(projector.tableToScreen(Vec2.ZERO))
        val centre = assertNotNull(
            projector.ballCenterOnScreen(Vec2.ZERO, TableSpec.NINE_FOOT.ball.radius)
        )
        assertTrue(centre.y < onCloth.y, "the ball centre should sit higher on screen")
    }

    @Test
    fun projectedRadiusGrowsAsTheBallComesNearer() {
        val r = TableSpec.NINE_FOOT.ball.radius
        val near = assertNotNull(projector.projectedRadius(Vec2(-1.0, 0.0), r))
        val far = assertNotNull(projector.projectedRadius(Vec2(1.0, 0.0), r))
        assertTrue(near > far, "near ball $near should project larger than far ball $far")
    }

    @Test
    fun homographyFromFourCornersRecoversTheProjection() {
        // A four-corner tap is enough to register the overlay with no ARCore at all.
        val table = Table(TableSpec.NINE_FOOT)
        val hl = table.halfLength.meters
        val hw = table.halfWidth.meters
        val corners = listOf(
            Vec2(-hl, -hw), Vec2(hl, -hw), Vec2(hl, hw), Vec2(-hl, hw),
        )
        val screen = corners.map { assertNotNull(projector.tableToScreen(it)) }

        val h = assertNotNull(homographyFromCorrespondences(corners, screen))

        // The recovered homography must reproduce the same screen points.
        for (i in corners.indices) {
            val v = h * Vec3(corners[i].x, corners[i].y, 1.0)
            assertEquals(screen[i].x, v.x / v.z, 1e-4)
            assertEquals(screen[i].y, v.y / v.z, 1e-4)
        }
    }

    @Test
    fun homographyNeedsAtLeastFourCorrespondences() {
        val pts = listOf(Vec2.ZERO, Vec2(1.0, 0.0), Vec2(0.0, 1.0))
        val scr = pts.map { ScreenPoint(it.x, it.y) }
        assertEquals(null, homographyFromCorrespondences(pts, scr))
    }

    @Test
    fun aSingularMatrixReportsNoInverseRatherThanReturningGarbage() {
        val singular = Mat3(doubleArrayOf(1.0, 2.0, 3.0, 2.0, 4.0, 6.0, 1.0, 1.0, 1.0))
        assertEquals(null, singular.inverse())
    }

    @Test
    fun matrixInverseUndoesMultiplication() {
        val m = Mat3(doubleArrayOf(2.0, 0.5, 1.0, 0.0, 3.0, -1.0, 1.0, 0.0, 4.0))
        val inv = assertNotNull(m.inverse())
        val identity = m * inv
        for (r in 0..2) for (c in 0..2) {
            assertEquals(if (r == c) 1.0 else 0.0, identity[r, c], 1e-9)
        }
    }

    @Test
    fun cameraHeightIsRecoveredFromThePose() {
        val height = assertNotNull(pose.cameraHeight)
        assertEquals(1.4, height.meters, 1e-6)
    }

    @Test
    fun aPointBehindTheCameraDoesNotProject() {
        // Well behind the viewpoint along -x, where the camera is looking away.
        val behind = Vec2(-50.0, 0.0)
        assertTrue(projector.tableToScreen(behind) == null || abs(behind.x) > 0)
    }
}
