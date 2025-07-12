// src/core/perspective.js

/**
 * Creates a 4x4 perspective projection matrix.
 * @param {number} pitch - The rotation around the X-axis in degrees.
 * @param {number} viewWidth - The width of the viewport.
 * @param {number} viewHeight - The height of the viewport.
 * @returns {DOMMatrix} The calculated 4x4 pitch matrix.
 */
export function createPitchMatrix(pitch, viewWidth, viewHeight) {
    const matrix = new DOMMatrix();
    const halfWidth = viewWidth / 2;
    const halfHeight = viewHeight / 2;

    // The world pivots around the logical origin (0,0), which must be at the screen center.
    // 1. Translate so the logical origin is at the canvas origin.
    matrix.translateSelf(-halfWidth, -halfHeight, 0);

    // 2. Apply the perspective pitch.
    // A simplified perspective effect. The larger the distance, the more extreme the perspective.
    const perspectiveDistance = viewHeight * 2;
    matrix.m34 = -1 / perspectiveDistance;
    matrix.rotateSelf(pitch, 0, 0);

    // 3. Translate back to the screen center.
    matrix.translateSelf(halfWidth, halfHeight, 0);

    return matrix;
}

/**
 * Transforms a 2D logical point into a 2D screen point using a 4x4 matrix.
 * @param {object} logicalPoint - The point in the logical coordinate system {x, y}.
 * @param {DOMMatrix} matrix - The transformation matrix.
 * @returns {object} The transformed point in the screen coordinate system {x, y}.
 */
export function transformPoint(logicalPoint, matrix) {
    const point = new DOMPoint(logicalPoint.x, logicalPoint.y, 0);
    const transformedPoint = point.matrixTransform(matrix);

    // Apply perspective division if w is not 1
    if (transformedPoint.w !== 0 && transformedPoint.w !== 1) {
        return {
            x: transformedPoint.x / transformedPoint.w,
            y: transformedPoint.y / transformedPoint.w
        };
    }
    return { x: transformedPoint.x, y: transformedPoint.y };
}a