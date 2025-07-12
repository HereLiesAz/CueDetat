import React, { useReducer, useEffect, useRef } from 'react';
import { drawScene } from './core/geometry';
import { reducer, initialState } from './core/stateReducer';
import './styles/theme.css';
import { insultingWarnings } from './core/warnings';

export default function App() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    drawScene(ctx, state);
  }, [state]);

  return (
    <div className="app-container">
      <canvas ref={canvasRef} id="table-canvas" width="800" height="400"></canvas>
      <div className="toast">{state.toast}</div>
      <div className="menu">
        <button onClick={() => dispatch({ type: 'TOGGLE_MODE' })}>
          {state.mode === 'protractor' ? 'Switch to Banking Mode' : 'Switch to Protractor Mode'}
        </button>
      </div>
    </div>
  );
}
