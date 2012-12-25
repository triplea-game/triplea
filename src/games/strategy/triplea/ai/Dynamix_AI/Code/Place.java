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
package games.strategy.triplea.ai.Dynamix_AI.Code;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.Dynamix_AI.DMatches;
import games.strategy.triplea.ai.Dynamix_AI.DSettings;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.FactoryCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.ai.Dynamix_AI.Group.PurchaseGroup;
import games.strategy.triplea.ai.Dynamix_AI.Others.Purchase_UnitPlacementLocationSorter;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.util.Match;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

/**
 * 
 * @author Stephen
 */
@SuppressWarnings("unchecked")
public class Place
{
	public static void place(final Dynamix_AI ai, final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		if (DSettings.LoadSettings().AIC_disableAllUnitPurchasesAndPlacements)
		{
			final String message = ai.getName() + " is skipping it's placement phase and destroying its " + player.getResources().getQuantity(GlobalCenter.GetPUResource()) + " PU's, as instructed.";
			DUtils.Log(Level.FINE, message);
			final Runnable runner = new Runnable()
			{
				public void run()
				{
					CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(message);
				}
			};
			try
			{
				SwingUtilities.invokeAndWait(runner);
			} catch (final InterruptedException ex)
			{
			} catch (final InvocationTargetException ex)
			{
			}
			final Change change = ChangeFactory.changeResourcesChange(player, GlobalCenter.GetPUResource(), -player.getResources().getQuantity(GlobalCenter.GetPUResource()));
			CachedInstanceCenter.CachedDelegateBridge.addChange(change);
			Dynamix_AI.Pause();
			return;
		}
		Unit aa = null;
		for (final ProductionRule rule : player.getProductionFrontier().getRules())
		{
			if (Matches.UnitTypeIsAAforAnything.match((UnitType) rule.getResults().keySet().toArray()[0]))
			{
				aa = ((UnitType) rule.getResults().keySet().toArray()[0]).create(player);
				break;
			}
		}
		if (aa != null)
		{
			for (final Territory aaBuildTer : FactoryCenter.get(data, player).ChosenAAPlaceTerritories)
			{
				final List<Unit> matchingAA = GetPlayerUnitsMatchingUnitsInList(Collections.singletonList(aa), player);
				if (matchingAA.isEmpty()) // This should not be happening, but it does for some reason
				{
					DUtils.Log(Level.FINER, "  AA unit placement on pre-assigned territory " + aaBuildTer.getName()
								+ " failed because the player's units matching Matches.UnitIsAA is null... Player's Units: {0}", player.getUnits().getUnits());
					continue;
				}
				final Unit nextAA = matchingAA.get(0);
				doPlace(ai, aaBuildTer, Collections.singleton(nextAA), data, placeDelegate);
			}
		}
		for (final Territory factoryTer : FactoryCenter.get(data, player).ChosenFactoryTerritories)
		{
			final PurchaseGroup pg = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(factoryTer);
			if (pg == null)
				break;
			final List<Unit> units = GetPlayerUnitsMatchingUnitsInList(pg.GetSampleUnits(), player);
			if (DSettings.LoadSettings().EnableUnitPlacementMultiplier && DSettings.LoadSettings().UnitPlacementMultiplyPercent != 100) // AI cheat for more interesting gameplay. Can be turned on with AI settings window.
			{
				final float multiplyAmount = DUtils.ToFloat(DSettings.LoadSettings().UnitPlacementMultiplyPercent);
				final List<Unit> hackedUnits = DUtils.GetXPercentOfTheUnitsInList_CreateMoreIfNeeded(units, multiplyAmount);
				final String message = ai.getName() + " use a UPM cheat, and place " + hackedUnits.size() + " units on " + factoryTer.getName();
				final List<Unit> fHackedUnits = hackedUnits;
				DUtils.Log(Level.FINE, message);
				final Runnable runner = new Runnable()
				{
					public void run()
					{
						CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(message, fHackedUnits); // Let the user see the hacked units in the sidebar
					}
				};
				try
				{
					SwingUtilities.invokeAndWait(runner);
				} catch (final InterruptedException ex)
				{
				} catch (final InvocationTargetException ex)
				{
				}
				final Change change = ChangeFactory.addUnits(factoryTer, hackedUnits);
				CachedInstanceCenter.CachedDelegateBridge.addChange(change);
				final Change change2 = ChangeFactory.removeUnits(player, units); // Now remove the left-to-place units
				CachedInstanceCenter.CachedDelegateBridge.addChange(change2);
				Dynamix_AI.Pause();
			}
			else
			{
				doPlace(ai, factoryTer, units, data, placeDelegate);
			}
		}
		for (final PurchaseGroup factory : FactoryCenter.get(data, player).FactoryPurchaseGroups)
		{
			final Territory bestFactoryPlaceTer = CalculateBestFactoryBuildTerritory(data, player);
			if (bestFactoryPlaceTer == null) // This should not be happening!
			{
				DUtils.Log(Level.FINER, "  No factory build ter found to place factory on!");
				break;
			}
			final List<Unit> units = GetPlayerUnitsMatchingUnitsInList(factory.GetSampleUnits(), player);
			doPlace(ai, bestFactoryPlaceTer, units, data, placeDelegate);
		}
		if (player.getUnits().someMatch(Matches.UnitCanProduceUnitsAndIsConstruction)) // If we have leftover factories to place
		{
			DUtils.Log(Level.FINE, "  There are factories leftover from the purchase phase, so looping and placing extra factories.");
			final List<Unit> leftoverUnits = DUtils.ToList(player.getUnits().getUnits());
			while (Match.someMatch(leftoverUnits, Matches.UnitCanProduceUnitsAndIsConstruction))
			{
				final Territory bestFactoryPlaceTer = CalculateBestFactoryBuildTerritory(data, player);
				if (bestFactoryPlaceTer == null) // This should not be happening!
				{
					DUtils.Log(Level.FINER, "  No factory build ter found to place factory on!");
					break;
				}
				Unit nextFactoryToPlace = null;
				for (final Unit unit : leftoverUnits)
				{
					if (Matches.UnitCanProduceUnitsAndIsConstruction.match(unit))
					{
						nextFactoryToPlace = unit;
						break;
					}
				}
				if (nextFactoryToPlace == null) // How could this happen... :\
					break;
				if (!doPlace(ai, bestFactoryPlaceTer, Collections.singletonList(nextFactoryToPlace), data, placeDelegate))
					leftoverUnits.remove(nextFactoryToPlace); // If factory placement failed, remove from list
			}
		}
		if (player.getUnits().size() > 0)
		{
			DUtils.Log(Level.FINE, "  There are units leftover from the purchase phase, so looping and placing extra units.");
			// If the game is reloaded, this country can place anywhere, or there was some sort of issue between purchase and place phase, we need to place all the leftover units
			final List<Territory> sortedPossiblePlaceLocations = Purchase_UnitPlacementLocationSorter.CalculateAndSortUnitPlacementLocations(ai, bid, data, player);
			for (final Territory placeLoc : sortedPossiblePlaceLocations)
			{
				final List<Unit> leftoverUnits = DUtils.ToList(player.getUnits().getUnits());
				if (leftoverUnits.isEmpty()) // If we've placed all extra units
					break;
				final PlaceableUnits pu = placeDelegate.getPlaceableUnits(leftoverUnits, placeLoc);
				if (pu.getErrorMessage() != null)
					continue; // Can't place here
				int maxUnitsWeCanPlaceHere = pu.getMaxUnits();
				if (maxUnitsWeCanPlaceHere == -1) // -1 means we can place unlimited amounts here
					maxUnitsWeCanPlaceHere = Integer.MAX_VALUE;
				List<Unit> unitsToPlace;
				if (maxUnitsWeCanPlaceHere >= leftoverUnits.size())
					unitsToPlace = leftoverUnits;
				else
					unitsToPlace = leftoverUnits.subList(0, maxUnitsWeCanPlaceHere);
				if (DSettings.LoadSettings().EnableUnitPlacementMultiplier && DSettings.LoadSettings().UnitPlacementMultiplyPercent != 100) // AI cheat for more interesting gameplay. Can be turned on with AI settings window.
				{
					final float multiplyAmount = DUtils.ToFloat(DSettings.LoadSettings().UnitPlacementMultiplyPercent);
					final List<Unit> hackedUnits = DUtils.GetXPercentOfTheUnitsInList_CreateMoreIfNeeded(unitsToPlace, multiplyAmount);
					final List<Unit> fHackedUnits = hackedUnits;
					final Territory fPlaceLoc = placeLoc;
					final Dynamix_AI fAI = ai;
					final Runnable runner = new Runnable()
					{
						public void run()
						{
							CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(
										fAI.getName() + " use a UPM cheat, and place " + fHackedUnits.size() + " units on " + fPlaceLoc.getName(), fHackedUnits); // Let the user see the hacked units in the sidebar
						}
					};
					try
					{
						SwingUtilities.invokeAndWait(runner);
					} catch (final Exception ex)
					{
						System.out.println(ex.toString());
					}
					final Change change = ChangeFactory.addUnits(placeLoc, hackedUnits);
					CachedInstanceCenter.CachedDelegateBridge.addChange(change);
					final Change change2 = ChangeFactory.removeUnits(player, unitsToPlace); // Now remove the left-to-place units
					CachedInstanceCenter.CachedDelegateBridge.addChange(change2);
					Dynamix_AI.Pause();
				}
				else
				{
					doPlace(ai, placeLoc, unitsToPlace, data, placeDelegate); // Place as much of the leftover as we can
				}
			}
		}
		if (DSettings.LoadSettings().EnableResourceCollectionIncreaser && DSettings.LoadSettings().ResourceCollectionIncreaseAmount != 0) // AI cheat for more interesting gameplay. Can be turned on with AI settings window.
		{
			final int PUChange = DSettings.LoadSettings().ResourceCollectionIncreaseAmount;
			if (PUChange > 0)
			{
				final int newPUs = player.getResources().getQuantity(GlobalCenter.GetPUResource()) + PUChange;
				final String message = ai.getName() + " use an RCI cheat, and increase their PUs from " + player.getResources().getQuantity(GlobalCenter.GetPUResource()) + " to " + newPUs;
				DUtils.Log(Level.FINE, message);
				final Runnable runner = new Runnable()
				{
					public void run()
					{
						CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(message);
					}
				};
				try
				{
					SwingUtilities.invokeAndWait(runner);
				} catch (final InterruptedException ex)
				{
				} catch (final InvocationTargetException ex)
				{
				}
				final Change change = ChangeFactory.changeResourcesChange(player, GlobalCenter.GetPUResource(), PUChange);
				CachedInstanceCenter.CachedDelegateBridge.addChange(change);
				Dynamix_AI.Pause();
			}
		}
		GlobalCenter.PUsAtEndOfLastTurn = player.getResources().getQuantity(GlobalCenter.GetPUResource());
	}
	
	private static boolean doPlace(final Dynamix_AI ai, Territory ter, final Collection<Unit> units, final GameData data, final IAbstractPlaceDelegate placer)
	{
		DUtils.Log(Level.FINER, "    Placing units. Territory: {0} Units: {1}", ter, DUtils.UnitList_ToString(units));
		// Temporary hack to get ships placed down. Later, I will code this correctly
		if (units.size() > 0 && UnitAttachment.get(units.iterator().next().getUnitType()).getIsSea()
					&& data.getMap().getNeighbors(ter, Matches.TerritoryIsWater).size() > 0)
		{
			final Set<Territory> openPorts = ter.getData().getMap()
						.getNeighbors(ter, DUtils.CompMatchAnd(Matches.TerritoryIsWater, Matches.territoryHasUnitsThatMatch(Matches.unitIsEnemyOf(ter.getData(), ter.getOwner())).invert()));
			if (openPorts.size() > 0)
				ter = openPorts.iterator().next();
			else
				ter = data.getMap().getNeighbors(ter, Matches.TerritoryIsWater).iterator().next();
		}
		final String message = placer.placeUnits(new ArrayList<Unit>(units), ter);
		if (message != null)
		{
			DUtils.Log(Level.FINER, "      Error occured: {0}", message);
			return false;
		}
		else
		{
			ai.pause();
			return true;
		}
	}
	
	private static List<Unit> GetPlayerUnitsMatchingUnitsInList(final List<Unit> units, final PlayerID player)
	{
		final List<Unit> result = new ArrayList<Unit>();
		final List<Unit> pUnits = new ArrayList<Unit>(player.getUnits().getUnits());
		for (final Unit unit : units)
		{
			for (final Unit pUnit : pUnits)
			{
				if (pUnit.getUnitType().equals(unit.getUnitType()))
				{
					result.add(pUnit);
					break;
				}
			}
			pUnits.removeAll(result);
		}
		return result;
	}
	
	private static Territory CalculateBestFactoryBuildTerritory(final GameData data, final PlayerID player)
	{
		final List<Territory> ourCapitals = TerritoryAttachment.getAllCapitals(player, data);
		final List<Territory> possibles = new ArrayList<Territory>(data.getMap().getTerritoriesOwnedBy(player));
		Territory highestScoringTer = null;
		int highestTerScore = Integer.MIN_VALUE;
		for (final Territory ter : possibles)
		{
			if (ter.getUnits().someMatch(Matches.UnitCanProduceUnitsAndIsConstruction))
				continue;
			if (StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasAttacked())
				continue;
			int score = 0;
			score -= DUtils.GetVulnerabilityOfArmy(data, player, ter, DUtils.ToList(ter.getUnits().getUnits()), 250) * 1000; // TODO: 250 run count should be in the user menu for this...
			score += TerritoryAttachment.get(ter).getProduction() * 10;
			if (!ourCapitals.isEmpty())
				score -= DUtils.GetJumpsFromXToY_NoCond(data, ter, ourCapitals.iterator().next());
			if (DMatches.territoryIsOnSmallIsland(data).match(ter))
				score -= 100000; // Atm, never place on islands unless we have to
			if (score > highestTerScore)
			{
				highestScoringTer = ter;
				highestTerScore = score;
			}
		}
		return highestScoringTer;
	}
}
