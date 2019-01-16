package com.github.sazanovich.mikita.parser;

/** Possible types of action that our Nevermore can do, according to our state. */
public class DEMAction {
    /**
     * Type of action:
     * 0 - Change coordinates
     * 1 - Attack Hero
     * 2 - Attack enemy creep (number of the creep is param1. Numeration from 1!!)
     * 3 - Use Ability (number of ability is param1)
     * 4 - Attack Tower
     * 5 - Attack our creep (number of the creep is param1. Numeration from 1!!)
     * -1 - do nothing (continue to make previous action)
     */
    public int actionType = -1;

    /** Parameter of action. */
    public int param;

    /** Coordinates change. */
    public int nx;
    public int ny;

    @Override
    public String toString() {
        String result = "DEMAction: ";
        switch (actionType) {
            case 0:
                result += String.format("Move on vector (%d, %d).", nx, ny);
                break;
            case 1:
                result += "Attack hero.";
                break;
            case 2:
                result += String.format("Attack creep #%d", param);
                break;
            case 3:
                result += "Use ability " + (param == 4 ? "Requiem" : String.format("Shadowraze%d", param));
                break;
            case 4:
                result += "Attack tower.";
                break;
            case -1:
                result += "Do nothing (continue).";
                break;
            default:
                result += "Unknown.";
        }

        return result;
    }

}
