package com.github.sazanovich.mikita.translate;

class RLDemoStep {

  RLState state;
  int action;

  RLDemoStep(RLState state, int action) {
    this.state = state;
    this.action = action;
  }
}
