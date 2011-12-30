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
package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class UnitInformation
{
	private Map<UnitType, UnitAttachment> m_unitInfoMap;
	private Iterator<UnitType> m_unitTypeIterator;
	private GameData m_data;
	private PrintGenerationData m_printData;
	
	private String capitalizeFirst(final String s)
	{
		return (s.length() > 0) ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s;
	}
	
	protected void saveToFile(final PrintGenerationData printData, final Map<UnitType, UnitAttachment> unitInfoMap)
	{
		FileWriter unitInformation = null;
		m_printData = printData;
		m_data = m_printData.getData();
		m_unitInfoMap = unitInfoMap;
		m_unitTypeIterator = m_unitInfoMap.keySet().iterator();
		m_printData.getOutDir().mkdir();
		try
		{
			final File outFile = new File(m_printData.getOutDir(), "General Information.csv");
			unitInformation = new FileWriter(outFile);
			for (int i = 0; i < 8; i++)
			{
				unitInformation.write(",");
			}
			unitInformation.write("Unit Information");
			for (int i = 10; i < 20; i++)
			{
				unitInformation.write(",");
			}
			unitInformation.write("\r\n");
			unitInformation.write("Unit,Cost,Movement,Attack,Defense,CanBlitz,Artillery?,ArtillerySupportable?" + ",Factory?,Marine?,Transport Cost,AA Gun?,Air Unit?,Strategic Bomber?,Carrier Cost,"
						+ "Sea Unit?,Two Hit?,Transport Capacity,Carrier Capacity,Submarine?,Destroyer?");
			unitInformation.write("\r\n");
			while (m_unitTypeIterator.hasNext())
			{
				final UnitType currentType = m_unitTypeIterator.next();
				final UnitAttachment currentAttachment = m_unitInfoMap.get(currentType);
				if (currentType.getName().equals(Constants.AAGUN_TYPE))
				{
					unitInformation.write(currentType.getName() + ",");
				}
				else
				{
					unitInformation.write(capitalizeFirst(currentType.getName()) + ",");
				}
				unitInformation.write(getCostInformation(currentType) + ",");
				unitInformation.write(currentAttachment.getMovement(PlayerID.NULL_PLAYERID) + "," + currentAttachment.getAttack(PlayerID.NULL_PLAYERID) + ","
							+ currentAttachment.getDefense(PlayerID.NULL_PLAYERID) + "," + (currentAttachment.getCanBlitz() == false ? "-" : "true") + ","
							+ (currentAttachment.isArtillery() == false ? "-" : "true") + "," + (currentAttachment.isArtillerySupportable() == false ? "-" : "true") + ","
							+ (currentAttachment.isFactory() == false ? "-" : "true") + "," + (currentAttachment.getIsMarine() == false ? "-" : "true") + ","
							+ (currentAttachment.getTransportCost() == -1 ? "-" : currentAttachment.getTransportCost()) + ","
							+ ((currentAttachment.getIsAAforCombatOnly() || currentAttachment.getIsAAforBombingThisUnitOnly()) == false ? "-" : "true") + ","
							+ (currentAttachment.isAir() == false ? "-" : "true") + "," + (currentAttachment.isStrategicBomber() == false ? "-" : "true") + ","
							+ (currentAttachment.getCarrierCost() == -1 ? "-" : currentAttachment.getCarrierCost()) + "," + (currentAttachment.isSea() == false ? "-" : "true") + ","
							+ (currentAttachment.isTwoHit() == false ? "-" : "true") + "," + (currentAttachment.getTransportCapacity() == -1 ? "-" : currentAttachment.getTransportCapacity()) + ","
							+ (currentAttachment.getCarrierCapacity() == -1 ? "-" : currentAttachment.getCarrierCapacity()) + "," + (currentAttachment.isSub() == false ? "-" : "true") + ","
							+ (currentAttachment.getIsDestroyer() == false ? "-" : "true"));
				unitInformation.write("\r\n");
			}
			unitInformation.write("\r\n");
			unitInformation.close();
		} catch (final FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private int getCostInformation(final UnitType type)
	{
		if (m_data.getProductionFrontierList().getProductionFrontier("production") != null)
		{
			final List<ProductionRule> productionRules = m_data.getProductionFrontierList().getProductionFrontier("production").getRules();
			final Iterator<ProductionRule> productionIterator = productionRules.iterator();
			while (productionIterator.hasNext())
			{
				final ProductionRule currentRule = productionIterator.next();
				final UnitType currentType = (UnitType) currentRule.getResults().keySet().iterator().next();
				if (currentType.equals(type))
				{
					final int cost = currentRule.getCosts().getInt(m_data.getResourceList().getResource(Constants.PUS));
					return cost;
				}
			}
		}
		else
		{
			if (BattleCalculator.getCostsForTUV(m_data.getPlayerList().getPlayers().iterator().next(), m_data).getInt(type) > 0)
				return BattleCalculator.getCostsForTUV(m_data.getPlayerList().getPlayers().iterator().next(), m_data).getInt(type);
		}
		return -1;
	}
}
