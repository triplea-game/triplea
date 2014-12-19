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

import java.util.List;
import java.util.logging.Level;

public class ProPurchaseOption
{
	private final ProductionRule productionRule;
	private final UnitType unitType;
	private final int cost;
	private final int movement;
	private final int quantity;
	private int hitPoints;
	private final double attack;
	private final boolean isArtillery;
	private final double amphibAttack;
	private final double defense;
	private final int transportCost;
	private final boolean isAir;
	private final boolean isSub;
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
	
	public ProPurchaseOption(final ProductionRule productionRule, final UnitType unitType, final PlayerID player, final GameData data)
	{
		this.productionRule = productionRule;
		this.unitType = unitType;
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
		isArtillery = unitAttachment.getArtillery();
		amphibAttack = attack + 0.5 * unitAttachment.getIsMarine() * quantity;
		defense = unitAttachment.getDefense(player) * quantity;
		transportCost = unitAttachment.getTransportCost() * quantity;
		isAir = unitAttachment.getIsAir();
		isSub = unitAttachment.getIsSub();
		isTransport = unitAttachment.getTransportCapacity() > 0;
		isCarrier = unitAttachment.getCarrierCapacity() > 0;
		transportCapacity = unitAttachment.getTransportCapacity() * quantity;
		carrierCapacity = unitAttachment.getCarrierCapacity() * quantity;
		transportEfficiency = (double) unitAttachment.getTransportCapacity() / cost;
		carrierEfficiency = (double) unitAttachment.getCarrierCapacity() / cost;
		if (hitPoints == 0)
			costPerHitPoint = Double.POSITIVE_INFINITY;
		else
			costPerHitPoint = (double) cost / hitPoints;
		hitPointEfficiency = (hitPoints + 0.1 * attack * 6 / data.getDiceSides() + 0.2 * defense * 6 / data.getDiceSides()) / cost;
		attackEfficiency = (1 + hitPoints) * (hitPoints + attack * 6 / data.getDiceSides() + 0.5 * defense * 6 / data.getDiceSides()) / cost;
		defenseEfficiency = (1 + hitPoints) * (hitPoints + 0.5 * attack * 6 / data.getDiceSides() + defense * 6 / data.getDiceSides()) / cost;
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
	
	public double getFodderEfficiency(final int enemyDistance, final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double artilleryFactor = calculateArtilleryFactor(ownedLocalUnits, unitsToPlace);
		final double distanceFactor = Math.sqrt(calculateLandDistanceFactor(enemyDistance));
		return calculateEfficiency(0.25, 0.25, artilleryFactor, distanceFactor, data);
	}
	
	public double getAttackEfficiency2(final int enemyDistance, final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double artilleryFactor = calculateArtilleryFactor(ownedLocalUnits, unitsToPlace);
		final double distanceFactor = calculateLandDistanceFactor(enemyDistance);
		return calculateEfficiency(1.25, 0.75, artilleryFactor, distanceFactor, data);
	}
	
	public double getDefenseEfficiency2(final int enemyDistance, final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double artilleryFactor = calculateArtilleryFactor(ownedLocalUnits, unitsToPlace);
		final double distanceFactor = calculateLandDistanceFactor(enemyDistance);
		return calculateEfficiency(0.75, 1.25, artilleryFactor, distanceFactor, data);
	}
	
	public double getSeaDefenseEfficiency(final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		final double artilleryFactor = calculateArtilleryFactor(ownedLocalUnits, unitsToPlace);
		return calculateEfficiency(0.75, 1, artilleryFactor, movement, data);
	}
	
	public double getAmphibEfficiency(final GameData data)
	{
		final double hitPointPerUnitFactor = (3 + hitPoints / quantity);
		final double transportCostFactor = Math.pow(1.0 / transportCost, .2);
		final double hitPointValue = 2 * hitPoints;
		final double attackValue = amphibAttack * 6 / data.getDiceSides();
		final double defenseValue = defense * 6 / data.getDiceSides();
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
	
	private double calculateArtilleryFactor(final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace)
	{
		if (!isArtillery)
			return 0;
		
		int numLocalArtillery = 0;
		int numLocalSupportable = 0;
		for (final Unit u : ownedLocalUnits)
		{
			if (UnitAttachment.get(u.getType()).getArtillery())
				numLocalArtillery++;
			if (UnitAttachment.get(u.getType()).getArtillerySupportable())
				numLocalSupportable++;
		}
		final int numNeededLocalSupportable = Math.max(1, 2 * numLocalArtillery - numLocalSupportable);
		int numPlaceArtillery = 0;
		int numPlaceSupportable = 0;
		for (final Unit u : unitsToPlace)
		{
			if (UnitAttachment.get(u.getType()).getArtillery())
				numPlaceArtillery++;
			if (UnitAttachment.get(u.getType()).getArtillerySupportable())
				numPlaceSupportable++;
		}
		final double supportableRatio = (double) numPlaceSupportable / (2 * numPlaceArtillery + numNeededLocalSupportable);
		final double artilleryFactor = 0.9 * Math.min(1.0, supportableRatio); // ranges from 0 to 0.9
		
		LogUtils.log(Level.FINEST, "artilleryFactor=" + artilleryFactor + ", numPlaceSupportable=" + numPlaceSupportable + ", numPlaceArtillery=" + numPlaceArtillery + ", numNeededLocalSupportable="
					+ numNeededLocalSupportable);
		
		return artilleryFactor;
	}
	
	private double calculateEfficiency(final double attackFactor, final double defenseFactor, final double artilleryFactor, final double distanceFactor, final GameData data)
	{
		final double hitPointPerUnitFactor = (3 + hitPoints / quantity);
		final double hitPointValue = 2 * hitPoints;
		final double attackValue = attackFactor * (attack + artilleryFactor * quantity) * 6 / data.getDiceSides();
		final double defenseValue = defenseFactor * defense * 6 / data.getDiceSides();
		return Math.pow((hitPointValue + attackValue + defenseValue) * hitPointPerUnitFactor * distanceFactor / cost, 30) / quantity;
	}
	
}
