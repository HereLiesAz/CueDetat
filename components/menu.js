// src/components/Menu.js
import React from 'react';

const MenuButton = ({ onClick, children }) => (
  <button onClick={onClick} title={children}>{children.split('\n')[0]}</button>
);

export default function Menu({ state, dispatch }) {
  return (
    <div className="menu">
      <div className="menu-column">
        {/* Placeholder for future color lock */}
        <MenuButton onClick={() => alert('Felt Color Lock: Not Implemented.')}>Felt<br/>Color</MenuButton>
        {/* Placeholder for Spin Control */}
        <MenuButton onClick={() => alert('Spin Control: Not Implemented.')}>Spin</MenuButton>
        {state.mode !== 'banking' && (
          <MenuButton onClick={() => dispatch({ type: 'TOGGLE_CUE_BALL' })}>
            {state.showOnPlaneBall ? 'Hide\nCue' : 'Show\nCue'}
          </MenuButton>
        )}
      </div>

      <div className="menu-center">
        {/* Placeholder for future table toggle */}
      </div>

      <div className="menu-column">
        <MenuButton onClick={() => dispatch({ type: 'RESET_VIEW' })}>Reset<br/>View</MenuButton>
      </div>
    </div>
  );
}