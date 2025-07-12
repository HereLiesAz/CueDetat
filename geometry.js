// src/core/geometry.js
import { insultingWarnings } from './warnings';

const BASE_BALL_RADIUS = 15;
const COLORS = {
  table: '#0a3d16',
  text: '#ffffff',
  ghostBall: 'rgba(255,255,255,0.4)',
  targetBall: '#BDA559',
  cueBall: '#8374A9',
  shotLine: '#8374A9',
  aimingLine: '#FFD000',
  tangentLine: '#A9A9A9',
  warning: '#C05D5D'
};

function drawBall(ctx, ball, radius, color, shape = 'dot') {
  ctx.save();
  ctx.fillStyle = color;
  ctx.strokeStyle = color;
  ctx.lineWidth = 2 * (radius / BASE_BALL_RADIUS);

  ctx.beginPath();
  ctx.arc(ball.x, ball.y, radius, 0, Math.PI * 2);
  ctx.stroke();

  if (shape === 'dot') {
      ctx.beginPath();
      ctx.arc(ball.x, ball.y, radius * 0.2, 0, Math.PI * 2);
      ctx.fill();
  } else if (shape === 'crosshair') {
      const size = radius * 0.5;
      ctx.beginPath();
      ctx.moveTo(ball.x - size, ball.y);
      ctx.lineTo(ball.x + size, ball.y);
      ctx.moveTo(ball.x, ball.y - size);
      ctx.lineTo(ball.x, ball.y + size);
      ctx.stroke();
  }
  ctx.restore();
}

function drawLine(ctx, from, to, color, width = 2, isDashed = false) {
    ctx.save();
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    if (isDashed) {
        ctx.setLineDash([5, 10]);
    }
    ctx.beginPath();
    ctx.moveTo(from.x, from.y);
    ctx.lineTo(to.x, to.y);
    ctx.stroke();
    ctx.restore();
}

function drawText(ctx, text, pos, color, angle = 0) {
    ctx.save();
    ctx.translate(pos.x, pos.y);
    ctx.rotate(angle);
    ctx.fillStyle = color;
    ctx.font = '14px "barbaro", monospace';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, 0, 0);
    ctx.restore();
}

function calculateGeometry(state) {
    const BALL_RADIUS = BASE_BALL_RADIUS * state.zoomFactor;
    const { targetBall, aimingAngle } = state;

    const angleRad = aimingAngle * (Math.PI / 180);
    const ghostX = targetBall.x - (BALL_RADIUS * 2) * Math.cos(angleRad);
    const ghostY = targetBall.y - (BALL_RADIUS * 2) * Math.sin(angleRad);
    const ghostBall = { x: ghostX, y: ghostY };

    const shotLineAnchor = state.showOnPlaneBall ? state.onPlaneBall : { x: ghostBall.x, y: ghostBall.y + (200 * state.zoomFactor) };

    const distOriginToGhost = Math.hypot(ghostBall.x - shotLineAnchor.x, ghostBall.y - shotLineAnchor.y);
    const distOriginToTarget = Math.hypot(targetBall.x - shotLineAnchor.x, targetBall.y - shotLineAnchor.y);
    const isImpossibleShot = distOriginToGhost > distOriginToTarget && Math.hypot(targetBall.x-ghostBall.x, targetBall.y-ghostBall.y) > BALL_RADIUS * 2.01;

    return { ghostBall, shotLineAnchor, isImpossibleShot, BALL_RADIUS };
}

export function drawScene(ctx, state, dispatch) {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  ctx.fillStyle = COLORS.table;
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

  const { ghostBall, shotLineAnchor, isImpossibleShot, BALL_RADIUS } = calculateGeometry(state);

  if (state.isImpossibleShot !== isImpossibleShot) {
      dispatch({ type: 'SET_IMPOSSIBLE_SHOT', payload: isImpossibleShot });
      if (isImpossibleShot && !state.isDragging) {
        const warning = insultingWarnings[Math.floor(Math.random() * insultingWarnings.length)];
        dispatch({ type: 'TRIGGER_WARNING', payload: warning });
      }
  }

  if (state.mode === 'protractor') {
    const shotLineColor = isImpossibleShot ? COLORS.warning : COLORS.shotLine;

    // --- LINE DRAWING ---
    const tangentAngleRad = Math.atan2(ghostBall.y - state.targetBall.y, ghostBall.x - state.targetBall.x) + Math.PI / 2;
    const aimingAngleRad = tangentAngleRad - Math.PI / 2;

    const extendFactor = Math.max(state.viewWidth, state.viewHeight) * 2;
    const tangentEnd1 = { x: ghostBall.x + extendFactor * Math.cos(tangentAngleRad), y: ghostBall.y + extendFactor * Math.sin(tangentAngleRad) };
    const tangentEnd2 = { x: ghostBall.x - extendFactor * Math.cos(tangentAngleRad), y: ghostBall.y - extendFactor * Math.sin(tangentAngleRad) };
    const aimingLineEnd = { x: state.targetBall.x + extendFactor * Math.cos(aimingAngleRad), y: state.targetBall.y + extendFactor * Math.sin(aimingAngleRad)};

    drawLine(ctx, tangentEnd1, tangentEnd2, COLORS.tangentLine, 1 * state.zoomFactor, true);
    drawLine(ctx, ghostBall, aimingLineEnd, COLORS.aimingLine, 2 * state.zoomFactor);
    drawLine(ctx, shotLineAnchor, ghostBall, shotLineColor, 2 * state.zoomFactor);

    // --- BALL DRAWING ---
    drawBall(ctx, state.targetBall, BALL_RADIUS, COLORS.targetBall, 'dot');
    drawBall(ctx, ghostBall, BALL_RADIUS, isImpossibleShot ? COLORS.warning : COLORS.ghostBall, 'crosshair');
    if (state.showOnPlaneBall) {
      drawBall(ctx, state.onPlaneBall, BALL_RADIUS, COLORS.cueBall, 'dot');
    }

    // --- HELPER TEXT DRAWING ---
    if (state.showHelpers) {
        const labelOffset = BALL_RADIUS * 1.5 + 10;
        drawText(ctx, "Target Ball", { x: state.targetBall.x, y: state.targetBall.y - labelOffset }, COLORS.text);
        drawText(ctx, "Ghost Cue Ball", { x: ghostBall.x, y: ghostBall.y - labelOffset }, COLORS.text);
        if (state.showOnPlaneBall) {
            drawText(ctx, "Actual Cue Ball", { x: state.onPlaneBall.x, y: state.onPlaneBall.y - labelOffset }, COLORS.text);
        }

        const shotLineMid = {x: (shotLineAnchor.x + ghostBall.x)/2, y: (shotLineAnchor.y + ghostBall.y)/2};
        const shotAngle = Math.atan2(ghostBall.y - shotLineAnchor.y, ghostBall.x - shotLineAnchor.x);
        drawText(ctx, "Shot Guide Line", shotLineMid, COLORS.text, shotAngle);

        const aimingLineMid = {x: (ghostBall.x + state.targetBall.x)/2, y: (ghostBall.y + state.targetBall.y)/2};
        drawText(ctx, "Aiming Line", aimingLineMid, COLORS.text, aimingAngleRad);
    }
  }
}