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
/*
 * Matches.java
 * 
 * Created on November 8, 2001, 4:29 PM
 * 
 * @version $LastChangedDate$
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Useful match interfaces.
 * 
 * Rather than writing code like,
 * 
 * 
 * <pre>
 * boolean hasLand = false;
 * Iterator iter = someCollection.iterator();
 * while (iter.hasNext())
 * {
 * 	Unit unit = (Unit) iter.next();
 * 	UnitAttachment ua = UnitAttachment.get(unit.getType());
 * 	if (ua.isAir)
 * 	{
 * 		hasAir = true;
 * 		break;
 * 	}
 * }
 * 
 * </pre>
 * 
 * You can write code like,
 * 
 * boolean hasLand = Match.someMatch(someCollection, Matches.UnitIsAir);
 * 
 * 
 * The benefits should be obvious to any right minded person.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class Matches
{
	public static final Match<Object> IsTerritory = new Match<Object>()
	{
		@Override
		public boolean match(final Object o)
		{
			return o != null && o instanceof Territory;
		}
	};
	public static final Match<Unit> UnitIsTwoHit = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.isTwoHit();
		}
	};
	public static final Match<Unit> UnitIsDamaged = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit.getHits() > 0;
		}
	};
	public static final Match<Unit> UnitIsNotDamaged = new InverseMatch<Unit>(UnitIsDamaged);
	public static final Match<Unit> UnitIsSea = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsSea();
		}
	};
	public static final Match<Unit> UnitIsSub = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsSub();
		}
	};
	public static final Match<Unit> UnitIsNotSub = new InverseMatch<Unit>(UnitIsSub);
	public static final Match<Unit> UnitIsCombatTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (ua.getIsCombatTransport() && ua.getIsSea());
		}
	};
	public static final Match<Unit> UnitIsNotCombatTransport = new InverseMatch<Unit>(UnitIsCombatTransport);
	public static final Match<Unit> UnitIsTransportButNotCombatTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (ua.getTransportCapacity() != -1 && ua.getIsSea() && !ua.getIsCombatTransport());
		}
	};
	public static final Match<Unit> UnitIsNotTransportButCouldBeCombatTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.getTransportCapacity() == -1)
				return true;
			else if (ua.getIsCombatTransport() && ua.getIsSea())
				return true;
			else
				return false;
		}
	};
	public static final Match<Unit> UnitCanMove = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit u)
		{
			return UnitAttachment.get(u.getType()).getMovement(u.getOwner()) > 0;
		}
	};
	public static final Match<Unit> UnitIsDestroyer = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsDestroyer();
		}
	};
	public static final Match<UnitType> UnitTypeIsDestroyer = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType type)
		{
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsDestroyer();
		}
	};
	public static final Match<Unit> UnitIsBB = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (!ua.getIsSea())
				return false;
			return (ua.isTwoHit());
		}
	};
	public static final Match<Unit> UnitIsRadarAA = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			if (!UnitIsAAforAnything.match(unit))
				return false;
			final TechAttachment ta = (TechAttachment) unit.getOwner().getAttachment(Constants.TECH_ATTACHMENT_NAME);
			if (ta == null)
				return false;
			return ta.hasAARadar();
		}
	};
	public static final Match<Unit> UnitIsTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (ua.getTransportCapacity() != -1 && ua.getIsSea());
		}
	};
	public static final Match<Unit> UnitIsNotTransport = UnitIsTransport.invert();
	public static final Match<Unit> UnitIsTransportAndNotDestroyer = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (!Matches.UnitIsDestroyer.match(unit) && ua.getTransportCapacity() != -1 && ua.getIsSea());
		}
	};
	public static final Match<Unit> UnitIsStrategicBomber = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return ua.getIsStrategicBomber();
		}
	};
	public static final Match<Unit> UnitIsNotStrategicBomber = new InverseMatch<Unit>(UnitIsStrategicBomber);
	public static final Match<UnitType> UnitTypeCanLandOnCarrier = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitAttachment ua = UnitAttachment.get(obj);
			if (ua == null)
				return false;
			return ua.getCarrierCost() != -1;
		}
	};
	public static final Match<UnitType> UnitTypeCannotLandOnCarrier = new InverseMatch<UnitType>(UnitTypeCanLandOnCarrier);
	public static final Match<Unit> unitHasMoved = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			return TripleAUnit.get(unit).getAlreadyMoved() > 0;
		}
	};
	public static final Match<Unit> unitHasNotMoved = new InverseMatch<Unit>(unitHasMoved);
	
	public static Match<Unit> unitCanAttack(final PlayerID id)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				if (ua.getMovement(id) <= 0)
					return false;
				return ua.getAttack(id) > 0;
			}
		};
	}
	
	public static Match<UnitType> unitTypeIsStatic(final PlayerID id)
	{
		return new Match<UnitType>()
		{
			@Override
			public boolean match(final UnitType uT)
			{
				final UnitAttachment ua = UnitAttachment.get(uT);
				return ua.getMovement(id) <= 0;
			}
		};
	}
	
	public static Match<UnitType> unitTypeCanAttack(final PlayerID id)
	{
		return new Match<UnitType>()
		{
			@Override
			public boolean match(final UnitType uT)
			{
				final UnitAttachment ua = UnitAttachment.get(uT);
				if (ua.getMovement(id) <= 0)
					return false;
				return ua.getAttack(id) > 0;
			}
		};
	}
	
	public static Match<Unit> unitHasAttackValueOfAtLeast(final int attackValue)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getAttack(unit.getOwner()) >= attackValue;
			}
		};
	}
	
	public static Match<Unit> unitHasDefendValueOfAtLeast(final int defendValue)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getDefense(unit.getOwner()) >= defendValue;
			}
		};
	}
	
	public static Match<Unit> unitIsEnemyOf(final GameData data, final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return data.getRelationshipTracker().isAtWar(u.getOwner(), player);
			}
		};
	}
	
	public static final Match<Unit> UnitIsNotSea = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return !ua.getIsSea();
		}
	};
	public static final Match<UnitType> UnitTypeIsSea = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitAttachment ua = UnitAttachment.get(obj);
			return ua.getIsSea();
		}
	};
	public static final Match<UnitType> UnitTypeIsNotSea = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType type)
		{
			final UnitAttachment ua = UnitAttachment.get(type);
			return !ua.getIsSea();
		}
	};
	public static final Match<UnitType> UnitTypeIsSeaOrAir = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType type)
		{
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsSea() || ua.getIsAir();
		}
	};
	public static final Match<UnitType> UnitTypeIsCarrier = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType type)
		{
			final UnitAttachment ua = UnitAttachment.get(type);
			return (ua.getCarrierCapacity() != -1);
		}
	};
	public static final Match<Unit> UnitIsAir = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsAir();
		}
	};
	public static final Match<Unit> UnitIsNotAir = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return !ua.getIsAir();
		}
	};
	
	public static Match<UnitType> unitTypeCanBombard(final PlayerID id)
	{
		return new Match<UnitType>()
		{
			@Override
			public boolean match(final UnitType type)
			{
				final UnitAttachment ua = UnitAttachment.get(type);
				return ua.getCanBombard(id);
			}
		};
	}
	
	public static Match<Unit> UnitCanBeGivenByTerritoryTo(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				final Unit unit = o;
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCanBeGivenByTerritoryTo().contains(player);
			}
		};
	}
	
	public static Match<Unit> UnitCanBeCapturedOnEnteringToInThisTerritory(final PlayerID player, final Territory terr, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				if (!games.strategy.triplea.Properties.getCaptureUnitsOnEnteringTerritory(data))
					return false;
				final Unit unit = o;
				final PlayerID unitOwner = unit.getOwner();
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				final boolean unitCanBeCapturedByPlayer = ua.getCanBeCapturedOnEnteringBy().contains(player);
				final TerritoryAttachment ta = TerritoryAttachment.get(terr);
				if (ta == null)
					return false;
				if (ta.getCaptureUnitOnEnteringBy() == null)
					return false;
				final boolean territoryCanHaveUnitsThatCanBeCapturedByPlayer = ta.getCaptureUnitOnEnteringBy().contains(player);
				final PlayerAttachment pa = PlayerAttachment.get(unitOwner);
				if (pa == null)
					return false;
				if (pa.getCaptureUnitOnEnteringBy() == null)
					return false;
				final boolean unitOwnerCanLetUnitsBeCapturedByPlayer = pa.getCaptureUnitOnEnteringBy().contains(player);
				return (unitCanBeCapturedByPlayer && territoryCanHaveUnitsThatCanBeCapturedByPlayer && unitOwnerCanLetUnitsBeCapturedByPlayer);
			}
		};
	}
	
	public static Match<Unit> UnitDestroyedWhenCapturedByOrFrom(final PlayerID playerBY)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				final Match<Unit> byOrFrom = new CompositeMatchOr<Unit>(UnitDestroyedWhenCapturedBy(playerBY), UnitDestroyedWhenCapturedFrom());
				return byOrFrom.match(o);
			}
		};
	}
	
	public static Match<Unit> UnitDestroyedWhenCapturedBy(final PlayerID playerBY)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				final UnitAttachment ua = UnitAttachment.get(u.getType());
				if (ua.getDestroyedWhenCapturedBy().isEmpty())
					return false;
				for (final Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy())
				{
					if (tuple.getFirst().equals("BY") && tuple.getSecond().equals(playerBY))
						return true;
				}
				return false;
			}
		};
	}
	
	public static Match<Unit> UnitDestroyedWhenCapturedFrom()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				final UnitAttachment ua = UnitAttachment.get(u.getType());
				if (ua.getDestroyedWhenCapturedBy().isEmpty())
					return false;
				for (final Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy())
				{
					if (tuple.getFirst().equals("FROM") && tuple.getSecond().equals(u.getOwner()))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitIsAirBase = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsAirBase();
		}
	};
	/**
	 * Factories are bombable/rocketable already.
	 * Use a CompositeMatchOr to find factories + canBeDamaged (Matches.UnitIsFactoryOrCanBeDamaged)
	 */
	public static final Match<Unit> UnitCanBeDamagedButIsNotFactory = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCanBeDamaged() && !ua.getIsFactory();
		}
	};
	
	public static Match<Unit> UnitIsAtMaxDamageOrNotCanBeDamaged(final Territory t)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				if (!ua.getCanBeDamaged() && !ua.getIsFactory())
					return true;
				if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(unit.getData()))
				{
					final TerritoryAttachment ta = TerritoryAttachment.get(t);
					final int currentDamage = ta.getProduction() - ta.getUnitProduction();
					return currentDamage >= 2 * ta.getProduction();
				}
				else if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData()))
				{
					final TripleAUnit taUnit = (TripleAUnit) unit;
					return taUnit.getUnitDamage() >= taUnit.getHowMuchDamageCanThisUnitTakeTotal(unit, t);
				}
				else
					return false;
			}
		};
	}
	
	public static Match<Unit> UnitHasSomeUnitDamage()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final TripleAUnit taUnit = (TripleAUnit) unit;
				return taUnit.getUnitDamage() > 0;
			}
		};
	}
	
	public static Match<Unit> UnitIsDisabled()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				if (!UnitIsFactoryOrCanBeDamaged.match(unit))
					return false;
				if (!games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData())
							|| games.strategy.triplea.Properties.getSBRAffectsUnitProduction(unit.getData()))
					return false;
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				final TripleAUnit taUnit = (TripleAUnit) unit;
				if (ua.getMaxOperationalDamage() < 0)
				{
					// factories may or may not have max operational damage set, so we must still determine here
					// assume that if maxOperationalDamage < 0, then the max damage must be based on the territory value (if the damage >= production of territory, then we are disabled)
					// TerritoryAttachment ta = TerritoryAttachment.get(t);
					// return taUnit.getUnitDamage() >= ta.getProduction();
					return false;
				}
				return taUnit.getUnitDamage() > ua.getMaxOperationalDamage(); // only greater than. if == then we can still operate
			}
		};
	}
	
	public static final Match<Unit> UnitCanDieFromReachingMaxDamage = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (!ua.getCanBeDamaged() && !ua.getIsFactory())
				return false;
			return ua.getCanDieFromReachingMaxDamage();
		}
	};
	public static final Match<Unit> UnitIsInfrastructure = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsInfrastructure();
		}
	};
	
	/**
	 * Checks for having attack/defense and for providing support. Does not check for having AA ability.
	 * 
	 * @param attack
	 * @param player
	 * @param data
	 * @return
	 */
	public static final Match<Unit> UnitIsSupporterOrHasCombatAbility(final boolean attack, final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				// if unit has attack or defense, return true
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				if (attack && ua.getAttack(player) > 0)
					return true;
				if (!attack && ua.getDefense(player) > 0)
					return true;
				// if unit can support other units, return true
				for (final UnitSupportAttachment rule : UnitSupportAttachment.get(data))
				{
					if (unit.getType().equals(rule.getAttachedTo()))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitCanScramble = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCanScramble();
		}
	};
	public static final Match<Unit> UnitWasScrambled = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasScrambled();
		}
	};
	public static final Match<Unit> UnitWasInAirBattle = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasInAirBattle();
		}
	};
	
	public static final Match<Territory> TerritoryIsIsland = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
			if (neighbors.size() == 1 && TerritoryIsWater.match(neighbors.iterator().next()))
				return true;
			return false;
		}
	};
	
	public static Match<Unit> unitCanBombard(final PlayerID id)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCanBombard(id);
			}
		};
	}
	
	public static final Match<Unit> UnitCanBlitz = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCanBlitz();
		}
	};
	public static final Match<Unit> UnitIsLandTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsLandTransport();
		}
	};
	
	public static final Match<Unit> UnitIsDestructibleInCombat(final PlayerID player, final Territory terr, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit obj)
			{
				final Unit unit = obj;
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return !ua.getIsFactory() && !ua.getIsInfrastructure() && !UnitCanBeCapturedOnEnteringToInThisTerritory(player, terr, data).match(unit);
			}
		};
	}
	
	public static final Match<Unit> UnitIsDestructibleInCombatShort = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return !ua.getIsFactory() && !ua.getIsInfrastructure();
		}
	};
	public static final Match<Unit> UnitIsSuicide = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsSuicide();
		}
	};
	public static final Match<Unit> UnitIsKamikaze = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsKamikaze();
		}
	};
	public static final Match<UnitType> UnitTypeIsAir = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAir();
		}
	};
	public static final Match<UnitType> UnitTypeIsNotAir = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return !ua.getIsAir();
		}
	};
	public static final Match<Unit> UnitCanLandOnCarrier = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCarrierCost() != -1;
		}
	};
	public static final Match<Unit> UnitIsCarrier = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCarrierCapacity() != -1;
		}
	};
	
	public static final Match<Unit> UnitIsAlliedCarrier(final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit obj)
			{
				final Unit unit = obj;
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCarrierCapacity() != -1 && data.getRelationshipTracker().isAllied(player, obj.getOwner());
			}
		};
	}
	
	public static final Match<Unit> UnitCanBeTransported = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getTransportCost() != -1;
		}
	};
	public static final Match<Unit> UnitCanNotBeTransported = new InverseMatch<Unit>(UnitCanBeTransported);
	public static final Match<Unit> UnitWasAmphibious = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasAmphibious();
		}
	};
	public static final Match<Unit> UnitWasNotAmphibious = new InverseMatch<Unit>(UnitWasAmphibious);
	public static final Match<Unit> UnitWasInCombat = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasInCombat();
		}
	};
	public static final Match<Unit> UnitWasUnloadedThisTurn = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getUnloadedTo() != null;
		}
	};
	public static final Match<Unit> UnitWasLoadedThisTurn = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasLoadedThisTurn();
		}
	};
	public static final Match<Unit> UnitWasNotLoadedThisTurn = new InverseMatch<Unit>(UnitWasLoadedThisTurn);
	public static final Match<Unit> UnitCanTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getTransportCapacity() != -1;
		}
	};
	public static final Match<UnitType> UnitTypeCanTransport = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getTransportCapacity() != -1;
		}
	};
	public static final Match<UnitType> UnitTypeCanBeTransported = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getTransportCost() != -1;
		}
	};
	public static final Match<UnitType> UnitTypeIsFactory = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsFactory();
		}
	};
	public static final Match<UnitType> UnitTypeCanProduceUnits = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getCanProduceUnits();
		}
	};
	public static final Match<UnitType> UnitTypeIsFactoryOrIsInfrastructure = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsFactory() || ua.getIsInfrastructure();
		}
	};
	public static final Match<UnitType> UnitTypeIsFactoryOrIsInfrastructureButNotAAofAnyKind = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitAttachment ua = UnitAttachment.get(obj);
			return (ua.getIsFactory() || ua.getIsInfrastructure()) && !UnitTypeIsAAforAnything.match(obj);
		}
	};
	public static final Match<UnitType> UnitTypeIsInfantry = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.isInfantry();
		}
	};
	public static final Match<UnitType> UnitTypeIsArtillery = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getArtillery();
		}
	};
	public static final Match<UnitType> UnitTypeHasMaxBuildRestrictions = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getMaxBuiltPerPlayer() >= 0;
		}
	};
	public static final Match<Unit> UnitIsFactory = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsFactory();
		}
	};
	public static final Match<Unit> UnitIsNotFactory = new InverseMatch<Unit>(UnitIsFactory);
	public static final Match<Unit> UnitCanProduceUnits = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getCanProduceUnits();
		}
	};
	public static final Match<UnitType> UnitTypeIsAAforAnything = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitAttachment ua = UnitAttachment.get(obj);
			return ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly() || ua.getIsAAforFlyOverOnly();
		}
	};
	public static final Match<Unit> UnitIsRocket = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsRocket();
		}
	};
	public static final Match<Unit> UnitHasStackingLimit = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getStackingLimit() != null;
		}
	};
	public static final Match<Unit> UnitCanNotMoveDuringCombatMove = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getCanNotMoveDuringCombatMove();
		}
	};
	
	public static final Match<Unit> UnitIsAAthatCanHitTheseUnits(final Collection<Unit> targets, final Match<Unit> typeOfAA)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit obj)
			{
				if (!typeOfAA.match(obj))
					return false;
				final UnitAttachment ua = UnitAttachment.get(obj.getType());
				final Set<UnitType> targetsAA = ua.getTargetsAA(obj.getData());
				for (final Unit u : targets)
				{
					if (targetsAA.contains(u.getType()))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitIsAAofTypeAA(final String typeAA)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit obj)
			{
				return UnitAttachment.get(obj.getType()).getTypeAA().matches(typeAA);
			}
		};
	}
	
	public static final Match<Unit> UnitIsAAthatWillNotFireIfPresentEnemyUnits(final Collection<Unit> enemyUnitsPresent)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit obj)
			{
				for (final Unit u : enemyUnitsPresent)
				{
					if (UnitAttachment.get(obj.getType()).getWillNotFireIfPresent().contains(u.getType()))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitIsAAthatCanFire(final Collection<Unit> unitsMovingOrAttacking, final PlayerID playerMovingOrAttacking, final Match<Unit> typeOfAA, final GameData data)
	{
		return new CompositeMatchAnd<Unit>(
					Matches.enemyUnit(playerMovingOrAttacking, data),
					Matches.unitIsBeingTransported().invert(),
					Matches.UnitIsAAthatCanHitTheseUnits(unitsMovingOrAttacking, typeOfAA),
					Matches.UnitIsAAthatWillNotFireIfPresentEnemyUnits(unitsMovingOrAttacking).invert());
	}
	
	public static final Match<Unit> UnitIsAAforCombatOnly = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforCombatOnly();
		}
	};
	public static final Match<Unit> UnitIsAAforBombingThisUnitOnly = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforBombingThisUnitOnly();
		}
	};
	public static final Match<Unit> UnitIsAAforFlyOverOnly = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforFlyOverOnly();
		}
	};
	public static final Match<Unit> UnitIsAAforAnything = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			return UnitTypeIsAAforAnything.match(obj.getType());
		}
	};
	public static final Match<Unit> UnitIsNotAA = new InverseMatch<Unit>(UnitIsAAforAnything);
	public static final Match<Unit> UnitIsFactoryOrIsInfrastructure = new CompositeMatchOr<Unit>(UnitIsFactory, UnitIsInfrastructure);
	
	public static final Match<Unit> UnitIsInfantry = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.isInfantry();
		}
	};
	public static final Match<Unit> UnitIsNotInfantry = new InverseMatch<Unit>(UnitIsInfantry);
	public static final Match<Unit> UnitIsMarine = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.isMarine();
		}
	};
	public static final Match<Unit> UnitIsNotMarine = new InverseMatch<Unit>(UnitIsMarine);
	public static final Match<Unit> UnitIsAirTransportable = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TechAttachment ta = TechAttachment.get(obj.getOwner());
			if (ta == null || !ta.hasParatroopers())
			{
				return false;
			}
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAirTransportable();
		}
	};
	public static final Match<Unit> UnitIsNotAirTransportable = new InverseMatch<Unit>(UnitIsAirTransportable);
	public static final Match<Unit> UnitIsAirTransport = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final TechAttachment ta = TechAttachment.get(obj.getOwner());
			if (ta == null || !ta.hasParatroopers())
			{
				return false;
			}
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAirTransport();
		}
	};
	public static final Match<Unit> UnitIsNotAirTransport = new InverseMatch<Unit>(UnitIsAirTransport);
	public static final Match<Unit> UnitIsArtillery = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getArtillery();
		}
	};
	public static final Match<Unit> UnitIsArtillerySupportable = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final UnitType type = obj.getUnitType();
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getArtillerySupportable();
		}
	};
	// TODO: CHECK whether this makes any sense
	public static final Match<Territory> TerritoryIsLandOrWater = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			return t != null && t instanceof Territory;
		}
	};
	public static final Match<Territory> TerritoryIsWater = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			return t.isWater();
		}
	};
	public static final Match<Territory> TerritoryIsVictoryCity = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta == null)
				return false;
			return ta.getVictoryCity();
		}
	};
	public static final Match<Territory> TerritoryHasSomeDamage = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta == null)
				return false;
			return ta.getUnitProduction() < ta.getProduction();
		}
	};
	
	public static Match<Unit> unitIsInTerritoryThatHasTerritoryDamage(final Territory t)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return TerritoryHasSomeDamage.match(t);
			}
		};
	}
	
	public static final Match<Territory> TerritoryIsLand = new InverseMatch<Territory>(TerritoryIsWater);
	public static final Match<Territory> TerritoryIsEmpty = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			return t.getUnits().size() == 0;
		}
	};
	
	/**
	 * Tests for Convoys and unowned water.
	 * Assumes player is either the owner of the territory we are testing, or about to become the owner.
	 * 
	 * @param player
	 * @param data
	 * @return
	 */
	public static Match<Territory> territoryCanCollectIncomeFrom(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(t);
				if (ta == null)
					return false;
				final OriginalOwnerTracker origOwnerTracker = new OriginalOwnerTracker();
				PlayerID origOwner = ta.getOccupiedTerrOf();
				if (origOwner == null)
					origOwner = origOwnerTracker.getOriginalOwner(t);
				if (t.isWater())
				{
					// if it's water, it is a Convoy Center
					// Can't get PUs for capturing a CC, only original owner can get them. (Except capturing null player CCs)
					if (!(origOwner == null || origOwner == PlayerID.NULL_PLAYERID || origOwner == player))
						return false;
				}
				if (ta.getConvoyRoute() && !ta.getConvoyAttached().isEmpty())
				{
					// Determine if at least one part of the convoy route is owned by us or an ally
					boolean atLeastOne = false;
					for (final Territory convoy : ta.getConvoyAttached())
					{
						if (data.getRelationshipTracker().isAllied(convoy.getOwner(), player) && TerritoryAttachment.get(convoy).getConvoyRoute())
							atLeastOne = true;
					}
					if (!atLeastOne)
						return false;
				}
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyLandNeighbor(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				// This method will still return true if territory t is an impassible or restricted territory With enemy neighbors. Makes sure your AI does not include any impassible or restricted territories by using this:
				// CompositeMatch<Territory> territoryHasEnemyLandNeighborAndIsNotImpassibleOrRestricted = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player), Matches.territoryHasEnemyLandNeighbor(data, player));
				final CompositeMatch<Territory> condition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
				if (data.getMap().getNeighbors(t, condition).size() > 0)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> TerritoryHasOwnedDestroyer(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final CompositeMatch<Unit> destroyerUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.unitIsOwnedBy(player));
				if (Matches.TerritoryIsWater.match(t) && t.getUnits().someMatch(destroyerUnit))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasAlliedFactoryNeighbor(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (data.getMap().getNeighbors(t, Matches.territoryHasAlliedFactory(data, player)).size() > 0)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasValidLandRouteTo(final GameData data, final Territory goTerr)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final CompositeMatch<Territory> validLandRoute = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
				if (data.getMap().getRoute(t, goTerr, validLandRoute) != null)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsInList(final Collection<Territory> list)
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
	
	public static Match<Territory> territoryIsNotInList(final Collection<Territory> list)
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
	
	/**
	 * @param data
	 *            game data
	 * @param player
	 * @return Match<Territory> that tests if there is a route to an enemy capital from the given territory
	 */
	public static Match<Territory> territoryHasRouteToEnemyCapital(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				for (final PlayerID ePlayer : data.getPlayerList().getPlayers())
				{
					final List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
					for (final Territory current : capitalsListOwned)
					{
						if (!data.getRelationshipTracker().isAtWar(player, current.getOwner()))
							continue;
						if (data.getMap().getDistance(t, current, Matches.TerritoryIsPassableAndNotRestricted(player, data)) != -1)
							return true;
					}
				}
				return false;
			}
		};
	}
	
	/**
	 * @param data
	 *            game data
	 * @param player
	 * @return true only if the route is land
	 */
	public static Match<Territory> territoryHasLandRouteToEnemyCapital(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				for (final PlayerID ePlayer : data.getPlayerList().getPlayers())
				{
					final List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
					for (final Territory current : capitalsListOwned)
					{
						if (!data.getRelationshipTracker().isAtWar(player, current.getOwner()))
							continue;
						if (data.getMap().getDistance(t, current, Matches.TerritoryIsNotImpassableToLandUnits(player, data)) != -1)
							return true;
					}
				}
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyFactoryNeighbor(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (data.getMap().getNeighbors(t, Matches.territoryHasEnemyFactory(data, player)).size() > 0)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasOwnedFactoryNeighbor(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (data.getMap().getNeighbors(t, Matches.territoryHasOwnedFactory(data, player)).size() > 0)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasOwnedIsFactoryOrCanProduceUnitsNeighbor(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (data.getMap().getNeighbors(t, Matches.territoryHasOwnedIsFactoryOrCanProduceUnits(data, player)).size() > 0)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasWaterNeighbor(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (data.getMap().getNeighbors(t, Matches.TerritoryIsWater).size() > 0)
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasAlliedFactory(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (!data.getRelationshipTracker().isAllied(t.getOwner(), player))
					return false;
				if (!t.getUnits().someMatch(Matches.UnitIsFactory))
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasOwnedFactory(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (!t.getOwner().equals(player))
					return false;
				if (!t.getUnits().someMatch(Matches.UnitIsFactory))
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (!t.getOwner().equals(player))
					return false;
				if (!t.getUnits().someMatch(Matches.UnitIsFactoryOrCanProduceUnits))
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasAlliedIsFactoryOrCanProduceUnits(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (!isTerritoryAllied(player, data).match(t))
					return false;
				if (!t.getUnits().someMatch(Matches.UnitIsFactoryOrCanProduceUnits))
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyFactory(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (!data.getRelationshipTracker().isAtWar(player, t.getOwner()))
					return false;
				if (t.getOwner().isNull())
					return false;
				if (!t.getUnits().someMatch(Matches.UnitIsFactory))
					return false;
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final CompositeMatch<Unit> nonCom = new CompositeMatchOr<Unit>();
				nonCom.add(UnitIsFactoryOrIsInfrastructure);
				nonCom.add(enemyUnit(player, data).invert());
				// nonCom.add(UnitCanBeCapturedOnEnteringToInThisTerritory(player, t, data)); //this is causing issues where the newly captured units fight against themselves
				return t.getUnits().allMatch(nonCom);
			}
		};
	}
	
	public static Match<Territory> TerritoryHasProductionValueAtLeast(final int prodVal)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.isWater())
					return false;
				final int terrProd = TerritoryAttachment.get(t).getProduction();
				if (terrProd >= prodVal)
					return true;
				return false;
			}
		};
	}
	
	public static final Match<Territory> TerritoryIsNeutral = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			if (t.isWater())
				return false;
			return t.getOwner().equals(PlayerID.NULL_PLAYERID);
		}
	};
	public final static Match<Territory> TerritoryIsNotNeutral = new InverseMatch<Territory>(TerritoryIsNeutral);
	public static final Match<Territory> TerritoryIsImpassable = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			if (t.isWater())
			{
				return false;
			}
			return TerritoryAttachment.get(t).getIsImpassible();
		}
	};
	public final static Match<Territory> TerritoryIsNotImpassable = new InverseMatch<Territory>(TerritoryIsImpassable);
	
	public static final Match<Territory> TerritoryIsPassableAndNotRestricted(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (Matches.TerritoryIsImpassable.match(t))
					return false;
				if (!Properties.getMovementByTerritoryRestricted(data))
					return true;
				final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
				if (ra == null || ra.getMovementRestrictionTerritories() == null)
					return true;
				final String movementRestrictionType = ra.getMovementRestrictionType();
				final Collection<Territory> listedTerritories = ra.getListedTerritories(ra.getMovementRestrictionTerritories());
				return (movementRestrictionType.equals("allowed") == listedTerritories.contains(t));
			}
		};
	}
	
	public final static Match<Territory> TerritoryIsImpassableToLandUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.isWater())
					return true;
				else if (Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(t))
					return true;
				return false;
			}
		};
	}
	
	public final static Match<Territory> TerritoryIsNotImpassableToLandUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return TerritoryIsImpassableToLandUnits(player, data).invert().match(t);
			}
		};
	}
	
	public static final Match<IBattle> BattleIsEmpty = new Match<IBattle>()
	{
		@Override
		public boolean match(final IBattle battle)
		{
			return battle.isEmpty();
		}
	};
	public static final Match<IBattle> BattleIsAmphibious = new Match<IBattle>()
	{
		@Override
		public boolean match(final IBattle battle)
		{
			return battle.isAmphibious();
		}
	};
	
	/**
	 * @param lowerLimit
	 *            lower limit for movement
	 * @param movement
	 *            referring movement
	 * @deprecated we can't trust on ints to see if we have enough movement, use hasEnnoughMovementforRoute()
	 * @return units match that have at least lower limit movement
	 */
	@Deprecated
	public static Match<Unit> unitHasEnoughMovement(final int lowerLimit, final IntegerMap<Unit> movement)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				return movement.getInt(o) >= lowerLimit;
			}
		};
	}
	
	/**
	 * @deprecated we can't trust on ints to see if we have enough movement, use hasEnnoughMovementforRoute()
	 */
	@Deprecated
	public static Match<Unit> UnitHasEnoughMovement(final int minMovement)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return TripleAUnit.get(unit).getMovementLeft() >= minMovement;
			}
		};
	}
	
	public static Match<Unit> UnitHasEnoughMovementForRoute(final Route route)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				int left = TripleAUnit.get(unit).getMovementLeft();
				int movementcost = route.getMovementCost(unit);
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				final PlayerID player = unit.getOwner();
				TerritoryAttachment taStart = null;
				TerritoryAttachment taEnd = null;
				if (route.getStart() != null)
					taStart = TerritoryAttachment.get(route.getStart());
				if (route.getEnd() != null)
					taEnd = TerritoryAttachment.get(route.getEnd());
				if (ua.getIsAir())
				{
					movementcost = route.getMovementCost(unit);
					if (taStart != null && taStart.getAirBase())
						left++;
					if (taEnd != null && taEnd.getAirBase())
						left++;
				}
				final GameStep stepName = unit.getData().getSequence().getStep();
				if (ua.getIsSea() && stepName.getDisplayName().equals("Non Combat Move"))
				{
					movementcost = route.getMovementCost(unit);
					// If a zone adjacent to the starting and ending sea zones
					// are allied navalbases, increase the range.
					// TODO Still need to be able to handle stops on the way
					// (history to get route.getStart()
					for (final Territory terrNext : unit.getData().getMap().getNeighbors(route.getStart(), 1))
					{
						final TerritoryAttachment taNeighbor = TerritoryAttachment.get(terrNext);
						if (taNeighbor != null && taNeighbor.getNavalBase() && unit.getData().getRelationshipTracker().isAllied(terrNext.getOwner(), player))
						{
							for (final Territory terrEnd : unit.getData().getMap().getNeighbors(route.getEnd(), 1))
							{
								final TerritoryAttachment taEndNeighbor = TerritoryAttachment.get(terrEnd);
								if (taEndNeighbor != null && taEndNeighbor.getNavalBase() && unit.getData().getRelationshipTracker().isAllied(terrEnd.getOwner(), player))
								{
									left++;
									break;
								}
							}
						}
					}
				}
				if (left == -1 || left < movementcost)
					return false;
				return true;
			}
		};
	}
	
	/**
	 * Match units that have at least 1 movement left
	 */
	public final static Match<Unit> unitHasMovementLeft = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit o)
		{
			return TripleAUnit.get(o).getMovementLeft() >= 1;
		}
	};
	
	public final static Match<Unit> UnitIsNotStatic(final PlayerID player)
	{
		return new InverseMatch<Unit>(UnitIsStatic(player));
	}
	
	// match units that have no movement as their attachment, like walls and fortresses (static = zero movement)
	public static final Match<Unit> UnitIsStatic(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit obj)
			{
				final UnitType type = obj.getUnitType();
				final UnitAttachment ua = UnitAttachment.get(type);
				return ua.getMovement(player) < 1;
			}
		};
	}
	
	public static Match<Unit> unitIsLandAndOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return !ua.getIsSea() && !ua.getIsAir() && unit.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> unitIsOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return unit.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> unitIsOwnedByOfAnyOfThesePlayers(final Collection<PlayerID> players)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return players.contains(unit.getOwner());
			}
		};
	}
	
	public static Match<Unit> unitHasDefenseThatIsMoreThanOrEqualTo(final int minDefense)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				// if (ua.isAA() || ua.isFactory() || ua.getIsInfrastructure())
				// return false;
				return ua.getDefense(unit.getOwner()) >= minDefense;
			}
		};
	}
	
	public static Match<Unit> unitIsTransporting()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
				if (transporting == null || transporting.isEmpty())
					return false;
				return true;
			}
		};
	}
	
	public static Match<Unit> unitHasEnoughTransportSpaceLeft(final int spaceNeeded)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
				int loadCost = 0;
				for (final Unit cargo : TripleAUnit.get(unit).getTransporting())
					loadCost += UnitAttachment.get(cargo.getUnitType()).getTransportCost();
				if (ua.getTransportCapacity() - loadCost >= spaceNeeded)
					return true;
				else
					return false;
			}
		};
	}
	
	public static Match<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units)
	{
		final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units);
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
				if (transporting == null)
					return false;
				return Util.someIntersect(UnitSeperator.categorize(transporting), unitCategories);
			}
		};
	}
	
	public static Match<Territory> isTerritoryAllied(final PlayerID player, final GameData data)
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
	
	public static Match<Territory> isTerritoryOwnedBy(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Territory> isTerritoryOwnedBy(final Collection<PlayerID> players)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				for (final PlayerID player : players)
				{
					if (t.getOwner().equals(player))
						return true;
				}
				return false;
			}
		};
	}
	
	public static Match<Unit> isUnitAllied(final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit t)
			{
				return data.getRelationshipTracker().isAllied(player, t.getOwner());
			}
		};
	}
	
	public static Match<Territory> isTerritoryFriendly(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.isWater())
					return true;
				if (t.getOwner().equals(player))
					return true;
				return data.getRelationshipTracker().isAllied(player, t.getOwner());
			}
		};
	}
	
	public static Match<Unit> unitIsEnemyAAforAnything(final PlayerID player, final GameData data)
	{
		final CompositeMatch<Unit> comp = new CompositeMatchAnd<Unit>();
		comp.add(UnitIsAAforAnything);
		comp.add(enemyUnit(player, data));
		return comp;
	}
	
	public static Match<Unit> unitIsEnemyAAforCombat(final PlayerID player, final GameData data)
	{
		final CompositeMatch<Unit> comp = new CompositeMatchAnd<Unit>();
		comp.add(UnitIsAAforCombatOnly);
		comp.add(enemyUnit(player, data));
		return comp;
	}
	
	public static Match<Unit> unitIsEnemyAAforBombing(final PlayerID player, final GameData data)
	{
		final CompositeMatch<Unit> comp = new CompositeMatchAnd<Unit>();
		comp.add(UnitIsAAforBombingThisUnitOnly);
		comp.add(enemyUnit(player, data));
		return comp;
	}
	
	public static Match<Unit> unitIsInTerritory(final Territory territory)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				return territory.getUnits().getUnits().contains(o);
			}
		};
	}
	
	public static Match<Territory> isTerritoryEnemy(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.getOwner().equals(player))
					return false;
				return data.getRelationshipTracker().isAtWar(player, t.getOwner());
			}
		};
	}
	
	public static Match<Territory> isTerritoryEnemyAndNotUnownedWater(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.getOwner().equals(player))
					return false;
				// if we look at territory attachments, may have funny results for blockades or other things that are passable and not owned. better to check them by alliance. (veqryn)
				// OLD code included: if(t.isWater() && t.getOwner().isNull() && TerritoryAttachment.get(t) == null){return false;}
				if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) // this will still return true for enemy and neutral owned convoy zones (water)
					return false;
				return data.getRelationshipTracker().isAtWar(player, t.getOwner());
			}
		};
	}
	
	public static Match<Territory> isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.getOwner().equals(player))
					return false;
				// if we look at territory attachments, may have funny results for blockades or other things that are passable and not owned. better to check them by alliance. (veqryn)
				// OLD code included: if(t.isWater() && t.getOwner().isNull() && TerritoryAttachment.get(t) == null){return false;}
				if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) // this will still return true for enemy and neutral owned convoy zones (water)
					return false;
				if (!Matches.TerritoryIsPassableAndNotRestricted(player, data).match(t))
					return false;
				return data.getRelationshipTracker().isAtWar(player, t.getOwner());
			}
		};
	}
	
	public static Match<Territory> TerritoryIsBlitzable(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				// cant blitz water
				if (t.isWater())
					return false;
				
				// cant blitz on neutrals
				if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && (t.isWater() | !games.strategy.triplea.Properties.getNeutralsBlitzable(data)))
					return false;
				
				// was conquered but not blitzed
				if (MoveDelegate.getBattleTracker(data).wasConquered(t) && !MoveDelegate.getBattleTracker(data).wasBlitzed(t))
					return false;
				
				final CompositeMatch<Unit> blitzableUnits = new CompositeMatchOr<Unit>();
				blitzableUnits.add(Matches.alliedUnit(player, data));
				// WW2V2, cant blitz through factories and aa guns
				// WW2V1, you can
				if (!games.strategy.triplea.Properties.getWW2V2(data) && !games.strategy.triplea.Properties.getBlitzThroughFactoriesAndAARestricted(data))
				{
					blitzableUnits.add(Matches.UnitIsFactoryOrIsInfrastructure);
				}
				if (t.getUnits().allMatch(blitzableUnits))
					return true;
				
				return false;
			}
		};
	}
	
	public static Match<Territory> isTerritoryFreeNeutral(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return (t.getOwner().equals(PlayerID.NULL_PLAYERID) && Properties.getNeutralCharge(data) <= 0);
			}
		};
	}
	
	/*
	    public static Match<Territory> isTerritoryEnemyAndWater(final PlayerID player, final GameData data)
	    {
	        return new Match<Territory>()
	        {
	            public boolean match(Territory t)
	            {
	                if(t.getOwner().equals(player))
	                    return false;
	                if(t.getOwner().equals(PlayerID.NULL_PLAYERID))
	                    return false;
	                return data.getRelationshipTracker().isAtWar(player, t.getOwner());
	            }
	        };
	    }
	*/
	public static Match<Unit> enemyUnit(final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return data.getRelationshipTracker().isAtWar(player, unit.getOwner());
			}
		};
	}
	
	public static Match<Unit> enemyUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				if (data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(unit.getOwner(), players))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Unit> unitOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				final Unit unit = o;
				return unit.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> carrierOwnedBy(final PlayerID player)
	{
		return new CompositeMatchAnd<Unit>(unitOwnedBy(player), Matches.UnitIsCarrier);
	}
	
	public static Match<Unit> unitOwnedBy(final List<PlayerID> players)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				for (final PlayerID p : players)
					if (o.getOwner().equals(p))
						return true;
				return false;
			}
		};
	}
	
	public static Match<Unit> alliedUnit(final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				if (unit.getOwner().equals(player))
					return true;
				return data.getRelationshipTracker().isAllied(player, unit.getOwner());
			}
		};
	}
	
	public static Match<Unit> alliedUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				if (Matches.unitIsOwnedByOfAnyOfThesePlayers(players).match(unit))
					return true;
				if (data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(unit.getOwner(), players))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIs(final Territory test)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.equals(test);
			}
		};
	}
	
	public static Match<Territory> territoryHasLandUnitsOwnedBy(final PlayerID player)
	{
		final CompositeMatch<Unit> unitOwnedBy = new CompositeMatchAnd<Unit>(unitIsOwnedBy(player), Matches.UnitIsLand);
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(unitOwnedBy);
			}
		};
	}
	
	public static Match<Territory> territoryHasUnitsOwnedBy(final PlayerID player)
	{
		final Match<Unit> unitOwnedBy = unitIsOwnedBy(player);
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(unitOwnedBy);
			}
		};
	}
	
	public static Match<Territory> territoryHasUnitsThatMatch(final Match<Unit> cond)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(cond);
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyAAforAnything(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(unitIsEnemyAAforAnything(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyAAforCombatOnly(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(Matches.unitIsEnemyAAforCombat(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyAAforBombing(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(Matches.unitIsEnemyAAforBombing(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasNoEnemyUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return !t.getUnits().someMatch(enemyUnit(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasNoAlliedUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return !t.getUnits().someMatch(alliedUnit(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasAlliedUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(alliedUnit(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data)
	{
		final CompositeMatch<Unit> match = new CompositeMatchAnd<Unit>();
		match.add(enemyUnit(player, data));
		match.add(new InverseMatch<Unit>(unitIsSubmerged(data)));
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(match);
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyLandUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(enemyUnit(player, data)) && t.getUnits().someMatch(Matches.UnitIsLand);
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyBlitzUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(enemyUnit(player, data)) && t.getUnits().someMatch(Matches.UnitCanBlitz);
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyUnits(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(enemyUnit(player, data));
			}
		};
	}
	
	public static Match<Territory> territoryHasOwnedTransportingUnits(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final CompositeMatch<Unit> match = new CompositeMatchAnd<Unit>();
				match.add(unitIsOwnedBy(player));
				match.add(transportIsTransporting());
				return t.getUnits().someMatch(match);
			}
		};
	}
	
	public static Match<Unit> transportCannotUnload(final Territory territory)
	{
		final TransportTracker transportTracker = new TransportTracker();
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit transport)
			{
				if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
					return true;
				if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, territory))
					return true;
				if (transportTracker.isTransportUnloadRestrictedInNonCombat(transport))
					return true;
				return false;
			}
		};
	}
	
	public static Match<Unit> transportIsNotTransporting()
	{
		final TransportTracker transportTracker = new TransportTracker();
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit transport)
			{
				if (transportTracker.isTransporting(transport))
					return false;
				return true;
			}
		};
	}
	
	public static Match<Unit> transportIsTransporting()
	{
		final TransportTracker transportTracker = new TransportTracker();
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit transport)
			{
				if (transportTracker.isTransporting(transport))
					return true;
				return false;
			}
		};
	}
	
	/**
	 * @return Match that tests the TripleAUnit getTransportedBy value
	 *         which is normally set for sea transport movement of land units,
	 *         and sometimes set for other things like para-troopers and dependent allied fighters sitting as cargo on a ship. (not sure if set for mech inf or not)
	 */
	public static Match<Unit> unitIsBeingTransported()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit dependent)
			{
				return ((TripleAUnit) dependent).getTransportedBy() != null;
			}
		};
	}
	
	/**
	 * @param units
	 *            referring unit
	 * @param route
	 *            referring route
	 * @param currentPlayer
	 *            current player
	 * @param data
	 *            game data
	 * @return Match that tests the TripleAUnit getTransportedBy value
	 *         (also tests for para-troopers, and for dependent allied fighters sitting as cargo on a ship)
	 */
	public static Match<Unit> unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(final Collection<Unit> units, final Route route, final PlayerID currentPlayer, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit dependent)
			{
				// transported on a sea transport
				final Unit transportedBy = ((TripleAUnit) dependent).getTransportedBy();
				if (transportedBy != null && units.contains(transportedBy))
					return true;
				// cargo on a carrier
				final Map<Unit, Collection<Unit>> carrierMustMoveWith = MoveValidator.carrierMustMoveWith(units, units, data, currentPlayer);
				if (carrierMustMoveWith != null)
				{
					for (final Unit unit : carrierMustMoveWith.keySet())
					{
						if (carrierMustMoveWith.get(unit).contains(dependent))
							return true;
					}
				}
				// paratrooper on an air transport
				final Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
				final Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsAirTransportable);
				if (!airTransports.isEmpty() && !paratroops.isEmpty())
				{
					if (MoveDelegate.mapAirTransports(route, paratroops, airTransports, true, currentPlayer).containsKey(dependent))
						return true;
				}
				return false;
			}
		};
	}
	
	public final static Match<Unit> UnitIsLand = new CompositeMatchAnd<Unit>(UnitIsNotSea, UnitIsNotAir);
	public final static Match<Unit> UnitIsNotLand = new InverseMatch<Unit>(UnitIsLand);
	
	public static Match<Unit> unitIsOfType(final UnitType type)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return unit.getType().equals(type);
			}
		};
	}
	
	public static Match<Unit> unitIsOfTypes(final Set<UnitType> types)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				if (types == null || types.isEmpty())
					return false;
				return types.contains(unit.getType());
			}
		};
	}
	
	public static Match<Territory> territoryWasFoughOver(final BattleTracker tracker)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return tracker.wasBattleFought(t) || tracker.wasBlitzed(t);
			}
		};
	}
	
	public static Match<Unit> unitIsSubmerged(final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return TripleAUnit.get(u).getSubmerged();
			}
		};
	}
	
	public static Match<Unit> unitIsNotSubmerged(final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return !TripleAUnit.get(u).getSubmerged();
			}
		};
	}
	
	public static final Match<UnitType> UnitTypeIsSub = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsSub();
		}
	};
	/**
	 * Will match any unit that is 2 hit points
	 */
	public static final Match<UnitType> UnitTypeIsBB = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType type = obj;
			final UnitAttachment ua = UnitAttachment.get(type);
			return ua.isTwoHit();
		}
	};
	
	public static Match<Unit> unitOwnerHasImprovedArtillerySupportTech()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return TechTracker.hasImprovedArtillerySupport(u.getOwner());
			}
		};
	}
	
	public static Match<Unit> unitIsNotInTerritories(final Collection<Territory> list)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				if (u == null)
					return false;
				if (list.isEmpty())
					return true;
				for (final Territory t : list)
				{
					if (t.getUnits().getUnits().contains(u))
						return false;
				}
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasNonAlliedCanal(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final Set<CanalAttachment> canalAttachments = CanalAttachment.get(t);
				if (canalAttachments.isEmpty())
					return false;
				for (final CanalAttachment attachment : canalAttachments)
				{
					if (attachment == null)
						continue;
					for (final Territory borderTerritory : attachment.getLandTerritories())
					{
						if (!data.getRelationshipTracker().isAllied(player, borderTerritory.getOwner()))
						{
							return true;
						}
						if (MoveDelegate.getBattleTracker(data).wasConquered(borderTerritory))
						{
							return true;
						}
					}
				}
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsBlockedSea(final PlayerID player, final GameData data)
	{
		final CompositeMatch<Unit> ignore = new CompositeMatchAnd<Unit>(Matches.UnitIsFactoryOrIsInfrastructure.invert(), Matches.alliedUnit(player, data).invert());
		final CompositeMatch<Unit> sub = new CompositeMatchAnd<Unit>(Matches.UnitIsSub.invert());
		final CompositeMatch<Unit> transport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransportButNotCombatTransport.invert(), Matches.UnitIsLand.invert());
		final CompositeMatch<Unit> unitCond = ignore;
		if (Properties.getIgnoreTransportInMovement(data))
			unitCond.add(transport);
		if (Properties.getIgnoreSubInMovement(data))
			unitCond.add(sub);
		final CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsThatMatch(unitCond).invert(), Matches.TerritoryIsWater);
		return routeCondition;
	}
	
	public static final Match<Unit> UnitCanRepairOthers = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			if (UnitIsDisabled().match(unit))
				return false;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.getRepairsUnits() == null)
				return false;
			return ua.getRepairsUnits().length > 0;
		}
	};
	
	public static Match<Unit> UnitCanRepairThisUnit(final Unit damagedUnit)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitCanRepair)
			{
				final UnitType type = unitCanRepair.getUnitType();
				final UnitAttachment ua = UnitAttachment.get(type);
				// TODO: make sure the unit is operational
				if (ua.getRepairsUnits() != null && ua.getListedUnits(ua.getRepairsUnits()).contains(damagedUnit.getType()))
					return true;
				return false;
			}
		};
	}
	
	/**
	 * @param territory
	 *            referring territory
	 * @param player
	 *            referring player
	 * @param data
	 *            game data
	 * @return Match that will return true if the territory contains a unit that can repair this unit
	 *         (It will also return true if this unit is Sea and an adjacent land territory has a land unit that can repair this unit.)
	 */
	public static Match<Unit> UnitCanBeRepairedByFacilitiesInItsTerritory(final Territory territory, final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit damagedUnit)
			{
				final Match<Unit> damaged = new CompositeMatchAnd<Unit>(Matches.UnitIsTwoHit, Matches.UnitIsDamaged);
				if (!damaged.match(damagedUnit))
					return false;
				final Match<Unit> repairUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanRepairOthers, Matches.UnitCanRepairThisUnit(damagedUnit));
				if (Match.someMatch(territory.getUnits().getUnits(), repairUnit))
					return true;
				if (Matches.UnitIsSea.match(damagedUnit))
				{
					final Match<Unit> repairUnitLand = new CompositeMatchAnd<Unit>(repairUnit, Matches.UnitIsLand);
					final List<Territory> neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
					for (final Territory current : neighbors)
					{
						if (Match.someMatch(current.getUnits().getUnits(), repairUnitLand))
							return true;
					}
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitCanGiveBonusMovement = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return ua.getGivesMovement().size() > 0;
		}
	};
	
	public static Match<Unit> UnitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitCanGiveBonusMovement)
			{
				if (UnitIsDisabled().match(unitCanGiveBonusMovement))
					return false;
				final UnitType type = unitCanGiveBonusMovement.getUnitType();
				final UnitAttachment ua = UnitAttachment.get(type);
				// TODO: make sure the unit is operational
				if (UnitCanGiveBonusMovement.match(unitCanGiveBonusMovement) && ua.getGivesMovement().getInt(unitWhichWillGetBonus.getType()) != 0)
					return true;
				return false;
			}
		};
	}
	
	/**
	 * @param territory
	 *            referring territory
	 * @param player
	 *            referring player
	 * @param data
	 *            game data
	 * @return Match that will return true if the territory contains a unit that can give bonus movement to this unit
	 *         (It will also return true if this unit is Sea and an adjacent land territory has a land unit that can give bonus movement to this unit.)
	 */
	public static Match<Unit> UnitCanBeGivenBonusMovementByFacilitiesInItsTerritory(final Territory territory, final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitWhichWillGetBonus)
			{
				final Match<Unit> givesBonusUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), UnitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
				if (Match.someMatch(territory.getUnits().getUnits(), givesBonusUnit))
					return true;
				if (Matches.UnitIsSea.match(unitWhichWillGetBonus))
				{
					final Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<Unit>(givesBonusUnit, Matches.UnitIsLand);
					final List<Territory> neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
					for (final Territory current : neighbors)
					{
						if (Match.someMatch(current.getUnits().getUnits(), givesBonusUnitLand))
							return true;
					}
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitCreatesUnits = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return (ua.getCreatesUnitsList() != null && ua.getCreatesUnitsList().size() > 0);
		}
	};
	public static final Match<Unit> UnitCreatesResources = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return (ua.getCreatesResourcesList() != null && ua.getCreatesResourcesList().size() > 0);
		}
	};
	public static final Match<UnitType> UnitTypeConsumesUnitsOnCreation = new Match<UnitType>()
	{
		@Override
		public boolean match(final UnitType obj)
		{
			final UnitType unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit);
			if (ua == null)
				return false;
			return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
		}
	};
	public static final Match<Unit> UnitConsumesUnitsOnCreation = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
		}
	};
	
	public static Match<Unit> UnitWhichConsumesUnitsHasRequiredUnits(final Collection<Unit> unitsInTerritoryAtStartOfTurn, final Territory territory)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitWhichRequiresUnits)
			{
				if (!Matches.UnitConsumesUnitsOnCreation.match(unitWhichRequiresUnits))
					return true;
				final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
				final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
				final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
				boolean canBuild = true;
				for (final UnitType ut : requiredUnits)
				{
					final Match<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), Matches.unitIsOfType(ut), Matches
								.UnitHasSomeUnitDamage().invert(), Matches.UnitIsNotDamaged, Matches.UnitIsDisabled().invert(), Matches.unitIsInTerritoryThatHasTerritoryDamage(territory).invert());
					final int requiredNumber = requiredUnitsMap.getInt(ut);
					final int numberInTerritory = Match.countMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndOfTypeAndNotDamaged);
					if (numberInTerritory < requiredNumber)
						canBuild = false;
					if (!canBuild)
						break;
				}
				return canBuild;
			}
		};
	}
	
	public static final Match<Unit> UnitRequiresUnitsOnCreation = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return (ua.getRequiresUnits() != null && ua.getRequiresUnits().size() > 0);
		}
	};
	
	public static Match<Unit> UnitWhichRequiresUnitsHasRequiredUnitsInList(final Collection<Unit> unitsInTerritoryAtStartOfTurn)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitWhichRequiresUnits)
			{
				if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits))
					return true;
				final Match<Unit> unitIsOwnedByAndNotDisabled = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), Matches.UnitIsDisabled().invert());
				unitsInTerritoryAtStartOfTurn.retainAll(Match.getMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndNotDisabled));
				boolean canBuild = false;
				final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
				final ArrayList<String[]> unitComboPossibilities = ua.getRequiresUnits();
				for (final String[] combo : unitComboPossibilities)
				{
					if (combo != null)
					{
						boolean haveAll = true;
						final Collection<UnitType> requiredUnits = ua.getListedUnits(combo);
						for (final UnitType ut : requiredUnits)
						{
							if (Match.countMatches(unitsInTerritoryAtStartOfTurn, Matches.unitIsOfType(ut)) < 1)
								haveAll = false;
							if (!haveAll)
								break;
						}
						if (haveAll)
							canBuild = true;
					}
					if (canBuild)
						break;
				}
				return canBuild;
			}
		};
	}
	
	public static final Match<Territory> territoryIsBlockadeZone = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null)
			{
				return ta.getBlockadeZone();
			}
			return false;
		}
	};
	public static final Match<Unit> UnitIsConstruction = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit obj)
		{
			final Unit unit = obj;
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return ua.getIsConstruction();
		}
	};
	public static final Match<Unit> UnitIsNotConstruction = new InverseMatch<Unit>(UnitIsConstruction);
	public static final Match<Unit> UnitIsFactoryOrConstruction = new CompositeMatchOr<Unit>(UnitIsFactory, UnitIsConstruction);
	public static final Match<Unit> UnitIsNotFactoryOrConstruction = new InverseMatch<Unit>(UnitIsFactoryOrConstruction);
	public static final Match<Unit> UnitIsFactoryOrCanBeDamaged = new CompositeMatchOr<Unit>(UnitIsFactory, UnitCanBeDamagedButIsNotFactory);
	public static final Match<Unit> UnitIsFactoryOrCanProduceUnits = new CompositeMatchOr<Unit>(UnitIsFactory, UnitCanProduceUnits);
	/**
	 * See if a unit can invade. Units with canInvadeFrom not set, or set to "all", can invade from any other unit. Otherwise, units must have a specific unit in this list to be able to invade from that unit.
	 */
	public static final Match<Unit> UnitCanInvade = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			// is the unit being transported?
			final Unit transport = TripleAUnit.get(unit).getTransportedBy();
			if (transport == null)
				return true; // Unit isn't transported so can Invade
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.canInvadeFrom(transport.getUnitType().getName());
		}
	};
	public static final Match<RelationshipType> RelationshipTypeIsAllied = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().isAllied();
		}
	};
	public static final Match<Relationship> RelationshipIsAllied = new Match<Relationship>()
	{
		@Override
		public boolean match(final Relationship relationship)
		{
			return relationship.getRelationshipType().getRelationshipTypeAttachment().isAllied();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeIsNeutral = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().isNeutral();
		}
	};
	public static final Match<Relationship> RelationshipIsNeutral = new Match<Relationship>()
	{
		@Override
		public boolean match(final Relationship relationship)
		{
			return relationship.getRelationshipType().getRelationshipTypeAttachment().isNeutral();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeIsAtWar = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().isWar();
		}
	};
	public static final Match<Relationship> RelationshipIsAtWar = new Match<Relationship>()
	{
		@Override
		public boolean match(final Relationship relationship)
		{
			return relationship.getRelationshipType().getRelationshipTypeAttachment().isWar();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeHelpsDefendAtSea = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getHelpsDefendAtSea();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeCanMoveLandUnitsOverOwnedLand = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getCanMoveLandUnitsOverOwnedLand();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeCanMoveAirUnitsOverOwnedLand = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getCanMoveAirUnitsOverOwnedLand();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeCanLandAirUnitsOnOwnedLand = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getCanLandAirUnitsOnOwnedLand();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeCanTakeOverOwnedTerritory = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getCanTakeOverOwnedTerritory();
		}
	};
	public static final Match<RelationshipType> RelationshipTypeGivesBackOriginalTerritories = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getGivesBackOriginalTerritories();
		}
	};
	
	public static final Match<String> isValidRelationshipName(final GameData data)
	{
		return new Match<String>()
		{
			@Override
			public boolean match(final String relationshipName)
			{
				return data.getRelationshipTypeList().getRelationshipType(relationshipName) != null;
			}
		};
	};
	
	public static final Match<PlayerID> isAtWar(final PlayerID player, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return Matches.RelationshipTypeIsAtWar.match(data.getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	};
	
	public static final Match<PlayerID> isAtWarWithAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(player2, players);
			}
		};
	};
	
	public static final Match<PlayerID> isAllied(final PlayerID player, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return Matches.RelationshipTypeIsAllied.match(data.getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	};
	
	public static final Match<PlayerID> isAlliedWithAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(player2, players);
			}
		};
	};
	
	public static final Match<PlayerID> isNeutral(final PlayerID player, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return Matches.RelationshipTypeIsNeutral.match(data.getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	};
	
	public static final Match<PlayerID> isNeutralWithAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return data.getRelationshipTracker().isNeutralWithAnyOfThesePlayers(player2, players);
			}
		};
	};
	
	public static final Match<Unit> UnitIsOwnedAndIsFactoryOrCanProduceUnits(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return (UnitIsFactoryOrCanProduceUnits.match(u) && unitIsOwnedBy(player).match(u));
			}
		};
	}
	
	public static final Match<Unit> UnitCanReceivesAbilityWhenWith()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return !UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith().isEmpty();
			}
		};
	}
	
	public static final Match<Unit> UnitCanReceivesAbilityWhenWith(final String filterForAbility, final String filterForUnitType)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				for (final String receives : UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith())
				{
					final String[] s = receives.split(":");
					if (s[0].equals(filterForAbility) && s[1].equals(filterForUnitType))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitHasWhenCombatDamagedEffect()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return !UnitAttachment.get(u.getType()).getWhenCombatDamaged().isEmpty();
			}
		};
	}
	
	public static final Match<Unit> UnitHasWhenCombatDamagedEffect(final String filterForEffect)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				if (!UnitHasWhenCombatDamagedEffect().match(u))
					return false;
				final TripleAUnit taUnit = (TripleAUnit) u;
				final int currentDamage = taUnit.getHits();
				final ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> whenCombatDamagedList = UnitAttachment.get(u.getType()).getWhenCombatDamaged();
				for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : whenCombatDamagedList)
				{
					final String effect = key.getSecond().getFirst();
					if (!effect.equals(filterForEffect))
						continue;
					final int damagedFrom = key.getFirst().getFirst();
					final int damagedTo = key.getFirst().getSecond();
					if (currentDamage >= damagedFrom && currentDamage <= damagedTo)
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Territory> TerritoryHasWhenCapturedByGoesTo()
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(t);
				if (ta == null)
					return false;
				if (!ta.getWhenCapturedByGoesTo().isEmpty())
					return true;
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitWhenCapturedChangesIntoDifferentUnitType()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				if (UnitAttachment.get(u.getType()).getWhenCapturedChangesInto().isEmpty())
					return false;
				return true;
			}
		};
	}
	
	public static final Match<PoliticalActionAttachment> PoliticalActionCanBeAttempted(final HashMap<ICondition, Boolean> testedConditions)
	{
		return new Match<PoliticalActionAttachment>()
		{
			@Override
			public boolean match(final PoliticalActionAttachment paa)
			{
				return paa.hasAttemptsLeft() && paa.canPerform(testedConditions);
			}
		};
	}
	
	public static final Match<PoliticalActionAttachment> PoliticalActionHasCostBetween(final int greaterThanEqualTo, final int lessThanEqualTo)
	{
		return new Match<PoliticalActionAttachment>()
		{
			@Override
			public boolean match(final PoliticalActionAttachment paa)
			{
				return (paa.getCostPU() >= greaterThanEqualTo && paa.getCostPU() <= lessThanEqualTo);
			}
		};
	}
	
	public static final Match<Unit> UnitCanOnlyPlaceInOriginalTerritories = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit u)
		{
			final UnitAttachment ua = UnitAttachment.get(u.getType());
			final Set<String> specialOptions = ua.getSpecial();
			for (final String option : specialOptions)
			{
				if (option.equals("canOnlyPlaceInOriginalTerritories"))
					return true;
			}
			return false;
		}
	};
	
	/**
	 * Accounts for OccupiedTerrOf. Returns false if there is no territory attachment (like if it is water).
	 * 
	 * @param player
	 * @return
	 */
	public static final Match<Territory> TerritoryIsOriginallyOwnedBy(final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(t);
				if (ta == null)
					return false;
				final PlayerID originalOwner = ta.getOriginalOwner();
				if ((originalOwner == null && player != null) || (originalOwner != null && player == null))
					return false;
				final PlayerID occupiedTerrOf = ta.getOccupiedTerrOf();
				if (occupiedTerrOf == null)
					return originalOwner.equals(player);
				else
					return occupiedTerrOf.equals(player);
			}
		};
	}
	
	/*public static final Match<Territory> TerritoryContainsUnitsOfAtLeastTwoPlayersAtWar(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(Territory t)
			{
				Collection<PlayerID> players = data.getPlayerList().getPlayers();
				Iterator<PlayerID> p1Iter = players.iterator();
				while (p1Iter.hasNext())
				{
					PlayerID p1 = p1Iter.next();
					p1Iter.remove();
					for (PlayerID p2 : players)
					{
						if (!data.getRelationshipTracker().isAtWar(p1, p2))
							continue;
						if (!t.getUnits().someMatch(unitIsOwnedBy(p1)))
							continue;
						if (!t.getUnits().someMatch(unitIsOwnedBy(p2)))
							continue;
						return true;
					}
				}
				return false;
			}
		};
	}*/
	public static final Match<PlayerID> isAlliedAndAlliancesCanChainTogether(final PlayerID player, final GameData data)
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player2)
			{
				return RelationshipTypeIsAlliedAndAlliancesCanChainTogether.match(data.getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	}
	
	public static final Match<RelationshipType> RelationshipTypeIsAlliedAndAlliancesCanChainTogether = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType rt)
		{
			return RelationshipTypeIsAllied.match(rt) && rt.getRelationshipTypeAttachment().getAlliancesCanChainTogether();
		}
	};
	
	public static final Match<RelationshipType> RelationshipTypeIsDefaultWarPosition = new Match<RelationshipType>()
	{
		@Override
		public boolean match(final RelationshipType rt)
		{
			return rt.getRelationshipTypeAttachment().getIsDefaultWarPosition();
		}
	};
	
	/**
	 * If player is null, this match Will return true if ANY of the relationship changes match the conditions. (since paa's can have more than 1 change).
	 * 
	 * @param player
	 *            CAN be null
	 * @param currentRelation
	 *            can NOT be null
	 * @param newRelation
	 *            can NOT be null
	 * @param data
	 *            can NOT be null
	 * @return
	 */
	public static final Match<PoliticalActionAttachment> politicalActionIsRelationshipChangeOf(final PlayerID player, final Match<RelationshipType> currentRelation,
				final Match<RelationshipType> newRelation, final GameData data)
	{
		return new Match<PoliticalActionAttachment>()
		{
			@Override
			public boolean match(final PoliticalActionAttachment paa)
			{
				for (final String relationshipChangeString : paa.getRelationshipChange())
				{
					final String[] relationshipChange = relationshipChangeString.split(":");
					final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
					final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
					if (player != null && !(p1.equals(player) || p2.equals(player)))
						continue;
					final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
					final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
					if (currentRelation.match(currentType) && newRelation.match(newType))
						return true;
				}
				return false;
			}
		};
	}
	
	public static Match<PoliticalActionAttachment> politicalActionAffectsAtLeastOneAlivePlayer(final PlayerID currentPlayer, final GameData data)
	{
		return new Match<PoliticalActionAttachment>()
		{
			@Override
			public boolean match(final PoliticalActionAttachment paa)
			{
				for (final String relationshipChangeString : paa.getRelationshipChange())
				{
					final String[] relationshipChange = relationshipChangeString.split(":");
					final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
					final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
					if (!currentPlayer.equals(p1))
					{
						if (p1.amNotDeadYet(data))
							return true;
					}
					if (!currentPlayer.equals(p2))
					{
						if (p2.amNotDeadYet(data))
							return true;
					}
				}
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryIsNotNeutralAndNotImpassibleOrRestricted(final PlayerID player, final GameData data)
	{
		final Match<Territory> notNeutralAndNotImpassibleOrRestricted = new CompositeMatchAnd<Territory>(TerritoryIsPassableAndNotRestricted(player, data), new InverseMatch<Territory>(
					TerritoryIsNeutral));
		return notNeutralAndNotImpassibleOrRestricted;
	}
	
	public static CompositeMatch<Territory> alliedNonConqueredNonPendingTerritory(final GameData data, final PlayerID player)
	{
		// these is a place where we can land
		// must be friendly and non conqueuerd land
		final CompositeMatch<Territory> friendlyGround = new CompositeMatchAnd<Territory>();
		friendlyGround.add(isTerritoryAllied(player, data));
		friendlyGround.add(new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				return !MoveDelegate.getBattleTracker(data).wasConquered(o);
			}
		});
		friendlyGround.add(new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				return !MoveDelegate.getBattleTracker(data).hasPendingBattle(o, false);
			}
		});
		friendlyGround.add(TerritoryIsLand);
		return friendlyGround;
	}
	
	public static Match<Unit> unitCanScrambleOnRouteDistance(final Route route)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return UnitAttachment.get(u.getType()).getMaxScrambleDistance() >= route.getMovementCost(u);
			}
		};
	}
	
	public static final Match<Unit> unitCanIntercept = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit u)
		{
			return UnitAttachment.get(u.getType()).getCanIntercept();
		}
	};
	
	public static final Match<Unit> unitCanEscort = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit u)
		{
			return UnitAttachment.get(u.getType()).getCanEscort();
		}
	};
	
	public static final Match<Territory> territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(final PlayerID attacker)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (t.getOwner().equals(attacker))
					return false;
				if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater())
					return false;
				if (!Matches.TerritoryIsPassableAndNotRestricted(attacker, t.getData()).match(t))
					return false;
				return RelationshipTypeCanTakeOverOwnedTerritory.match(t.getData().getRelationshipTracker().getRelationshipType(attacker, t.getOwner()));
			}
		};
	}
	
	/** Creates new Matches */
	private Matches()
	{
	}
}
