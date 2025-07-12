// /core/stateReducer.js

export const initialState = {
  mode: 'protractor',
  cueBall: { x: 150, y: 200 },
  targetBall: { x: 400, y: 200 },
  ghostBall: { x: 0, y: 0 },
  toast: '',
};

export function reducer(state, action) {
  switch (action.type) {
    case 'TOGGLE_MODE':
      return {
        ...state,
        mode: state.mode === 'protractor' ? 'banking' : 'protractor',
        toast: state.mode === 'protractor'
          ? 'You’ve switched to banking. Like that’ll help.'
          : 'Back to protractor. Hope springs eternal.',
      };
    default:
      return state;
  }
}
