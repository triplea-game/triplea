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
package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.Dynamix_AI.Others.TerritoryStatus;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.List;

/**
 * Some notes on the matches in this class:
 * 
 * First, to keep the matches organized, I would like all the matches to be put into their section, which should be created if not yet existing.
 * All unit matches in one section, all territory matches in another, etc.
 * 
 * Also, make sure there are markers to show the start and end of each section, as well as ten lines of blank space between each section.
 * 
 * @author Stephen
 */
public class DMatches
{
	// /////////////////////////////////////////////Unit Group Matches///////////////////////////////////////////////
	public static Match<UnitGroup> UnitGroupCanReach_Some(final Territory target)
	{
		return new Match<UnitGroup>()
		{
			@Override
			public boolean match(final UnitGroup ug)
			{
				final Route ncmRoute = ug.GetNCMRoute(target);
				if (ncmRoute == null)
					return false;
				return Match.someMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(ncmRoute.getLength()));
			}
		};
	}
	
	public static Match<UnitGroup> UnitGroupCanReach_All(final Territory target)
	{
		return new Match<UnitGroup>()
		{
			@Override
			public boolean match(final UnitGroup ug)
			{
				final Route ncmRoute = ug.GetNCMRoute(target);
				if (ncmRoute == null)
					return false;
				return Match.allMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(ncmRoute.getLength()));
			}
		};
	}
	
	public static Match<UnitGroup> UnitGroupHasEnoughMovement_Some(final int minMovement)
	{
		return new Match<UnitGroup>()
		{
			@Override
			public boolean match(final UnitGroup ug)
			{
				return Match.someMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(minMovement));
			}
		};
	}
	
	public static Match<UnitGroup> UnitGroupHasEnoughMovement_All(final int minMovement)
	{
		return new Match<UnitGroup>()
		{
			@Override
			public boolean match(final UnitGroup ug)
			{
				return Match.allMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(minMovement));
			}
		};
	}
	
	public static Match<UnitGroup> UnitGroupUnitsMatchX_All(final Match<Unit> match)
	{
		return new Match<UnitGroup>()
		{
			@Override
			public boolean match(final UnitGroup ug)
			{
				return Match.allMatch(ug.GetUnits(), match);
			}
		};
	}
	
	public static final Match<UnitGroup> UnitGroupIsSeaOrAir = new Match<UnitGroup>()
	{
		@Override
		public boolean match(final UnitGroup unitGroup)
		{
			final UnitAttachment ua = UnitAttachment.get(unitGroup.GetFirstUnit().getType());
			return ua.isSea() || ua.isAir();
		}
	};
	public static final Match<UnitGroup> UnitGroupIsLand = new Match<UnitGroup>()
	{
		@Override
		public boolean match(final UnitGroup unitGroup)
		{
			final UnitAttachment ua = UnitAttachment.get(unitGroup.GetFirstUnit().getType());
			return !ua.isSea();
		}
	};
	
	// /////////////////////////////////////////////End Unit Group Matches///////////////////////////////////////////////
	// /////////////////////////////////////////////Unit Matches///////////////////////////////////////////////
	public static Match<Unit> unitIs(final Unit u1)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u2)
			{
				return u1 == u2;
			}
		};
	}
	
	public static Match<Unit> unitIsNotInList(final List<Unit> list)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit ter)
			{
				return !list.contains(ter);
			}
		};
	}
	
	public static Match<Unit> unitIsInList(final List<Unit> list)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit ter)
			{
				return list.contains(ter);
			}
		};
	}
	
	public static Match<Unit> unitIsNNEnemyOf(final GameData data, final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				if (u.getOwner().isNull())
					return false;
				return !data.getRelationshipTracker().isAllied(u.getOwner(), player);
			}
		};
	}
	
	public static final Match<Unit> UnitIsMoveableType = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
			return ua.getMovement(unit.getOwner()) > 0;
		}
	};
	public static Match<Unit> UnitIsNonAAMoveableType = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
			return ua.getMovement(unit.getOwner()) > 0 && !ua.isAA();
		}
	};
	public static final Match<Unit> UnitCanAttack = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
			if (ua.isAA())
				return false;
			if (ua.isFactory())
				return false;
			return true;
		}
	};
	public static final Match<Unit> UnitCanDefend = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
			if (ua.isAA())
				return false;
			if (ua.isFactory())
				return false;
			return true;
		}
	};
	
	// /////////////////////////////////////////////End Unit Matches///////////////////////////////////////////////
	// /////////////////////////////////////////////Territory Matches///////////////////////////////////////////////
	public static Match<Territory> terIsFriendlyEmptyAndWithoutEnemyNeighbors(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (!DMatches.territoryIsOwnedByXOrAlly(data, player).match(ter))
					return false;
				if (Matches.territoryHasUnitsThatMatch(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1)).match(ter))
					return false;
				if (data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByNNEnemy(data, player)).size() > 0) // If it's next to enemy
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> TerritoryHasVulnerabilityEqualToOrMoreThan(final GameData data, final PlayerID player, final float minVulnerability, final int runCount)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (DUtils.GetVulnerabilityOfArmy(data, player, ter, DUtils.ToList(ter.getUnits().getUnits()), runCount) >= minVulnerability)
					return true;
				else
					return false;
			}
		};
	}
	
	public static Match<Territory> TerritoryHasSurvivalChanceEqualToOrMoreThan(final GameData data, final PlayerID player, final float minSurvivalChance, final int runCount)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (DUtils.GetSurvivalChanceOfArmy(data, player, ter, DUtils.ToList(ter.getUnits().getUnits()), runCount) >= minSurvivalChance)
					return true;
				else
					return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsInList(final List<Territory> list)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				return list.contains(ter);
			}
		};
	}
	
	public static Match<Territory> territoryIsNotInList(final List<Territory> list)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				return !list.contains(ter);
			}
		};
	}
	
	public static Match<Territory> territoryIsWithinXLMovesOfATerInList(final List<Territory> list, final int maxJumpDist, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory territory)
			{
				for (final Territory ter : list)
				{
					if (DUtils.CanWeGetFromXToY_ByPassableLand(data, ter, territory) && DUtils.GetJumpsFromXToY_PassableLand(data, ter, territory) <= maxJumpDist)
					{
						return true;
					}
				}
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsOwnedByEnemy(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				// Note that first block was added so that water is not considered 'enemy' territory (technically 'neutral', but is really ownerless)
				if (!(t.isWater() && t.getOwner().isNull()) && data.getRelationshipTracker().isAtWar(player, t.getOwner()))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsOwnedByNNEnemy(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (!t.getOwner().isNull() && data.getRelationshipTracker().isAtWar(player, t.getOwner()))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsOwnedBy(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getOwner().getName().equals(player.getName());
			}
		};
	}
	
	public static final Match<Territory> TerritoryIsLandAndPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
	
	public static Match<Territory> territoryIsLandAndPassableTo(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				final GameData data = player.getData();
				if (Matches.TerritoryIsImpassable.match(ter))
					return false;
				if (!Properties.getMovementByTerritoryRestricted(data))
					return true;
				final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
				if (ra == null || ra.getMovementRestrictionTerritories() == null)
					return true;
				final String movementRestrictionType = ra.getMovementRestrictionType();
				final Collection<Territory> listedTerritories = ra.getListedTerritories(ra.getMovementRestrictionTerritories());
				return (movementRestrictionType.equals("allowed") == listedTerritories.contains(ter));
			}
		};
	}
	
	public static Match<Territory> territoryIsWaterAndPassableTo(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (!ter.isWater())
					return false;
				final GameData data = player.getData();
				for (final CanalAttachment attachment : CanalAttachment.get(ter))
				{
					if (attachment == null)
						continue;
					for (final Territory borderTerritory : attachment.getLandTerritories())
					{
						if (!data.getRelationshipTracker().isAllied(player, borderTerritory.getOwner()))
							return false;
						if (MoveDelegate.getBattleTracker(data).wasConquered(borderTerritory))
							return false;
					}
				}
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryIsLandAndOwnedBy(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				return !ter.isWater() && player.equals(ter.getOwner());
			}
		};
	}
	
	public static Match<Territory> territoryIsOwnedByXOrAlly(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return data.getRelationshipTracker().isAllied(player, t.getOwner());
			}
		};
	}
	
	public static Match<Territory> territoryMatchesDMatch(final GameData data, final PlayerID player, final Match<TerritoryStatus> DMatch)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				return DMatch.match(StatusCenter.get(data, player).GetStatusOfTerritory(ter));
			}
		};
	}
	
	public static Match<Territory> territoryHasNNEnemyLandUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(DUtils.CompMatchAnd(DMatches.unitIsNNEnemyOf(data, player), Matches.UnitIsLand));
			}
		};
	}
	
	public static Match<Territory> territoryContainsMultipleAlliances(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				PlayerID lastPlayer = null;
				for (final Unit unit : t.getUnits())
				{
					if (lastPlayer == null)
						lastPlayer = unit.getOwner();
					if (!data.getRelationshipTracker().isAllied(lastPlayer, unit.getOwner()))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Territory> territoryIsCapital = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory ter)
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(ter);
			if (ta != null && ta.isCapital())
				return true;
			return false;
		}
	};
	
	public static Match<Territory> territoryIsCapitalAndOwnedBy(final GameData data, final PlayerID player)
	{
		return DUtils.CompMatchAnd(territoryIsCapital, Matches.isTerritoryOwnedBy(player));
	}
	
	public static Match<Territory> territoryIsCapitalAndOwnedByEnemy(final GameData data, final PlayerID player)
	{
		return DUtils.CompMatchAnd(territoryIsCapital, Matches.isTerritoryEnemy(player, data));
	}
	
	public static Match<Territory> territoryCanHaveUnitsPlacedOnIt(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (ter.isWater())
					return false;
				if (!ter.getOwner().equals(player))
					return false;
				if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
					return false;
				if (ter.getUnits().someMatch(Matches.UnitIsFactory))
					return true;
				// Special placement rules for China on ww2v3, etc.
				if (DUtils.CanPlayerPlaceAnywhere(data, player))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsIsolated(final GameData data)
	{
		return territoryIsNotIsolated(data).invert();
	}
	
	public static Match<Territory> territoryIsNotIsolated(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (data.getMap().getNeighbors(ter).isEmpty())
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryIsOnSmallIsland(final GameData data)
	{
		return territoryIsNotOnSmallIsland(data).invert();
	}
	
	public static Match<Territory> territoryIsNotOnSmallIsland(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (data.getMap().getNeighbors(ter, Matches.TerritoryIsLand).isEmpty())
					return false; // If we have no land neighbors, we're obviously a small island
				final List<Territory> nearbyTersOnContinent = DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, ter, 2, Matches.TerritoryIsLand, Matches.TerritoryIsLand);
				nearbyTersOnContinent.remove(ter);
				nearbyTersOnContinent.removeAll(data.getMap().getNeighbors(ter));
				if (nearbyTersOnContinent.isEmpty())
					return false; // We're touching all nearby ters on continent, so we're on a small island
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasRouteMatchingXToTerritoryMatchingY(final GameData data, final Match<Territory> routeMatch, final Match<Territory> targetMatch)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				final List<Territory> tersMatchingYWithRouteMatchingX = DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, ter, Integer.MAX_VALUE, targetMatch, routeMatch);
				if (tersMatchingYWithRouteMatchingX.isEmpty())
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasUnitsMatchingXThatCanReach(final PlayerID player, final GameData data, final Match<Territory> terMatch, final Match<Unit> unitMatch)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return DUtils.GetNUnitsMatchingXThatCanReach(data, t, terMatch, unitMatch, 1).size() >= 1;
			}
		};
	}
	
	public static Match<Territory> territoryHasNNEnemyUnitsThatCanReach(final PlayerID player, final GameData data, final Match<Territory> terMatch)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return DUtils.GetNNNEnemyUnitsThatCanReach(data, t, player, terMatch, 1).size() >= 1;
			}
		};
	}
	
	public static Match<Territory> territoryIsConsideredSafeToNCMInto(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (ter.isWater())
					return false;
				if (StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasRetreatedFrom)
					return false;
				final boolean hasAttackers = DUtils.GetNNNEnemyLUnitsThatCanReach(data, ter, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand, 1).size() >= 1;
				// If there are land attackers and we haven't reinforced this ter, we dont want to move here (since it wasn't retreated from, this must be a ter that has been made vulnerable by an ncm move this turn)
				if (hasAttackers
							&& !DUtils.CompMatchOr(DMatches.TS_WasReinforced_Frontline, DMatches.TS_WasReinforced_Stabalize).match(
										StatusCenter.get(data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(ter)))
					return false;
				return true;
			}
		};
	}
	
	// /////////////////////////////////////////////End Territory Matches///////////////////////////////////////////////
	// /////////////////////////////////////////////Territory Status Matches///////////////////////////////////////////////
	public static final Match<TerritoryStatus> TS_WasAttacked = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasAttacked();
		}
	};
	public static final Match<TerritoryStatus> TS_WasAttacked_LandGrab = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasAttacked_LandGrab;
		}
	};
	public static final Match<TerritoryStatus> TS_WasAttacked_Stabalize = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasAttacked_Stabalize;
		}
	};
	public static final Match<TerritoryStatus> TS_WasAttacked_Offensive = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasAttacked_Offensive;
		}
	};
	public static final Match<TerritoryStatus> TS_WasAttacked_Trade = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasAttacked_Trade;
		}
	};
	public static final Match<TerritoryStatus> TS_WasReinforced = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasReinforced();
		}
	};
	public static final Match<TerritoryStatus> TS_WasReinforced_Block = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasReinforced_Block;
		}
	};
	public static final Match<TerritoryStatus> TS_WasReinforced_Stabalize = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasReinforced_Stabalize;
		}
	};
	public static final Match<TerritoryStatus> TS_WasReinforced_Frontline = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasReinforced_Frontline;
		}
	};
	public static final Match<TerritoryStatus> TS_WasRetreatedFrom = new Match<TerritoryStatus>()
	{
		@Override
		public boolean match(final TerritoryStatus ts)
		{
			return ts.WasRetreatedFrom;
		}
	};
	// /////////////////////////////////////////////End Territory Status Matches///////////////////////////////////////////////
}
