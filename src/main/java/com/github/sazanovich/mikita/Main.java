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
        Path observationFolder = Paths.get(args[1]);
        Path clientInfoFolder = Paths.get(args[2]);
        if (!Files.exists(observationFolder)) {
            Files.createDirectory(observationFolder);
        }
        System.out.println(replayFolder);
        System.out.println(observationFolder);
        List<Path> replayPaths =
            Files
                .readAllLines(Paths.get(replayFolder.toString(), "meta.txt"))
                .stream()
                .sorted()
                .map(replayName -> Paths.get(replayFolder.toString(), replayName))
                .collect(Collectors.toList());
        int totalCnt = 0;
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
            assert demStates[beginTick].ourTeam == 2;

            String replayFileName = replayPath.getFileName().toString();
            String obsFileName = replayFileName.substring(0, replayFileName.indexOf('.')) + ".obs";
            Path clientPath = Paths.get(clientInfoFolder.toString(), obsFileName);
            Path outputPath = Paths.get(observationFolder.toString(), obsFileName);
            System.out.println(clientPath);
            System.out.println(outputPath);

            int cnt = 0;
            try (Translator translator = new Translator(clientPath, outputPath)) {
                for (int tick = beginTick; tick < endTick; tick++) {
                    if (translator.saveStepIfConvertible(demStates[tick], demActions[tick])) {
                        cnt += 1;
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println("Total demo pairs: " + cnt);
            totalCnt += cnt;
        }
        System.out.println("Overall, we have " + totalCnt + " demo pairs");
    }
}
