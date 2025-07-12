// src/components/MenuModal.js
import React from 'react';
import logo from '../assets/logo_cue_detat.png';

const MenuItem = ({ onClick, children }) => (
    <div className="menu-item" onClick={onClick}>
        {children}
    </div>
);

const MenuDivider = () => <div className="menu-divider" />;

export default function MenuModal({ state, dispatch, onClose }) {
  const handleEvent = (type) => {
    dispatch({ type });
    onClose();
  };

  return (
    <div className="menu-modal-overlay" onClick={onClose}>
        <div className="menu-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="menu-header">
                <img src={logo} alt="Cue D'état Logo" className="menu-logo" />
                <span>v1.0 (Web Edition)</span>
            </div>
            <MenuDivider />

            {/* Section 1: Core Controls */}
            <div className="menu-section">
                <MenuItem onClick={() => handleEvent('TOGGLE_BANKING_MODE')}>
                    {state.mode === 'banking' ? 'Ghost Ball Aiming' : 'Calculate Bank'}
                </MenuItem>
                {state.mode !== 'banking' && (
                    <MenuItem onClick={() => handleEvent('TOGGLE_CUE_BALL')}>
                        {state.showOnPlaneBall ? 'Hide Cue Ball' : 'Toggle Cue Ball'}
                    </MenuItem>
                )}
                <MenuItem onClick={() => alert('Camera Toggle: Not Applicable for Web.')}>
                    Camera is Always Off
                </MenuItem>
            </div>
            <MenuDivider />

            {/* Section 2: Table & Unit Settings */}
            <div className="menu-section">
                {state.mode !== 'banking' && (
                    <MenuItem onClick={() => handleEvent('TOGGLE_TABLE')}>
                        {state.showTable ? 'Hide Table' : 'Show Table'}
                    </MenuItem>
                )}
                <MenuItem onClick={() => handleEvent('TOGGLE_TABLE_SIZE_DIALOG')}>Table Size: {state.tableSize.name}</MenuItem>
                <MenuItem onClick={() => alert('Units: Not Implemented.')}>Use Imperial Units</MenuItem>
            </div>
            <MenuDivider />

            {/* Section 3: Appearance */}
            <div className="menu-section">
                 <MenuItem onClick={() => alert('Theme Toggle: Not Implemented.')}>Embrace the Darkness</MenuItem>
                 <MenuItem onClick={() => handleEvent('TOGGLE_LUMINANCE_DIALOG')}>Luminance</MenuItem>
                 <MenuItem onClick={() => handleEvent('TOGGLE_GLOW_DIALOG')}>Glow Stick</MenuItem>
            </div>
            <MenuDivider />

            {/* Section 4: Help & Info */}
            <div className="menu-section">
                 <MenuItem onClick={() => handleEvent('TOGGLE_HELPERS')}>
                    {state.showHelpers ? 'OK, I get it.' : 'WTF is all this?'}
                 </MenuItem>
                 <MenuItem onClick={() => handleEvent('START_TUTORIAL')}>Show Tutorial</MenuItem>
            </div>
        </div>
    </div>
  );
}