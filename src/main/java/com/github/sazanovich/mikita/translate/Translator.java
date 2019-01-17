package com.github.sazanovich.mikita.translate;

import com.github.sazanovich.mikita.parser.DEMAction;
import com.github.sazanovich.mikita.parser.DEMState;
import com.github.sazanovich.mikita.parser.DEMState.CreepState;
import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

public class Translator implements AutoCloseable {

  private static final Gson gson = new Gson();
  private final PrintWriter printWriter;

  public Translator(Path outputPath) throws FileNotFoundException {
    printWriter = new PrintWriter(new FileOutputStream(outputPath.toFile()));
  }

  public boolean saveStepIfConvertible(DEMState demState, DEMAction demAction) {
    verifyState(demState);

    RLState state = new RLState(demState);
    int rlAction = RLActionConverter.demActionToRLAction(demState, demAction);
    RLDemoStep demoStep = new RLDemoStep(state, rlAction);

    String jsonRepresentation = gson.toJson(demoStep);
    printWriter.println(jsonRepresentation);
    return true;
  }

  @Override
  public void close() {
    printWriter.close();
  }

  private static void verifyState(DEMState demState) {
    for (int i = 0; i < demState.enemyCreeps.size(); i++) {
      CreepState creepState = demState.enemyCreeps.get(i);
      assert creepState.isVisible;
      if (i != 0) {
        CreepState lastCreepState = demState.enemyCreeps.get(i - 1);
        assert Util.getDistance(demState.ourX, demState.ourY, lastCreepState.x, lastCreepState.y) <=
            Util.getDistance(demState.ourX, demState.ourY, creepState.x, creepState.y);
      }
    }
  }
}
