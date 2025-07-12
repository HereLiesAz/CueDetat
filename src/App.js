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
import TableSizeDialog from './components/dialogs/TableSizeDialog';
import LuminanceDialog from './components/dialogs/LuminanceDialog';
import GlowStickDialog from './components/dialogs/GlowStickDialog';
import TutorialOverlay from './components/TutorialOverlay';
import CameraFeed from './components/CameraFeed'; // The new disciple
import './styles/theme.css';

function App() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const canvasRef = useRef(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [permissionState, setPermissionState] = useState('idle'); // idle, granted, denied

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
    const handleMouseMove = (e) => {
        dispatch({ type: 'MOUSE_MOVE', payload: { x: e.clientX, y: e.clientY } });
    };
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  useEffect(() => {
    if(state.mode === 'protractor' && (state.selectedSpinOffset || state.lingeringSpinOffset)) {
        const paths = calculateSpinPaths(state);
        dispatch({ type: 'SET_SPIN_PATHS', payload: paths });
    } else if (state.spinPaths.length > 0) {
        dispatch({ type: 'SET_SPIN_PATHS', payload: [] });
    }
  }, [state.selectedSpinOffset, state.lingeringSpinOffset, state.targetBall, state.onPlaneBall, state.aimingAngle, state.zoomFactor, state.mode]);

  useEffect(() => {
    if (permissionState !== 'granted') return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const animationFrameId = requestAnimationFrame(() => drawScene(ctx, state, dispatch));
    return () => cancelAnimationFrame(animationFrameId);
  }, [state, permissionState]);

  useEffect(() => {
    if (state.isImpossibleShot && !state.isDragging && !state.warning) {
      const warning = insultingWarnings[Math.floor(Math.random() * insultingWarnings.length)];
      dispatch({ type: 'TRIGGER_WARNING', payload: warning });
      setTimeout(() => dispatch({ type: 'TRIGGER_WARNING', payload: null }), 3000);
    }
  }, [state.isImpossibleShot, state.isDragging, state.warning]);

  const requestCameraPermission = () => {
    setPermissionState('granted'); // This will trigger the CameraFeed component to mount and request permission
  };

  if (permissionState !== 'granted') {
    return (
        <div className="permission-screen">
            <h1>Cue D'état requires camera access.</h1>
            <p>This app overlays aiming guides on your camera feed. Your camera data is not stored or transmitted.</p>
            <button className="permission-button" onClick={requestCameraPermission}>Grant Permission</button>
            {permissionState === 'denied' && <p className="permission-denied-text">Permission was denied. Please grant camera access in your browser settings.</p>}
        </div>
    );
  }

  return (
    <div className="app-container">
      <CameraFeed onStreamReady={() => {}} onError={() => setPermissionState('denied')} />
      <canvas ref={canvasRef} width={state.viewWidth} height={state.viewHeight} />

      {isMenuOpen && <MenuModal state={state} dispatch={dispatch} onClose={() => setIsMenuOpen(false)} />}
      {state.showTableSizeDialog && <TableSizeDialog dispatch={dispatch} onClose={() => dispatch({type: 'TOGGLE_TABLE_SIZE_DIALOG'})} />}
      {state.showLuminanceDialog && <LuminanceDialog state={state} dispatch={dispatch} onClose={() => dispatch({type: 'TOGGLE_LUMINANCE_DIALOG'})} />}
      {state.showGlowDialog && <GlowStickDialog state={state} dispatch={dispatch} onClose={() => dispatch({type: 'TOGGLE_GLOW_DIALOG'})} />}
      <TutorialOverlay state={state} dispatch={dispatch} />

      <div className="ui-overlay">
        {state.warning && (
          <div
            key={state.warning}
            className="impossible-shot-warning"
            style={{ top: `${20 + Math.random() * 50}%`, left: `${10 + Math.random() * 40}%`, transform: `rotate(${Math.random() * 20 - 10}deg)`}}
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