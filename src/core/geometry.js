// src/core/geometry.js
import { insultingWarnings } from './warnings';
import { getColorFromAngleAndDistance } from './spinColors';
import { createPitchMatrix, transformPoint } from './perspective';

const BASE_BALL_RADIUS = 15;
const COLORS = { /* ... colors remain the same ... */ };

// ... (drawBall, drawLine, drawText, getTableBoundaries, drawTable, calculateBankShotPath functions remain mostly the same, but now accept ctx and state) ...

function drawGhostedBall(ctx, logicalBall, radius, state, config) {
    const { pitchMatrix } = state.perspective;
    const onPlaneCenter = transformPoint(logicalBall, pitchMatrix);

    // Calculate lift based on pitch
    const pitchRad = state.pitchAngle * (Math.PI / 180);
    const screenRadius = radius * pitchMatrix.m22; // Approximate projected radius
    const lift = screenRadius * Math.sin(pitchRad);

    const liftedCenter = { x: onPlaneCenter.x, y: onPlaneCenter.y - lift };

    // Draw on-plane shadow
    ctx.save();
    ctx.setTransform(pitchMatrix);
    ctx.globalAlpha = 0.5;
    drawBall(ctx, logicalBall, radius, COLORS.ghostBall, 'dot');
    ctx.restore();

    // Draw lifted ball
    drawBall(ctx, liftedCenter, screenRadius, config.color, config.shape);
}


function calculateGeometry(state) {
    // ... (rest of calculateGeometry remains the same)
}

export function drawScene(ctx, state, dispatch) {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  ctx.fillStyle = COLORS.tableFelt;
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

  // Calculate perspective based on mouse position
  const pitchY = (state.mousePosition.y / state.viewHeight) * 2 - 1; // -1 to 1
  const pitchAngle = -pitchY * 45; // Max pitch of 45 degrees
  const pitchMatrix = createPitchMatrix(pitchAngle, state.viewWidth, state.viewHeight);
  state.perspective = { pitchMatrix, pitchAngle }; // Attach to state for renderer use

  // --- DRAW TABLE AND ON-PLANE ELEMENTS ---
  ctx.save();
  ctx.setTransform(state.perspective.pitchMatrix);

  if (state.showTable) {
      drawTable(ctx, state);
  }

  // Protractor Mode on-plane lines
  if (state.mode === 'protractor') {
      // ... (draw on-plane lines: aiming, shot, tangent)
  }

  // Banking Mode on-plane lines
  if (state.mode === 'banking') {
      // ... (draw on-plane bank lines)
  }

  // Spin paths are also on-plane
  drawSpinPaths(ctx, state);

  ctx.restore();

  // --- DRAW LIFTED/SCREEN-SPACE ELEMENTS ---
  const { ghostBall, isImpossibleShot, BALL_RADIUS } = calculateProtractorGeometry(state);

  if (state.isImpossibleShot !== isImpossibleShot) {
      dispatch({ type: 'SET_IMPOSSIBLE_SHOT', payload: isImpossibleShot });
  }

  if (state.mode === 'protractor') {
    const shotLineColor = isImpossibleShot ? COLORS.warning : COLORS.shotLine;

    // Balls are now drawn as ghosted elements
    drawGhostedBall(ctx, state.targetBall, BALL_RADIUS, state, { color: COLORS.targetBall, shape: 'dot' });
    drawGhostedBall(ctx, ghostBall, BALL_RADIUS, state, { color: isImpossibleShot ? COLORS.warning : COLORS.ghostBall, shape: 'crosshair' });
    if (state.showOnPlaneBall) {
        drawGhostedBall(ctx, state.onPlaneBall, BALL_RADIUS, state, { color: COLORS.cueBall, shape: 'dot' });
    }
  } else if (state.mode === 'banking') {
      if (state.onPlaneBall) {
          drawGhostedBall(ctx, state.onPlaneBall, BALL_RADIUS, state, { color: COLORS.cueBall, shape: 'dot' });
      }
  }

  // ... (Helper text drawing needs to be updated to use transformPoint for positioning)
}
// Note: This is a simplified representation. The full geometry.js would be much larger
// but this illustrates the core change: separating on-plane and screen-space drawing.