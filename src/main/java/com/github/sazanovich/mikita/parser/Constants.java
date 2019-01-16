package com.github.sazanovich.mikita.parser;

/**
 * String constants that identify fields in some entities.
 */
public class Constants {
    public static final String TEAM = "m_iTeamNum";

    public static final String HERO_KILLS = "m_iHeroKills";
    public static final String TOWER_KILLS = "m_iTowerKills";

    public static final String HP = "m_iHealth";
    public static final String MAX_HP = "m_iMaxHealth";

    public static final String MANA = "m_flMana";
    public static final String MAX_MANA = "m_flMaxMana";

    public static final String LVL = "m_iCurrentLevel";

    public static final String FACING = "CBodyComponent.m_angRotation";

    // There are many values of hero's damage. This is _probably_ the necessary ones.
    public static final String ATTACK_DAMAGE_MIN = "m_iDamageMin";
    public static final String ATTACK_DAMAGE_MAX = "m_iDamageMax";
    public static final String ATTACK_DAMAGE_BONUS = "m_iDamageBonus";

    // Gets data about visibility to all teams. Bit magic required to get the one you need.
    public static final String VISIBILITY = "m_iTaggedAsVisibleByTeam";

    public static final String ABILITY_COOLDOWN = "m_fCooldown";
    public static final String ABILITY_RANGE = "m_iCastRange";
    public static final String IS_ABILITY_ACTIVATED = "m_bInAbilityPhase";
    public static final String ABILITY_COST = "m_iManaCost";

    // Some kind of creep id.
    public static final String CREEP_ID = "m_nHierarchyId";
    public static final String CREEP_MAGIC_RESIST = "m_flMagicalResistanceValue";


}
