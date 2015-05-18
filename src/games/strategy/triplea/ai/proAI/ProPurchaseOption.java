package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.IntegerMap;
import games.strategy.util.LinkedIntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ProPurchaseOption
{
	private final ProductionRule productionRule;
	private final UnitType unitType;
	private final PlayerID player;
	private final int cost;
	private final int movement;
	private final int quantity;
	private int hitPoints;
	private final double attack;
	private final double amphibAttack;
	private final double defense;
	private final int transportCost;
	private final int carrierCost;
	private final boolean isAir;
	private final boolean isSub;
	private final boolean isDestroyer;
	private final boolean isTransport;
	private final boolean isCarrier;
	private final boolean isInfra;
	private final int transportCapacity;
	private final int carrierCapacity;
	private final double transportEfficiency;
	private final double carrierEfficiency;
	private double costPerHitPoint;
	private final double hitPointEfficiency;
	private final double attackEfficiency;
	private final double defenseEfficiency;
	private final int maxBuiltPerPlayer;
	private final Set<UnitSupportAttachment> unitSupportAttachments;
	private boolean isAttackSupport;
	private boolean isDefenseSupport;
	
	public ProPurchaseOption(final ProductionRule productionRule, final UnitType unitType, final PlayerID player, final GameData data)
	{
		this.productionRule = productionRule;
		this.unitType = unitType;
		this.player = player;
		final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
		final Resource PUs = data.getResourceList().getResource(Constants.PUS);
		cost = productionRule.getCosts().getInt(PUs);
		movement = unitAttachment.getMovement(player);
		quantity = productionRule.getResults().totalValues();
		isInfra = unitAttachment.getIsInfrastructure();
		hitPoints = unitAttachment.getHitPoints() * quantity;
		if (isInfra)
			hitPoints = 0;
		attack = unitAttachment.getAttack(player) * quantity;
		amphibAttack = attack + 0.5 * unitAttachment.getIsMarine() * quantity;
		defense = unitAttachment.getDefense(player) * quantity;
		transportCost = unitAttachment.getTransportCost() * quantity;
		carrierCost = unitAttachment.getCarrierCost() * quantity;
		isAir = unitAttachment.getIsAir();
		isSub = unitAttachment.getIsSub();
		isDestroyer = unitAttachment.getIsDestroyer();
		isTransport = unitAttachment.getTransportCapacity() > 0;
		isCarrier = unitAttachment.getCarrierCapacity() > 0;
		transportCapacity = unitAttachment.getTransportCapacity() * quantity;
		carrierCapacity = unitAttachment.getCarrierCapacity() * quantity;
		transportEfficiency = (double) unitAttachment.getTransportCapacity() / cost;
		carrierEfficiency = (double) unitAttachment.getCarrierCapacity() / cost;
		if (hitPoints == 0)
			costPerHitPoint = Double.POSITIVE_INFINITY;
		else
			costPerHitPoint = ((double) cost) / hitPoints;
		hitPointEfficiency = (hitPoints + 0.1 * attack * 6 / data.getDiceSides() + 0.2 * defense * 6 / data.getDiceSides()) / cost;
		attackEfficiency = (1 + hitPoints) * (hitPoints + attack * 6 / data.getDiceSides() + 0.5 * defense * 6 / data.getDiceSides()) / cost;
		defenseEfficiency = (1 + hitPoints) * (hitPoints + 0.5 * attack * 6 / data.getDiceSides() + defense * 6 / data.getDiceSides()) / cost;
		maxBuiltPerPlayer = unitAttachment.getMaxBuiltPerPlayer();
		
		// Support fields
		unitSupportAttachments = UnitSupportAttachment.get(unitType);
		isAttackSupport = false;
		isDefenseSupport = false;
		for (final UnitSupportAttachment usa : unitSupportAttachments)
		{
			if (usa.getOffence())
				isAttackSupport = true;
			if (usa.getDefence())
				isDefenseSupport = true;
		}
	}
	
	@Override
	public String toString()
	{
		return productionRule + " | cost=" + cost + " | moves=" + movement + " | quantity=" + quantity
					+ " | hitPointEfficiency=" + hitPointEfficiency + " | attackEfficiency=" + attackEfficiency + " | defenseEfficiency=" + defenseEfficiency
					+ " | isSub=" + isSub + " | isTransport=" + isTransport + " | isCarrier=" + isCarrier;
	}
	
	public ProductionRule getProductionRule()
	{
		return productionRule;
	}
	
	public int getCost()
	{
		return cost;
	}
	
	public int getMovement()
	{
		return movement;
	}
	
	public int getQuantity()
	{
		return quantity;
	}
	
	public int getHitPoints()
	{
		return hitPoints;
	}
	
	public double getAttack()
	{
		return attack;
	}
	
	public double getDefense()
	{
		return defense;
	}
	
	public boolean isSub()
	{
		return isSub;
	}
	
	public boolean isDestroyer()
	{
		return isDestroyer;
	}
	
	public boolean isTransport()
	{
		return isTransport;
	}
	
	public boolean isCarrier()
	{
		return isCarrier;
	}
	
	public double getTransportEfficiency()
	{
		return transportEfficiency;
	}
	
	public double getCarrierEfficiency()
	{
		return carrierEfficiency;
	}
	
	public double getHitPointEfficiency()
	{
		return hitPointEfficiency;
	}
	
	public double getAttackEfficiency()
	{
		return attackEfficiency;
	}
	
	public double getDefenseEfficiency()
	{
		return defenseEfficiency;
	}
	
	public UnitType getUnitType()
	{
		return unitType;
	}
	
	public int getTransportCapacity()
	{
		return transportCapacity;
	}
	
	public int getCarrierCapacity()
	{
		return carrierCapacity;
	}
	
	public int getTransportCost()
	{
		return transportCost;
	}
	
	public int getCarrierCost()
	{
		return carrierCost;
	}
	
	public boolean isAir()
	{
		return isAir;
	}
	
	public void setCostPerHitPoint(final double costPerHitPoint)
	{
		this.costPerHitPoint = costPerHitPoint;
	}
	
	public double getCostPerHitPoint()
	{
		return costPerHitPoint;
	}
	
	public int getMaxBuiltPerPlayer()
	{
		return maxBuiltPerPlayer;
	}
	
	public boolean isAttackSupport()
	{
		return isAttackSupport;
	}
	
	public boolean isDefenseSupport()
	{
		return isDefenseSupport;
	}
	
	public double getFodderEfficiency(final int enemyDistance, final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double supportAttackFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
		final double supportDefenseFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
		final double distanceFactor = Math.sqrt(calculateLandDistanceFactor(enemyDistance));
		return calculateEfficiency(0.25, 0.25, supportAttackFactor, supportDefenseFactor, distanceFactor, data);
	}
	
	public double getAttackEfficiency2(final int enemyDistance, final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double supportAttackFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
		final double supportDefenseFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
		final double distanceFactor = calculateLandDistanceFactor(enemyDistance);
		return calculateEfficiency(1.25, 0.75, supportAttackFactor, supportDefenseFactor, distanceFactor, data);
	}
	
	public double getDefenseEfficiency2(final int enemyDistance, final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double supportAttackFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
		final double supportDefenseFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
		final double distanceFactor = calculateLandDistanceFactor(enemyDistance);
		return calculateEfficiency(0.75, 1.25, supportAttackFactor, supportDefenseFactor, distanceFactor, data);
	}
	
	public double getSeaDefenseEfficiency(final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace, final boolean needDestroyer, final int unusedCarrierCapacity)
	{
		if (isAir && (carrierCost <= 0 || carrierCost > unusedCarrierCapacity))
			return 0;
		final double supportAttackFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
		final double supportDefenseFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
		double seaFactor = 1;
		if (needDestroyer && isDestroyer)
			seaFactor = 8;
		if (carrierCost > 0)
			seaFactor = 4;
		if (carrierCapacity > 0 && unusedCarrierCapacity <= 0)
			seaFactor = 2;
		return calculateEfficiency(0.75, 1, supportAttackFactor, supportDefenseFactor, movement, seaFactor, data);
	}
	
	public double getAmphibEfficiency(final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double supportAttackFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
		final double supportDefenseFactor = calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
		final double hitPointPerUnitFactor = (3 + hitPoints / quantity);
		final double transportCostFactor = Math.pow(1.0 / transportCost, .2);
		final double hitPointValue = 2 * hitPoints;
		final double attackValue = (amphibAttack + supportAttackFactor * quantity) * 6 / data.getDiceSides();
		final double defenseValue = (defense + supportDefenseFactor * quantity) * 6 / data.getDiceSides();
		return Math.pow((hitPointValue + attackValue + defenseValue) * hitPointPerUnitFactor * transportCostFactor / cost, 30) / quantity;
	}
	
	public double getTransportEfficiency(final GameData data)
	{
		return Math.pow(transportEfficiency, 30) / quantity;
	}
	
	private double calculateLandDistanceFactor(final int enemyDistance)
	{
		final double distance = Math.max(0, enemyDistance - 1.5);
		final double moveFactor = 1 + 2 * (Math.pow(2, movement - 1) - 1) / Math.pow(2, movement - 1); // 1, 2, 2.5, 2.75, etc
		final double distanceFactor = Math.pow(moveFactor, distance / 7.5);
		return distanceFactor;
	}
	
	// TODO: doesn't consider enemy support
	private double calculateSupportFactor(final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace, final GameData data, final boolean defense)
	{
		if ((!isAttackSupport && !defense) || (!isDefenseSupport && defense))
			return 0;
		
		final List<Unit> units = new ArrayList<Unit>(ownedLocalUnits);
		units.addAll(unitsToPlace);
		units.addAll(unitType.create(1, player, true));
		final Set<List<UnitSupportAttachment>> supportsAvailable = new HashSet<List<UnitSupportAttachment>>();
		final IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
		DiceRoll.getSupport(units, supportsAvailable, supportLeft, new HashMap<UnitSupportAttachment, LinkedIntegerMap<Unit>>(), data, defense, true);
		double totalSupportFactor = 0;
		for (final UnitSupportAttachment usa : unitSupportAttachments)
		{
			for (final List<UnitSupportAttachment> bonusType : supportsAvailable)
			{
				if (!bonusType.contains(usa))
					continue;
				
				// Find number of support provided and supportable units
				int numAddedSupport = usa.getNumber();
				if (usa.getImpArtTech() && TechTracker.hasImprovedArtillerySupport(player))
					numAddedSupport *= 2;
				int numSupportProvided = -numAddedSupport;
				final Set<Unit> supportableUnits = new HashSet<Unit>();
				for (final UnitSupportAttachment usa2 : bonusType)
				{
					numSupportProvided += supportLeft.getInt(usa2);
					supportableUnits.addAll(Match.getMatches(units, Matches.unitIsOfTypes(usa2.getUnitType())));
				}
				final int numSupportableUnits = supportableUnits.size();
				
				// Find ratio of supportable to support units (optimal 2 to 1)
				final int numExtraSupportableUnits = Math.max(0, numSupportableUnits - numSupportProvided);
				final double ratio = Math.min(1, 2.0 * numExtraSupportableUnits / (numSupportableUnits + numAddedSupport)); // ranges from 0 to 1
				
				// Find approximate strength bonus provided
				double bonus = 0;
				if (usa.getStrength())
					bonus += usa.getBonus();
				if (usa.getRoll())
					bonus += (usa.getBonus() * data.getDiceSides() * 0.75);
				
				// Find support factor value
				final double supportFactor = Math.pow(numAddedSupport * 0.9, 0.9) * bonus * ratio;
				totalSupportFactor += supportFactor;
				
				LogUtils.log(Level.FINEST, unitType.getName() + ", bonusType=" + usa.getBonusType() + ", supportFactor=" + supportFactor + ", numSupportProvided=" + numSupportProvided
							+ ", numSupportableUnits=" + numSupportableUnits + ", numAddedSupport=" + numAddedSupport + ", ratio=" + ratio + ", bonus=" + bonus);
			}
		}
		LogUtils.log(Level.FINER, unitType.getName() + ", defense=" + defense + ", totalSupportFactor=" + totalSupportFactor);
		
		return totalSupportFactor;
	}
	
	private double calculateEfficiency(final double attackFactor, final double defenseFactor, final double supportAttackFactor, final double supportDefenseFactor, final double distanceFactor,
				final GameData data)
	{
		return calculateEfficiency(attackFactor, defenseFactor, supportAttackFactor, supportDefenseFactor, distanceFactor, 1, data);
	}
	
	private double calculateEfficiency(final double attackFactor, final double defenseFactor, final double supportAttackFactor, final double supportDefenseFactor, final double distanceFactor,
				final double seaFactor, final GameData data)
	{
		final double hitPointPerUnitFactor = (3 + hitPoints / quantity);
		final double hitPointValue = 2 * hitPoints;
		final double attackValue = attackFactor * (attack + supportAttackFactor * quantity) * 6 / data.getDiceSides();
		final double defenseValue = defenseFactor * (defense + supportDefenseFactor * quantity) * 6 / data.getDiceSides();
		return Math.pow((hitPointValue + attackValue + defenseValue) * hitPointPerUnitFactor * distanceFactor * seaFactor / cost, 30) / quantity;
	}
	
}
