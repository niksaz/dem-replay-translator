package com.github.sazanovich.mikita.translate;

import com.github.sazanovich.mikita.parser.DEMAction;
import com.github.sazanovich.mikita.parser.DEMState;
import com.google.gson.Gson;

public class Translator {

  private static final Gson gson = new Gson();

  public void saveStep(DEMState demState, DEMAction demAction) {
    RLState state = new RLState(demState);
    int action = demAction.actionType;  // TODO: More thorough conversion
    RLDemoStep demoStep = new RLDemoStep(state, action);
    System.out.println(demState.enemyTowerHp);
    System.out.println(demState.enemyTowerX);
    System.out.println(demState.enemyTowerY);

    String jsonRepresentation = gson.toJson(demoStep);
    System.out.println(jsonRepresentation);
  }
}
