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

import java.io.*;
import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.formatter.*;
import games.strategy.util.Match;
import games.strategy.triplea.TripleAUnit;

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

    public static DiceRoll rollAA(int numberOfAirUnits, IDelegateBridge bridge, Territory location, GameData data)
    {
        int hits = 0;
        int[] dice = new int[0];
        List<Die> sortedDice = new ArrayList<Die>();
        boolean isEditMode = EditDelegate.getEditMode(data);

        if (data.getProperties().get(Constants.LOW_LUCK, false))
        {
            // Low luck rolling
            hits = numberOfAirUnits / Constants.MAX_DICE;
            int hitsFractional = numberOfAirUnits % Constants.MAX_DICE;

            if (hitsFractional > 0)
            {
                String annotation = "Roll AA guns in " + location.getName();
                if (isEditMode)
                {
                    ITripleaPlayer player = (ITripleaPlayer)bridge.getRemote();
                    dice = player.selectFixedDice(1, 1, true, annotation);
                }
                else
                    dice = bridge.getRandom(Constants.MAX_DICE, 1, annotation);
                boolean hit = hitsFractional > dice[0];
                Die die = new Die(dice[0], hitsFractional, hit ? DieType.HIT : DieType.MISS);
                
                sortedDice.add(die);
                if (hit)
                {
                    hits++;
                }
            }
        } 
        else
        {
            
            // Normal rolling
            String annotation = "Roll AA guns in " + location.getName();
            if (isEditMode)
            {
                ITripleaPlayer player = (ITripleaPlayer)bridge.getRemote();
                dice = player.selectFixedDice(numberOfAirUnits, 1, true, annotation);
            }
            else
                dice = bridge.getRandom(Constants.MAX_DICE, numberOfAirUnits, annotation);
            for (int i = 0; i < dice.length; i++)
            {
                boolean hit = dice[i] == 0;
                sortedDice.add(new Die(dice[i], 1, hit ? DieType.HIT : DieType.MISS));
                if (hit)
                    hits++;
            }
        }

        DiceRoll roll = new DiceRoll(sortedDice, hits);
        String annotation = "AA guns fire in " + location + " : " + MyFormatter.asDice(dice);
        bridge.getHistoryWriter().addChildToEvent(annotation, roll);
        return roll;
    }

    /**
     * Roll dice for units.
     * @param annotation TODO
     *  
     */
    public static DiceRoll rollDice(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {
        // Decide whether to use low luck rules or normal rules.
        if (data.getProperties().get(Constants.LOW_LUCK, false))
        {
            return rollDiceLowLuck(units, defending, player, bridge, data, battle, annotation);
        } else
        {
            return rollDiceNormal(units, defending, player, bridge, data, battle, annotation);
        }
    }

    /**
     * Roll dice for units using low luck rules. Low luck rules based on rules
     * in DAAK.
     */
    private static DiceRoll rollDiceLowLuck(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {


        int rollCount = BattleCalculator.getRolls(units, player, defending);
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(0), 0);
        }

        int artillerySupportAvailable = 0;
        if (!defending)
            artillerySupportAvailable = Match.countMatches(units, Matches.UnitIsArtillery);

        Iterator iter = units.iterator();

        int power = 0;
        int hitCount = 0;

        // We iterate through the units to find the total strength of the units
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());
            int rolls = defending ? 1 : ua.getAttackRolls(player);
            for (int i = 0; i < rolls; i++)
            {
                int strength;
                if (defending)
                    strength = ua.getDefense(current.getOwner());
                else
                {
                    strength = ua.getAttack(current.getOwner());
                    if (ua.isArtillerySupportable() && artillerySupportAvailable > 0)
                    {
                        strength++;
                        artillerySupportAvailable--;
                    }
                    if (ua.getIsMarine() && battle.isAmphibious())
                    {
                        Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                        if(!landUnits.contains(current))
                            ++strength;
                    } 
                }
               
                power += strength;
            }
        }

        // Get number of hits
        hitCount = power / Constants.MAX_DICE;

        int[] random = new int[0];

        List<Die> dice = new ArrayList<Die>();
        // We need to roll dice for the fractional part of the dice.
        power = power % Constants.MAX_DICE;
        if (power != 0)
        {
            boolean isEditMode = EditDelegate.getEditMode(data);
            if (isEditMode)
            {
                ITripleaPlayer tripleAplayer = (ITripleaPlayer)bridge.getRemote();
                random = tripleAplayer.selectFixedDice(1, power, false, annotation);
            }
            else
                random = bridge.getRandom(Constants.MAX_DICE, 1, annotation);
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
     * Roll dice for units per normal rules.
     */
    private static DiceRoll rollDiceNormal(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
    {
        

        boolean lhtrBombers = bridge.getPlayerID().getData().getProperties().get(Constants.LHTR_HEAVY_BOMBERS, false);
        
        int rollCount = BattleCalculator.getRolls(units, player, defending);
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(), 0);
        }

        int artillerySupportAvailable = 0;
        if (!defending)
            artillerySupportAvailable = Match.countMatches(units, Matches.UnitIsArtillery);

        int[] random;
        boolean isEditMode = EditDelegate.getEditMode(data);
        if (isEditMode)
        {
            ITripleaPlayer tripleAplayer = (ITripleaPlayer)bridge.getRemote();
            random = tripleAplayer.selectFixedDice(rollCount, 0, true, annotation);
        }
        else
            random = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);

        List<Die> dice = new ArrayList<Die>();
        
        Iterator iter = units.iterator();

        int hitCount = 0;
        int diceIndex = 0;
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());

            int rolls = BattleCalculator.getRolls(current, player, defending);

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
                        //If it's Pacific_Edition and Japan's turn one, all but Chinese defend at a 1
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
                        if (ua.isArtillerySupportable() && artillerySupportAvailable > 0)
                        {
                            strength++;
                            artillerySupportAvailable--;
                        }
                        if (ua.getIsMarine() && battle.isAmphibious())
                        {
                            Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                            if(landUnits.contains(current))
                                ++strength;
                        } 
                        if (ua.getIsDestroyer() && battle.isAmphibious())
                        	strength--;
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
    
    public static boolean isFirstTurnLimitedRoll(PlayerID player) 
    {
        if(player.isNull()) {
            return false;
        }
        
        return player.getData().getProperties().get(Constants.PACIFIC_EDITION, false) 
        && player.getData().getSequence().getRound() == 1 
        && player.getData().getSequence().getStep().getName().equals("japaneseBattle") 
        && !player.equals(player.getData().getPlayerList().getPlayerID(Constants.CHINESE));
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
    	BattleTracker bt = new BattleTracker();
     	bt = DelegateFinder.battleDelegate(data).getBattleTracker();
    
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

}
