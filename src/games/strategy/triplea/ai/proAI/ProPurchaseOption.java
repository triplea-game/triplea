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
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;

public class ProPurchaseOption
{
	private final ProductionRule productionRule;
	private final UnitType unitType;
	private final int cost;
	private final int movement;
	private final int quantity;
	private final int hitPoints;
	private int attack;
	private final int defense;
	private final int transportCost;
	private final boolean isAir;
	private final boolean isSub;
	private final boolean isTransport;
	private final boolean isCarrier;
	private final int transportCapacity;
	private final int carrierCapacity;
	private final double transportEfficiency;
	private final double carrierEfficiency;
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
		hitPoints = unitAttachment.getHitPoints() * quantity;
		attack = unitAttachment.getAttack(player) * quantity;
		if (unitAttachment.getArtillery())
			attack += quantity;
		defense = unitAttachment.getDefense(player) * quantity;
		transportCost = unitAttachment.getTransportCost();
		isAir = unitAttachment.getIsAir();
		isSub = unitAttachment.getIsSub();
		isTransport = unitAttachment.getTransportCapacity() > 0;
		isCarrier = unitAttachment.getCarrierCapacity() > 0;
		transportCapacity = unitAttachment.getTransportCapacity();
		carrierCapacity = unitAttachment.getCarrierCapacity();
		transportEfficiency = (double) unitAttachment.getTransportCapacity() * quantity / cost;
		carrierEfficiency = (double) unitAttachment.getCarrierCapacity() * quantity / cost;
		hitPointEfficiency = (double) hitPoints / cost;
		attackEfficiency = (hitPoints + attack + 0.5 * defense) / cost;
		defenseEfficiency = (hitPoints + 0.5 * attack + defense) / cost;
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
	
	public int getAttack()
	{
		return attack;
	}
	
	public int getDefense()
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
	
}
