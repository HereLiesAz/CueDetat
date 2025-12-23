package com.hereliesaz.cuedetat.utils

import android.graphics.PointF
import android.opengl.Matrix

object ArMath {
    // 50 logical units / 0.05715 meters (57.15mm standard ball)
    const val METERS_TO_LOGICAL_SCALE = 874.89f

    /**
     * Converts a point from AR World Space to Logical Table Space.
     *
     * @param worldPoint The [x, y, z] coordinates in World Space.
     * @param tablePose The 4x4 Matrix representing the Table's Pose in World Space.
     * @return The logical PointF (x, y) relative to the table center.
     */
    fun worldToLogical(worldPoint: FloatArray, tablePose: FloatArray): PointF {
        // Invert the table pose matrix to get "World to Table" matrix
        val invertedTableMatrix = FloatArray(16)
        Matrix.invertM(invertedTableMatrix, 0, tablePose, 0)

        // Transform the world point by the inverted matrix
        val pointVector = floatArrayOf(worldPoint[0], worldPoint[1], worldPoint[2], 1f)
        val resultVector = FloatArray(4)
        Matrix.multiplyMV(resultVector, 0, invertedTableMatrix, 0, pointVector, 0)

        // Result is in Table Space (Meters)
        // Assuming Table Space is:
        // X = Right, Y = Forward (or similar, depending on ARCore plane coords)
        // Z = Up (Normal)
        // We map X/Z or X/Y to Logical X/Y.
        // ARCore Planes usually have Y as up-normal? No, ARCore World is Y-up.
        // But the Plane Pose defines the plane's local coordinate system X (right), Y (forward, approx), Z (up/normal).
        // So in "Plane Space", Z should be 0 (on surface).
        // So we use X and Y (or -Y depending on convention).
        // Logical coordinates: X is width, Y is length.
        // Let's assume standard mapping: X -> X, -Z -> Y (if Y is up).
        // Wait, Plane Pose:
        // The local coordinate system of the plane is defined such that the plane is the XY plane (Z=0),
        // with Z pointing along the plane normal.
        // So X and Y are the surface coordinates.

        // ARCore Plane Local Coordinate System:
        // Y is the Up vector (Normal to the plane).
        // X and Z are tangent to the plane.
        // We map the horizontal plane (XZ) to the Logical 2D Table (XY).

        val localX = resultVector[0]
        val localY = resultVector[2] // Use Z for the 2D plane depth

        // Scale to Logical
        return PointF(localX * METERS_TO_LOGICAL_SCALE, localY * METERS_TO_LOGICAL_SCALE)
    }
}
