package com.github.sazanovich.mikita.translate;

import com.github.sazanovich.mikita.parser.DEMAction;
import com.github.sazanovich.mikita.parser.DEMState;
import com.github.sazanovich.mikita.parser.DEMState.CreepState;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Scanner;

public class Translator implements AutoCloseable {

  private static final Gson gson = new Gson();
  private final Scanner scanner;
  private final PrintWriter printWriter;

  public Translator(Path clientInfo, Path outputPath) throws FileNotFoundException {
    scanner = new Scanner(new FileInputStream(clientInfo.toFile()));
    printWriter = new PrintWriter(new FileOutputStream(outputPath.toFile()));
  }

  public boolean saveStepIfConvertible(DEMState demState, DEMAction demAction) {
    verifyState(demState);

    String clientInfo = scanner.nextLine();
    int[] movesForbiddenInDir = parseNumpyIntArray(clientInfo);

    RLState state = new RLState(demState, movesForbiddenInDir);
    int rlAction = RLActionConverter.demActionToRLAction(demState, demAction);
    RLDemoStep demoStep = new RLDemoStep(state, rlAction);

    String jsonRepresentation = gson.toJson(demoStep);
    printWriter.println(jsonRepresentation);
    return true;
  }

  @Override
  public void close() {
    scanner.close();
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

  static int[] parseNumpyIntArray(String stringRepr) {
    stringRepr = stringRepr.substring(1, stringRepr.length() - 1);
    String[] parts = stringRepr.split(",");
    int[] result = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      result[i] = Integer.parseInt(parts[i]);
    }
    return result;
  }
}
