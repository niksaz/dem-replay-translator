package com.github.sazanovich.mikita.translate;

import com.github.sazanovich.mikita.parser.DEMState;
import com.github.sazanovich.mikita.parser.DEMState.CreepState;
import java.util.Arrays;

class RLState {

  private static final float MAX_ABS_X = 1; //8288.0f;
  private static final float MAX_ABS_Y = 1; //8288.0f;
  private static final int TOTAL_DIRS = 8;
  private static final int NEARBY_RADIUS = 1600;

  private final float[] hero_info;
  private final float[] enemy_info;

  RLState(DEMState demState) {
    float coorX = demState.ourX / MAX_ABS_X;
    float coorY = demState.ourY / MAX_ABS_Y;
    assert demState.ourMaxHp != 0;
    float hp = ((float) demState.ourHp) / demState.ourMaxHp;

    hero_info = new float[11];
    hero_info[0] = coorX;
    hero_info[1] = coorY;
    // TODO: Ask the Dota 2 client these questions
    for (int i = 0; i < TOTAL_DIRS; i++) {
      hero_info[2 + i] = 1;
    }
    hero_info[10] = hp;

    enemy_info = new float[6];
    Arrays.fill(enemy_info, 1);  // Meaning nothing is visible
    // Update info about the nearest enemy creep, if it is present
    if (!demState.enemyCreeps.isEmpty()) {
      CreepState creepState = demState.enemyCreeps.get(0);
      float creepDst = Util.getDistance(demState.ourX, demState.ourY, creepState.x, creepState.y);
      creepDst /= NEARBY_RADIUS;
      enemy_info[0] = 0;
      enemy_info[1] = creepDst;
    }
    // Info about the nearest enemy hero
    enemy_info[2] = 1;
    enemy_info[3] = 1;
    // Info about the nearest enemy tower
    enemy_info[4] = 1;
    enemy_info[5] = 1;
  }
}