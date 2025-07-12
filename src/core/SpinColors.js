// src/core/spinColors.js

const stops = [
    { angle: 0, color: { r: 255, g: 0, b: 0 } },      // Red
    { angle: 90, color: { r: 0, g: 0, b: 255 } },    // Blue
    { angle: 180, color: { r: 0, g: 255, b: 0 } },   // Green
    { angle: 270, color: { r: 255, g: 255, b: 0 } },  // Yellow
    { angle: 360, color: { r: 255, g: 0, b: 0 } }    // Red
];

export function lerp(a, b, t) {
    return a + (b - a) * t;
}

export function lerpColor(c1, c2, t) {
    return {
        r: Math.round(lerp(c1.r, c2.r, t)),
        g: Math.round(lerp(c1.g, c2.g, t)),
        b: Math.round(lerp(c1.b, c2.b, t))
    };
}

export function getColorFromAngleAndDistance(angleDegrees, distance) {
    const normalizedAngle = (angleDegrees + 360) % 360;

    let startStop = stops[0];
    let endStop = stops[stops.length-1];
    for (let i = 0; i < stops.length - 1; i++) {
        if (normalizedAngle >= stops[i].angle && normalizedAngle <= stops[i+1].angle) {
            startStop = stops[i];
            endStop = stops[i+1];
            break;
        }
    }

    const range = endStop.angle - startStop.angle;
    const fraction = range === 0 ? 0 : (normalizedAngle - startStop.angle) / range;

    const baseHueColor = lerpColor(startStop.color, endStop.color, fraction);

    // Interpolate between white and the hue based on distance
    const finalColor = lerpColor({ r: 255, g: 255, b: 255 }, baseHueColor, distance);

    return finalColor;
}