package com.github.sazanovich.mikita.parser;

import com.github.sazanovich.mikita.util.IntPair;
import skadistats.clarity.Clarity;
import skadistats.clarity.model.*;
import skadistats.clarity.model.Vector;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.gameevents.OnCombatLogEntry;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

import java.io.IOException;
import java.util.*;

/**
 * Class that parses Dota 2 replay file and adds everything it saw in DEMState and DEMAction array.
 */
public class Parser {
    private DEMState[] states;
    private DEMAction[] actions;

    /**
     * Maps creep id to its state. We need to maintain creep list from state to state, add and remove
     * creeps. If we keep creep states in its outer state, there will be many difficulties
     * with updating and removing dead creeps. (Map is basically faster.)
     */
    private HashMap<Integer, DEMState.CreepState> ourCreeps = new HashMap<>();
    private HashMap<Integer, DEMState.CreepState> enemyCreeps = new HashMap<>();

    /** Team of our Nevermore. We require that it was the winner's team. */
    private int winnerTeam;

    /** Current tick. */
    private int tick = 0;
    /** Tick when state 4 began. */
    private int beginTick = 0;
    /** Tick when state 5 ended. */
    private int endTick = 0;

    /**
     * DEMState of the game (has nothing to do with states), that describes game status.
     * 4 means that game in process without creeps, 5 - with creeps, other numbers - game isn't in process.
     * Useful because states, when status isn't 5 or 4, have no info.
     */
    private int gameState;

    /** Replay file name. */
    private final String replayFile;

    /**
     * Constructs Parser: gets basic info about match and creates empty states and actions,
     * then writes there basic match info.
     * @param arg arg given to Main - way to directory
     * @throws IOException if replay isn't found or can't be read
     */
    public Parser(String arg) throws IOException {
        replayFile = arg;

        Demo.CDemoFileInfo info = Clarity.infoForFile(replayFile);
        winnerTeam = info.getGameInfo().getDota().getGameWinner();

        states = new DEMState[info.getPlaybackTicks()];
        actions = new DEMAction[info.getPlaybackTicks()];
        String enemyName = info.getGameInfo().getDota().getPlayerInfo(1).getGameTeam() == winnerTeam ?
                info.getGameInfo().getDota().getPlayerInfo(0).getHeroName() :
                info.getGameInfo().getDota().getPlayerInfo(1).getHeroName();
        enemyName = enemyName.replace("npc_dota_hero_", "");

        for (int i = 0; i < info.getPlaybackTicks(); i++) {
            actions[i] = new DEMAction();
            states[i] = new DEMState();
            states[i].ourTeam = winnerTeam;
            states[i].enemyName = enemyName;
        }
    }


    public DEMState[] getStates() {
        return states;
    }

    public DEMAction[] getActions() {
        return actions;
    }

    public IntPair getTickBorders() {
        return new IntPair(beginTick, endTick);
    }

    /**
     * Gets entity, looks what type it is, depending on type saves info to state.
     * @param e given entity
     */
    private boolean saveInfoFromEntity(Entity e) {
        switch (Util.getEntityType(e, winnerTeam)) {
            case OUR_HERO:
                states[tick].ourHp = e.getProperty(Constants.HP);
                states[tick].ourMaxHp = e.getProperty(Constants.MAX_HP);
                states[tick].ourMana = ((Float) e.getProperty(Constants.MANA)).intValue();
                states[tick].ourMaxMana = ((Float) e.getProperty(Constants.MAX_MANA)).intValue();
                states[tick].ourLvl = e.getProperty(Constants.LVL);
                states[tick].ourX = Util.getCoordFromEntity(e).fst;
                states[tick].ourY = Util.getCoordFromEntity(e).snd;
                states[tick].ourFacing = ((Vector) e.getProperty(Constants.FACING)).getElement(1);
                states[tick].ourAttackDamage = (Integer) e.getProperty(Constants.ATTACK_DAMAGE_BONUS)
                        + ((Integer) e.getProperty(Constants.ATTACK_DAMAGE_MIN) + (Integer) e.getProperty(
                    Constants.ATTACK_DAMAGE_MAX)) / 2;
                break;

            case ENEMY_HERO:
                states[tick].isEnemyVisible = ((Integer) e.getProperty(Constants.VISIBILITY) & (1 << winnerTeam)) != 0;
                states[tick].enemyHp = e.getProperty(Constants.HP);
                states[tick].enemyMaxHp = e.getProperty(Constants.MAX_HP);
                states[tick].enemyMana = ((Float) e.getProperty(Constants.MANA)).intValue();
                states[tick].enemyMaxMana = ((Float) e.getProperty(Constants.MAX_MANA)).intValue();
                states[tick].enemyLvl = e.getProperty(Constants.LVL);
                states[tick].enemyX = Util.getCoordFromEntity(e).fst;
                states[tick].enemyY = Util.getCoordFromEntity(e).snd;
                states[tick].enemyFacing = ((Vector) e.getProperty(Constants.FACING)).getElement(1);
                states[tick].enemyAttackDamage = (Integer) e.getProperty(
                    Constants.ATTACK_DAMAGE_BONUS)
                        + ((Integer) e.getProperty(Constants.ATTACK_DAMAGE_MIN) + (Integer) e.getProperty(
                    Constants.ATTACK_DAMAGE_MAX)) / 2;
                break;

            case OUR_TOWER:
                states[tick].ourTowerHp = e.getProperty(Constants.HP);
                break;

            case ENEMY_TOWER:
                states[tick].enemyTowerHp = e.getProperty(Constants.HP);
                states[tick].enemyTowerX = Util.getCoordFromEntity(e).fst;
                states[tick].enemyTowerY = Util.getCoordFromEntity(e).snd;
                states[tick].isEnemyTowerVisible  = ((Integer) e.getProperty(Constants.VISIBILITY) & (1 << winnerTeam)) != 0;

                break;

            case OUR_TEAM:
                states[tick].ourScore = Util.getScoreFromEntity(e);
                break;

            case ENEMY_TEAM:
                states[tick].enemyScore = Util.getScoreFromEntity(e);
                break;

            case OUR_DATA:
                states[tick].ourGold = (Integer) e.getProperty("m_vecDataTeam.0000.m_iReliableGold")
                        + (Integer) e.getProperty("m_vecDataTeam.0000.m_iUnreliableGold");

                break;

            case OUR_ABILITY:
                int mana = states[tick - 1].ourMana;
                switch (Util.getAbilityTypeFromEntity(e)) {
                    case 1:
                        states[tick].isOurAbility1Available = Util.isAbilityAvailable(e, mana);
                        break;
                    case 2:
                        states[tick].isOurAbility2Available = Util.isAbilityAvailable(e, mana);
                        break;
                    case 3:
                        states[tick].isOurAbility3Available = Util.isAbilityAvailable(e, mana);
                        break;
                    case 4:
                        states[tick].isOurAbility4Available = Util.isAbilityAvailable(e, mana);
                        break;
                }

                break;

            case OUR_CREEP:
                if (gameState != 5 || Util.getCreepTypeFromEntity(e) == -1)
                    break;

                Util.updateCreepMapFromEntity(e, ourCreeps);
                break;
            case ENEMY_CREEP:
                if (gameState != 5 || Util.getCreepTypeFromEntity(e) == -1)
                    break;

                Util.updateCreepMapFromEntity(e, enemyCreeps);
                break;
            default:
                return false;
        }

        return true;
    }


    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
        tick = ctx.getTick();
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        if (gameState == 4 || gameState == 5) {
            Util.stateClosure(tick, states);

            Util.saveCreepInfoToState(states[tick], ourCreeps, true);
            Util.saveCreepInfoToState(states[tick], enemyCreeps, false);

            Util.actionClosure(tick, states, actions, enemyCreeps, ourCreeps);
        }
    }

    @OnEntityCreated
    public void onCreated(Entity e) {
        saveInfoFromEntity(e);
    }

    @OnEntityUpdated
    public void onUpdated(Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (gameState != 5 && gameState != 4) {
            return;
        }

        saveInfoFromEntity(e);
    }

    /**
     * This thing catches messages that were sent by player. Used to get data about
     * desires to attack and abilities.
     * @param ctx context (don't really know what is it)
     * @param message message sent by user
     */
    @OnMessage(DotaUserMessages.CDOTAUserMsg_SpectatorPlayerUnitOrders.class)
    public void onSpectatorPlayerUnitOrders(Context ctx, DotaUserMessages.CDOTAUserMsg_SpectatorPlayerUnitOrders message) {
        Entity e = ctx.getProcessor(Entities.class).getByIndex(message.getEntindex());
        if (e == null || !e.hasProperty(Constants.TEAM)) {
            return;
        }

        int team = e.getProperty(Constants.TEAM);
        if (team == winnerTeam) {
            int orderType = message.getOrderType();

            if (orderType == 2 || orderType == 4) {
                Entity target = ctx.getProcessor(Entities.class).getByIndex(message.getTargetIndex());
                String targetName = target.getDtClass().getDtName();
                if (targetName.startsWith("CDOTA_Unit_Hero")) {
                    actions[tick].actionType = 1;
                } else if (targetName.startsWith("CDOTA_BaseNPC_Creep")) {
                    actions[tick].actionType = 2;
                    actions[tick].param = message.getTargetIndex();

                } else if (targetName.startsWith("CDOTA_BaseNPC_Tower")) {
                    actions[tick].actionType = 4;
                }

            } else if (orderType == 8) {
                Entity ability = ctx.getProcessor(Entities.class).getByIndex(message.getAbilityId());
                if (ability != null && Util.getAbilityTypeFromEntity(ability) != 0) {
                    actions[tick].actionType = 3;
                    actions[tick].param = Util.getAbilityTypeFromEntity(ability);
                }
            } else if (orderType == 1) {
                actions[tick].actionType = 0;
                actions[tick].nx = ((Float) message.getPosition().getX()).intValue();
                actions[tick].ny = ((Float) message.getPosition().getY()).intValue();
            }
        }
    }

    /**
     * Processor of combat log. Used to get game state, damage received by our hero and abilities cast.
     * @param cle combat log entry
     */
    @OnCombatLogEntry
    public void onCombatLogEntry(CombatLogEntry cle) {
        switch (cle.getType()) {
            case DOTA_COMBATLOG_DAMAGE:
                if (cle.getTargetName().equals("npc_dota_hero_nevermore") && cle.getTargetTeam() == winnerTeam) {
                    String attackerName = cle.getAttackerName();

                    if (attackerName.startsWith("npc_dota_hero_")) {
                        states[tick].timeSinceDamagedByHero = 0;
                    } else if (attackerName.startsWith("npc_dota_creep_")) {
                        states[tick].timeSinceDamagedByCreep = 0;
                    } else if (attackerName.startsWith("npc_dota_badguys_tower")
                            || attackerName.startsWith("npc_dota_goodguys_tower")) {
                        states[tick].timeSinceDamagedByTower = 0;
                    }
                } else if (cle.getAttackerName().equals("npc_dota_hero_nevermore")
                        && cle.getAttackerTeam() == winnerTeam) {
                    
                    if (cle.getTargetName().startsWith("npc_dota_hero")) {
                        states[tick].recentlyHitHero++;
                        if (cle.getHealth() == 0) {
                            states[tick].recentlyKilledHero++;
                        }
                    } else if (cle.getTargetName().startsWith("npc_dota_creep") && cle.getTargetTeam() != winnerTeam) {
                        states[tick].recentlyHitCreep++;
                        if (cle.getHealth() == 0) {
                            states[tick].recentlyKilledCreep++;
                        }
                    }
                }
                break;
            case DOTA_COMBATLOG_GAME_STATE:
                if (cle.getValue() == 4) {
                    beginTick = tick;
                } else if (gameState == 5) {
                    endTick = tick - 2;

                }
                gameState = cle.getValue();
                break;
        }
    }

    public void run() throws Exception {
        new SimpleRunner(new MappedFileSource(replayFile)).runWith(this);
    }
}
