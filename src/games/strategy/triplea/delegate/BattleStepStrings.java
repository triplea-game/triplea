/*
 * BattleStepStrings.java
 *
 * Created on January 16, 2002, 10:39 AM
 */

package games.strategy.triplea.delegate;

/**
 * Constants for the battle steps
 *
 * @author  Sean Bridges
 */
public interface BattleStepStrings 
{
	final static String AA_GUNS_FIRE = "AA guns fire";
	final static String SELECT_AA_CASUALTIES = "Select AA casualties";
	final static String REMOVE_AA_CASUALTIES = "Remove AA casualties";
	
	final static String NAVAL_BOMBARDMENT = "Naval bombardment";
	final static String SELECT_NAVAL_BOMBARDMENT_CASUALTIES = "Select naval bombardment casualties";
	
	final static String ATTACKER_SUBS_FIRE = "Subs sneak attack";
	final static String DEFENDER_SELECT_SUB_CASUALTIES = "Select sneak attack casualties";
	final static String DEFENDER_REMOVE_SUB_CASUALTIES = "Remove sneak attack casualties";
	
	final static String ATTACKER_FIRES = "Attacker fires";
	final static String DEFENDER_SELECT_CASUALTIES = "Defender select casualties";
	
	final static String DEFENDER_FIRES_SUBS = "Defender fires subs";
	final static String ATTACKER_SELECT_SUB_CASUALTIES = "Attacker select sub casualties";
	
	final static String DEFENDER_FIRES = "Defender fires";
	final static String ATTACKER_SELECT_CASUALTIES = "Attacker select casualties";
	
	final static String REMOVE_CASUALTIES = "Remove casualties";
	final static String ATTACKER_SUBS_WITHDRAW = "Attacker subs withdraw";
	final static String DEFENDER_SUBS_WITHDRAW = "Defender subs withdraw";
	final static String ATTACKER_WITHDRAW = "Attacker withdraws";
}