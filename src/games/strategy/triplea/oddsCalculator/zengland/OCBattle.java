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
package games.strategy.triplea.oddsCalculator.zengland;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class OCBattle
{
	public static final int TAKEN = 1;
	public static final String TAKENSTRING = "Taken";
	public static final int DEFENDED = 2;
	public static final String DEFENDEDSTRING = "Defended";
	public static final int CLEARED = 3;
	public static final String CLEAREDSTRING = "Cleared";
	public static final int INDECISIVE = 4;
	public static final String INDECISIVESTRING = "Indecisive";
	public static final int CLEAREDAIR = 5;
	public static final String CLEAREDAIRSTRING = "Cleared with air";
	private Vector<UnitGroup> attackers;
	private Vector<UnitGroup> defenders;
	private int rounds;
	private boolean preserveLand;
	private boolean aaPresent;
	private boolean landBattle;
	private Vector<String> attOOL = null;
	private Vector<String> defOOL = null;
	private int resultStatus = OCBattle.INDECISIVE;
	private String resultStatusString = OCBattle.INDECISIVESTRING;
	private boolean rollAntiAirSep;
	private boolean isAmphib;
	// results section
	private int battles = 0;
	private float controlPercent = 0.00f;
	private int controleds = 0;
	private float airWinPercent = 0.00f;
	private int airWins = 0;
	private float clearedPercent = 0.00f;
	private int cleareds = 0;
	private float indecisivePercent = 0.00f;
	private int indecisives = 0;
	private float lossPercent = 0.00f;
	private int losses = 0;
	private Vector<UnitGroup> locAttackers = new Vector<UnitGroup>();
	private Vector<UnitGroup> locDefenders = new Vector<UnitGroup>();
	private long remAtts = 0;
	private int avgRemAtts = 0;
	private long remDefs = 0;
	private int avgRemDefs = 0;
	
	public float getAirWinPercent()
	{
		return airWinPercent;
	}
	
	public int getAirWins()
	{
		return airWins;
	}
	
	public int getBattles()
	{
		return battles;
	}
	
	public float getClearedPercent()
	{
		return clearedPercent;
	}
	
	public int getCleareds()
	{
		return cleareds;
	}
	
	public int getControleds()
	{
		return controleds;
	}
	
	public float getControlPercent()
	{
		return controlPercent;
	}
	
	public float getIndecisivePercent()
	{
		return indecisivePercent;
	}
	
	public int getIndecisives()
	{
		return indecisives;
	}
	
	public int getLosses()
	{
		return losses;
	}
	
	public float getLossPercent()
	{
		return lossPercent;
	}
	
	private boolean isWW2V2(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V2(data);
	}
	
	public OCBattle(final Vector<UnitGroup> attackers, final Vector<UnitGroup> defenders, final int rounds, final boolean preserveLand, final boolean aaPresent, final boolean landBattle,
				final boolean rollAntiAirSep, final boolean isAmphib, final Vector<String> ool)
	{
		super();
		setAttackers(attackers);
		setDefenders(defenders);
		this.rounds = rounds;
		this.preserveLand = preserveLand;
		this.aaPresent = aaPresent;
		this.landBattle = landBattle;
		this.rollAntiAirSep = rollAntiAirSep;
		this.attOOL = ool;
		this.defOOL = ool;
		setAmphib(isAmphib);
	}
	
	public OCBattle(final Territory territory, final GameData m_data)
	{
		final Vector<UnitGroup> terrAttackers = new Vector<UnitGroup>(0);
		final Vector<UnitGroup> terrDefenders = new Vector<UnitGroup>(0);
		final int terrRounds = 0;
		final boolean terrPreserveLand = true;
		boolean terrAAPresent = false;
		boolean terrLandBattle = true;
		if (territory.isWater())
			terrLandBattle = false;
		boolean terrRollAntiAirSep = false;
		// need to detect whether to roll AA shots seperately for fighters and bombers
		final boolean isAmphib = false;
		// need to detect whether this is an amphib battle
		// Vector terOOL = null;
		// get OOL from game data
		terrRollAntiAirSep = isWW2V2(m_data);
		final Set<UnitCategory> units = UnitSeperator.categorize(territory.getUnits().getUnits());
		final Iterator<UnitCategory> iter = units.iterator();
		PlayerID currentPlayer = null;
		while (iter.hasNext())
		{
			final UnitCategory item = iter.next();
			if (item.getOwner() != currentPlayer)
			{
				currentPlayer = item.getOwner();
			}
			final List<games.strategy.engine.data.Unit> unitList = item.getUnits();
			final int numUnits = item.getUnits().size();
			final Iterator<games.strategy.engine.data.Unit> unitIterator = unitList.iterator();
			final int unitCost = 3;
			int attackValue = 1;
			int defendValue = 2;
			int moveValue = 1;
			String name = StandardUnits.InfName;
			boolean noRetal = false;
			int unitType = OCUnit.LANDUNIT;
			boolean supportShot = false;
			boolean canHitAir = true;
			int maxHits = 1;
			int maxRolls = 1;
			int maxHp = 1;
			boolean blocksNoRetHit = false;
			boolean boostsInfAtt = false;
			boolean boostAmphib = false;
			UnitAttachment ua = null;
			while (unitIterator.hasNext())
			{
				final games.strategy.engine.data.Unit current = unitIterator.next();
				ua = UnitAttachment.get(current.getType());
				attackValue = ua.getAttack(currentPlayer);
				defendValue = ua.getDefense(currentPlayer);
				moveValue = ua.getMovement(currentPlayer);
				name = current.getType().getName();
				noRetal = ua.getIsSub();
				if (ua.getIsAir())
				{
					unitType = OCUnit.AIRUNIT;
				}
				else if (ua.getIsAAforCombatOnly())
				{
					unitType = OCUnit.AAUNIT;
				}
				else if (ua.getIsSea())
				{
					unitType = OCUnit.SEAUNIT;
				}
				else
				{
					unitType = OCUnit.LANDUNIT;
				}
				supportShot = ua.getCanBombard(currentPlayer);
				canHitAir = !ua.getIsSub();
				maxHits = ua.getAttackRolls(currentPlayer);
				maxRolls = 1; // TODO: Determine if this is an LHTR heavy bomber
				if (ua.getIsTwoHit())
					maxHp = 2;
				else
					maxHp = 1;
				blocksNoRetHit = ua.getIsDestroyer() && isWW2V2(m_data);
				boostsInfAtt = ua.getArtillery();
				boostAmphib = ua.getIsMarine();
			}
			if (ua.getIsAAforCombatOnly())
			{
				terrAAPresent = true;
			}
			else
			{
				final games.strategy.triplea.oddsCalculator.zengland.OCUnit currUnit = new games.strategy.triplea.oddsCalculator.zengland.OCUnit(unitCost, attackValue, defendValue, moveValue, name,
							noRetal, unitType, supportShot, canHitAir, maxHits, maxRolls, maxHp, blocksNoRetHit, boostsInfAtt, boostAmphib);
				final UnitGroup currUnitGroup = new UnitGroup(currUnit, numUnits);
				final PlayerID playerID = m_data.getSequence().getStep().getPlayerID();
				System.out.println(item);
				System.out.println(playerID);
				if (playerID != null && playerID.equals(item.getOwner()))
				// this check is not correct, it should be owner or it's allies
				{
					terrAttackers.add(currUnitGroup);
				}
				else
				{
					terrDefenders.add(currUnitGroup);
				}
			}
		}
		setAttackers(terrAttackers);
		setDefenders(terrDefenders);
		this.rounds = terrRounds;
		this.preserveLand = terrPreserveLand;
		this.aaPresent = terrAAPresent;
		this.landBattle = terrLandBattle;
		this.rollAntiAirSep = terrRollAntiAirSep;
		this.attOOL = null;
		this.defOOL = null;
		setAmphib(isAmphib);
	}
	
	public boolean isAaPresent()
	{
		return aaPresent;
	}
	
	public void setAaPresent(final boolean aaPresent)
	{
		this.aaPresent = aaPresent;
	}
	
	public Vector<UnitGroup> getAttackers()
	{
		return attackers;
	}
	
	public void setAttackers(final Vector<UnitGroup> attackers)
	{
		this.attackers = attackers;
		// remove empty unitGroups
		final int size = this.attackers.size();
		for (int i = size - 1; i >= 0; i--)
		{
			final int curSize = this.attackers.elementAt(i).getNumUnits();
			if (curSize <= 0)
			{
				this.attackers.removeElementAt(i);
			}
		}
	}
	
	public Vector<UnitGroup> getDefenders()
	{
		return defenders;
	}
	
	public void setDefenders(final Vector<UnitGroup> defenders)
	{
		this.defenders = defenders;
		final int size = this.defenders.size();
		for (int i = size - 1; i >= 0; i--)
		{
			final int curSize = this.defenders.elementAt(i).getNumUnits();
			if (curSize <= 0)
			{
				this.defenders.removeElementAt(i);
			}
		}
	}
	
	public boolean isPreserveLand()
	{
		return preserveLand;
	}
	
	public void setPreserveLand(final boolean preserveLand)
	{
		this.preserveLand = preserveLand;
	}
	
	public int getRounds()
	{
		return rounds;
	}
	
	public void setRounds(final int rounds)
	{
		this.rounds = rounds;
	}
	
	public boolean isLandBattle()
	{
		return landBattle;
	}
	
	public void setLandBattle(final boolean landBattle)
	{
		this.landBattle = landBattle;
	}
	
	private void updateStats()
	{
		battles++;
		if (getResultStatus() == OCBattle.CLEARED)
		{
			cleareds++;
		}
		else if (getResultStatus() == OCBattle.TAKEN)
		{
			controleds++;
		}
		else if (getResultStatus() == OCBattle.DEFENDED)
		{
			losses++;
		}
		else if (getResultStatus() == OCBattle.INDECISIVE)
		{
			indecisives++;
		}
		else if (getResultStatus() == OCBattle.CLEAREDAIR)
		{
			airWins++;
		}
		controlPercent = ((float) controleds / (float) battles) * 100;
		airWinPercent = ((float) airWins / (float) battles) * 100;
		clearedPercent = ((float) cleareds / (float) battles) * 100;
		indecisivePercent = ((float) indecisives / (float) battles) * 100;
		lossPercent = ((float) losses / (float) battles) * 100;
		remAtts += getNumberOfUnits(locAttackers);
		remDefs += getNumberOfUnits(locDefenders);
		avgRemAtts = (int) remAtts / battles;
		avgRemDefs = (int) remDefs / battles;
	}
	
	public void rollBattles(final int numBattlesToRoll)
	{
		for (int i = 0; i < numBattlesToRoll; i++)
		{
			rollBattle();
			updateStats();
		}
	}
	
	public void rollBattle()
	{
		locAttackers = cloneVector(attackers);
		locDefenders = cloneVector(defenders);
		// Roll AA gun if applicable
		if (!rollAntiAirSep)
		{
			final int numberOfAir = getNumberOfAir(locAttackers);
			int numAAhits = 0;
			if (aaPresent && landBattle && numberOfAir > 0)
			{
				final OCUnit aa = OCUnit.newAA();
				for (int i = 0; i < numberOfAir; i++)
				{
					final int hitVal = aa.rollDefend();
					if (hitVal <= aa.getDefendValue())
					{
						numAAhits++;
					}
				}
				if (numAAhits > 0)
				{
					// remove units hit by AAGun
					for (int i = 0; i < numAAhits; i++)
					{
						boolean removed = removeUnit(StandardUnits.BmbName, locAttackers);
						if (!removed)
						{
							removed = removeUnit(StandardUnits.FtrName, locAttackers);
						}
						removed = false;
					}
				}
			}
		}
		else
		// Roll anti-air separately for fighters and bombers
		{
			final int numFtrs = getNumberOfFighters(locAttackers);
			if (aaPresent && landBattle && numFtrs > 0)
			{
				final OCUnit aa = OCUnit.newAA();
				for (int i = 0; i < numFtrs; i++)
				{
					final int hitVal = aa.rollDefend();
					if (hitVal <= aa.getDefendValue())
					{
						removeUnit(StandardUnits.FtrName, locAttackers);
					}
				}
			}
			final int numBmbs = getNumberOfBombers(locAttackers);
			if (aaPresent && landBattle && numBmbs > 0)
			{
				final OCUnit aa = OCUnit.newAA();
				for (int i = 0; i < numBmbs; i++)
				{
					final int hitVal = aa.rollDefend();
					if (hitVal <= aa.getDefendValue())
					{
						removeUnit(StandardUnits.BmbName, locAttackers);
					}
				}
			}
		}
		// Roll Support Shots if applicable
		final Integer supportUnits[] = getSupportUnits(locAttackers);
		if (supportUnits != null && supportUnits.length > 0)
		{
			int supportHits = 0;
			for (int i = 0; i < supportUnits.length; i++)
			{
				final UnitGroup curG = locAttackers.elementAt(supportUnits[i].intValue());
				supportHits += curG.rollUnitsAttack();
				locAttackers.removeElementAt(supportUnits[i].intValue());
			}
			// remove units hit by support shots
			removeUnits(supportHits, locDefenders, false);
		}
		// Remove ineligible units
		removeIncorrectUnits(locAttackers);
		removeIncorrectUnits(locDefenders);
		// Roll rounds
		int i = 1; // start with round 1
		while (hasUnits(locAttackers) && hasUnits(locDefenders) && (i <= rounds || rounds == 0))
		{
			rollRound();
			i++;
		}
		// Set result status
		setResultStatus();
	}
	
	private Vector<UnitGroup> cloneVector(final Vector<UnitGroup> units)
	{
		final Vector<UnitGroup> copy = new Vector<UnitGroup>();
		final int unitSize = units.size();
		for (int i = 0; i < unitSize; i++)
		{
			copy.addElement((UnitGroup) units.elementAt(i).clone());
		}
		return copy;
	}
	
	private void removeIncorrectUnits(final Vector<UnitGroup> units)
	{
		final int size = units.size();
		for (int i = size - 1; i >= 0; i--)
		{
			final UnitGroup ug = units.elementAt(i);
			final OCUnit u = ug.getUnit();
			if (landBattle && u.getUnitType() == OCUnit.SEAUNIT)
			{
				units.removeElementAt(i);
			}
			else if (!landBattle && u.getUnitType() == OCUnit.LANDUNIT)
			{
				units.removeElementAt(i);
			}
		}
	}
	
	private void setResultStatus()
	{
		final boolean hasA = hasUnits(locAttackers);
		final boolean hasD = hasUnits(locDefenders);
		if (!hasD && !hasA)
		{
			resultStatus = OCBattle.CLEARED;
			resultStatusString = OCBattle.CLEAREDSTRING;
		}
		else if (hasD && hasA)
		{
			resultStatus = OCBattle.INDECISIVE;
			resultStatusString = OCBattle.INDECISIVESTRING;
		}
		else if (hasD)
		{
			resultStatus = OCBattle.DEFENDED;
			resultStatusString = OCBattle.DEFENDEDSTRING;
		}
		else if (hasA && allAir(attackers))
		{
			resultStatus = OCBattle.CLEAREDAIR;
			resultStatusString = OCBattle.CLEAREDAIRSTRING;
		}
		else if (hasA)
		{
			resultStatus = OCBattle.TAKEN;
			resultStatusString = OCBattle.TAKENSTRING;
		}
	}
	
	private boolean hasUnits(final Vector<UnitGroup> units)
	{
		final boolean hasU = false;
		final int size = units.size();
		for (int i = 0; i < size; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getNumUnits() > 0)
				return true;
		}
		return hasU;
	}
	
	private boolean allAir(final Vector<UnitGroup> units)
	{
		final boolean allAir = true;
		final int size = units.size();
		for (int i = 0; i < size; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			final OCUnit u = ug.getUnit();
			final int numUnits = ug.getNumUnits();
			if (u.getUnitType() != OCUnit.AIRUNIT && numUnits > 0)
			{
				return false;
			}
		}
		return allAir;
	}
	
	private void removeUnits(final int hits, final Vector<UnitGroup> units, final boolean attackers)
	{
		for (int i = 0; i < hits && units.size() > 0; i++)
		{
			removeUnit(units, attackers);
		}
	}
	
	private void removeUnit(final Vector<UnitGroup> units, final boolean attackers)
	{
		// hit multi-hit units first (eg 2 hit battleships)
		final UnitGroup ug = extraHp(units);
		if (ug != null)
		{
			ug.removeExtraHp();
			return;
		}
		Vector<String> localOOL = null;
		if (attackers)
			localOOL = attOOL;
		else
			localOOL = defOOL;
		if (localOOL != null)
		{
			if (getNumberOfLand(units) == 1 && this.preserveLand)
			{
				final UnitGroup last = getLastLand(units);
				localOOL.remove(last.getUnit().getName());
				localOOL.addElement(last.getUnit().getName());
			}
			for (int i = 0; i < localOOL.size(); i++)
			{
				final String curRem = localOOL.elementAt(i);
				final boolean removed = removeUnit(curRem, units);
				if (removed)
					return;
			}
		}
		else
		// no OOL defined, go by lowest attack/defense value
		{
			final UnitGroup lowest = getLowestUnitGroup(units, attackers);
			final boolean removed = removeUnit(lowest.getUnit().getName(), units);
			if (removed)
				return;
		}
	}
	
	private UnitGroup getLowestUnitGroup(final Vector<UnitGroup> units, final boolean attackers)
	{
		UnitGroup current = null;
		final int unitSize = units.size();
		int lowest = 6;
		for (int i = 0; i < unitSize; i++)
		{
			if (attackers)
			{
				if (units.elementAt(i).getUnit().getAttackValue() < lowest)
				{
					if (getNumberOfLand(units) == 1 && this.preserveLand && getNumberOfUnits(units) != 1 && units.elementAt(i).getUnit().getUnitType() == OCUnit.LANDUNIT)
					{
						continue;
					}
					current = units.elementAt(i);
					lowest = units.elementAt(i).getUnit().getAttackValue();
				}
			}
			else
			{
				if (units.elementAt(i).getUnit().getDefendValue() < lowest)
				{
					current = units.elementAt(i);
					lowest = units.elementAt(i).getUnit().getDefendValue();
				}
			}
		}
		return current;
	}
	
	private UnitGroup getLastLand(final Vector<UnitGroup> units)
	{
		UnitGroup lastLand = null;
		final int vectorSize = units.size();
		for (int i = 0; i < vectorSize; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().getUnitType() == OCUnit.LANDUNIT && ug.getNumUnits() > 0)
			{
				lastLand = ug;
			}
		}
		return lastLand;
	}
	
	private int getNumberOfLand(final Vector<UnitGroup> units)
	{
		int numLand = 0;
		final int vectorSize = units.size();
		for (int i = 0; i < vectorSize; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().getUnitType() == OCUnit.LANDUNIT)
			{
				numLand += ug.getNumUnits();
			}
		}
		return numLand;
	}
	
	private UnitGroup extraHp(final Vector<UnitGroup> units)
	{
		UnitGroup ug = null;
		final int size = units.size();
		for (int i = 0; i < size; i++)
		{
			final UnitGroup curG = units.elementAt(i);
			if (curG.getNumUnits() < curG.getTotalHp())
			{
				ug = curG;
				return ug;
			}
		}
		return ug;
	}
	
	private boolean removeUnit(final String curName, final Vector<UnitGroup> units)
	{
		for (int j = units.size() - 1; j >= 0; j--)
		{
			final UnitGroup ug = units.elementAt(j);
			final OCUnit cur = ug.getUnit();
			if (cur.getName().equals(curName))
			{
				ug.removeUnit();
				if (ug.getNumUnits() == 0)
					units.removeElementAt(j);
				return true;
			}
		}
		return false;
	}
	
	private Integer[] getSupportUnits(final Vector<UnitGroup> units)
	{
		Integer suppArray[] = null;
		if (landBattle)
		{
			final List<Integer> support = new ArrayList<Integer>();
			final int aSize = units.size();
			for (int i = 0; i < aSize; ++i)
			{
				final UnitGroup curG = units.elementAt(i);
				final OCUnit cur = curG.getUnit();
				if (cur.isSupportShot())
				{
					support.add(Integer.valueOf(i));
				}
			}
			suppArray = support.toArray(new Integer[support.size()]);
		}
		return suppArray;
	}
	
	private int getNumberOfAir(final Vector<UnitGroup> units)
	{
		int numAir = 0;
		final int vectorSize = units.size();
		for (int i = 0; i < vectorSize; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().getUnitType() == OCUnit.AIRUNIT)
			{
				numAir += ug.getNumUnits();
			}
		}
		return numAir;
	}
	
	private int getNumberOfFighters(final Vector<UnitGroup> units)
	{
		int numFtrs = 0;
		final int vectorSize = units.size();
		for (int i = 0; i < vectorSize; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().getName().equals(StandardUnits.FtrName))
			{
				numFtrs += ug.getNumUnits();
			}
		}
		return numFtrs;
	}
	
	private int getNumberOfBombers(final Vector<UnitGroup> units)
	{
		int numBmbs = 0;
		final int vectorSize = units.size();
		for (int i = 0; i < vectorSize; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().getName().equals(StandardUnits.BmbName))
			{
				numBmbs += ug.getNumUnits();
			}
		}
		return numBmbs;
	}
	
	public void rollRound()
	{
		int size = locAttackers.size();
		int attackerHits = 0;
		int specialHits = 0;
		final boolean attAllAir = (getNumberOfUnits(locAttackers) == getNumberOfAir(attackers));
		final boolean defAllAir = (getNumberOfUnits(locDefenders) == getNumberOfAir(defenders));
		final boolean defBlockNoRet = hasBlockNoRet(locDefenders);
		final int boostedInf = getBoosters(locAttackers);
		for (int i = 0; i < size; i++)
		{
			final UnitGroup curG = locAttackers.elementAt(i);
			final OCUnit cur = curG.getUnit();
			if (!cur.isCanHitAir() && defAllAir)
				continue;
			int hits = 0;
			if (boostedInf > 0 && cur.getName().equals(StandardUnits.InfName))
			{
				hits = curG.rollUnitsAttack(boostedInf);
			}
			else if (isAmphib && cur.isBoostAmphib())
			{
				cur.setAttackValue(cur.getAttackValue() + 2);
				hits = curG.rollUnitsAttack();
				cur.setAttackValue(cur.getAttackValue() - 2);
			}
			else
			{
				hits = curG.rollUnitsAttack();
			}
			if (cur.isNoRetaliationHit() && !defBlockNoRet)
			{
				specialHits += hits;
			}
			else
			{
				attackerHits += hits;
			}
		}
		removeUnits(specialHits, locDefenders, false);
		size = locDefenders.size();
		int defenderHits = 0;
		for (int i = 0; i < size; i++)
		{
			final UnitGroup curG = locDefenders.elementAt(i);
			final OCUnit cur = curG.getUnit();
			if (!cur.isCanHitAir() && attAllAir)
				continue;
			defenderHits += curG.rollUnitsDefend();
		}
		removeUnits(attackerHits, locDefenders, false);
		removeUnits(defenderHits, locAttackers, true);
	}
	
	protected int getNumberOfUnits(final Vector<UnitGroup> units)
	{
		int numUnits = 0;
		final int size = units.size();
		for (int i = 0; i < size; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			numUnits += ug.getNumUnits();
		}
		return numUnits;
	}
	
	private int getBoosters(final Vector<UnitGroup> units)
	{
		int boosters = 0;
		final int size = units.size();
		for (int i = 0; i < size; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().isBoostsInfAtt())
			{
				boosters += ug.getNumUnits();
			}
		}
		return boosters;
	}
	
	private boolean hasBlockNoRet(final Vector<UnitGroup> units)
	{
		boolean blocked = false;
		final int size = units.size();
		for (int i = 0; i < size && !blocked; i++)
		{
			final UnitGroup ug = units.elementAt(i);
			if (ug.getUnit().isBlockNoRetalHit())
				blocked = true;
		}
		return blocked;
	}
	
	public static void main(final String args[])
	{
	}
	
	@Override
	public String toString()
	{
		String results = "";
		results += resultStatusString + " ";
		if (resultStatus != OCBattle.CLEARED)
		{
			results += "with units:\nAttackers\n\t";
			results += unitVectorToString(attackers);
			results += "\nDefenders\n\t";
			results += unitVectorToString(defenders);
		}
		return results;
	}
	
	private String unitVectorToString(final Vector<UnitGroup> units)
	{
		String res = "";
		if (units.size() == 0)
		{
			res += " none";
		}
		else
		{
			final int size = units.size();
			for (int i = 0; i < size; i++)
			{
				res += " " + units.elementAt(i).toString();
			}
		}
		return res;
	}
	
	public boolean isRollAntiAirSep()
	{
		return rollAntiAirSep;
	}
	
	public void setRollAntiAirSep(final boolean rollAntiAirSep)
	{
		this.rollAntiAirSep = rollAntiAirSep;
	}
	
	public int getResultStatus()
	{
		return resultStatus;
	}
	
	public String getResultStatusString()
	{
		return resultStatusString;
	}
	
	public Vector<String> getAttOOL()
	{
		return attOOL;
	}
	
	public void setAttOOL(final Vector<String> attOOL)
	{
		this.attOOL = attOOL;
	}
	
	public Vector<String> getDefOOL()
	{
		return defOOL;
	}
	
	public void setDefOOL(final Vector<String> defOOL)
	{
		this.defOOL = defOOL;
	}
	
	public boolean isAmphib()
	{
		return isAmphib;
	}
	
	public void setAmphib(final boolean isAmphib)
	{
		this.isAmphib = isAmphib;
	}
	
	public int getAvgRemAtts()
	{
		return avgRemAtts;
	}
	
	public int getAvgRemDefs()
	{
		return avgRemDefs;
	}
}
