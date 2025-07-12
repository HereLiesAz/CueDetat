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
  showTable: false,
  isImpossibleShot: false,
  isSpinControlVisible: false,

  // Logical Positions & Properties
  aimingAngle: -90,
  onPlaneBall: { x: 400, y: 450 },
  targetBall: { x: 400, y: 200 },
  tableSize: { name: "8'", ratio: 2.0, aspectRatio: 2.0 },
  bankingAimTarget: { x: 400, y: 100 },
  bankShotPath: [],

  // UI Controls State
  zoomFactor: 1.0,
  spinControlCenter: { x: 120, y: window.innerHeight * 0.75 },
  selectedSpinOffset: null,
  lingeringSpinOffset: null,
  spinPaths: [], // To hold calculated Bezier paths

  // Interaction State
  interactionMode: 'NONE',
  isDragging: false,
};

function resetPositions(state) {
    const { viewWidth, viewHeight, showTable, tableSize, zoomFactor, mode } = state;
    const BALL_RADIUS = 15 * zoomFactor;

    if (mode === 'banking' || showTable) {
        const tableRatio = tableSize.ratio;
        const aspectRatio = tableSize.aspectRatio;
        const tablePlayingSurfaceHeight = (BALL_RADIUS * 2) * tableRatio;
        const tableHeight = tablePlayingSurfaceHeight * aspectRatio;

        return {
            aimingAngle: -90,
            targetBall: { x: viewWidth / 2, y: viewHeight / 2 },
            onPlaneBall: { x: viewWidth / 2, y: (viewHeight / 2) + (tableHeight / 4) },
            bankingAimTarget: { x: viewWidth / 2, y: (viewHeight / 2) - 200 }
        };
    } else {
        return {
            aimingAngle: -90,
            targetBall: { x: viewWidth / 2, y: viewHeight / 2 },
            onPlaneBall: { x: viewWidth / 2, y: viewHeight * 0.75 }
        };
    }
}

export function reducer(state, action) {
  switch (action.type) {
    case 'SET_VIEW_SIZE': {
      const newState = { ...state, viewWidth: action.payload.width, viewHeight: action.payload.height, spinControlCenter: { x: 120, y: action.payload.height * 0.75 } };
      return { ...newState, ...resetPositions(newState) };
    }
    case 'TOGGLE_HELPERS':
      return { ...state, showHelpers: !state.showHelpers };
    case 'TOGGLE_CUE_BALL':
      return { ...state, showOnPlaneBall: !state.showOnPlaneBall };
    case 'TOGGLE_SPIN_CONTROL':
      return { ...state, isSpinControlVisible: !state.isSpinControlVisible };
    case 'TOGGLE_TABLE': {
      const newShowTable = !state.showTable;
      const newState = { ...state, showTable: newShowTable };
      return newShowTable ? { ...newState, ...resetPositions(newState) } : newState;
    }
    case 'TOGGLE_BANKING_MODE': {
        const isBanking = state.mode !== 'banking';
        const newState = { ...state, mode: isBanking ? 'banking' : 'protractor', showTable: isBanking, onPlaneBall: isBanking ? state.onPlaneBall : null };
        return { ...newState, ...resetPositions(newState) };
    }
    case 'GESTURE_START': {
      const { x, y } = action.payload;
      const BALL_RADIUS = 15 * state.zoomFactor;
      const BALL_TOUCH_RADIUS = BALL_RADIUS * 1.5;
      let mode = 'NONE';

      if (state.mode === 'banking') {
          const bankBallDist = Math.hypot(x - state.onPlaneBall.x, y - state.onPlaneBall.y);
          if (bankBallDist < BALL_TOUCH_RADIUS) mode = 'MOVING_BANKING_BALL';
          else mode = 'AIMING_BANK_SHOT';
      } else {
          const cueDist = state.showOnPlaneBall ? Math.hypot(x - state.onPlaneBall.x, y - state.onPlaneBall.y) : Infinity;
          const targetDist = Math.hypot(x - state.targetBall.x, y - state.targetBall.y);
          mode = 'ROTATING_PROTRACTOR';
          if (state.showOnPlaneBall && cueDist < BALL_TOUCH_RADIUS) mode = 'MOVING_CUE';
          else if (targetDist < BALL_TOUCH_RADIUS) mode = 'MOVING_TARGET';
      }
      return { ...state, isDragging: true, interactionMode: mode, warning: null };
    }
    case 'GESTURE_DRAG': {
      const { dx, dy, x, y } = action.payload;
      let newState = { ...state };
      switch (state.interactionMode) {
        case 'ROTATING_PROTRACTOR':
            const centerX = state.targetBall.x;
            const centerY = state.targetBall.y;
            const startAngle = Math.atan2(y - dy - centerY, x - dx - centerX);
            const endAngle = Math.atan2(y - centerY, x - centerX);
            newState.aimingAngle = state.aimingAngle + (endAngle - startAngle) * (180 / Math.PI);
            break;
        case 'MOVING_TARGET':
          newState.targetBall = { x: state.targetBall.x + dx, y: state.targetBall.y + dy };
          break;
        case 'MOVING_CUE':
          newState.onPlaneBall = { x: state.onPlaneBall.x + dx, y: state.onPlaneBall.y + dy };
          break;
        case 'MOVING_BANKING_BALL':
            newState.onPlaneBall = { x: state.onPlaneBall.x + dx, y: state.onPlaneBall.y + dy };
            break;
        case 'AIMING_BANK_SHOT':
            newState.bankingAimTarget = { x: x, y: y };
            break;
        default: break;
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
        return { ...state, ...resetPositions(state), isImpossibleShot: false, warning: null, lingeringSpinOffset: null, selectedSpinOffset: null, spinPaths: [] };
    case 'SET_BANK_PATH':
        return { ...state, bankShotPath: action.payload };
    case 'SPIN_APPLIED':
        return { ...state, selectedSpinOffset: action.payload, lingeringSpinOffset: null };
    case 'SPIN_SELECTION_ENDED':
        return { ...state, lingeringSpinOffset: state.selectedSpinOffset, selectedSpinOffset: null };
    case 'CLEAR_SPIN_STATE':
        return { ...state, lingeringSpinOffset: null, spinPaths: [] };
    case 'SET_SPIN_PATHS':
        return { ...state, spinPaths: action.payload };
    default:
      return state;
  }
}