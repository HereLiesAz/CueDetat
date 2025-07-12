// src/core/useGestures.js
import { useRef, useEffect } from 'react';

export function useGestures(canvasRef, dispatch) {
  const isDraggingRef = useRef(false);
  const lastPositionRef = useRef({ x: 0, y: 0 });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const handleMouseDown = (e) => {
      isDraggingRef.current = true;
      const rect = canvas.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      lastPositionRef.current = { x, y };
      dispatch({ type: 'GESTURE_START', payload: { x, y } });
    };

    const handleMouseMove = (e) => {
      if (!isDraggingRef.current) return;
      const rect = canvas.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      const dx = x - lastPositionRef.current.x;
      const dy = y - lastPositionRef.current.y;
      dispatch({ type: 'GESTURE_DRAG', payload: { dx, dy, x, y } });
      lastPositionRef.current = { x, y };
    };

    const handleMouseUp = () => {
      if (isDraggingRef.current) {
        isDraggingRef.current = false;
        dispatch({ type: 'GESTURE_END' });
      }
    };

    canvas.addEventListener('mousedown', handleMouseDown);
    canvas.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp); // Use window to catch mouse up events outside canvas

    return () => {
      canvas.removeEventListener('mousedown', handleMouseDown);
      canvas.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [canvasRef, dispatch]);
}