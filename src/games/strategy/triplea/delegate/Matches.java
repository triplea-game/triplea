/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Matches.java
 *
 * Created on November 8, 2001, 4:29 PM
 * @version $LastChangedDate$
 */

package games.strategy.triplea.delegate;


import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Util;
import java.util.Collection;

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
 * while(iter.hasNext())
 * {
 *       Unit unit = (Unit) iter.next();
 *    UnitAttatchment ua = UnitAttatchment.get(unit.getType());
 *      if(ua.isAir)
 *      {
 *        hasAir = true;
 *        break;
 *      }
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
 * @author  Sean Bridges
 * @version 1.0
 */
public class Matches
{

    public static final Match<Object> IsTerritory = new Match<Object> ()
    {
        public boolean match(Object o)
        {
            return o != null && o instanceof Territory;
        }
    };

    public static final Match<Unit> UnitIsTwoHit = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.isTwoHit();
        }
    };

    public static final Match<Unit> UnitIsDamaged = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            return unit.getHits() == 1;
        }
    };

    public static final Match<Unit> UnitIsNotDamaged = new InverseMatch<Unit>(UnitIsDamaged);
    
    public static final Match<Unit> UnitIsSea = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.isSea();
        }
    };

    
    
    
    public static final Match<Unit> UnitIsSub = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.isSub();
        }
    };

    public static final Match<Unit> UnitIsNotSub = new InverseMatch<Unit>(UnitIsSub);

    public static final Match<Unit> UnitCanMove = new Match<Unit>()
    {
        public boolean match(Unit u)
        {
            
            return UnitAttachment.get(u.getType()).getMovement(u.getOwner()) > 0;
        }
    };
    
    
    public static final Match<Unit> UnitIsDestroyer = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getIsDestroyer();
        }
    };

    public static final Match<Unit> UnitIsTransport = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getTransportCapacity() != -1;
        }
    };

    public static final Match<Unit> UnitIsNotTransport = new InverseMatch<Unit>(UnitIsTransport);

    public static final Match<Unit> UnitTypeIsTransport = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.isTransport();
        }
    };
    
    public static final Match<Unit> UnitIsStrategicBomber = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.isStrategicBomber();
        }
    };

    public static Match<Unit> unitCanAttack(final PlayerID id)
    {
        return new Match<Unit>()
        {
            public boolean match(Unit unit)
            {
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                return ua.getAttack(id) != 0;
            }
        };
    }

    public static final Match<Unit> UnitIsNotSea = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return !ua.isSea();
        }
    };

    public static final Match<UnitType> UnitTypeIsSea = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            
            UnitAttachment ua = UnitAttachment.get(obj);
            return ua.isSea();
        }
    };

    public static final Match<UnitType> UnitTypeIsNotSea = new Match<UnitType>()
    {
        public boolean match(UnitType type)
        {
            UnitAttachment ua = UnitAttachment.get(type);
            return !ua.isSea();
        }
    };
    
    public static final Match<UnitType> UnitTypeIsSeaOrAir = new Match<UnitType>()
    {
        public boolean match(UnitType type)
        {
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isSea() || ua.isAir();
        }
    };



    public static final Match<Unit> UnitIsAir = new Match<Unit>()
    {
        public boolean match(Unit unit)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.isAir();
        }
    };

    public static final Match<Unit> UnitIsNotAir = new Match<Unit>()
    {
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
        
            public boolean match(UnitType type)
            {
                UnitAttachment ua = UnitAttachment.get(type);
                return ua.getCanBombard(id);
            }
        };
    }

    
    public static Match<Unit> unitCanBombard(final PlayerID id)
    {
        return new Match<Unit>()
        {
        
            public boolean match(Unit unit)
            {
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                return ua.getCanBombard(id);
            }
        };
    }

    public static final Match<Unit> UnitCanBlitz = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getCanBlitz();
        }
    };

    public static final Match<Unit> UnitIsDestructible = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return !ua.isFactory() && !ua.isAA();
        }
    };


    public static final Match<UnitType> UnitTypeIsAir = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isAir();
        }
    };

    public static final Match<UnitType> UnitTypeIsNotAir = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return !ua.isAir();
        }
    };

    public static final Match<Unit> UnitCanLandOnCarrier = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getCarrierCost() != -1;
        }
    };

    public static final Match<Unit> UnitIsCarrier = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getCarrierCapacity() != -1;
        }
    };

    public static final Match<Unit> UnitCanBeTransported  = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getTransportCost() != -1;
        }
    };

    public static final Match<Unit> UnitCanTransport  = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            Unit unit = (Unit) obj;
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            return ua.getTransportCapacity() != -1;
        }
    };

    public static final Match<UnitType> UnitTypeCanTransport  = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.getTransportCapacity() != -1;
        }
    };


    public static final Match<UnitType> UnitTypeCanBeTransported  = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.getTransportCost() != -1;
        }
    };


    public static final Match<UnitType> UnitTypeIsFactory = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isFactory();
        }
    };

    public static final Match<Unit> UnitIsFactory = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            UnitType type = ((Unit) obj).getUnitType();
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isFactory();
        }
    };

    public static final Match<Unit> UnitIsNotFactory = new InverseMatch<Unit>(UnitIsFactory);


    public static final Match<UnitType> UnitTypeIsAA = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitAttachment ua = UnitAttachment.get((UnitType) obj);
            return ua.isAA();
        }
    };
    
    public static final Match<Unit> UnitIsAA = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            UnitType type = ((Unit) obj).getUnitType();
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isAA();
        }
    };
    
    public static final Match<Unit> UnitIsNotAA = new InverseMatch<Unit>(UnitIsAA);

    public static final Match<Unit> UnitIsArtillery = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            UnitType type = ((Unit) obj).getUnitType();
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isArtillery();
        }
    };
    
    public static final Match<Unit> UnitIsArtillerySupportable = new Match<Unit>()
    {
        public boolean match(Unit obj)
        {
            UnitType type = ((Unit) obj).getUnitType();
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isArtillerySupportable();
        }
    };   

    public static final Match<Territory> TerritoryIsWater = new Match<Territory>()
    {
        public boolean match(Territory t)
        {
            return t.isWater();
        }
    };
    
    public static final Match<Territory> TerritoryIsVictoryCity = new Match<Territory>()
    {
        public boolean match(Territory t)
        {
            
            TerritoryAttachment ta = TerritoryAttachment.get(t);
            if(ta == null)
                return false;
            return ta.isVictoryCity();
        }
    };
    

    public static final Match<Territory> TerritoryIsLand = new InverseMatch<Territory>(TerritoryIsWater);

    public static final Match<Territory> TerritoryIsEmpty = new Match<Territory>()
    {
        public boolean match(Territory t)
        {
            return t.getUnits().size() == 0;
        }
    };
      
    public static Match<Territory> territoryHasConvoyRoute(final Territory current)
    {
    	return new Match<Territory>()
        {
        	public boolean match(Territory terr)
            {
            	return TerritoryAttachment.get(terr).isConvoyRoute();
            }
        };
    }

    public static Match<Territory> territoryHasConvoyOwnedBy (final PlayerID player, final GameData data, final Territory origTerr)
    {
    	return new Match<Territory>()
    	{
        	public boolean match(Territory t)
            {
            	TerritoryAttachment ta = TerritoryAttachment.get(t);
                /*If the neighboring territory is a convoy route and matches the current territory's convoy route 
                *(territories may touch more than 1 route)*/
                if (ta != null && ta.isConvoyRoute() && ta.getConvoyAttached().equals(origTerr.getName()))
                {
                	//And see if it's owned by an ally.
                    if(data.getAllianceTracker().isAllied(t.getOwner(), player))
                    	return true;
                	}
                return false;
        	}
    	};
    }

    public static Match<Territory> territoryHasOwnedFactory(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(!t.getOwner().equals(player))
                    return false;
                if(!t.getUnits().someMatch(Matches.UnitIsFactory))
                    return false;
                return true;
            }
        };
    }

    public static Match<Territory> territoryHasEnemyFactory(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(data.getAllianceTracker().isAllied(player, t.getOwner()))
                    return false;
                if(t.getOwner().isNull())
                    return false;
                if(!t.getUnits().someMatch(Matches.UnitIsFactory))
                    return false;
                return true;
            }
        };
    }
    

    public static Match<Territory> territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                CompositeMatch<Unit> nonCom = new CompositeMatchOr<Unit>();
                nonCom.add(UnitIsAAOrFactory);
                nonCom.add(alliedUnit(player, data));
                return t.getUnits().allMatch(nonCom);
            }
        };
    }

    public static final Match<Territory> TerritoryIsNeutral = new Match<Territory>()
    {
        public boolean match(Territory t)
        {
            if(t.isWater() )
                return false;
            return t.getOwner().equals(PlayerID.NULL_PLAYERID);
        }
    };


    public static final Match<Territory> TerritoryIsImpassible = new Match<Territory>()
    {
        public boolean match(Territory t)
        {
            if (t.isWater())
            {
              return false;
            }
            else
            {
              return TerritoryAttachment.get(t).isImpassible();
            }
        }
    };
  

    public static final Match<Battle> BattleIsEmpty = new Match<Battle>()
    {
        public boolean match(Battle battle)
        {
            return battle.isEmpty();
        }
    };
    
    public static final Match<Battle> BattleIsAmphibious = new Match<Battle>()
    {
        public boolean match(Battle battle)
        {
            return battle.isAmphibious();
        }
    };


    /**
     *  Match units that have at least lower limit movement
     */
    public static Match<Unit> unitHasEnoughMovement(final int lowerLimit, final IntegerMap<Unit> movement)
    {
        return new Match<Unit>()
        {
            public boolean match(Unit o)
            {
                return movement.getInt(o) >= lowerLimit;
            }
        };
    }

    public static Match<Unit> unitIsLandAndOwnedBy(final PlayerID player)
    {
        return new Match<Unit>()
        {
            public boolean match(Unit unit)
            {
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                return !ua.isSea() &&  unit.getOwner().equals(player);
            }
        };
    }
    
    public static Match<Unit> unitIsOwnedBy(final PlayerID player)
    {
        return new Match<Unit>()
        {
            public boolean match(Unit unit)
            {
                return unit.getOwner().equals(player);
            }
        };
    }

    public static Match<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units)
    {
        final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units);

        return new Match<Unit>()
        {
            public boolean match(Unit unit)
            {
                Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
                if(transporting == null)
                    return false;
                return Util.someIntersect(UnitSeperator.categorize(transporting), unitCategories);
            }
        };
    }
    public static Match<Territory> isTerritoryAllied(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }
    
    public static Match<Territory> isTerritoryOwnedBy(final PlayerID player)
    {
        return new Match<Territory>()
        {
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
            public boolean match(Unit t)
            {
                return data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }


    public static Match<Territory> isTerritoryFriendly(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(t.isWater())
                    return true;
                if(t.getOwner().equals(player))
                    return true;
                return data.getAllianceTracker().isAllied(player, t.getOwner());
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

  public static Match<Unit> unitIsInTerritory(final Territory territory)
  {
      return new Match<Unit>()
      {
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
            public boolean match(Territory t)
            {
                if(t.getOwner().equals(player))
                    return false;
                return !data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }

    public static Match<Territory> isTerritoryEnemyAndNotNuetralWater(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(t.getOwner().equals(player))
                    return false;
                if(t.isWater() && t.getOwner().isNull()) 
                    return false;
                return !data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }

    
    public static Match<Territory> isTerritoryEnemyAndNotNeutral(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(t.getOwner().equals(player))
                    return false;
                if(t.getOwner().equals(PlayerID.NULL_PLAYERID))
                    return false;
                return !data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }

    public static Match<Territory> isTerritoryFreeNeutral(final GameData data)
    {
        return new Match<Territory>()
        {
          public boolean match(Territory t)
          {
            return (t.getOwner().equals(PlayerID.NULL_PLAYERID) && Properties.getNeutralCharge(data) == 0);
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
                return !data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }
*/

    public static Match<Unit> enemyUnit(PlayerID player, GameData data)
    {
        return new InverseMatch<Unit>(alliedUnit(player, data));
    }

    public static Match<Unit> unitOwnedBy(final PlayerID player)
    {
        return new Match<Unit>()
        {
            public boolean match(Unit o)
            {
                Unit unit = (Unit) o;
                return unit.getOwner().equals(player);
            }
        };

    }



    public static Match<Unit> alliedUnit(final PlayerID player, final GameData data)
    {
        return new Match<Unit>()
        {
            public boolean match(Unit unit)
            {
                if(unit.getOwner().equals(player))
                    return true;
                return data.getAllianceTracker().isAllied(player, unit.getOwner());
            }
        };
    }

  public static Match<Territory> territoryIs(final Territory test)
  {

    return new Match<Territory>()
     {

       public boolean match(Territory t)
       {
         return t.equals(test);
       }
     };

  }

  public static Match<Territory> territoryHasLandUnitsOwnedBy(final PlayerID player)
  {
      final CompositeMatch<Unit> unitOwnedBy = new CompositeMatchAnd<Unit>(unitIsOwnedBy(player), Matches.UnitIsLand );

      return new Match<Territory>()
  {

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

    public boolean match(Territory t)
    {
      return t.getUnits().someMatch(unitOwnedBy);
    }
  };

  }

  public static Match<Territory> territoryHasEnemyAA(final PlayerID player, final GameData data)
  {


    return new Match<Territory>()
    {
      Match<Unit> unitIsEnemyAA = unitIsEnemyAA(player, data);

      public boolean match(Territory t)
      {
        return t.getUnits().someMatch( unitIsEnemyAA );
      }
    };

  }


    public static Match<Territory> territoryHasNoEnemyUnits(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return t.getUnits().allMatch( alliedUnit(player,data));
            }
        };

    }

    public static Match<Territory> territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data)
    {

        final CompositeMatch<Unit> match = new CompositeMatchAnd<Unit>();
        match.add(enemyUnit(player,data));
        match.add(new InverseMatch<Unit>( unitIsSubmerged(data)));
        
        return new Match<Territory>()
        {
            
            public boolean match(Territory t)
            {
                return t.getUnits().someMatch( match );
            }
        };

    }

    
    public static Match<Territory> territoryHasEnemyLandUnits(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return t.getUnits().someMatch( enemyUnit(player,data)) && t.getUnits().someMatch(Matches.UnitIsLand);
            }
        };

    }
    
    public static Match<Territory> territoryHasEnemyUnits(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return t.getUnits().someMatch( enemyUnit(player,data));
            }
        };

    }

    public static Match<Unit> transportCannotUnload(final Territory territory)
    {
        final TransportTracker transportTracker = new TransportTracker();

        return new Match<Unit>() 
        {
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


    public final static Match<Unit> UnitIsLand = new CompositeMatchAnd<Unit>( UnitIsNotSea, UnitIsNotAir);
    public final static Match<Unit> UnitIsNotLand = new InverseMatch<Unit>(UnitIsLand);

    public static Match<Unit> unitIsOfType(final UnitType type)
    {
        return new Match<Unit>()
        {
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
            public boolean match(Unit u)
            {
                
                return TripleAUnit.get(u).getSubmerged();
            }
        };
        
    }
    

        
    public static final Match<Unit> UnitIsAAOrFactory = new CompositeMatchOr<Unit>(UnitIsAA, UnitIsFactory);

    /** Creates new Matches */
    private Matches()
    {
    }

}
