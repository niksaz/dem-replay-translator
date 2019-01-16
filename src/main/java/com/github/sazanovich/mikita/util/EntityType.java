package com.github.sazanovich.mikita.util;

public enum EntityType {
    OUR_HERO, ENEMY_HERO,   // Contain HP, mana, coords, facing, level and damage.
    OUR_CREEP, ENEMY_CREEP, // Contain all creep info.
    OUR_TOWER, ENEMY_TOWER, // Contain tower HP.

    OUR_TEAM, ENEMY_TEAM,   // Contain score
    OUR_DATA,               // Contains gold
    ENEMY_DATA,
    OUR_ABILITY,            // Contains info about ability availability
    ENEMY_ABILITY,

    UNKNOWN                 // type is unknown (not one of listed before)
}
