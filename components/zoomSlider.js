// src/components/ZoomSlider.js
import React from 'react';

export default function ZoomSlider({ state, dispatch }) {
  const handleChange = (e) => {
    // Map slider value (0-100) to a zoom factor (e.g., 0.5x to 2.5x)
    const sliderValue = e.target.value;
    const minZoom = 0.5;
    const maxZoom = 2.5;
    const newZoom = minZoom + (sliderValue / 100) * (maxZoom - minZoom);
    dispatch({ type: 'SET_ZOOM', payload: newZoom });
  };

  // Map current zoom factor back to a slider value
  const minZoom = 0.5;
  const maxZoom = 2.5;
  const currentSliderValue = ((state.zoomFactor - minZoom) / (maxZoom - minZoom)) * 100;

  return (
    <div className="zoom-slider-container">
       <div className="zoom-label">ZOOM</div>
      <input
        type="range"
        min="0"
        max="100"
        value={currentSliderValue}
        onChange={handleChange}
        className="zoom-slider"
        orient="vertical"
      />
    </div>
  );
}