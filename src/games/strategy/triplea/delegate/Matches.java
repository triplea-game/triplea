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
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
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
import java.util.Iterator;
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
 * 
 * while (iter.hasNext())
 * {
 * 	Unit unit = (Unit) iter.next();
 * 	UnitAttatchment ua = UnitAttatchment.get(unit.getType());
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
		public boolean match(Object o)
		{
			return o != null && o instanceof Territory;
		}
	};
	
	public static final Match<Unit> UnitIsTwoHit = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.isTwoHit();
		}
	};
	
	public static final Match<Unit> UnitIsDamaged = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			return unit.getHits() > 0;
		}
	};
	
	public static final Match<Unit> UnitIsNotDamaged = new InverseMatch<Unit>(UnitIsDamaged);
	
	public static final Match<Unit> UnitIsSea = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.isSea();
		}
	};
	
	public static final Match<Unit> UnitIsSub = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.isSub();
		}
	};
	
	public static final Match<Unit> UnitIsNotSub = new InverseMatch<Unit>(UnitIsSub);
	
	public static final Match<Unit> UnitIsCombatTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (ua.isCombatTransport() && ua.isSea());
		}
	};
	
	public static final Match<Unit> UnitIsNotCombatTransport = new InverseMatch<Unit>(UnitIsCombatTransport);
	
	public static final Match<Unit> UnitIsTransportButNotCombatTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (ua.getTransportCapacity() != -1 && ua.isSea() && !ua.isCombatTransport());
		}
	};
	
	public static final Match<Unit> UnitIsNotTransportButCouldBeCombatTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.getTransportCapacity() == -1)
				return true;
			else if (ua.isCombatTransport() && ua.isSea())
				return true;
			else
				return false;
		}
	};
	
	public static final Match<Unit> UnitCanMove = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit u)
		{
			
			return UnitAttachment.get(u.getType()).getMovement(u.getOwner()) > 0;
		}
	};
	
	public static final Match<Unit> UnitIsDestroyer = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsDestroyer();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsDestroyer = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType type)
		{
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsDestroyer();
		}
	};
	
	public static final Match<Unit> UnitIsBB = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (!ua.isSea())
				return false;
			return (ua.isTwoHit());
		}
	};
	
	public static final Match<Unit> UnitIsRadarAA = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			if (!(UnitIsAA.match(unit) || UnitIsAAforCombatOnly.match(unit) || UnitIsAAforBombingThisUnitOnly.match(unit)))
			{
				return false;
			}
			
			TechAttachment ta = (TechAttachment) unit.getOwner().getAttachment(Constants.TECH_ATTACHMENT_NAME);
			if (ta == null)
				return false;
			return ta.hasAARadar();
			
		}
	};
	
	public static final Match<Unit> UnitIsTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (ua.getTransportCapacity() != -1 && ua.isSea());
		}
	};
	
	public static final Match<Unit> UnitIsNotTransport = UnitIsTransport.invert();
	
	public static final Match<Unit> UnitIsTransportAndNotDestroyer = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return (!Matches.UnitIsDestroyer.match(unit) && ua.getTransportCapacity() != -1 && ua.isSea());
		}
	};
	
	public static final Match<Unit> UnitIsStrategicBomber = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return ua.isStrategicBomber();
		}
	};
	
	public static final Match<Unit> UnitIsNotStrategicBomber = new InverseMatch<Unit>(UnitIsStrategicBomber);
	
	public static final Match<UnitType> UnitTypeCanLandOnCarrier = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitAttachment ua = UnitAttachment.get(obj);
			if (ua == null)
				return false;
			return ua.getCarrierCost() != -1;
		}
	};
	
	public static final Match<UnitType> UnitTypeCannotLandOnCarrier = new InverseMatch<UnitType>(UnitTypeCanLandOnCarrier);
	
	public static final Match<Unit> unitHasMoved = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			return TripleAUnit.get(unit).getAlreadyMoved() > 0;
		}
	};
	
	public static final Match<Unit> unitHasNotMoved = new InverseMatch<Unit>(unitHasMoved);
	
	public static Match<Unit> unitCanAttack(final PlayerID id)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
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
			public boolean match(UnitType uT)
			{
				UnitAttachment ua = UnitAttachment.get(uT);
				return ua.getMovement(id) <= 0;
			}
		};
	}
	
	public static Match<UnitType> unitTypeCanAttack(final PlayerID id)
	{
		return new Match<UnitType>()
		{
			
			@Override
			public boolean match(UnitType uT)
			{
				UnitAttachment ua = UnitAttachment.get(uT);
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
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getAttack(unit.getOwner()) >= attackValue;
			}
		};
	}
	
	public static Match<Unit> unitHasDefendValueOfAtLeast(final int defendValue)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getDefense(unit.getOwner()) >= defendValue;
			}
		};
	}
	
	public static Match<Unit> unitIsEnemyOf(final GameData data, final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit u)
			{
				return data.getRelationshipTracker().isAtWar(u.getOwner(), player);
			}
		};
	}
	
	public static final Match<Unit> UnitIsNotSea = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return !ua.isSea();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsSea = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			
			UnitAttachment ua = UnitAttachment.get(obj);
			return ua.isSea();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsNotSea = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType type)
		{
			UnitAttachment ua = UnitAttachment.get(type);
			return !ua.isSea();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsSeaOrAir = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType type)
		{
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isSea() || ua.isAir();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsCarrier = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType type)
		{
			UnitAttachment ua = UnitAttachment.get(type);
			return (ua.getCarrierCapacity() != -1);
		}
	};
	
	public static final Match<Unit> UnitIsAir = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.isAir();
		}
	};
	
	public static final Match<Unit> UnitIsNotAir = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return !ua.isAir();
		}
	};
	
	public static Match<UnitType> unitTypeCanBombard(final PlayerID id)
	{
		return new Match<UnitType>()
		{
			
			@Override
			public boolean match(UnitType type)
			{
				UnitAttachment ua = UnitAttachment.get(type);
				return ua.getCanBombard(id);
			}
		};
	}
	
	public static Match<Unit> UnitCanBeGivenByTerritoryTo(final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
			{
				Unit unit = o;
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCanBeGivenByTerritoryTo().contains(player);
			}
		};
	}
	
	public static Match<Unit> UnitCanBeCapturedOnEnteringToInThisTerritory(final PlayerID player, final Territory terr, final GameData data)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
			{
				if (!games.strategy.triplea.Properties.getCaptureUnitsOnEnteringTerritory(data))
					return false;
				Unit unit = o;
				PlayerID unitOwner = unit.getOwner();
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				boolean unitCanBeCapturedByPlayer = ua.getCanBeCapturedOnEnteringBy().contains(player);
				TerritoryAttachment ta = TerritoryAttachment.get(terr);
				if (ta == null)
					return false;
				if (ta.getCaptureUnitOnEnteringBy() == null)
					return false;
				boolean territoryCanHaveUnitsThatCanBeCapturedByPlayer = ta.getCaptureUnitOnEnteringBy().contains(player);
				PlayerAttachment pa = PlayerAttachment.get(unitOwner);
				if (pa == null)
					return false;
				if (pa.getCaptureUnitOnEnteringBy() == null)
					return false;
				boolean unitOwnerCanLetUnitsBeCapturedByPlayer = pa.getCaptureUnitOnEnteringBy().contains(player);
				return (unitCanBeCapturedByPlayer && territoryCanHaveUnitsThatCanBeCapturedByPlayer && unitOwnerCanLetUnitsBeCapturedByPlayer);
			}
		};
	}
	
	public static Match<Unit> UnitDestroyedWhenCapturedByOrFrom(final PlayerID playerBY)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
			{
				Match<Unit> byOrFrom = new CompositeMatchOr<Unit>(UnitDestroyedWhenCapturedBy(playerBY), UnitDestroyedWhenCapturedFrom());
				return byOrFrom.match(o);
			}
		};
	}
	
	public static Match<Unit> UnitDestroyedWhenCapturedBy(final PlayerID playerBY)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit u)
			{
				UnitAttachment ua = UnitAttachment.get(u.getType());
				if (ua.getDestroyedWhenCapturedBy().isEmpty())
					return false;
				for (Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy())
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
			public boolean match(Unit u)
			{
				UnitAttachment ua = UnitAttachment.get(u.getType());
				if (ua.getDestroyedWhenCapturedBy().isEmpty())
					return false;
				for (Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy())
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
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
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
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCanBeDamaged() && !ua.isFactory();
		}
	};
	
	public static Match<Unit> UnitIsAtMaxDamageOrNotCanBeDamaged(final Territory t)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				if (!ua.getCanBeDamaged() && !ua.isFactory())
					return true;
				
				if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(unit.getData()))
				{
					TerritoryAttachment ta = TerritoryAttachment.get(t);
					int currentDamage = ta.getProduction() - ta.getUnitProduction();
					return currentDamage >= 2 * ta.getProduction();
				}
				else if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData()))
				{
					TripleAUnit taUnit = (TripleAUnit) unit;
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
			public boolean match(Unit unit)
			{
				TripleAUnit taUnit = (TripleAUnit) unit;
				return taUnit.getUnitDamage() > 0;
			}
		};
	}
	
	public static Match<Unit> UnitIsDisabled()
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				if (!UnitIsFactoryOrCanBeDamaged.match(unit))
					return false;
				
				if (!games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData())
							|| games.strategy.triplea.Properties.getSBRAffectsUnitProduction(unit.getData()))
					return false;
				
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				TripleAUnit taUnit = (TripleAUnit) unit;
				
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
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (!ua.getCanBeDamaged() && !ua.isFactory())
				return false;
			return ua.getCanDieFromReachingMaxDamage();
		}
	};
	
	public static final Match<Unit> UnitIsInfrastructure = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsInfrastructure();
		}
	};
	
	public static final Match<Unit> UnitIsSupporterOrHasCombatAbility(final boolean attack, final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				// if unit has attack or defense, return true
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				if (attack && ua.getAttack(player) > 0)
					return true;
				if (!attack && ua.getDefense(player) > 0)
					return true;
				
				// if unit can support other units, return true
				Iterator<UnitSupportAttachment> iter = UnitSupportAttachment.get(data).iterator();
				while (iter.hasNext())
				{
					UnitSupportAttachment rule = iter.next();
					if (unit.getType().equals(rule.getAttatchedTo()))
						return true;
				}
				return false;
			}
		};
	}
	
	public static final Match<Unit> UnitCanScramble = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCanScramble();
		}
	};
	
	public static final Match<Unit> UnitWasScrambled = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasScrambled();
		}
	};
	
	public static Match<Unit> unitCanBombard(final PlayerID id)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCanBombard(id);
			}
		};
	}
	
	public static final Match<Unit> UnitCanBlitz = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCanBlitz();
		}
	};
	
	public static final Match<Unit> UnitIsLandTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsLandTransport();
		}
	};
	
	public static final Match<Unit> UnitIsDestructibleInCombat(final PlayerID player, final Territory terr, final GameData data)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit obj)
			{
				Unit unit = obj;
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return !ua.isFactory() && !ua.isAA() && !ua.getIsInfrastructure() && !UnitCanBeCapturedOnEnteringToInThisTerritory(player, terr, data).match(unit);
			}
		};
	}
	
	public static final Match<Unit> UnitIsDestructibleInCombatShort = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return !ua.isFactory() && !ua.isAA() && !ua.getIsInfrastructure();
		}
	};
	
	public static final Match<Unit> UnitIsSuicide = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsSuicide();
		}
	};
	
	public static final Match<Unit> UnitIsKamikaze = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getIsKamikaze();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsAir = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isAir();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsNotAir = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return !ua.isAir();
		}
	};
	
	public static final Match<Unit> UnitCanLandOnCarrier = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCarrierCost() != -1;
		}
	};
	
	public static final Match<Unit> UnitIsCarrier = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getCarrierCapacity() != -1;
		}
	};
	
	public static final Match<Unit> UnitIsAlliedCarrier(final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit obj)
			{
				Unit unit = obj;
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCarrierCapacity() != -1 && data.getRelationshipTracker().isAllied(player, obj.getOwner());
			}
		};
	}
	
	public static final Match<Unit> UnitCanBeTransported = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getTransportCost() != -1;
		}
	};
	
	public static final Match<Unit> UnitCanNotBeTransported = new InverseMatch<Unit>(UnitCanBeTransported);
	
	public static final Match<Unit> UnitWasAmphibious = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasAmphibious();
		}
	};
	
	public static final Match<Unit> UnitWasNotAmphibious = new InverseMatch<Unit>(UnitWasAmphibious);
	
	public static final Match<Unit> UnitWasInCombat = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasInCombat();
		}
	};
	
	public static final Match<Unit> UnitWasUnloadedThisTurn = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getUnloadedTo() != null;
		}
	};
	
	public static final Match<Unit> UnitWasLoadedThisTurn = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TripleAUnit taUnit = (TripleAUnit) obj;
			return taUnit.getWasLoadedThisTurn();
		}
	};
	
	public static final Match<Unit> UnitWasNotLoadedThisTurn = new InverseMatch<Unit>(UnitWasLoadedThisTurn);
	
	public static final Match<Unit> UnitCanTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.getTransportCapacity() != -1;
		}
	};
	
	public static final Match<UnitType> UnitTypeCanTransport = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getTransportCapacity() != -1;
		}
	};
	
	public static final Match<UnitType> UnitTypeCanBeTransported = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getTransportCost() != -1;
		}
	};
	
	public static final Match<UnitType> UnitTypeIsFactory = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isFactory();
		}
	};
	
	public static final Match<UnitType> UnitTypeCanProduceUnits = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getCanProduceUnits();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsFactoryOrIsInfrastructure = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isFactory() || ua.getIsInfrastructure();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsFactoryOrIsInfrastructureButNotAAofAnyKind = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return (ua.isFactory() || ua.getIsInfrastructure()) && !(ua.isAA() || ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly());
		}
	};
	
	public static final Match<UnitType> UnitTypeIsInfantry = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isInfantry();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsArtillery = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isArtillery();
		}
	};
	
	public static final Match<UnitType> UnitTypeHasMaxBuildRestrictions = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getMaxBuiltPerPlayer() >= 0;
		}
	};
	
	public static final Match<Unit> UnitIsFactory = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isFactory();
		}
	};
	
	public static final Match<Unit> UnitIsNotFactory = new InverseMatch<Unit>(UnitIsFactory);
	
	public static final Match<Unit> UnitCanProduceUnits = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getCanProduceUnits();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsAA = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitAttachment ua = UnitAttachment.get(obj);
			return ua.isAA();
		}
	};
	public static final Match<UnitType> UnitTypeIsAAofAnyKind = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitAttachment ua = UnitAttachment.get(obj);
			return ua.isAA() || ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly();
		}
	};
	
	public static final Match<UnitType> UnitTypeIsAAOrFactory = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitAttachment ua = UnitAttachment.get(obj);
			if (ua.isAA() || ua.isFactory())
				return true;
			return false;
		}
	};
	
	public static final Match<UnitType> UnitTypeIsAAOrIsFactoryOrIsInfrastructure = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitAttachment ua = UnitAttachment.get(obj);
			if (ua.isAA() || ua.isFactory() || ua.getIsInfrastructure())
				return true;
			return false;
		}
	};
	
	public static final Match<Unit> UnitIsAAorIsRocket = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isAA() || ua.getIsRocket();
		}
	};
	
	public static final Match<Unit> UnitIsAAorIsAAmovement = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isAA() || ua.getIsAAmovement();
		}
	};
	
	public static final Match<Unit> UnitIsAA = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isAA();
		}
	};
	
	public static final Match<Unit> UnitIsAAforCombatOnly = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforCombatOnly();
		}
	};
	
	public static final Match<Unit> UnitIsAAforCombat = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforCombatOnly() || ua.isAA();
		}
	};
	
	public static final Match<Unit> UnitIsAAforBombingThisUnitOnly = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforBombingThisUnitOnly();
		}
	};
	
	public static final Match<Unit> UnitIsAAforBombing = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforBombingThisUnitOnly() || ua.isAA();
		}
	};
	
	public static final Match<Unit> UnitIsAAforAnything = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.getIsAAforCombatOnly() || ua.getIsAAforBombingThisUnitOnly() || ua.isAA();
		}
	};
	
	public static final Match<Unit> UnitIsNotAA = new InverseMatch<Unit>(UnitIsAA);
	
	public static final Match<Unit> UnitIsInfantry = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isInfantry();
		}
	};
	
	public static final Match<Unit> UnitIsNotInfantry = new InverseMatch<Unit>(UnitIsInfantry);
	
	public static final Match<Unit> UnitIsMarine = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isMarine();
		}
	};
	
	public static final Match<Unit> UnitIsNotMarine = new InverseMatch<Unit>(UnitIsMarine);
	
	public static final Match<Unit> UnitIsAirTransportable = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TechAttachment ta = TechAttachment.get(obj.getOwner());
			if (ta == null || !ta.hasParatroopers())
			{
				return false;
			}
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isAirTransportable();
		}
	};
	
	public static final Match<Unit> UnitIsNotAirTransportable = new InverseMatch<Unit>(UnitIsAirTransportable);
	
	public static final Match<Unit> UnitIsAirTransport = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			TechAttachment ta = TechAttachment.get(obj.getOwner());
			if (ta == null || !ta.hasParatroopers())
			{
				return false;
			}
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isAirTransport();
		}
	};
	
	public static final Match<Unit> UnitIsNotAirTransport = new InverseMatch<Unit>(UnitIsAirTransport);
	
	public static final Match<Unit> UnitIsArtillery = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isArtillery();
		}
	};
	
	public static final Match<Unit> UnitIsArtillerySupportable = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			UnitType type = obj.getUnitType();
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isArtillerySupportable();
		}
	};
	
	// TODO: CHECK whether this makes any sense
	public static final Match<Territory> TerritoryIsLandOrWater = new Match<Territory>()
	{
		
		@Override
		public boolean match(Territory t)
		{
			return t != null && t instanceof Territory;
		}
	};
	
	public static final Match<Territory> TerritoryIsWater = new Match<Territory>()
	{
		
		@Override
		public boolean match(Territory t)
		{
			return t.isWater();
		}
	};
	
	public static final Match<Territory> TerritoryIsVictoryCity = new Match<Territory>()
	{
		
		@Override
		public boolean match(Territory t)
		{
			
			TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta == null)
				return false;
			return ta.isVictoryCity();
		}
	};
	
	public static final Match<Territory> TerritoryHasSomeDamage = new Match<Territory>()
	{
		
		@Override
		public boolean match(Territory t)
		{
			TerritoryAttachment ta = TerritoryAttachment.get(t);
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
			public boolean match(Unit u)
			{
				return TerritoryHasSomeDamage.match(t);
			}
		};
	}
	
	public static final Match<Territory> TerritoryIsLand = new InverseMatch<Territory>(TerritoryIsWater);
	
	public static final Match<Territory> TerritoryIsEmpty = new Match<Territory>()
	{
		
		@Override
		public boolean match(Territory t)
		{
			return t.getUnits().size() == 0;
		}
	};
	
	public static Match<Territory> territoryHasConvoyRoute(final Territory current)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory terr)
			{
				return TerritoryAttachment.get(terr).isConvoyRoute();
			}
		};
	}
	
	public static Match<Territory> territoryHasConvoyOwnedBy(final PlayerID player, final GameData data, final Territory origTerr)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				TerritoryAttachment ta = TerritoryAttachment.get(t);
				/*If the neighboring territory is a convoy route and matches the current territory's convoy route
				*(territories may touch more than 1 route)*/
				if (ta != null && ta.isConvoyRoute() && ta.getConvoyAttached().equals(origTerr.getName()))
				{
					// And see if it's owned by an ally.
					if (data.getRelationshipTracker().isAllied(t.getOwner(), player))
						return true;
				}
				return false;
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyLandNeighbor(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				// This method will still return true if territory t is an impassible or restricted territory With enemy neighbors. Makes sure your AI does not include any impassible or restricted territories by using this:
				// CompositeMatch<Territory> territoryHasEnemyLandNeighborAndIsNotImpassibleOrRestricted = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player), Matches.territoryHasEnemyLandNeighbor(data, player));
				CompositeMatch<Territory> condition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
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
			public boolean match(Territory t)
			{
				CompositeMatch<Unit> destroyerUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.unitIsOwnedBy(player));
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
			{
				CompositeMatch<Territory> validLandRoute = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
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
			public boolean match(Territory ter)
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
			public boolean match(Territory ter)
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
			public boolean match(Territory t)
			{
				for (PlayerID ePlayer : data.getPlayerList().getPlayers())
				{
					List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
					Iterator<Territory> iter = capitalsListOwned.iterator();
					while (iter.hasNext())
					{
						Territory current = iter.next();
						if (!data.getRelationshipTracker().isAtWar(player, current.getOwner()))
							continue;
						if (data.getMap().getDistance(t, current, Matches.TerritoryIsPassableAndNotRestricted(player)) != -1)
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
			public boolean match(Territory t)
			{
				for (PlayerID ePlayer : data.getPlayerList().getPlayers())
				{
					List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
					Iterator<Territory> iter = capitalsListOwned.iterator();
					while (iter.hasNext())
					{
						Territory current = iter.next();
						if (!data.getRelationshipTracker().isAtWar(player, current.getOwner()))
							continue;
						if (data.getMap().getDistance(t, current, Matches.TerritoryIsNotImpassableToLandUnits(player)) != -1)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
			{
				CompositeMatch<Unit> nonCom = new CompositeMatchOr<Unit>();
				nonCom.add(UnitIsAAOrFactory);
				nonCom.add(enemyUnit(player, data).invert());
				nonCom.add(UnitIsInfrastructure); // UnitIsAAOrIsFactoryOrIsInfrastructure
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
			public boolean match(Territory t)
			{
				if (t.isWater())
					return false;
				
				int terrProd = TerritoryAttachment.get(t).getProduction();
				if (terrProd >= prodVal)
					return true;
				return false;
			}
		};
	}
	
	public static final Match<Territory> TerritoryIsNeutral = new Match<Territory>()
	{
		
		@Override
		public boolean match(Territory t)
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
		public boolean match(Territory t)
		{
			if (t.isWater())
			{
				return false;
			}
			return TerritoryAttachment.get(t).isImpassible();
		}
	};
	
	public final static Match<Territory> TerritoryIsNotImpassable = new InverseMatch<Territory>(TerritoryIsImpassable);
	
	public static final Match<Territory> TerritoryIsPassableAndNotRestricted(final PlayerID player)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				GameData data = player.getData();
				if (Matches.TerritoryIsImpassable.match(t))
					return false;
				if (!Properties.getMovementByTerritoryRestricted(data))
					return true;
				
				RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
				if (ra == null || ra.getMovementRestrictionTerritories() == null)
					return true;
				
				String movementRestrictionType = ra.getMovementRestrictionType();
				Collection<Territory> listedTerritories = ra.getListedTerritories(ra.getMovementRestrictionTerritories());
				return (movementRestrictionType.equals("allowed") == listedTerritories.contains(t));
			}
		};
	}
	
	public final static Match<Territory> TerritoryIsImpassableToLandUnits(final PlayerID player)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				if (t.isWater())
					return true;
				else if (Matches.TerritoryIsPassableAndNotRestricted(player).invert().match(t))
					return true;
				return false;
			}
		};
	}
	
	public final static Match<Territory> TerritoryIsNotImpassableToLandUnits(final PlayerID player)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				return TerritoryIsImpassableToLandUnits(player).invert().match(t);
			}
		};
	}
	
	public static final Match<IBattle> BattleIsEmpty = new Match<IBattle>()
	{
		
		@Override
		public boolean match(IBattle battle)
		{
			return battle.isEmpty();
		}
	};
	
	public static final Match<IBattle> BattleIsAmphibious = new Match<IBattle>()
	{
		
		@Override
		public boolean match(IBattle battle)
		{
			return battle.isAmphibious();
		}
	};
	
	/**
	 * @param lowerLimit
	 *            lower limit for movement
	 * @param movement
	 *            referring movement
	 * @return units match that have at least lower limit movement
	 */
	public static Match<Unit> unitHasEnoughMovement(final int lowerLimit, final IntegerMap<Unit> movement)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
			{
				return movement.getInt(o) >= lowerLimit;
			}
		};
	}
	
	public static Match<Unit> UnitHasEnoughMovement(final int minMovement)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				return TripleAUnit.get(unit).getMovementLeft() >= minMovement;
			}
		};
	}
	
	/**
	 * Match units that have at least 1 movement left
	 */
	public final static Match<Unit> unitHasMovementLeft = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit o)
		{
			return TripleAUnit.get(o).getMovementLeft() >= 1;
		}
	};
	
	public final static Match<Unit> UnitIsNotStatic(PlayerID player)
	{
		return new InverseMatch<Unit>(UnitIsStatic(player));
	}
	
	// match units that have no movement as their attachment, like walls and fortresses (static = zero movement)
	public static final Match<Unit> UnitIsStatic(final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit obj)
			{
				UnitType type = obj.getUnitType();
				UnitAttachment ua = UnitAttachment.get(type);
				return ua.getMovement(player) < 1;
			}
		};
	}
	
	public static Match<Unit> unitIsLandAndOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				return !ua.isSea() && !ua.isAir() && unit.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> unitIsOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				return unit.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> unitHasDefenseThatIsMoreThanOrEqualTo(final int minDefense)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				if (ua.isAA() || ua.isFactory() || ua.getIsInfrastructure())
					return false;
				return ua.getDefense(unit.getOwner()) >= minDefense;
			}
		};
	}
	
	public static Match<Unit> unitIsTransporting()
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit unit)
			{
				Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
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
			public boolean match(Unit unit)
			{
				UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
				int loadCost = 0;
				for (Unit cargo : TripleAUnit.get(unit).getTransporting())
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
			public boolean match(Unit unit)
			{
				Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
			{
				return t.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> isUnitAllied(final PlayerID player, final GameData data)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit t)
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
			public boolean match(Territory t)
			{
				if (t.isWater())
					return true;
				if (t.getOwner().equals(player))
					return true;
				return data.getRelationshipTracker().isAllied(player, t.getOwner());
			}
		};
	}
	
	public static Match<Unit> unitIsEnemyAA(final PlayerID player, final GameData data)
	{
		CompositeMatch<Unit> comp = new CompositeMatchAnd<Unit>();
		comp.add(UnitIsAA);
		comp.add(enemyUnit(player, data));
		return comp;
	}
	
	public static Match<Unit> unitIsEnemyAAforCombat(final PlayerID player, final GameData data)
	{
		CompositeMatch<Unit> comp = new CompositeMatchAnd<Unit>();
		comp.add(new CompositeMatchOr<Unit>(UnitIsAA, UnitIsAAforCombatOnly));
		comp.add(enemyUnit(player, data));
		return comp;
	}
	
	public static Match<Unit> unitIsEnemyAAforBombing(final PlayerID player, final GameData data)
	{
		CompositeMatch<Unit> comp = new CompositeMatchAnd<Unit>();
		comp.add(new CompositeMatchOr<Unit>(UnitIsAA, UnitIsAAforBombingThisUnitOnly));
		comp.add(enemyUnit(player, data));
		return comp;
	}
	
	public static Match<Unit> unitIsInTerritory(final Territory territory)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
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
			public boolean match(Territory t)
			{
				if (t.getOwner().equals(player))
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
			public boolean match(Territory t)
			{
				if (t.getOwner().equals(player))
					return false;
				// if we look at territory attachments, may have funny results for blockades or other things that are passable and not owned. better to check them by alliance. (veqryn)
				// OLD code included: if(t.isWater() && t.getOwner().isNull() && TerritoryAttachment.get(t) == null){return false;}
				if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) // this will still return true for enemy and neutral owned convoy zones (water)
					return false;
				if (!Matches.TerritoryIsPassableAndNotRestricted(player).match(t))
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
			public boolean match(Territory t)
			{
				if (t.getOwner().equals(player))
					return false;
				if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && (t.isWater() | !games.strategy.triplea.Properties.getNeutralsBlitzable(data)))
					return false;
				return !data.getRelationshipTracker().isAllied(player, t.getOwner());
			}
		};
	}
	
	public static Match<Territory> isTerritoryFreeNeutral(final GameData data)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
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
			public boolean match(Unit unit)
			{
				return data.getRelationshipTracker().isAtWar(player, unit.getOwner());
			}
		};
	}
	
	public static Match<Unit> unitOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
			{
				Unit unit = o;
				return unit.getOwner().equals(player);
			}
		};
	}
	
	public static Match<Unit> unitOwnedBy(final List<PlayerID> players)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit o)
			{
				for (PlayerID p : players)
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
			public boolean match(Unit unit)
			{
				if (unit.getOwner().equals(player))
					return true;
				return data.getRelationshipTracker().isAllied(player, unit.getOwner());
			}
		};
	}
	
	public static Match<Territory> territoryIs(final Territory test)
	{
		
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
			{
				return t.getUnits().someMatch(cond);
			}
		};
		
	}
	
	public static Match<Territory> territoryHasEnemyAA(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			Match<Unit> unitIsEnemyAA = unitIsEnemyAA(player, data);
			
			@Override
			public boolean match(Territory t)
			{
				return t.getUnits().someMatch(unitIsEnemyAA);
			}
		};
	}
	
	public static Match<Territory> territoryHasEnemyAAforCombatOnly(final PlayerID player, final GameData data)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Territory t)
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
			public boolean match(Unit transport)
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
			public boolean match(Unit transport)
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
			public boolean match(Unit transport)
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
			public boolean match(Unit dependent)
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
			public boolean match(Unit dependent)
			{
				// transported on a sea transport
				Unit transportedBy = ((TripleAUnit) dependent).getTransportedBy();
				if (transportedBy != null && units.contains(transportedBy))
					return true;
				
				// cargo on a carrier
				Map<Unit, Collection<Unit>> carrierMustMoveWith = MoveValidator.carrierMustMoveWith(units, units, data, currentPlayer);
				if (carrierMustMoveWith != null)
				{
					for (Unit unit : carrierMustMoveWith.keySet())
					{
						if (carrierMustMoveWith.get(unit).contains(dependent))
							return true;
					}
				}
				
				// paratrooper on an air transport
				Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
				Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsAirTransportable);
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
			public boolean match(Unit unit)
			{
				return unit.getType().equals(type);
			}
		};
	}
	
	public static Match<Territory> territoryWasFoughOver(final BattleTracker tracker)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
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
			public boolean match(Unit u)
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
			public boolean match(Unit u)
			{
				
				return !TripleAUnit.get(u).getSubmerged();
			}
		};
	}
	
	public static final Match<UnitType> UnitTypeIsSub = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isSub();
		}
	};
	
	/**
	 * Will match any unit that is 2 hit points
	 */
	public static final Match<UnitType> UnitTypeIsBB = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType type = obj;
			UnitAttachment ua = UnitAttachment.get(type);
			return ua.isTwoHit();
		}
	};
	
	public static Match<Unit> unitOwnerHasImprovedArtillerySupportTech()
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit u)
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
			public boolean match(Unit u)
			{
				if (u == null)
					return false;
				if (list.isEmpty())
					return true;
				Iterator<Territory> tIter = list.iterator();
				while (tIter.hasNext())
				{
					Territory t = tIter.next();
					if (t.getUnits().getUnits().contains(u))
						return false;
				}
				return true;
			}
		};
	}
	
	public static Match<Territory> territoryHasNonAlliedCanal(final PlayerID player)
	{
		return new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				GameData data = player.getData();
				Set<CanalAttachment> canalAttachments = CanalAttachment.get(t);
				if (canalAttachments.isEmpty())
					return false;
				
				Iterator<CanalAttachment> iter = canalAttachments.iterator();
				while (iter.hasNext())
				{
					CanalAttachment attachment = iter.next();
					if (attachment == null)
						continue;
					for (Territory borderTerritory : attachment.getLandTerritories())
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
		// UnitIsAAOrIsFactoryOrIsInfrastructure
		CompositeMatch<Unit> ignore = new CompositeMatchAnd<Unit>(Matches.UnitIsAAOrFactory.invert(), Matches.alliedUnit(player, data).invert(), Matches.UnitIsInfrastructure.invert());
		CompositeMatch<Unit> sub = new CompositeMatchAnd<Unit>(Matches.UnitIsSub.invert());
		CompositeMatch<Unit> transport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransportButNotCombatTransport.invert(), Matches.UnitIsLand.invert());
		CompositeMatch<Unit> unitCond = ignore;
		if (Properties.getIgnoreTransportInMovement(data))
			unitCond.add(transport);
		if (Properties.getIgnoreSubInMovement(data))
			unitCond.add(sub);
		CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsThatMatch(unitCond).invert(), Matches.TerritoryIsWater);
		
		return routeCondition;
	}
	
	public static final Match<Unit> UnitCanRepairOthers = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit unit)
		{
			if (UnitIsDisabled().match(unit))
				return false;
			
			UnitAttachment ua = UnitAttachment.get(unit.getType());
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
			public boolean match(Unit unitCanRepair)
			{
				UnitType type = unitCanRepair.getUnitType();
				UnitAttachment ua = UnitAttachment.get(type);
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
			public boolean match(Unit damagedUnit)
			{
				Match<Unit> damaged = new CompositeMatchAnd<Unit>(Matches.UnitIsTwoHit, Matches.UnitIsDamaged);
				if (!damaged.match(damagedUnit))
					return false;
				
				Match<Unit> repairUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanRepairOthers, Matches.UnitCanRepairThisUnit(damagedUnit));
				
				if (Match.someMatch(territory.getUnits().getUnits(), repairUnit))
					return true;
				
				if (Matches.UnitIsSea.match(damagedUnit))
				{
					Match<Unit> repairUnitLand = new CompositeMatchAnd<Unit>(repairUnit, Matches.UnitIsLand);
					List<Territory> neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
					Iterator<Territory> iter = neighbors.iterator();
					while (iter.hasNext())
					{
						Territory current = iter.next();
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
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
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
			public boolean match(Unit unitCanGiveBonusMovement)
			{
				if (UnitIsDisabled().match(unitCanGiveBonusMovement))
					return false;
				
				UnitType type = unitCanGiveBonusMovement.getUnitType();
				UnitAttachment ua = UnitAttachment.get(type);
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
			public boolean match(Unit unitWhichWillGetBonus)
			{
				Match<Unit> givesBonusUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), UnitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
				
				if (Match.someMatch(territory.getUnits().getUnits(), givesBonusUnit))
					return true;
				
				if (Matches.UnitIsSea.match(unitWhichWillGetBonus))
				{
					Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<Unit>(givesBonusUnit, Matches.UnitIsLand);
					List<Territory> neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
					Iterator<Territory> iter = neighbors.iterator();
					while (iter.hasNext())
					{
						Territory current = iter.next();
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
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return (ua.getCreatesUnitsList() != null && ua.getCreatesUnitsList().size() > 0);
		}
	};
	
	public static final Match<Unit> UnitCreatesResources = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return (ua.getCreatesResourcesList() != null && ua.getCreatesResourcesList().size() > 0);
		}
	};
	
	public static final Match<UnitType> UnitTypeConsumesUnitsOnCreation = new Match<UnitType>()
	{
		
		@Override
		public boolean match(UnitType obj)
		{
			UnitType unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit);
			if (ua == null)
				return false;
			return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
		}
	};
	
	public static final Match<Unit> UnitConsumesUnitsOnCreation = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
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
			public boolean match(Unit unitWhichRequiresUnits)
			{
				if (!Matches.UnitConsumesUnitsOnCreation.match(unitWhichRequiresUnits))
					return true;
				
				UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
				IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
				Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
				
				boolean canBuild = true;
				for (UnitType ut : requiredUnits)
				{
					Match<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), Matches.unitIsOfType(ut), Matches
								.UnitHasSomeUnitDamage().invert(), Matches.UnitIsNotDamaged, Matches.UnitIsDisabled().invert(), Matches.unitIsInTerritoryThatHasTerritoryDamage(territory).invert());
					int requiredNumber = requiredUnitsMap.getInt(ut);
					int numberInTerritory = Match.countMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndOfTypeAndNotDamaged);
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
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
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
			public boolean match(Unit unitWhichRequiresUnits)
			{
				if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits))
					return true;
				
				Match<Unit> unitIsOwnedByAndNotDisabled = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), Matches.UnitIsDisabled().invert());
				unitsInTerritoryAtStartOfTurn.retainAll(Match.getMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndNotDisabled));
				
				boolean canBuild = false;
				UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
				ArrayList<String[]> unitComboPossibilities = ua.getRequiresUnits();
				for (String[] combo : unitComboPossibilities)
				{
					if (combo != null)
					{
						boolean haveAll = true;
						Collection<UnitType> requiredUnits = ua.getListedUnits(combo);
						for (UnitType ut : requiredUnits)
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
	
	public static final Match<Unit> UnitIsAAOrFactory = new CompositeMatchOr<Unit>(UnitIsAA, UnitIsFactory);
	
	public static final Match<Unit> UnitIsAAOrIsAAmovementOrIsFactory = new CompositeMatchOr<Unit>(UnitIsAAorIsAAmovement, UnitIsFactory);
	
	public static final Match<Unit> UnitIsAAOrIsFactoryOrIsInfrastructure = new CompositeMatchOr<Unit>(UnitIsAA, UnitIsFactory, UnitIsInfrastructure);
	
	public static final Match<Territory> territoryIsBlockadeZone = new Match<Territory>()
		{
			
			@Override
			public boolean match(Territory t)
			{
				TerritoryAttachment ta = TerritoryAttachment.get(t);
				if (ta != null)
				{
					return ta.isBlockadeZone();
				}
				return false;
			}
		};
	
	public static final Match<Unit> UnitIsConstruction = new Match<Unit>()
	{
		
		@Override
		public boolean match(Unit obj)
		{
			Unit unit = obj;
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua == null)
				return false;
			return ua.isConstruction();
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
		public boolean match(Unit unit)
		{
			// is the unit being transported?
			Unit transport = TripleAUnit.get(unit).getTransportedBy();
			if (transport == null)
				return true; // Unit isn't transported so can Invade
				
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			return ua.canInvadeFrom(transport.getUnitType().getName());
		}
	};
	
	public static final Match<RelationshipType> RelationshipIsAllied = new Match<RelationshipType>()
	{
		
		@Override
		public boolean match(RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().isAllied();
		}
	};
	
	public static final Match<RelationshipType> RelationshipIsNeutral = new Match<RelationshipType>()
	{
		
		@Override
		public boolean match(RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().isNeutral();
		}
	};
	
	public static final Match<RelationshipType> RelationshipIsAtWar = new Match<RelationshipType>()
	{
		
		@Override
		public boolean match(RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().isWar();
		}
	};
	
	public static final Match<RelationshipType> RelationshipHelpsDefendAtSea = new Match<RelationshipType>()
	{
		
		@Override
		public boolean match(RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().helpsDefendAtSea();
		}
	};
	
	public static final Match<RelationshipType> RelationshipCanMoveLandUnitsOverOwnedLand = new Match<RelationshipType>()
	{
		
		@Override
		public boolean match(RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getCanMoveLandUnitsOverOwnedLand();
		}
	};
	
	public static final Match<RelationshipType> RelationshipCanMoveAirUnitsOverOwnedLand = new Match<RelationshipType>()
	{
		
		@Override
		public boolean match(RelationshipType relationship)
		{
			return relationship.getRelationshipTypeAttachment().getCanMoveAirUnitsOverOwnedLand();
		}
	};
	
	public static final Match<String> isValidRelationshipName(final GameData data)
	{
		return new Match<String>()
		{
			
			@Override
			public boolean match(String relationshipName)
			{
				return data.getRelationshipTypeList().getRelationshipType(relationshipName) != null;
			}
		};
	};
	
	public static final Match<PlayerID> isAtWar(final PlayerID player)
	{
		return new Match<PlayerID>()
		{
			
			@Override
			public boolean match(PlayerID player2)
			{
				return Matches.RelationshipIsAtWar.match(player.getData().getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	};
	
	public static final Match<PlayerID> isAllied(final PlayerID player)
	{
		return new Match<PlayerID>()
		{
			
			@Override
			public boolean match(PlayerID player2)
			{
				return Matches.RelationshipIsAllied.match(player.getData().getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	};
	
	public static final Match<PlayerID> isNeutral(final PlayerID player)
	{
		return new Match<PlayerID>()
		{
			
			@Override
			public boolean match(PlayerID player2)
			{
				return Matches.RelationshipIsNeutral.match(player.getData().getRelationshipTracker().getRelationshipType(player, player2));
			}
		};
	};
	
	public static final Match<Unit> UnitIsOwnedAndIsFactoryOrCanProduceUnits(final PlayerID player)
	{
		return new Match<Unit>()
		{
			
			@Override
			public boolean match(Unit u)
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
			public boolean match(Unit u)
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
			public boolean match(Unit u)
			{
				for (String receives : UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith())
				{
					String[] s = receives.split(":");
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
			public boolean match(Unit u)
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
			public boolean match(Unit u)
			{
				if (!UnitHasWhenCombatDamagedEffect().match(u))
					return false;
				TripleAUnit taUnit = (TripleAUnit) u;
				int currentDamage = taUnit.getHits();
				ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> whenCombatDamagedList = UnitAttachment.get(u.getType()).getWhenCombatDamaged();
				Iterator<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> iter = whenCombatDamagedList.iterator();
				while (iter.hasNext())
				{
					Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key = iter.next();
					String effect = key.getSecond().getFirst();
					if (!effect.equals(filterForEffect))
						continue;
					int damagedFrom = key.getFirst().getFirst();
					int damagedTo = key.getFirst().getSecond();
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
			public boolean match(Territory t)
			{
				TerritoryAttachment ta = TerritoryAttachment.get(t);
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
			public boolean match(Unit u)
			{
				if (UnitAttachment.get(u.getType()).getWhenCapturedChangesInto().isEmpty())
					return false;
				return true;
			}
		};
	}
	
	public static final Match<PoliticalActionAttachment> PoliticalActionCanBeAttempted = new Match<PoliticalActionAttachment>()
	{
		
		@Override
		public boolean match(PoliticalActionAttachment paa)
		{
			return paa.hasAttemptsLeft() && paa.canPerform();
		}
	};
	
	/** Creates new Matches */
	private Matches()
	{
	}
	
}
