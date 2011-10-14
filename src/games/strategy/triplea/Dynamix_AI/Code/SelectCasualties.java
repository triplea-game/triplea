/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI.Code;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class SelectCasualties
{
    private static boolean useDefaultSelectionThisTime = false;
    public static void NotifyCasualtySelectionError(String error)
    {
        if(error.equals("Wrong number of casualties selected"))
        {
            DUtils.Log(Level.FINER, "  Wrong number of casualties selected for current battle, so attempting to use default casualties");
            useDefaultSelectionThisTime = true;
        }
    }
    public static CasualtyDetails selectCasualties(Dynamix_AI ai, GameData data, Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, CasualtyList defaultCasualties, GUID battleID)
    {
        ai.pause();
        HashSet<Unit> damaged = new HashSet<Unit>();
        HashSet<Unit> destroyed = new HashSet<Unit>();
        if (useDefaultSelectionThisTime)
        {
            useDefaultSelectionThisTime = false;
            damaged.addAll(defaultCasualties.getDamaged());
            destroyed.addAll(defaultCasualties.getKilled());
            
            /*for (Unit unit : defaultCasualties)
            {
                boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
                //If it appears in casualty list once, it's damaged, if twice, it's damaged and additionally destroyed
                if (unit.getHits() == 0 && twoHit && !damaged.contains(unit))
                    damaged.add(unit);
                else
                    destroyed.add(unit);
            }*/
        }
        else
        {
            while (damaged.size() + destroyed.size() < count)
            {
                Unit untouchedTwoHitUnit = null;
                for (Unit unit : selectFrom)
                {
                    UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
                    if (ua.isTwoHit() && unit.getHits() == 0 && !damaged.contains(unit)) //If this is an undamaged, un-selected as casualty, two hit unit
                    {
                        untouchedTwoHitUnit = unit;
                        break;
                    }
                }
                if (untouchedTwoHitUnit != null) //We try to damage untouched two hit units first, if there are any
                {
                    damaged.add(untouchedTwoHitUnit);
                    continue;
                }

                Unit highestScoringUnit = null;
                float highestScore = Integer.MIN_VALUE;
                for (Unit unit : selectFrom) //Problem with calcing for the best unit to select as a casualties is that the battle calculator needs to call this very method to calculate the battle, resulting in a never ending loop!
                {
                    UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
                    TripleAUnit ta = TripleAUnit.get(unit);

                    if (destroyed.contains(unit))
                        continue;

                    float score = 0;

                    score -= DUtils.GetTUVOfUnit(unit, GlobalCenter.GetPUResource());

                    score -= DUtils.GetValueOfUnits(Collections.singleton(unit)); //Valuable units should get killed later

                    if (dependents.containsKey(unit)) //If we have units depending on this unit, knock down the score by 1000. (Such as a transport with units on it)
                        score -= 1000;

                    if (ua.isTwoHit() && (unit.getHits() > 0 || damaged.contains(unit))) //Since two hit units can get repaired, we don't want them destroyed unless necessary, so knock the score down by 100.
                        score -= 100;

                    if (score > highestScore)
                    {
                        highestScore = score;
                        highestScoringUnit = unit;
                    }
                }

                if (highestScoringUnit != null)
                {
                    destroyed.add(highestScoringUnit);
                    continue;
                }
            }
        }

        DUtils.Log(Level.FINER, "  Casualties selected. Damaged: {0}, Destroyed {1}", damaged, destroyed);

        CasualtyDetails m2 = new CasualtyDetails(DUtils.ToList(destroyed), DUtils.ToList(damaged), false);
        return m2;
    }
}
