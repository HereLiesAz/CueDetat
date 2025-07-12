// src/App.js
import React, { useReducer, useEffect, useRef, useLayoutEffect } from 'react';
import { drawScene } from './core/geometry';
import { reducer, initialState } from './core/stateReducer';
import { useGestures } from './core/useGestures';
import { insultingWarnings } from './core/warnings';
import Menu from './components/Menu';
import ZoomSlider from './components/ZoomSlider';
import './styles/theme.css';

function App() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const canvasRef = useRef(null);
  const warningRef = useRef(null);

  useLayoutEffect(() => {
    const updateSize = () => {
      dispatch({ type: 'SET_VIEW_SIZE', payload: { width: window.innerWidth, height: window.innerHeight } });
    };
    window.addEventListener('resize', updateSize);
    updateSize();
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  useGestures(canvasRef, dispatch);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    drawScene(ctx, state, dispatch);
  }, [state]);

  useEffect(() => {
    if (state.isImpossibleShot && !state.isDragging) {
      const warning = insultingWarnings[Math.floor(Math.random() * insultingWarnings.length)];
      dispatch({ type: 'TRIGGER_WARNING', payload: warning });
    }
  }, [state.isImpossibleShot, state.isDragging]);


  return (
    <div className="app-container">
      <canvas ref={canvasRef} width={state.viewWidth} height={state.viewHeight} />

      <div className="ui-overlay">
        {state.warning && (
          <div
            ref={warningRef}
            key={state.warning}
            className="impossible-shot-warning"
            style={{
                top: `${20 + Math.random() * 50}%`,
                left: `${10 + Math.random() * 40}%`,
                transform: `rotate(${Math.random() * 20 - 10}deg)`
            }}
          >
            {state.warning}
          </div>
        )}

        <ZoomSlider state={state} dispatch={dispatch} />
        <Menu state={state} dispatch={dispatch} />

      </div>
    </div>
  );
}

export default App;