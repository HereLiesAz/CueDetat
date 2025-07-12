// src/core/stateReducer.js

export const initialState = {
  // System state
  viewWidth: 800,
  viewHeight: 600,
  toast: null,
  warning: null,

  // Mode & Toggles
  mode: 'protractor', // 'protractor' or 'banking'
  showOnPlaneBall: true,
  showHelpers: false,
  isImpossibleShot: false,

  // Logical Positions & Properties
  aimingAngle: -90, // Degrees, as per scripture
  onPlaneBall: { x: 400, y: 450 },
  targetBall: { x: 400, y: 200 },

  // UI Controls State
  zoomFactor: 1.0, // New: Global zoom state

  // Interaction State
  interactionMode: 'NONE', // NONE, ROTATING, MOVING_TARGET, MOVING_CUE
  isDragging: false
};

export function reducer(state, action) {
  switch (action.type) {
    case 'SET_VIEW_SIZE':
      return {
        ...state,
        viewWidth: action.payload.width,
        viewHeight: action.payload.height,
        onPlaneBall: { x: action.payload.width / 2, y: action.payload.height * 0.75 },
        targetBall: { x: action.payload.width / 2, y: action.payload.height / 2 },
      };

    case 'TOGGLE_HELPERS':
      return { ...state, showHelpers: !state.showHelpers };

    case 'TOGGLE_CUE_BALL':
      return { ...state, showOnPlaneBall: !state.showOnPlaneBall };

    case 'GESTURE_START': {
      const { x, y } = action.payload;
      const BALL_TOUCH_RADIUS = 15 * state.zoomFactor * 1.5; // Generous touch target
      const cueDist = Math.hypot(x - state.onPlaneBall.x, y - state.onPlaneBall.y);
      const targetDist = Math.hypot(x - state.targetBall.x, y - state.targetBall.y);

      let mode = 'ROTATING';
      if (state.showOnPlaneBall && cueDist < BALL_TOUCH_RADIUS) {
        mode = 'MOVING_CUE';
      } else if (targetDist < BALL_TOUCH_RADIUS) {
        mode = 'MOVING_TARGET';
      }

      return { ...state, isDragging: true, interactionMode: mode, warning: null };
    }

    case 'GESTURE_DRAG': {
      const { dx, dy, x, y } = action.payload;
      let newState = { ...state };

      switch (state.interactionMode) {
        case 'ROTATING':
            const centerX = state.targetBall.x;
            const centerY = state.targetBall.y;
            const startAngle = Math.atan2(y - dy - centerY, x - dx - centerX);
            const endAngle = Math.atan2(y - centerY, x - centerX);
            const angleDelta = (endAngle - startAngle) * (180 / Math.PI);
            newState.aimingAngle = (state.aimingAngle + angleDelta);
            break;
        case 'MOVING_TARGET':
          newState.targetBall = { x: state.targetBall.x + dx, y: state.targetBall.y + dy };
          break;
        case 'MOVING_CUE':
          newState.onPlaneBall = { x: state.onPlaneBall.x + dx, y: state.onPlaneBall.y + dy };
          break;
        default:
          break;
      }
      return newState;
    }

    case 'GESTURE_END':
      return { ...state, isDragging: false, interactionMode: 'NONE' };

    case 'SET_IMPOSSIBLE_SHOT':
        return { ...state, isImpossibleShot: action.payload };

    case 'TRIGGER_WARNING':
        return { ...state, warning: action.payload };

    case 'SET_ZOOM':
      return { ...state, zoomFactor: action.payload };

    case 'RESET_VIEW':
        return {
            ...state,
            aimingAngle: -90,
            zoomFactor: 1.0,
            onPlaneBall: { x: state.viewWidth / 2, y: state.viewHeight * 0.75 },
            targetBall: { x: state.viewWidth / 2, y: state.viewHeight / 2 },
            isImpossibleShot: false,
            warning: null
        }

    default:
      return state;
  }
}