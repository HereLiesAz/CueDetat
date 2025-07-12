// src/components/UIControls.js
import React from 'react';

const Fab = ({ onClick, children, title, active = false }) => (
  <button
    onClick={onClick}
    title={title || (typeof children === 'string' ? children.replace('\n', ' ') : '')}
    style={{ backgroundColor: active ? 'var(--darker-accent-gold)' : '' }}
    className="fab"
  >
    {children}
  </button>
);

export default function UIControls({ state, dispatch, onMenuClick }) {
  return (
    <div className="ui-controls-container">
      <div className="menu-column-left">
        <Fab onClick={onMenuClick}>Menu</Fab>
        <Fab onClick={() => alert('Felt Color Lock: Not Implemented.')}>Felt<br/>Color</Fab>
        <Fab onClick={() => dispatch({ type: 'TOGGLE_SPIN_CONTROL' })} active={state.isSpinControlVisible}>Spin</Fab>
        <Fab onClick={() => dispatch({ type: 'ADD_OBSTACLE' })}>Add<br/>Ball</Fab>
        {state.mode !== 'banking' && (
          <Fab onClick={() => dispatch({ type: 'TOGGLE_CUE_BALL' })} active={state.showOnPlaneBall}>
            {state.showOnPlaneBall ? 'Hide\nCue' : 'Show\nCue'}
          </Fab>
        )}
      </div>

      <div className="menu-center-button">
        {!state.showTable && state.mode !== 'banking' && (
            <Fab onClick={() => dispatch({ type: 'TOGGLE_TABLE' })} title="Toggle Table">
                Table
            </Fab>
        )}
      </div>

      <div className="menu-column-right">
        <Fab onClick={() => dispatch({ type: 'RESET_VIEW' })}>Reset</Fab>
      </div>
    </div>
  );
}