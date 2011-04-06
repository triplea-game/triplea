/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Used to store information about a dice roll.
 *  # of rolls at 5, at 4, etc.<p>
 *  
 *  Externalizble so we can efficiently write out our dice as ints
 *  rather than as full objects.
 */
public class DiceRoll implements Externalizable
{

    private List<Die> m_rolls;
    //this does not need to match the Die with isHit true
    //since for low luck we get many hits with few dice
    private int m_hits;
    
    /**
     * Returns an int[] with 2 values, the first is the max attack, the second is the max dice sides for the AA unit with that attack value
     */
    public static int[] getAAattackAndMaxDiceSides(Territory location, PlayerID player, GameData data, Match<Unit> typeOfAA)
    {
    	int[] attackThenDiceSides = new int[2];
    	CompositeMatch<Unit> enemyAA = new CompositeMatchAnd<Unit>(typeOfAA, Matches.unitIsEnemyOf(data, player));
    	
    	Collection<Unit> eAA = new ArrayList<Unit>(Match.getMatches(location.getUnits().getUnits(), enemyAA));
        int highestAttack = 0;
		int diceSize = data.getDiceSides();
		int chosenDiceSize = diceSize;
        for (Unit u : eAA)
        {
    		UnitAttachment ua = UnitAttachment.get(u.getType());
    		int uaDiceSides = ua.getAttackAAmaxDieSides();
    		if (uaDiceSides < 1)
    			uaDiceSides = diceSize;
    		
    		int attack = ua.getAttackAA();
    		if (attack > 0 && Matches.UnitIsRadarAA.match(u))
    			attack++; // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something other than 6
    		
    		if (attack > uaDiceSides)
    			attack = uaDiceSides;
    		
    		if (((float) attack) / ((float) uaDiceSides) > ((float) highestAttack) / ((float) chosenDiceSize))
    		{
    			highestAttack = attack;
    			chosenDiceSize = uaDiceSides;
    		}
        }
        if (highestAttack > chosenDiceSize / 2 && chosenDiceSize > 1)
        	highestAttack = chosenDiceSize / 2; // sadly the whole low luck section falls apart if AA are hitting at greater than half the value of dice, and I don't feel like rewriting it
        
        attackThenDiceSides[0] = highestAttack;
        attackThenDiceSides[1] = chosenDiceSize;
        return attackThenDiceSides;
    }

    public static DiceRoll rollAA(Collection<Unit> attackingUnits, IDelegateBridge bridge, Territory location, GameData data, Match<Unit> typeOfAA)
    {
        int hits = 0;
        
        List<Die> sortedDice = new ArrayList<Die>();
        
        int attackThenDiceSides[] = getAAattackAndMaxDiceSides(location, bridge.getPlayerID(), data, typeOfAA);
        int highestAttack = attackThenDiceSides[0];
        int chosenDiceSize = attackThenDiceSides[1];
        
        int hitAt = highestAttack - 1; // zero based
        int power = highestAttack; // not zero based
        
        //LOW LUCK
        if (highestAttack > 0 && (games.strategy.triplea.Properties.getLow_Luck(data) || games.strategy.triplea.Properties.getLL_AA_ONLY(data)))
        {
        	String annotation = "Roll AA guns in " + location.getName();
        	int groupSize = chosenDiceSize / power;
            
            List<Unit> airUnits = Match.getMatches(attackingUnits, Matches.UnitIsAir);
            if(Properties.getChoose_AA_Casualties(data)) 
            {
            	hits += getLowLuckHits(bridge, sortedDice, power, annotation,
						airUnits.size());
            } else 
            {
            	Tuple<List<Unit>, List<Unit>> airSplit = BattleCalculator.categorizeLowLuckAirUnits(airUnits, location, chosenDiceSize, groupSize);
            	            
            	//this will not roll any dice, since the first group is 
            	//a multiple of 3 or 6
            	hits += getLowLuckHits(bridge, sortedDice, power, annotation,
						airSplit.getFirst().size());
            	//this will roll dice, unless it is empty
            	hits += getLowLuckHits(bridge, sortedDice, power, annotation,
						airSplit.getSecond().size());

	
            }
        } 
        else if (highestAttack > 0) // Normal rolling
        {            
            String annotation = "Roll AA guns in " + location.getName();
           
            int[] dice = bridge.getRandom(chosenDiceSize, Match.countMatches(attackingUnits, Matches.UnitIsAir), annotation);
            
            for (int i = 0; i < dice.length; i++)
            {
                boolean hit = dice[i] <= hitAt;
                sortedDice.add(new Die(dice[i], hitAt+1, hit ? DieType.HIT : DieType.MISS));
                if (hit)
                    hits++;
            }
        }

        DiceRoll roll = new DiceRoll(sortedDice, hits);        
        String annotation = "AA guns fire in " + location + " : " + MyFormatter.asDice(roll);
        bridge.getHistoryWriter().addChildToEvent(annotation, roll);
        return roll;
    }

	private static int getLowLuckHits(IDelegateBridge bridge, 
			List<Die> sortedDice, int power, String annotation,
			int numberOfAirUnits) {
		
		int hits = (numberOfAirUnits * power) / bridge.getPlayerID().getData().getDiceSides();
		int hitsFractional = (numberOfAirUnits * power) % bridge.getPlayerID().getData().getDiceSides();

		if (hitsFractional > 0)
		{			
			int[] dice = bridge.getRandom(bridge.getPlayerID().getData().getDiceSides(), 1, annotation);
		    boolean hit = hitsFractional > dice[0];
		    if(hit) {
		    	hits++;
		    }
		    Die die = new Die(dice[0], hitsFractional, hit ? DieType.HIT : DieType.MISS);
		    sortedDice.add(die);      
		}
		return hits;
	}

    /**
     * Roll dice for units.
     * @param annotation 
     *  
     */
    public static DiceRoll rollDice(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {
        // Decide whether to use low luck rules or normal rules.
        if (games.strategy.triplea.Properties.getLow_Luck(data))
        {
            return rollDiceLowLuck(units, defending, player, bridge, data, battle, annotation);
        } else
        {
            return rollDiceNormal(units, defending, player, bridge, data, battle, annotation);
        }
    }
    
    /**
     * Roll n-sided dice.
     * @param annotation 
     * 0 based, add 1 to get actual die roll
     */
    public static DiceRoll rollNDice(IDelegateBridge bridge, int rollCount, int sides, String annotation)
    {   
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(), 0);
        }

        int[] random;        
        random = bridge.getRandom(sides, rollCount, annotation);

        List<Die> dice = new ArrayList<Die>();
        int diceIndex = 0;

        for (int i = 0; i < rollCount; i++)
        {
            dice.add(new Die(random[diceIndex], 1, DieType.IGNORED ));
            diceIndex++;
        }

        DiceRoll rVal = new DiceRoll(dice, rollCount);
        return rVal;
    }
    
    /**
     * Roll dice for units using low luck rules. Low luck rules based on rules
     * in DAAK.
     */
    private static DiceRoll rollDiceLowLuck(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {
    	boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
    	int artillerySupportAvailable = getArtillerySupportAvailable(units, defending, player);
        int rollCount = BattleCalculator.getRolls(units, player, defending, artillerySupportAvailable);
        Set<List<UnitSupportAttachment>> supportRules = new HashSet<List<UnitSupportAttachment>>();
        IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
        getSupport(units,supportRules,supportLeft,data,defending);
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(0), 0);
        }

        Iterator<Unit> iter = units.iterator();

        int power = 0;
        int hitCount = 0;

        // We iterate through the units to find the total strength of the units
        while (iter.hasNext())
        {
            Unit current = iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());            
            int rolls = BattleCalculator.getRolls(current, player, defending, artillerySupportAvailable);
            int totalStr =0;
            for (int i = 0; i < rolls; i++)
            {
            	if(i > 1 && lhtrBombers && ua.isStrategicBomber() )
            	{
            		if( totalStr < data.getDiceSides()) {
            			power+=1;
            			totalStr+=1;
            		}
            		continue;
            	}
                int strength;
                if (defending)
                {
                    strength = ua.getDefense(current.getOwner());
                    //If it's a sneak attack, defenders roll at a 1
                    if (isFirstTurnLimitedRoll(player))
                    {
                        strength = Math.min(1, strength);
                    }
                    else strength += getSupport(current.getType(), supportRules, supportLeft);
                }                
                else
                {
                    strength = ua.getAttack(current.getOwner());
                    strength += getSupport(current.getType(), supportRules, supportLeft);
                    if (ua.getIsMarine() && battle.isAmphibious())
                    {
                        Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                        if(landUnits.contains(current))
                            ++strength;
                    } 
                    if (ua.isSea() && battle.isAmphibious())
                    	strength = ua.getBombard(current.getOwner());  
                    strength += getSupport(current.getType(), supportRules, supportLeft);
                }
                totalStr += strength;
                power += Math.min(Math.max(strength, 0), data.getDiceSides());;
            }
        }

        // Get number of hits
        hitCount = power / data.getDiceSides();

        int[] random = new int[0];

        List<Die> dice = new ArrayList<Die>();
        // We need to roll dice for the fractional part of the dice.
        power = power % data.getDiceSides();
        if (power != 0)
        {
        	random = bridge.getRandom(data.getDiceSides(), 1, annotation);
            boolean hit = power > random[0]; 
            if (hit)
            {
                hitCount++;
            }
            dice.add(new Die(random[0], power, hit ? DieType.HIT : DieType.MISS ));
        }

        // Create DiceRoll object
        DiceRoll rVal = new DiceRoll(dice, hitCount);
        bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
        return rVal;
    }

    /**
     * @param units
     * @param defending
     * @param player
     * @return
     */
    public static int getArtillerySupportAvailable(List<Unit> units, boolean defending, PlayerID player)
    {
        int artillerySupportAvailable = 0;
        if (!defending)
        {
            Collection<Unit> arty = Match.getMatches(units, Matches.UnitIsArtillery);
            Iterator<Unit> iter = arty.iterator();
            while (iter.hasNext())
            {
            	Unit current = (Unit) iter.next();
                UnitAttachment ua = UnitAttachment.get(current.getType());
                artillerySupportAvailable += ua.getUnitSupportCount(current.getOwner());
            }

            //If ImprovedArtillery, double number of units to support
            if(isImprovedArtillerySupport(player))
                artillerySupportAvailable *= 2;
        }
        return artillerySupportAvailable;
    }
    public static int getArtillerySupportAvailable(Unit u, boolean defending, PlayerID player)
    {
        if (Matches.UnitIsArtillery.match(u) && !defending)
        {
        	UnitAttachment ua = UnitAttachment.get(u.getType());
        	int artillerySupportAvailable = ua.getUnitSupportCount(u.getOwner());
        	if(isImprovedArtillerySupport(player))
                artillerySupportAvailable *= 2;
        	return artillerySupportAvailable;
        }
        return 0;
    }

    public static int getSupportableAvailable(List<Unit> units, boolean defending, PlayerID player)
    {
        if (!defending)
        	return Match.countMatches(units, Matches.UnitIsArtillerySupportable);
        return 0;
    }
    public static int getSupportableAvailable(Unit u, boolean defending, PlayerID player)
    {
        if (Matches.UnitIsArtillerySupportable.match(u) && !defending)
        	return 1;
        return 0;
    }

    /*
     * populates support rule set, grouped in lists of non-stacking rules
     * populates rule use counter
     * handling defence here for simplicity
     */
    public static void getSupport(List<Unit> units, Set<List<UnitSupportAttachment>> support, IntegerMap<UnitSupportAttachment> supportLeft, GameData data, boolean defending) {
    	
    	Iterator<UnitSupportAttachment> iter = UnitSupportAttachment.get(data).iterator();
    	while(iter.hasNext()){
    		UnitSupportAttachment rule = iter.next();
    		if(rule.getPlayers().isEmpty())
    			continue;
    		if( defending && rule.getDefence() || 
    				!defending && rule.getOffence() )
    		{
    			CompositeMatchAnd<Unit> canSupport = new CompositeMatchAnd<Unit>(Matches.unitIsOfType((UnitType)rule.getAttatchedTo()),Matches.unitOwnedBy(rule.getPlayers()));  			
    			List<Unit> supporters = Match.getMatches(units, canSupport);
    			int numSupport = supporters.size();
    			if(rule.getImpArtTech())
    				numSupport += Match.getMatches(supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()).size();
    			String bonusType = rule.getBonusType();
    			supportLeft.put(rule, numSupport*rule.getNumber());
    			Iterator<List<UnitSupportAttachment>> iter2 = support.iterator(); 
    			List<UnitSupportAttachment> ruleType = null;
    			boolean found = false;
    			while( iter2.hasNext()){
    				ruleType = iter2.next();
    				if( ruleType.get(0).getBonusType().equals(bonusType) ){
    					found = true;
    					break;
    				}
    			}
    			if( !found ) {
    				ruleType = new ArrayList<UnitSupportAttachment>();
    				support.add(ruleType);
    			}
    			ruleType.add(rule);
    		}
    	}
    sortSupportRules(support);
}

    /*
     * get support bonus for individual unit
     * decrements the rule counter.
     */
    
    public static int getSupport (UnitType type, Set<List<UnitSupportAttachment>> support, IntegerMap<UnitSupportAttachment> supportLeft) {
    	
    	int strength = 0;
    	Iterator<List<UnitSupportAttachment>> iter = support.iterator();
    	while( iter.hasNext()) {
    		Iterator<UnitSupportAttachment> iter2 = iter.next().iterator();
    		while(iter2.hasNext()){
    			UnitSupportAttachment rule = iter2.next();
    			if( rule.getUnitTypes().contains(type) && supportLeft.getInt(rule) > 0) {
    				strength += rule.getBonus();
    				supportLeft.add(rule,-1);
    				break;
    			}
    		}
    	}
    	return strength;
    }
    
    public static void sortByStrength (List<Unit> units, final boolean defending){
    	
    	Comparator<Unit> comp = new Comparator<Unit>()
        {
            public int compare(Unit u1, Unit u2)
            {
            	Integer v1, v2;
            	if( defending ) {
            		v1 = new Integer(UnitAttachment.get(u1.getType()).getDefense(u1.getOwner()));
            		v2 = new Integer(UnitAttachment.get(u2.getType()).getDefense(u2.getOwner()));
            	}
            	else {
            		v1 = new Integer(UnitAttachment.get(u1.getType()).getAttack(u1.getOwner()));
            		v2 = new Integer(UnitAttachment.get(u2.getType()).getAttack(u2.getOwner()));
            	}
            	return v1.compareTo(v2);
            }
        };
        Collections.sort(units, comp);
    }
    
    private static void sortSupportRules (Set<List<UnitSupportAttachment>> support){
    	
    	
    	Comparator<UnitSupportAttachment> comp = new Comparator<UnitSupportAttachment>()
        {
            public int compare(UnitSupportAttachment u1, UnitSupportAttachment u2)
            {
            	Integer v1 = new Integer(Math.abs(u1.getBonus()));
            	Integer v2 = new Integer(Math.abs(u2.getBonus()));
            	return v2.compareTo(v1);	
            }
        };
        Iterator<List<UnitSupportAttachment>> iter = support.iterator(); 
		while( iter.hasNext()){
        Collections.sort(iter.next(), comp);
		}
    }
    /**
     * Roll dice for units per normal rules.
     */
    private static DiceRoll rollDiceNormal(List<Unit> unitsList, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {
        List<Unit> units = new ArrayList<Unit>(unitsList);
    	sortByStrength(units, defending);
    	boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);

        int artillerySupportAvailable = getArtillerySupportAvailable(units, defending, player);
        Set<List<UnitSupportAttachment>> supportRules = new HashSet<List<UnitSupportAttachment>>();
        IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
        getSupport(units,supportRules,supportLeft,data,defending);
        int rollCount = BattleCalculator.getRolls(units, player, defending, artillerySupportAvailable);
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(), 0);
        }
        int[] random;
       
        random = bridge.getRandom(data.getDiceSides(), rollCount, annotation);

        List<Die> dice = new ArrayList<Die>();
        
        Iterator<Unit> iter = units.iterator();

        int hitCount = 0;
        int diceIndex = 0;
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());
            int rolls = BattleCalculator.getRolls(current, player, defending, artillerySupportAvailable);

            //lhtr heavy bombers take best of n dice for both attack and defense
            if(rolls > 1 && lhtrBombers && ua.isStrategicBomber())
            {
                int strength;
                if(defending)
                    strength = ua.getDefense(current.getOwner());
                else
                    strength = ua.getAttack(current.getOwner());
                
                strength += getSupport(current.getType(), supportRules, supportLeft);
                strength = Math.min(Math.max(strength, 0), data.getDiceSides());
                
                int minIndex = 0;
                int min = data.getDiceSides();
                for( int i = 0; i < rolls; i++){
                	if(random[diceIndex+i] < min) {
                		min = random[diceIndex+i];
                		minIndex = i;
                	}
                }
                boolean hit = strength > random[diceIndex+minIndex];
                dice.add(new Die(random[diceIndex+minIndex], strength, hit ? DieType.HIT : DieType.MISS));
                for( int i = 0; i < rolls; i++){
                	if( i != minIndex)
                		dice.add(new Die(random[diceIndex+i], strength, DieType.IGNORED));
                }
                if(hit)
                    hitCount++;
                
                diceIndex += rolls; 
            }
            else
            {
                for (int i = 0; i < rolls; i++)
                {
                    int strength;
                    if (defending)
                    {
                    	strength = ua.getDefense(current.getOwner());
                    	if (isFirstTurnLimitedRoll(player))
                    	{
                    		strength = Math.min(1, strength);
                    	}
                    	else 
                    		strength += getSupport(current.getType(), supportRules, supportLeft);
                    }
                    else
                    {
                        strength = ua.getAttack(current.getOwner());
                        if (ua.getIsMarine() && battle.isAmphibious())
                        {
                            Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                            if(landUnits.contains(current))
                                ++strength;
                        } 
                        //get bombarding unit's strength
                        if (ua.isSea() && battle.isAmphibious())
                        	strength = ua.getBombard(current.getOwner());  
                        strength += getSupport(current.getType(), supportRules, supportLeft);
                    }
                    strength = Math.min(Math.max(strength, 0), data.getDiceSides());
                    boolean hit = strength > random[diceIndex];
                    dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
    
                    if (hit)
                        hitCount++;
                    diceIndex++;
                }
            }
        }

        DiceRoll rVal = new DiceRoll(dice, hitCount);
        bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
        return rVal;
    }
    
    /**
     * Roll dice for units per normal rules.
     */
    /*
    private static DiceRoll rollDiceNormalold(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {
        
    	boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);

        int artillerySupportAvailable = getArtillerySupportAvailable(units, defending, player);
        //int rollCount = BattleCalculator.getRolls(units, player, defending);
        int rollCount = BattleCalculator.getRolls(units, player, defending, artillerySupportAvailable);
        
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(), 0);
        }

        int[] random;
       
        random = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);

        List<Die> dice = new ArrayList<Die>();
        
        Iterator<Unit> iter = units.iterator();

        int hitCount = 0;
        int diceIndex = 0;
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());
            
            int rolls = BattleCalculator.getRolls(current, player, defending, artillerySupportAvailable);

            //lhtr heavy bombers take best of n dice for both attack and defense
            if(rolls > 1 && lhtrBombers && ua.isStrategicBomber())
            {
                int strength;
                if(defending)
                    strength = ua.getDefense(current.getOwner());
                else
                    strength = ua.getAttack(current.getOwner());
                
                //it is easier to assume two for now
                //if it is something else, the code below gets a
                //bit more general
                if(rolls != 2)
                    throw new IllegalStateException("Only expecting 2 dice for lhtr heavy bombers");
                

                
                if(random[diceIndex] <= random[diceIndex+1])
                {
                    boolean hit = strength > random[diceIndex];
                    dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
                    dice.add(new Die(random[diceIndex+1], strength, DieType.IGNORED));
                    if(hit)
                        hitCount++;
                }
                else
                {
                    boolean hit = strength >= random[diceIndex + 1];
                    dice.add(new Die(random[diceIndex],  strength, DieType.IGNORED));
                    dice.add(new Die(random[diceIndex+1], strength, hit ? DieType.HIT : DieType.MISS));
                    if(hit)
                        hitCount++;

                }
                    
                //2 dice
                diceIndex++;
                diceIndex++;
                
            }
            else
            {
                for (int i = 0; i < rolls; i++)
                {
                    int strength;
                    if (defending)
                        //If it's a sneak attack, defenders roll at a 1
                    {
                        strength = ua.getDefense(current.getOwner());
                        if (isFirstTurnLimitedRoll(player))
                        {
                            strength = Math.min(1, strength);
                        }
                    }
                    else
                    {
                        strength = ua.getAttack(current.getOwner());
                        if (ua.isArtillerySupportable() && artillerySupportAvailable > 0 && strength < Constants.MAX_DICE)
                        {
                        	//TODO probably need a map here to properly add artilleryBonus
                            strength++;
                            artillerySupportAvailable--;
                        } 
                        if (ua.getIsMarine() && battle.isAmphibious())
                        {
                            Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                            if(landUnits.contains(current))
                                ++strength;
                        } 
                        //get bombarding unit's strength
                        if (ua.isSea() && battle.isAmphibious())
                        	strength = ua.getBombard(current.getOwner());                        	
                    }
    
                    boolean hit = strength > random[diceIndex];
                    dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
    
                    if (hit)
                        hitCount++;
                    diceIndex++;
                }
            }
        }

        DiceRoll rVal = new DiceRoll(dice, hitCount);
        bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
        return rVal;
    }
    */
    public static boolean isFirstTurnLimitedRoll(PlayerID player) 
    {
    	//If player is null, Round > 1, or player has negate rule set: return false
        if(player.isNull() || player.getData().getSequence().getRound() != 1 || isNegateDominatingFirstRoundAttack(player) )
            return false;
        
        return isDominatingFirstRoundAttack(player.getData().getSequence().getStep().getPlayerID());        
    }
    
    private static boolean isDominatingFirstRoundAttack(PlayerID player)    
    {
    	if(player == null) {
    		return false;
    	}
        RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        if(ra == null)
        	return false;
        return ra.getDominatingFirstRoundAttack();
    }

    private static boolean isNegateDominatingFirstRoundAttack(PlayerID player)    
    {
        RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        if(ra == null)
        	return false;
        return ra.getNegateDominatingFirstRoundAttack();
    }

    
    
    
    public static boolean isAmphibious(Collection<Unit> m_units)
    {
    	Iterator<Unit> unitIter = m_units.iterator();    	
    	while (unitIter.hasNext())
    	{
    		TripleAUnit checkedUnit = (TripleAUnit) unitIter.next();    		
    		if (checkedUnit.getWasAmphibious())
    		{
    			return true;
    		}
    	}
    	return false;    	
    }
    
    //Determine if it's an assaulting Marine so the attach value can be increased
    public static boolean isAmphibiousMarine(UnitAttachment ua, GameData data)
    {
    	BattleTracker bt;
    	data.acquireReadLock();
    	try
    	{
    	    bt = DelegateFinder.battleDelegate(data).getBattleTracker();
    	} finally {
    	    data.releaseReadLock();
    	}
    	
    
    	Collection<Territory> m_pendingBattles = bt.getPendingBattleSites(false);
     	Iterator<Territory> territories = m_pendingBattles.iterator();
    
    	while (territories.hasNext())
    	{
    		Territory terr = (Territory) territories.next();
    		Battle battle = bt.getPendingBattle(terr, false);
         	if ( battle != null && battle.isAmphibious() && ua.getIsMarine())
         		return true;
    	}
    		return false;
    }

    private static boolean isImprovedArtillerySupport(PlayerID player)
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta == null)
        	return false;
        return ta.hasImprovedArtillerySupport();     
    }


    /**
     * @param units
     * @param player
     * @param battle
     * @return
     */
    public static String getAnnotation(List<Unit> units, PlayerID player, Battle battle)
    {
        StringBuilder buffer = new StringBuilder(80);
        buffer.append(player.getName()).append(" roll dice for ").append(MyFormatter.unitsToTextNoOwner(units));
        if (battle != null)
            buffer.append(" in ").append(battle.getTerritory().getName()).append(", round ").append((battle.getBattleRound() + 1));
        return buffer.toString();

    }

    /**
     * 
     * @param dice
     *            int[] the dice, 0 based
     * @param hits
     *            int - the number of hits
     * @param rollAt
     *            int - what we roll at, [0,Constants.MAX_DICE]
     * @param hitOnlyIfEquals
     *            boolean - do we get a hit only if we are equals, or do we hit
     *            when we are equal or less than for example a 5 is a hit when
     *            rolling at 6 for equal and less than, but is not for equals
     */
    public DiceRoll(int[] dice, int hits, int rollAt, boolean hitOnlyIfEquals)
    {
        m_hits = hits;
        m_rolls = new ArrayList<Die>(dice.length);
        
        for(int i =0; i < dice.length; i++)
        {
            boolean hit;
            if(hitOnlyIfEquals)
                hit = (rollAt == dice[i]);
            else
                hit = dice[i] <= rollAt;
           
            m_rolls.add(new Die(dice[i], rollAt, hit ? DieType.HIT : DieType.MISS));
        }
    }

    //only for externalizable
    public DiceRoll()
    {
        
    }
    
    private DiceRoll(List<Die> dice, int hits)
    {
        m_rolls = new ArrayList<Die>(dice);
        m_hits = hits;
    }

    public int getHits()
    {
        return m_hits;
    }

    /**
     * @param rollAt
     *            the strength of the roll, eg infantry roll at 2, expecting a
     *            number in [1,6]
     * @return in int[] which shouldnt be modifed, the int[] is 0 based, ie
     *         0..MAX_DICE
     */
    public List<Die> getRolls(int rollAt)
    {
        List<Die> rVal = new ArrayList<Die>();
        for(Die die : m_rolls)
        {
            if(die.getRolledAt() == rollAt)
                rVal.add(die);
        }
        return rVal;
    }
    
    public int size() {
    	return m_rolls.size();
    }
    
    public Die getDie(int index) {
    	return m_rolls.get(index);
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        int[] dice = new int[m_rolls.size()];
        for(int i =0; i < m_rolls.size(); i++)
        {
            dice[i] = m_rolls.get(i).getCompressedValue();
        }
        out.writeObject(dice);
        out.writeInt(m_hits);
        
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int[] dice = (int[]) in.readObject();
        m_rolls = new ArrayList<Die>(dice.length);
        for(int i=0; i < dice.length; i++)
        {
            m_rolls.add(Die.getFromWriteValue(dice[i]));
        }
        
        m_hits = in.readInt();
        
    }
    
    public String toString() {
    	return "DiceRoll dice:" + m_rolls + " hits:" + m_hits;
    }
}
