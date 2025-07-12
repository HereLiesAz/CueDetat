// src/components/MenuModal.js
import React from 'react';
import logo from '../assets/logo_cue_detat.png'; // The sacred import

const MenuItem = ({ onClick, children }) => (
    <div className="menu-item" onClick={onClick}>
        {children}
    </div>
);

const MenuDivider = () => <div className="menu-divider" />;

export default function MenuModal({ state, dispatch, onClose }) {
  return (
    <div className="menu-modal-overlay" onClick={onClose}>
        <div className="menu-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="menu-header">
                <img src={logo} alt="Cue D'état Logo" className="menu-logo" />
                <span>v0.1 (Web Edition)</span>
            </div>
            <MenuDivider />

            {/* Section 1: Core Controls */}
            <div className="menu-section">
                <MenuItem onClick={() => { dispatch({ type: 'TOGGLE_BANKING_MODE' }); onClose(); }}>
                    {state.mode === 'banking' ? 'Ghost Ball Aiming' : 'Calculate Bank'}
                </MenuItem>
                {state.mode !== 'protractor' && (
                    <MenuItem onClick={() => { dispatch({ type: 'TOGGLE_CUE_BALL' }); onClose(); }}>
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
                    <MenuItem onClick={() => { dispatch({ type: 'TOGGLE_TABLE' }); onClose(); }}>
                        {state.showTable ? 'Hide Table' : 'Show Table'}
                    </MenuItem>
                )}
                <MenuItem onClick={() => alert('Table Size: 8\' Only For Now.')}>Table Size: 8'</MenuItem>
                <MenuItem onClick={() => alert('Units: Not Implemented.')}>Use Imperial Units</MenuItem>
            </div>
            <MenuDivider />

            {/* Section 3: Appearance */}
            <div className="menu-section">
                 <MenuItem onClick={() => alert('Theme Toggle: Not Implemented.')}>Embrace the Darkness</MenuItem>
                 <MenuItem onClick={() => alert('Luminance: Not Implemented.')}>Luminance</MenuItem>
                 <MenuItem onClick={() => alert('Glow Stick: Not Implemented.')}>Glow Stick</MenuItem>
            </div>
            <MenuDivider />

            {/* Section 4: Help & Info */}
            <div className="menu-section">
                 <MenuItem onClick={() => { dispatch({ type: 'TOGGLE_HELPERS' }); onClose(); }}>
                    {state.showHelpers ? 'OK, I get it.' : 'WTF is all this?'}
                 </MenuItem>
                 <MenuItem onClick={() => alert('Tutorial: Read the README.')}>Show Tutorial</MenuItem>
            </div>
        </div>
    </div>
  );
}