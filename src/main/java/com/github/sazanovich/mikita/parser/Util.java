package com.github.sazanovich.mikita.parser;

import com.github.sazanovich.mikita.util.EntityType;
import com.github.sazanovich.mikita.util.IntPair;
import skadistats.clarity.model.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import static java.lang.Integer.min;
import static java.lang.Math.abs;


/**
 * Utility Functions for replay parser.
 */
public class Util {
    /**
     * Gets coordinates on the field from entity.
     * @param e given entity
     * @return pair of coordinates (x, y)
     */
    public static IntPair getCoordFromEntity(Entity e) {
        Integer cellX = e.getProperty("CBodyComponent.m_cellX");
        Integer cellY = e.getProperty("CBodyComponent.m_cellY");
        Float vecX = e.getProperty("CBodyComponent.m_vecX");
        Float vecY = e.getProperty("CBodyComponent.m_vecY");

        // This is magic to get map coordinates. Taken from here:
        // https://github.com/spheenik/clarity-analyzer/blob/master/src/main/java/skadistats/clarity/analyzer/main/icon/EntityIcon.java
        return new IntPair(cellX * 128 + vecX.intValue() - 16384, cellY * 128 + vecY.intValue() - 16384);
    }

    /**
     * Gets entity type.
     * @param e given entity
     * @param winnerTeam team which we are belong to
     * @return type of entity or UNKNOWN, if type is unknown
     */
    public static EntityType getEntityType(Entity e, Integer winnerTeam) {
        String entityName = e.getDtClass().getDtName();
        if (entityName.startsWith("CDOTA_Unit_Hero")) {
            return e.getProperty(Constants.TEAM) == winnerTeam ?
                    EntityType.OUR_HERO :
                    EntityType.ENEMY_HERO;
        }

        if (entityName.startsWith("CDOTA_BaseNPC_Tower")) {
            int team = e.getProperty(Constants.TEAM);
            return team == winnerTeam ?
                    EntityType.OUR_TOWER :
                    team == 5 - winnerTeam ? EntityType.ENEMY_TOWER : EntityType.UNKNOWN;
        }

        if (entityName.startsWith("CDOTA_BaseNPC_Creep")) {
            int team = e.getProperty(Constants.TEAM);
            return team == winnerTeam ?
                    EntityType.OUR_CREEP :
                    team == 5 - winnerTeam ? EntityType.ENEMY_CREEP : EntityType.UNKNOWN;
        }

        if (entityName.startsWith("CDOTATeam")) {
            int team = e.getProperty(Constants.TEAM);
            return team == winnerTeam ?
                    EntityType.OUR_TEAM :
                    team == 5 - winnerTeam ? EntityType.ENEMY_TEAM : EntityType.UNKNOWN;
        }

        if (entityName.startsWith("CDOTA_Data")) {
            int team = e.getProperty(Constants.TEAM);
            return team == winnerTeam ?
                    EntityType.OUR_DATA :
                    team == 5 - winnerTeam ? EntityType.ENEMY_DATA : EntityType.UNKNOWN;
        }

        if (entityName.startsWith("CDOTA_Ability_Nevermore_")) {
            int team = e.getProperty(Constants.TEAM);
            return team == winnerTeam ?
                    EntityType.OUR_ABILITY :
                    team == 5 - winnerTeam ? EntityType.ENEMY_ABILITY : EntityType.UNKNOWN;
        }


        return EntityType.UNKNOWN;
    }

    /**
     * Gets score of team from team entity.
     * @param e given team entity
     * @return score of the team
     */
    public static int getScoreFromEntity(Entity e) {
        Integer towerKills = e.getProperty(Constants.TOWER_KILLS);
        if (towerKills > 0) {
            return 2;
        }

        return (Integer) e.getProperty(Constants.HERO_KILLS);
    }

    /**
     * Gets ability number from given ability entity.
     * @param e given ability entity
     * @return number of ability or 0, if ability doesn't fit
     */
    public static int getAbilityTypeFromEntity(Entity e) {
        assert e != null;
        String entityName = e.getDtClass().getDtName();
        if (entityName.endsWith("Requiem")) {
            return 4;
        }

        int range = e.getProperty(Constants.ABILITY_RANGE);
        switch (range) {
            case 200:
                return 1;
            case 450:
                return 2;
            case 700:
                return 3;
            default:
                return 0;
        }
    }

    /**
     * Gets creep type from creep entity.
     * @param e given creep entity
     * @return creep type or -1, if type is unknown
     */
    public static int getCreepTypeFromEntity(Entity e) {
        if (!e.getDtClass().getDtName().equals("CDOTA_BaseNPC_Creep_Lane")
                && !e.getDtClass().getDtName().equals("CDOTA_BaseNPC_Creep_Siege")) {
            return -1;
        }

        // Identifies siege creep
        if (e.getDtClass().getDtName().equals("CDOTA_BaseNPC_Creep_Siege")) {
            return 2;
        }

        // Identifies range creep
        if ((Float) e.getProperty(Constants.MAX_MANA) != 0) {
            return 1;
        }

        // Identifies melee creep
        if ((Float) e.getProperty(Constants.CREEP_MAGIC_RESIST) == 0) {
            return 0;
        }

        return -1;
    }

    /**
     * Updates given map with creep state using given entity. If entity has id that is already in the map,
     * state is being updated (or deleted, if entity hp == 0). If not, state is added.
     * @param e given entity
     * @param creeps creeps map
     */
    public static void updateCreepMapFromEntity(Entity e, Map<Integer, DEMState.CreepState> creeps) {
        Integer id = e.getProperty(Constants.CREEP_ID);
        if (creeps.containsKey(id)) {
            if ((Integer) e.getProperty(Constants.HP) == 0) {
                creeps.remove(id);
            } else {
                DEMState.CreepState state = creeps.get(id);
                Util.updateCreepStateFromEntity(e, state);
            }
        } else if ((Integer) e.getProperty(Constants.HP) != 0) {
            DEMState.CreepState state = new DEMState.CreepState();
            Util.updateCreepStateFromEntity(e, state);
            creeps.put(id, state);
        }
    }

    /**
     * Gets info if ability available. It is iff cooldown is 0 and it isn't activated and mana is enough.
     * @param e given ability entity
     * @param mana mana of ability holder
     * @return is ability available
     */
    public static boolean isAbilityAvailable(Entity e, int mana) {
        return (Float) e.getProperty(Constants.ABILITY_COOLDOWN) == 0
                && !(Boolean) e.getProperty(Constants.IS_ABILITY_ACTIVATED)
                && (Integer) e.getProperty("m_iLevel") != 0
                && mana >= (Integer) e.getProperty(Constants.ABILITY_COST);
    }


    /**
     * Completes state info of states[tick], using info from previous state. If some state wasn't updated
     * on this tick, then it hadn't changed, therefore it's the same as previous one.
     * It doesn't complete creep info, because it requires other things and more difficult.
     * @param tick number (tick) of updated state
     * @param states states where all states are (including this and previous)
     */
    public static void stateClosure(int tick, DEMState[] states) {
        states[tick].time = tick;

        if (states[tick].ourScore == 0) {
            states[tick].ourScore = states[tick - 1].ourScore;
        }

        if (states[tick].enemyScore == 0) {
            states[tick].enemyScore = states[tick - 1].enemyScore;
        }

        if (states[tick].ourLvl == 0) {
            states[tick].ourHp = states[tick - 1].ourHp;
            states[tick].ourMaxHp = states[tick - 1].ourMaxHp;
            states[tick].ourMana = states[tick - 1].ourMana;
            states[tick].ourMaxMana = states[tick - 1].ourMaxMana;
            states[tick].ourLvl = states[tick - 1].ourLvl;
            states[tick].ourX = states[tick - 1].ourX;
            states[tick].ourY = states[tick - 1].ourY;
            states[tick].ourFacing = states[tick - 1].ourFacing;
            states[tick].ourAttackDamage = states[tick - 1].ourAttackDamage;
        }

        if (states[tick].timeSinceDamagedByHero == null) {
            Integer lastTime = states[tick - 1].timeSinceDamagedByHero;
            if (lastTime == null || lastTime < 0 || lastTime >= 900) {
                states[tick].timeSinceDamagedByHero = -1;
            } else {
                states[tick].timeSinceDamagedByHero = lastTime + 1;
            }
        }

        if (states[tick].timeSinceDamagedByTower == null) {
            Integer lastTime = states[tick - 1].timeSinceDamagedByTower;
            if (lastTime == null || lastTime < 0 || lastTime >= 900) {
                states[tick].timeSinceDamagedByTower = -1;
            } else {
                states[tick].timeSinceDamagedByTower = lastTime + 1;
            }
        }

        if (states[tick].timeSinceDamagedByCreep == null) {
            Integer lastTime = states[tick - 1].timeSinceDamagedByCreep;
            if (lastTime == null || lastTime < 0 || lastTime >= 900) {
                states[tick].timeSinceDamagedByCreep = -1;
            } else {
                states[tick].timeSinceDamagedByCreep = lastTime + 1;
            }
        }

        if (states[tick].ourGold == -1) {
            states[tick].ourGold = states[tick - 1].ourGold;
        }
        
        if (states[tick].isOurAbility1Available == null) {
            states[tick].isOurAbility1Available = states[tick - 1].isOurAbility1Available == null ? false : states[tick - 1].isOurAbility1Available;
        }

        if (states[tick].isOurAbility2Available == null) {
            states[tick].isOurAbility2Available = states[tick - 1].isOurAbility2Available == null ? false : states[tick - 1].isOurAbility2Available;;
        }

        if (states[tick].isOurAbility3Available == null) {
            states[tick].isOurAbility3Available = states[tick - 1].isOurAbility3Available == null ? false : states[tick - 1].isOurAbility3Available;;
        }

        if (states[tick].isOurAbility4Available == null) {
            states[tick].isOurAbility4Available = states[tick - 1].isOurAbility4Available == null ? false : states[tick - 1].isOurAbility4Available;;
        }

        if (states[tick].enemyLvl == 0) {
            states[tick].isEnemyVisible = states[tick - 1].isEnemyVisible;
            states[tick].enemyHp = states[tick - 1].enemyHp;
            states[tick].enemyMaxHp = states[tick - 1].enemyMaxHp;
            states[tick].enemyMana = states[tick - 1].enemyMana;
            states[tick].enemyMaxMana = states[tick - 1].enemyMaxMana;
            states[tick].enemyLvl = states[tick - 1].enemyLvl;
            states[tick].enemyX = states[tick - 1].enemyX;
            states[tick].enemyY = states[tick - 1].enemyY;
            states[tick].enemyFacing = states[tick - 1].enemyFacing;
            states[tick].enemyAttackDamage = states[tick - 1].enemyAttackDamage;
        }

        if (states[tick].ourTowerHp == 0) {
            states[tick].ourTowerHp = states[tick - 1].ourTowerHp;
        }

        if (states[tick].enemyTowerHp == 0) {
            states[tick].enemyTowerHp = states[tick - 1].enemyTowerHp;
            states[tick].enemyTowerX = states[tick - 1].enemyTowerX;
            states[tick].enemyTowerY = states[tick - 1].enemyTowerY;
            states[tick].isEnemyTowerVisible = states[tick - 1].isEnemyTowerVisible;
        }
    }

    // Calculates square of distance from creep to hero.
    private static int squareDistToHero(DEMState.CreepState s, int heroX, int heroY) {
        return (s.x - heroX) * (s.x - heroX) + (s.y - heroY) * (s.y - heroY);
    }

    /**
     * Updates creep state from its entity.
     * @param e creep entity
     * @param state creep state that has to be updated
     */
    public static void updateCreepStateFromEntity(Entity e, DEMState.CreepState state) {
        state.type = Util.getCreepTypeFromEntity(e);
        state.hp = e.getProperty(Constants.HP);
        state.maxHp = e.getProperty(Constants.MAX_HP);

        IntPair p = Util.getCoordFromEntity(e);
        state.x = p.fst;
        state.y = p.snd;

        int oppositeTeam = 5 - (Integer) e.getProperty(Constants.TEAM);
        state.isVisible = ((Integer) e.getProperty(Constants.VISIBILITY) & (1 << oppositeTeam)) != 0;
    }

    /**
     * Saves creeps info from creep map to given state. It is separated from stateClosure, because
     * creep info is kept in different way while parsing.
     * @param state outer state of creep states
     * @param creeps map that has creep id and its state
     * @param isOurs are those creeps belong to our team
     */
    public static void saveCreepInfoToState(DEMState state, Map<Integer, DEMState.CreepState> creeps, boolean isOurs) {
        ArrayList<DEMState.CreepState> list = new ArrayList<>(creeps.values());
        int heroX = state.ourX;
        int heroY = state.ourY;
        list.sort(Comparator.comparingInt(s -> squareDistToHero(s, heroX, heroY)));

        ArrayList<DEMState.CreepState> stateList;
        if (isOurs) {
            stateList = state.ourCreeps = new ArrayList<>();
        } else {
            stateList = state.enemyCreeps = new ArrayList<>();
        }

        for (int i = 0; i < min(10, list.size()); i++) {
            if (squareDistToHero(list.get(i), heroX, heroY) >= 1600 * 1600)
                break;

            if (isOurs || list.get(i).isVisible)
                stateList.add(new DEMState.CreepState(list.get(i)));
        }
    }

    public static void actionClosure(int tick, DEMState[] states, DEMAction[] actions,
                                     Map<Integer, DEMState.CreepState> enemyCreeps,
                                     Map<Integer, DEMState.CreepState> ourCreeps) {
        switch (actions[tick].actionType) {
            case -1:
                assert actions[tick].nx == 0 && actions[tick].ny == 0;
                break;
            case 0:
                if (actions[tick].nx == states[tick].ourX && actions[tick].ny == states[tick].ourY) {
                    // We are moving to our current location -> doing nothing.
                    actions[tick].actionType = -1;
                }
                break;
            case 2:
                ArrayList<Integer> listEnemy = new ArrayList<>(enemyCreeps.keySet());
                ArrayList<Integer> listOur = new ArrayList<>(ourCreeps.keySet());
                int heroX = states[tick].ourX;
                int heroY = states[tick].ourY;

                listEnemy.sort(Comparator.comparingInt(s -> squareDistToHero(enemyCreeps.get(s), heroX, heroY)));
                listOur.sort(Comparator.comparingInt(s -> squareDistToHero(ourCreeps.get(s), heroX, heroY)));

                int num = actions[tick].param;

                if (listOur.contains(num)) {
                    actions[tick].actionType = 5;
                    actions[tick].param = listOur.indexOf(num) + 1;
                } else {
                    actions[tick].param = listEnemy.indexOf(num) + 1;
                    if (actions[tick].param == 0) {
                        actions[tick].param = 1;
                    }
                }

                break;
            case 3:
                switch (actions[tick].param) {
                    case 1:
                        states[tick].isOurAbility1Available = true;
                        break;
                    case 2:
                        states[tick].isOurAbility2Available = true;
                        break;
                    case 3:
                        states[tick].isOurAbility3Available = true;
                        break;
                    case 4:
                        states[tick].isOurAbility4Available = true;
                        break;
                }
        }
    }
}
