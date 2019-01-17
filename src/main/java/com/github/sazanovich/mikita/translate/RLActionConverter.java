package com.github.sazanovich.mikita.translate;

import com.github.sazanovich.mikita.parser.DEMAction;
import com.github.sazanovich.mikita.parser.DEMState;

class RLActionConverter {

  private static final int MOVE_ACTIONS_TOTAL = 8;
  private static final int ATTACK_CREEP = MOVE_ACTIONS_TOTAL + 1;
  private static final int ATTACK_HERO = ATTACK_CREEP + 1;
  private static final int ATTACK_TOWER = ATTACK_HERO + 1;
  private static final int DO_NOTHING_ACTION = 12;

  static int demActionToRLAction(DEMState demState, DEMAction demAction) {
    switch (demAction.actionType) {
      // Leave movement actions
      case 0:
        float diffX = demAction.nx - demState.ourX;
        float diffY = demAction.ny - demState.ourY;
        assert diffX != 0 || diffY != 0;
        double anglePi = Math.atan2(diffY, diffX);
        if (anglePi < 0) {
          anglePi += 2 * Math.PI;
        }
        double degrees = anglePi / Math.PI * 180;
        return (int) Math.round(degrees / (360.0 / MOVE_ACTIONS_TOTAL)) % MOVE_ACTIONS_TOTAL;
      // Leave the action of attacking the enemy hero
      case 1:
        return ATTACK_HERO;
      // Leave the action of attacking an enemy creep
      case 2:
        return ATTACK_CREEP;
      // Leave the action of attacking the enemy tower
      case 4:
        return ATTACK_TOWER;
      // Ignore the ability usage and attacking your own creeps
      case 3:
      case 5:
      default:
        return DO_NOTHING_ACTION;
    }
  }

  private RLActionConverter() {}
}
