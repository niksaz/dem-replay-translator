package com.github.sazanovich.mikita.parser;

import java.util.ArrayList;

/**
 * DEMState of our Nevermore in a current tick.
 */
public class DEMState {
    /*/***************************
      Data of the game in whole.
    *****************************/

    /** Team of our Nevermore: 2 for radiant, 3 for dire. */
    public int ourTeam;

    /** Name of enemy hero. */
    public String enemyName;

    /*/***************************
      Data of the game in this moment.
    *****************************/

    /** Time of the state. Measured in ticks. If 0, state is invalid. */
    public int time;

    /** Points of our team. */
    public int ourScore;

    /** Points of enemy team. */
    public int enemyScore;

    /*/***************************
      Data of our Nevermore in this moment.
    *****************************/

    /** Coordinate X. */
    public int ourX;

    /** Coordinate Y. */
    public int ourY;

    /** The facing of this unit on a 360 degree rotation. */
    public float ourFacing;

    /** Level of our hero. If 0, basic data (coords, hp, mana, lvl) of our Nevermore is invalid. */
    public int ourLvl;

    public int ourHp;

    public int ourMaxHp;

    public int ourMana;

    public int ourMaxMana;

    /** Our hero attack damage. It's calculated as (min basic dmg + max basic dmg) / 2 + bonus dmg. */
    public int ourAttackDamage;

    /** Gold of our hero. If -1, gold data is invalid. */
    public int ourGold = -1;

    /** Is castable ability "Shadowraze", near. */
    public Boolean isOurAbility1Available = null;

    /** Is castable ability "Shadowraze", medium. */
    public Boolean isOurAbility2Available = null;

    /** Is castable ability "Shadowraze", far. */
    public Boolean isOurAbility3Available = null;

    /** Is castable ability "Requiem of Souls".*/
    public Boolean isOurAbility4Available = null;


    /**
     * Bundle of time since damaged by someone.
     * -1, if it wasn't yet or was a long time ago.
     * null, if uninitialized.
     * Max = 900.
     */
    public Integer timeSinceDamagedByHero = null;

    public Integer timeSinceDamagedByTower = null;

    public Integer timeSinceDamagedByCreep = null;

    /*/***************************
      Data of enemy in this moment.
    *****************************/

    /**
     * Flag, detecting if enemy visible by our hero.
     */
    public boolean isEnemyVisible;

    public int enemyX;

    public int enemyY;

    /** HP of enemy hero. */
    public int enemyHp;

    /** Maximum of enemy HP. */
    public int enemyMaxHp;

    /** Mana of enemy hero. */
    public int enemyMana;

    /** Maximum of enemy mana. */
    public int enemyMaxMana;

    /** Level of enemy hero. If 0, data of enemy is invalid. */
    public int enemyLvl;

    /** The facing of this unit on a 360 degree rotation. */
    public float enemyFacing;

    public int enemyAttackDamage;

    public int recentlyHitCreep = 0;

    public int recentlyKilledCreep = 0;

    public int recentlyHitHero = 0;

    public int recentlyKilledHero = 0;

    /*/***************************
      Data of nearby creeps.
    *****************************/

    public static class CreepState {
        /**
         * There are 8 types of creeps:
         * 0 - Melee creep;
         * 1 - Ranged creep;
         * 2 - Siege creep;
         * Probably, other five types don't spawn in 1v1 mode, but I'm not sure.
         */
        public int type;

        /** Creep HP. */
        public int hp;

        /** Creep max HP. */
        public int maxHp;

        /** Creep X coordinate. */
        public int x;

        /** Creep Y coordinate. */
        public int y;

        /** Flag detecting if this creep is visible by other team. Useful only for enemy creeps. */
        public boolean isVisible = false;

        /** Creates empty state. */
        public CreepState() {
            x = -1;
            y = -1;
            hp = -1;
            maxHp = -1;
            type = -1;
        }

        public CreepState(CreepState s) {
            x = s.x;
            y = s.y;
            hp = s.hp;
            maxHp = s.maxHp;
            type = s.type;
        }
    }

    /** List of nearby friendly creeps. */
    public ArrayList<CreepState> ourCreeps = new ArrayList<>();

    /** List of nearby visible enemy creeps. */
    public ArrayList<CreepState> enemyCreeps = new ArrayList<>();

    /*/***************************
      Data of middle towers.
    *****************************/

    /** HP of our tower. */
    public int ourTowerHp;

    /** HP of enemy tower. */
    public int enemyTowerHp;
    public int enemyTowerX;
    public int enemyTowerY;


    /**
     * Prints all available data of the state.
     */
    public void print() {
        if (ourTeam == 2) {
            System.out.printf("DATA: Our Nevermore vs %s. ", enemyName);
            System.out.printf("Tick: %d. Score: %d : %d\n", time, ourScore, enemyScore);
        } else {
            System.out.printf("DATA: %s vs Our Nevermore. ", enemyName);
            System.out.printf("Tick: %d. Score: %d : %d\n", time, enemyScore, ourScore);
        }

        System.out.printf("OUR HERO DATA. HP: %d / %d  |  ", ourHp, ourMaxHp);
        System.out.printf("Mana: %d / %d  |  ", ourMana, ourMaxMana);
        System.out.printf("Level: %d  |  ", ourLvl);
        System.out.printf("Attack damage: %d  |  ", ourAttackDamage);
        System.out.printf("Gold: %d \n", ourGold);
        System.out.printf("Coordinates: (%d, %d); facing %f \n", ourX, ourY, ourFacing);

        System.out.print("OPPONENT DATA. ");
        System.out.printf("HP: %d / %d  |  ", enemyHp, enemyMaxHp);
        System.out.printf("Mana: %d / %d  |  ", enemyMana, enemyMaxMana);
        System.out.printf("Level: %d  |  ", enemyLvl);
        System.out.printf("Attack damage: %d \n", enemyAttackDamage);
        System.out.printf("Coordinates: (%d, %d); ", enemyX, enemyY);
        if (isEnemyVisible) {
            System.out.printf("facing %f \n", enemyFacing);
        } else {
            System.out.print("Enemy isn't visible! \n");
        }

        System.out.print("OUR CREEPS: \n");
        for (CreepState s : ourCreeps) {
            System.out.printf("TYPE: %d, HP: %d / %d; Coordinates: (%d, %d) \n", s.type, s.hp, s.maxHp, s.x, s.y);
        }
        System.out.print("ENEMY CREEPS: \n");
        for (CreepState s : enemyCreeps) {
            System.out.printf("TYPE: %d, HP: %d / %d; Coordinates: (%d, %d) \n", s.type, s.hp, s.maxHp, s.x, s.y);
        }

        System.out.printf("OUR TOWER: %d / 1600 \n", ourTowerHp);
        System.out.printf("ENEMY TOWER: %d / 1600 \n", enemyTowerHp);
    }

}
