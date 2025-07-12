// src/components/SpinControl.js
import React, { useRef, useEffect } from 'react';
import { lerpColor, getColorFromAngleAndDistance } from '../core/spinColors';

const SPIN_CONTROL_SIZE = 120; // in pixels
const SPIN_CONTROL_RADIUS = SPIN_CONTROL_SIZE / 2;

function drawSpinControl(ctx, selectedOffset, lingeringOffset, alpha) {
    const center = { x: SPIN_CONTROL_RADIUS, y: SPIN_CONTROL_RADIUS };
    ctx.clearRect(0, 0, SPIN_CONTROL_SIZE, SPIN_CONTROL_SIZE);

    // Draw the color wheel
    const numArcs = 72;
    const arcAngle = 360 / numArcs;
    for (let i = 0; i < numArcs; i++) {
        const startAngle = i * arcAngle;
        const colorSampleAngle = startAngle + (arcAngle / 2);
        const color = getColorFromAngleAndDistance(colorSampleAngle, 1.0);

        ctx.beginPath();
        ctx.moveTo(center.x, center.y);
        ctx.arc(center.x, center.y, SPIN_CONTROL_RADIUS, startAngle * (Math.PI / 180), (startAngle + arcAngle) * (Math.PI / 180));
        ctx.closePath();
        ctx.fillStyle = `rgba(${color.r}, ${color.g}, ${color.b}, ${alpha})`;
        ctx.fill();
    }

    // Draw white overlay
    const gradient = ctx.createRadialGradient(center.x, center.y, 0, center.x, center.y, SPIN_CONTROL_RADIUS);
    gradient.addColorStop(0, `rgba(255, 255, 255, ${alpha})`);
    gradient.addColorStop(1, `rgba(255, 255, 255, 0)`);
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, SPIN_CONTROL_SIZE, SPIN_CONTROL_SIZE);

    // Draw outline
    ctx.beginPath();
    ctx.arc(center.x, center.y, SPIN_CONTROL_RADIUS, 0, 2 * Math.PI);
    ctx.strokeStyle = `rgba(255, 255, 255, ${0.5 * alpha})`;
    ctx.lineWidth = 2;
    ctx.stroke();

    // Draw indicators
    const drawIndicator = (offset, indicatorAlpha) => {
        const dragX = offset.x - center.x;
        const dragY = offset.y - center.y;
        const distance = Math.hypot(dragX, dragY);
        const clampedDistance = Math.min(distance, SPIN_CONTROL_RADIUS);
        const angle = Math.atan2(dragY, dragX);
        const indicatorX = center.x + clampedDistance * Math.cos(angle);
        const indicatorY = center.y + clampedDistance * Math.sin(angle);

        ctx.beginPath();
        ctx.arc(indicatorX, indicatorY, 5, 0, 2 * Math.PI);
        ctx.fillStyle = `rgba(255, 255, 255, ${indicatorAlpha})`;
        ctx.fill();
        ctx.strokeStyle = `rgba(255, 255, 255, ${indicatorAlpha})`;
        ctx.stroke();
    };

    if (lingeringOffset) {
        drawIndicator(lingeringOffset, 0.6 * alpha);
    }
    if (selectedOffset) {
        drawIndicator(selectedOffset, 1.0 * alpha);
    }
}

export default function SpinControl({ state, dispatch }) {
    const canvasRef = useRef(null);
    const isDraggingRef = useRef(false);
    const alphaAnimatable = useRef(1.0);
    const fadeOutJob = useRef(null);

    useEffect(() => {
        if (state.lingeringSpinOffset) {
            if (fadeOutJob.current) cancelAnimationFrame(fadeOutJob.current);
            const startTime = performance.now();
            const duration = 5000;
            const fade = (currentTime) => {
                const elapsedTime = currentTime - startTime;
                if (elapsedTime < duration) {
                    alphaAnimatable.current = 1.0 - (elapsedTime / duration);
                    fadeOutJob.current = requestAnimationFrame(fade);
                } else {
                    alphaAnimatable.current = 0;
                    dispatch({ type: 'CLEAR_SPIN_STATE' });
                }
            };
            setTimeout(() => { fadeOutJob.current = requestAnimationFrame(fade); }, 5000);
        } else {
            if (fadeOutJob.current) cancelAnimationFrame(fadeOutJob.current);
            alphaAnimatable.current = 1.0;
        }
    }, [state.lingeringSpinOffset, dispatch]);

    useEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const renderLoop = () => {
            drawSpinControl(ctx, state.selectedSpinOffset, state.lingeringSpinOffset, alphaAnimatable.current);
            requestAnimationFrame(renderLoop);
        };
        const handle = requestAnimationFrame(renderLoop);
        return () => cancelAnimationFrame(handle);
    }, [state.selectedSpinOffset, state.lingeringSpinOffset]);

    const handleInteraction = (e, type) => {
        const rect = e.target.getBoundingClientRect();
        const offset = { x: e.clientX - rect.left, y: e.clientY - rect.top };
        if (type === 'start' || type === 'drag') {
            dispatch({ type: 'SPIN_APPLIED', payload: offset });
        } else {
            dispatch({ type: 'SPIN_SELECTION_ENDED' });
        }
    };

    return (
        <div className="spin-control-container" style={{ left: state.spinControlCenter.x - SPIN_CONTROL_RADIUS, top: state.spinControlCenter.y - SPIN_CONTROL_RADIUS }}>
            <canvas
                ref={canvasRef}
                width={SPIN_CONTROL_SIZE}
                height={SPIN_CONTROL_SIZE}
                onMouseDown={(e) => { isDraggingRef.current = true; handleInteraction(e, 'start'); }}
                onMouseMove={(e) => { if (isDraggingRef.current) handleInteraction(e, 'drag'); }}
                onMouseUp={(e) => { if (isDraggingRef.current) { isDraggingRef.current = false; handleInteraction(e, 'end'); } }}
                onMouseLeave={(e) => { if (isDraggingRef.current) { isDraggingRef.current = false; handleInteraction(e, 'end'); } }}
            />
        </div>
    );
}