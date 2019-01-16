package com.github.sazanovich.mikita;

import com.github.sazanovich.mikita.parser.DEMAction;
import com.github.sazanovich.mikita.parser.DEMState;
import com.github.sazanovich.mikita.parser.Parser;
import com.github.sazanovich.mikita.translate.Translator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {
        Path replayFolder = Paths.get(args[0]);
        List<Path> replayPaths =
            Files
                .readAllLines(Paths.get(replayFolder.toString(), "meta.txt"))
                .stream()
                .sorted()
                .map(replayName -> Paths.get(replayFolder.toString(), replayName))
                .collect(Collectors.toList());
        Translator translator = new Translator();
        for (Path replayPath : replayPaths) {
            String stringPath = replayPath.toString();
            System.out.println(stringPath);
            Parser parser = new Parser(stringPath);

            try {
                parser.run();
            } catch (Throwable e) {
                e.printStackTrace();
                continue;
            }
            DEMState[] demStates = parser.getStates();
            DEMAction[] demActions = parser.getActions();

            int beginTick = parser.getTickBorders().fst;
            int endTick = parser.getTickBorders().snd;
            System.out.println("beginTick: " + beginTick + ", endTick: " + endTick);
            assert demStates[beginTick].ourTeam == 2;

            for (int tick = beginTick; tick < endTick; tick++) {
                if (demActions[tick].actionType == -1) {
                    continue;
                }
                try {
                    translator.saveStep(demStates[tick], demActions[tick]);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
