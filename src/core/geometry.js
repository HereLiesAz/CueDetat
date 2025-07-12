// src/core/geometry.js
import { insultingWarnings } from './warnings';
import { getColorFromAngleAndDistance } from './spinColors';

const BASE_BALL_RADIUS = 15;
const COLORS = {
  tableFelt: '#0a3d16',
  tableOutline: '#8EA96E',
  text: '#ffffff',
  ghostBall: 'rgba(255,255,255,0.4)',
  targetBall: '#BDA559',
  cueBall: '#8374A9',
  shotLine: '#8374A9',
  aimingLine: '#FFD000',
  tangentLine: '#A9A9A9',
  warning: '#C05D5D',
  pocket: '#000000',
  pocketed: '#FFFFFF',
  bankColors: ['#FFD000', '#DDC400', '#BBA800', '#998C00']
};

// ... (drawBall, drawLine, drawText functions remain unchanged) ...
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

function drawText(ctx, text, pos, color, angle = 0, zoomFactor = 1) {
    ctx.save();
    ctx.translate(pos.x, pos.y);
    ctx.rotate(angle);
    ctx.fillStyle = color;
    ctx.font = `bold ${14 * zoomFactor}px "barbaro", monospace`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, 0, 10 * zoomFactor);
    ctx.restore();
}


function getTableBoundaries(state) {
    const BALL_RADIUS = BASE_BALL_RADIUS * state.zoomFactor;
    const tableRatio = state.tableSize.ratio;
    const aspectRatio = state.tableSize.aspectRatio;
    const tablePlayingSurfaceHeight = (BALL_RADIUS * 2) * tableRatio;
    const tablePlayingSurfaceWidth = tablePlayingSurfaceHeight * aspectRatio;
    const { viewWidth, viewHeight } = state;
    const left = (viewWidth - tablePlayingSurfaceWidth) / 2;
    const top = (viewHeight - tablePlayingSurfaceHeight) / 2;
    const right = left + tablePlayingSurfaceWidth;
    const bottom = top + tablePlayingSurfaceHeight;
    return { left, top, right, bottom, width: tablePlayingSurfaceWidth, height: tablePlayingSurfaceHeight };
}

function drawTable(ctx, state) {
    const { left, top, width, height } = getTableBoundaries(state);
    ctx.fillStyle = COLORS.tableFelt;
    ctx.fillRect(left, top, width, height);
    ctx.strokeStyle = COLORS.tableOutline;
    ctx.lineWidth = 5 * state.zoomFactor;
    ctx.strokeRect(left, top, width, height);
    const pocketRadius = BASE_BALL_RADIUS * 1.8 * state.zoomFactor;
    const pockets = [
        { x: left, y: top }, { x: left + width/2, y: top }, { x: left + width, y: top },
        { x: left, y: top + height }, { x: left + width/2, y: top + height }, { x: left + width, y: top + height }
    ];
    pockets.forEach(p => {
        ctx.fillStyle = COLORS.pocket;
        ctx.beginPath();
        ctx.arc(p.x, p.y, pocketRadius, 0, 2 * Math.PI);
        ctx.fill();
    });
}

function calculateSpinPaths(state) {
    const spinOffset = state.lingeringSpinOffset || state.selectedSpinOffset;
    if (!spinOffset) return []; // No spin, no paths

    const { ghostBall, BALL_RADIUS } = calculateProtractorGeometry(state);
    const tangentAngleRad = Math.atan2(ghostBall.y - state.targetBall.y, ghostBall.x - state.targetBall.x) + Math.PI / 2;
    const tangentDx = Math.cos(tangentAngleRad);
    const tangentDy = Math.sin(tangentAngleRad);

    const spinControlRadius = 60;
    const spinMagnitude = Math.hypot(spinOffset.x - spinControlRadius, spinOffset.y - spinControlRadius) / spinControlRadius;
    const spinAngle = Math.atan2(spinOffset.y - spinControlRadius, spinOffset.x - spinControlRadius);

    const maxCurveOffset = BALL_RADIUS * 2.5;
    const curveAmount = spinMagnitude * spinMagnitude * maxCurveOffset;

    const maxPathLength = 20 * BALL_RADIUS;

    const controlPoint1 = {
        x: ghostBall.x + tangentDx * maxPathLength * 0.33,
        y: ghostBall.y + tangentDy * maxPathLength * 0.33,
    };
    const endPoint = {
        x: ghostBall.x + tangentDx * maxPathLength + (curveAmount * Math.cos(spinAngle)),
        y: ghostBall.y + tangentDy * maxPathLength + (curveAmount * Math.sin(spinAngle)),
    };
    const controlPoint2 = {
        x: endPoint.x - tangentDx * maxPathLength * 0.33,
        y: endPoint.y - tangentDy * maxPathLength * 0.33,
    };

    const color = getColorFromAngleAndDistance(spinAngle * (180/Math.PI), spinMagnitude);
    return [{ p0: ghostBall, p1: controlPoint1, p2: controlPoint2, p3: endPoint, color }];
}

function drawSpinPaths(ctx, state) {
    if (!state.spinPaths || state.spinPaths.length === 0) return;

    state.spinPaths.forEach(path => {
        ctx.beginPath();
        ctx.moveTo(path.p0.x, path.p0.y);
        ctx.bezierCurveTo(path.p1.x, path.p1.y, path.p2.x, path.p2.y, path.p3.x, path.p3.y);
        ctx.strokeStyle = `rgb(${path.color.r}, ${path.color.g}, ${path.color.b})`;
        ctx.lineWidth = 2 * state.zoomFactor;
        ctx.stroke();
    });
}

function calculateBankShotPath(state) {
    const bounds = getTableBoundaries(state);
    if (!state.onPlaneBall) return [];
    let currentPos = state.onPlaneBall;
    const aimTarget = state.bankingAimTarget;
    let dx = aimTarget.x - currentPos.x;
    let dy = aimTarget.y - currentPos.y;
    const mag = Math.hypot(dx, dy);
    if (mag === 0) return [];
    dx /= mag; dy /= mag;

    const path = [{...currentPos}];
    const maxBounces = 4;

    for (let i = 0; i < maxBounces; i++) {
        let t = Infinity;
        let wallNormal = null;

        if (dx !== 0) {
            const tLeft = (bounds.left - currentPos.x) / dx;
            const tRight = (bounds.right - currentPos.x) / dx;
            if (tLeft > 0.001 && tLeft < t) { t = tLeft; wallNormal = {x: 1, y: 0}; }
            if (tRight > 0.001 && tRight < t) { t = tRight; wallNormal = {x: -1, y: 0}; }
        }
        if (dy !== 0) {
            const tTop = (bounds.top - currentPos.y) / dy;
            const tBottom = (bounds.bottom - currentPos.y) / dy;
            if (tTop > 0.001 && tTop < t) { t = tTop; wallNormal = {x: 0, y: 1}; }
            if (tBottom > 0.001 && tBottom < t) { t = tBottom; wallNormal = {x: 0, y: -1}; }
        }

        if (t === Infinity) break;

        const nextPos = { x: currentPos.x + dx * t, y: currentPos.y + dy * t };
        path.push(nextPos);
        currentPos = nextPos;

        const dot = dx * wallNormal.x + dy * wallNormal.y;
        dx -= 2 * dot * wallNormal.x;
        dy -= 2 * dot * wallNormal.y;
    }
    return path;
}


function calculateProtractorGeometry(state) {
    const BALL_RADIUS = BASE_BALL_RADIUS * state.zoomFactor;
    const { targetBall, aimingAngle, showOnPlaneBall, onPlaneBall } = state;

    const angleRad = aimingAngle * (Math.PI / 180);
    const ghostX = targetBall.x - (BALL_RADIUS * 2) * Math.cos(angleRad);
    const ghostY = targetBall.y - (BALL_RADIUS * 2) * Math.sin(angleRad);
    const ghostBall = { x: ghostX, y: ghostY };

    const shotLineAnchor = showOnPlaneBall ? onPlaneBall : { x: ghostBall.x, y: ghostBall.y + (200 * state.zoomFactor) };
    const distOriginToGhost = Math.hypot(ghostBall.x - shotLineAnchor.x, ghostBall.y - shotLineAnchor.y);
    const distOriginToTarget = Math.hypot(targetBall.x - shotLineAnchor.x, targetBall.y - shotLineAnchor.y);
    const isImpossibleShot = distOriginToGhost > distOriginToTarget && Math.hypot(targetBall.x - ghostBall.x, targetBall.y - ghostBall.y) > BALL_RADIUS * 2.01;

    return { ghostBall, shotLineAnchor, isImpossibleShot, BALL_RADIUS };
}

export function drawScene(ctx, state, dispatch) {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  ctx.fillStyle = COLORS.tableFelt;
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

  if (state.showTable) {
      drawTable(ctx, state);
  }

  if (state.mode === 'protractor') {
    const { ghostBall, shotLineAnchor, isImpossibleShot, BALL_RADIUS } = calculateProtractorGeometry(state);
    if (state.isImpossibleShot !== isImpossibleShot) {
        dispatch({ type: 'SET_IMPOSSIBLE_SHOT', payload: isImpossibleShot });
    }

    const shotLineColor = isImpossibleShot ? COLORS.warning : COLORS.shotLine;
    const tangentAngleRad = Math.atan2(ghostBall.y - state.targetBall.y, ghostBall.x - state.targetBall.x) + Math.PI / 2;
    const aimingAngleRad = tangentAngleRad - Math.PI / 2;
    const extendFactor = Math.max(state.viewWidth, state.viewHeight) * 2;

    const tangentEnd1 = { x: ghostBall.x + extendFactor * Math.cos(tangentAngleRad), y: ghostBall.y + extendFactor * Math.sin(tangentAngleRad) };
    const tangentEnd2 = { x: ghostBall.x - extendFactor * Math.cos(tangentAngleRad), y: ghostBall.y - extendFactor * Math.sin(tangentAngleRad) };
    const aimingLineEnd = { x: state.targetBall.x + extendFactor * Math.cos(aimingAngleRad), y: state.targetBall.y + extendFactor * Math.sin(aimingAngleRad)};

    drawLine(ctx, tangentEnd1, tangentEnd2, COLORS.tangentLine, 1 * state.zoomFactor, true);
    drawLine(ctx, ghostBall, aimingLineEnd, COLORS.aimingLine, 2 * state.zoomFactor);
    drawLine(ctx, shotLineAnchor, ghostBall, shotLineColor, 2 * state.zoomFactor);

    drawSpinPaths(ctx, state);

    drawBall(ctx, state.targetBall, BALL_RADIUS, COLORS.targetBall, 'dot');
    drawBall(ctx, ghostBall, BALL_RADIUS, isImpossibleShot ? COLORS.warning : COLORS.ghostBall, 'crosshair');
    if (state.showOnPlaneBall) {
      drawBall(ctx, state.onPlaneBall, BALL_RADIUS, COLORS.cueBall, 'dot');
    }

    if (state.showHelpers) {
        const labelOffset = BALL_RADIUS * 1.5 + 10;
        drawText(ctx, "Target Ball", { x: state.targetBall.x, y: state.targetBall.y - labelOffset }, COLORS.text, 0, state.zoomFactor);
        drawText(ctx, "Ghost Cue Ball", { x: ghostBall.x, y: ghostBall.y - labelOffset }, COLORS.text, 0, state.zoomFactor);
        if (state.showOnPlaneBall) {
            drawText(ctx, "Actual Cue Ball", { x: state.onPlaneBall.x, y: state.onPlaneBall.y - labelOffset }, COLORS.text, 0, state.zoomFactor);
        }
        const shotLineMid = {x: (shotLineAnchor.x + ghostBall.x)/2, y: (shotLineAnchor.y + ghostBall.y)/2};
        const shotAngle = Math.atan2(ghostBall.y - shotLineAnchor.y, ghostBall.x - shotLineAnchor.x);
        drawText(ctx, "Shot Guide Line", shotLineMid, COLORS.text, shotAngle, state.zoomFactor);

        const aimingLineMid = {x: (ghostBall.x + state.targetBall.x)/2, y: (ghostBall.y + state.targetBall.y)/2};
        drawText(ctx, "Aiming Line", aimingLineMid, COLORS.text, aimingAngleRad, state.zoomFactor);
    }
  } else if (state.mode === 'banking') {
    const bankPath = calculateBankShotPath(state);
    dispatch({ type: 'SET_BANK_PATH', payload: bankPath });

    const BALL_RADIUS = BASE_BALL_RADIUS * state.zoomFactor;
    if (state.onPlaneBall) {
        drawBall(ctx, state.onPlaneBall, BALL_RADIUS, COLORS.cueBall, 'dot');
    }

    for (let i = 0; i < state.bankShotPath.length - 1; i++) {
        const start = state.bankShotPath[i];
        const end = state.bankShotPath[i+1];
        const color = COLORS.bankColors[i] || COLORS.bankColors.at(-1);
        drawLine(ctx, start, end, color, (4 - i) * 1.5 * state.zoomFactor);
    }
  }
}

// Attach the new geometry function to be accessible
export { calculateSpinPaths };