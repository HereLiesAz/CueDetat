// /core/geometry.js

export function drawScene(ctx, state) {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

  drawTable(ctx);

  if (state.mode === 'protractor') {
    drawGhostBall(ctx, state);
    drawCueLine(ctx, state);
  } else {
    drawBankShot(ctx, state);
  }
}

export function drawTable(ctx) {
  ctx.fillStyle = '#1a1a1a';
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  ctx.strokeStyle = '#444';
  ctx.strokeRect(10, 10, ctx.canvas.width - 20, ctx.canvas.height - 20);
}

export function drawCueLine(ctx, state) {
  const { cueBall, ghostBall } = state;
  ctx.strokeStyle = '#00ffcc';
  ctx.beginPath();
  ctx.moveTo(cueBall.x, cueBall.y);
  ctx.lineTo(ghostBall.x, ghostBall.y);
  ctx.stroke();
}

export function drawGhostBall(ctx, state) {
  const { targetBall, cueBall } = state;
  const angle = Math.atan2(targetBall.y - cueBall.y, targetBall.x - cueBall.x);
  const ghostX = targetBall.x - 30 * Math.cos(angle);
  const ghostY = targetBall.y - 30 * Math.sin(angle);

  state.ghostBall = { x: ghostX, y: ghostY };

  ctx.fillStyle = 'rgba(255,255,255,0.3)';
  ctx.beginPath();
  ctx.arc(ghostX, ghostY, 10, 0, Math.PI * 2);
  ctx.fill();
}

export function drawBankShot(ctx, state) {
  const { cueBall } = state;
  ctx.strokeStyle = '#ff3333';
  ctx.beginPath();
  ctx.moveTo(cueBall.x, cueBall.y);
  ctx.lineTo(ctx.canvas.width - cueBall.x, cueBall.y);
  ctx.stroke();
}
