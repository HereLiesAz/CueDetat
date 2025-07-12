// src/App.js
import React, { useReducer, useEffect, useRef, useLayoutEffect, useState } from 'react';
import { drawScene, calculateSpinPaths } from './core/geometry';
import { reducer, initialState } from './core/stateReducer';
import { useGestures } from './core/useGestures';
import { insultingWarnings } from './core/warnings';
import UIControls from './components/UIControls';
import ZoomSlider from './components/ZoomSlider';
import MenuModal from './components/MenuModal';
import SpinControl from './components/SpinControl';
import './styles/theme.css';

function App() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const canvasRef = useRef(null);
  const warningRef = useRef(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  useLayoutEffect(() => {
    const updateSize = () => {
      dispatch({ type: 'SET_VIEW_SIZE', payload: { width: window.innerWidth, height: window.innerHeight } });
    };
    window.addEventListener('resize', updateSize);
    updateSize();
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  useGestures(canvasRef, dispatch);

  // Effect for calculating derived state like spin paths
  useEffect(() => {
    if(state.mode === 'protractor' && (state.selectedSpinOffset || state.lingeringSpinOffset)) {
        const paths = calculateSpinPaths(state);
        dispatch({ type: 'SET_SPIN_PATHS', payload: paths });
    } else if (state.spinPaths.length > 0) {
        dispatch({ type: 'SET_SPIN_PATHS', payload: [] });
    }
  }, [state.selectedSpinOffset, state.lingeringSpinOffset, state.targetBall, state.onPlaneBall, state.aimingAngle, state.zoomFactor]);

  // Main drawing effect
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    drawScene(ctx, state, dispatch);
  }, [state]); // Re-draw whenever state changes

  // Warning trigger effect
  useEffect(() => {
    if (state.isImpossibleShot && !state.isDragging && !state.warning) {
      const warning = insultingWarnings[Math.floor(Math.random() * insultingWarnings.length)];
      dispatch({ type: 'TRIGGER_WARNING', payload: warning });
      setTimeout(() => dispatch({ type: 'TRIGGER_WARNING', payload: null }), 3000);
    }
  }, [state.isImpossibleShot, state.isDragging, state.warning]);


  return (
    <div className="app-container">
      <canvas ref={canvasRef} width={state.viewWidth} height={state.viewHeight} />

      {isMenuOpen && <MenuModal state={state} dispatch={dispatch} onClose={() => setIsMenuOpen(false)} />}

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
      </div>

      <UIControls state={state} dispatch={dispatch} onMenuClick={() => setIsMenuOpen(true)} />
      <ZoomSlider state={state} dispatch={dispatch} />
      {state.isSpinControlVisible && <SpinControl state={state} dispatch={dispatch} />}

    </div>
  );
}

export default App;