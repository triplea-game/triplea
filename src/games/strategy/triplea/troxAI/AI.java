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

package games.strategy.triplea.troxAI;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.message.MoveDescription;
import games.strategy.util.IntegerMap;

import java.util.*;


/**
 *
 * @author  Troy Graber
 * @version 1.0
 */
public class AI
{
	private Personality m_personality;
	private PlayerID m_id;
	private GameData m_data;
	private int m_count;
	
	public AI(Personality per, PlayerID pid, GameData gd)
	{
		m_personality = per;
		m_id = pid;
		m_data = gd;//m_id.getData();
	}
	
	public void setData(GameData g)
	{
		m_data = g;
		return;
	}

	public Collection selectUnitsWithRange(PlayerID p, Collection u, int move)
	{
		//return u;
		Collection goodu = new ArrayList();
		Iterator i = u.iterator();
		while(i.hasNext() )
		{
			Unit u1 = (Unit)i.next();
			int m = UnitAttatchment.get(u1.getUnitType()).getMovement(p);
			if(UnitAttatchment.get(u1.getUnitType()).isAir())
				m--;
			if(u1.getOwner() == p)
			{
				//System.out.println("Current Fitness ");	
				if(m >= move)
				{
					if(!UnitAttatchment.get(u1.getUnitType()).isAA())
					{
						goodu.add(u1);
					}
				}
			}
		}
		return goodu;
	}
	
	
	public Collection selectNonCombatMoves()
	{	
	
	  	GameMap m = m_data.getMap();
	  	Collection moves = new ArrayList();
  		AllianceTracker m_allies = m_data.getAllianceTracker();
		Collection c = new ArrayList();
		Collection t = m.getTerritoriesOwnedBy(m_id);
		//System.out.println("Non Combat");
		Iterator i1 = m_data.getPlayerList().getPlayers().iterator();
		while(i1.hasNext() )
		{
			PlayerID p = (PlayerID)i1.next();
			if(!m_allies.isAllied(m_id, p))
			{
				//Territory temp = TerritoryAttatchment.getCapital(p, m_data);
				//c.add(temp);
				c.addAll(m.getTerritoriesOwnedBy(p));
			}
		}
		//int num = c.size();
		Iterator i2 = t.iterator();
  		while(i2.hasNext() )
		{
			Territory t1 = (Territory) i2.next();
			Territory t2 = null;
			int best = 99999999;
			Collection u = new ArrayList();
			u.addAll(t1.getUnits().getUnits());
			u = selectUnitsWithRange(m_id, u,1);
			if(u.size() > 0)
			{
				Iterator i3 = c.iterator();
				while(i3.hasNext() )
				{
					Territory temp = (Territory) i3.next();
					int dist = m.getLandDistance(t1, temp);
					if(dist < best && dist != -1)
					{
						t2 = temp;
						best = dist;
					}
				}
				if(t2 != null)
				{
					Collection n = m.getNeighbors(t1);	
					Iterator i4 = n.iterator();
					while(i4.hasNext())
					{
						Territory t3 = (Territory) i4.next();
						int dist = m.getLandDistance(t3, t2);
						if(dist != -1 && dist < best && (m_allies.isAllied(m_id,t3.getOwner()) || m_id == t3.getOwner()))
						{
							Route r = m.getLandRoute(t1, t3);
							MoveDescription m1 = new MoveDescription(u, r);
							if(r != null && u.size()>=1)
							{
								moves.add(m1);
								//System.out.println(""+ t1.toString() + " " + t2.toString()+ " " + t3.toString());
								//System.out.println(""+ m1.toString());
							}
							break;
						}
					}
				}
			}	
		}	
		return moves;
	}
	
	
	public Collection selectCombatMoves()
	{	//Medium AI
		GameMap m = m_data.getMap();
		//PlayerList pls = m_data.getPlayerList();
		Iterator i1 = m_data.getPlayerList().getPlayers().iterator();
		Collection c = new ArrayList();
		AllianceTracker m_allies = m_data.getAllianceTracker();
		Collection moves = new ArrayList();
		int best = 0;
		Collection bestmoves = new ArrayList();
		//System.out.println("Combat");
		while(i1.hasNext() )
		{
			PlayerID p = (PlayerID)i1.next();
			//System.out.println(p.toString());
			if(!m_allies.isAllied(m_id, p))
			{
				c.addAll(m.getTerritoriesOwnedBy(p));
			}
		}

	  	Iterator iter = c.iterator();
	  	while(iter.hasNext() )
		{
			Territory territory = (Territory) iter.next();
			//x++;
			Collection u = new ArrayList();
			//Set adjacent = m.getNeighbors(territory, 3);
			Iterator iter2 = m.getNeighbors(territory, 3).iterator();
			while(iter2.hasNext() )
			{
				Territory t2 = (Territory) iter2.next();
				if(!t2.isWater() && m_allies.isAllied(m_id,t2.getOwner()) && t2.getUnits().getUnits().size() >=1)
				{
					Collection temp = new ArrayList();
					temp.addAll(t2.getUnits().getUnits());
					Collection newu = selectUnitsWithRange(m_id, temp, m.getLandDistance(territory, t2));
					//System.out.println("Current Fitness " + t2.getUnits().getUnits().size());	
					Route r = m.getLandRoute(t2, territory);
					
					if(r != null && r.getLength() >= 1)
					{
						boolean trap = true;
						//System.out.println(r.getStart().toString());
						for(int q = 0; q < r.getLength() - 1; q++)
						{
							//System.out.println(r.at(q).toString());
							if(!m_allies.isAllied(r.at(q).getOwner(), m_id))
								trap = false;
						}
						MoveDescription cow = new MoveDescription(newu, r);
						
						if(trap && newu.size() >= 1 && newu.size() <= temp.size())
						{
							moves.add(cow);
							u.addAll(newu);
						}
					}
				}
			}

			if(u.size() != 0)
			{
				Collection temp = new ArrayList();
				temp.addAll(territory.getUnits().getUnits());
				int current = evaluateAttackhelper(u, temp);
				//System.out.println("Current Fitness " + current);
				if(territory.getUnits().getUnits().size() == 0)
				{
					//System.out.println("empty Country" + best);
					//current = best + 1;
				}
				if(current > best)
				{
					best = current;
					bestmoves = moves;
					moves = new ArrayList();
				}
			}
			
		}
		Iterator cow = bestmoves.iterator();
		while(cow.hasNext())
		{
		    //MoveDescription temp = (MoveDescription)cow.next();
			//System.out.println("Move " + temp.toString());
		}
		//if(bestmoves == null || bestmoves.isEmpty())
			{
				//System.out.println("turn " + m_id.toString());
				//System.out.println("Current Fitness " + best);
				
				//System.out.println("There are Moves");
			}
		return bestmoves;
	}
	
	public int evaluateAttackhelper(Collection attacker, Collection defender)
	{
		int like = 0;
		Collection a2 = null;
		Collection d2 = null;
		int acost;
		int dcost;
		double odds = 0;
		if(attacker.size() == 0)
			return 0;
		defender = removeAA(defender);
		
		m_count = 0;
		BattleResult b = Simbattlehelper(attacker, defender);
		odds = b.getNumber();
		a2 = b.getAttack();
		d2 = b.getDefend();
		//System.out.println("odds =" + odds);
		odds = odds / (attacker.size() *1.0);
		//System.out.println("odds =" + odds);
		//System.out.println("avalue =" + (valuehelper(attacker) - valuehelper(a2)));
		//System.out.println("dvalue =" + (valuehelper(defender) - valuehelper(d2)));
		acost = valuehelper(attacker) - valuehelper(a2);
		dcost = valuehelper(defender) - valuehelper(d2);
		//System.out.println(like +"+"+ acost + "+" + dcost);
		like = like + (((m_personality.getAttrition()) / 10) * dcost) - acost;
		//System.out.println(like);
		like = like + (int)(60.0 * (odds - ((m_personality.getAgressiveness())/10)));
		//System.out.println(like);
		if(odds < 0 && like > 0)
		{
			like = like * - 1;	
		}
		if(odds == 1)
		{
			like = like + 30;
			//System.out.println("Odds " + odds);
		}
		
		return like;// + 50;
	}
	
	public int evaluateAttack(UnitCollection attacker, UnitCollection defender)
	{

		return evaluateAttackhelper(attacker.getUnits(), defender.getUnits());
	}
	
	public int valuehelper(Collection u)
	{
		int value = 0;
		if(u == null || u.isEmpty())
			return 0;
		Iterator i = u.iterator();
		while(i.hasNext())
		{
			Unit current = (Unit)i.next(); 
			value = value + GetCost(current);
		}
		return value;
	}
	
	public int value(UnitCollection u)
	{

		return valuehelper(u.getUnits());
	}
	
	public BattleResult Simbattlehelper(Collection attacker, Collection defender)
	{
		Collection a2;
		Collection d2;
		int attack = 0;
		int defend = 0;
		int acasualities = 0;
		int dcasualities = 0;
		
		
		if((attacker == null || attacker.isEmpty()) && (defender==null || defender.isEmpty()))
		{
			//System.out.println("None left ");
			return new BattleResult(0, new ArrayList(), new ArrayList());
		}
		else if(attacker == null || attacker.isEmpty())
		{
			//System.out.println("defenders left " + defender.size());
			return new BattleResult(defender.size() * -1, new ArrayList(), defender);
			//return (defender.size() * -1);
		}
		else if(defender == null || defender.isEmpty())
		{
			//System.out.println("attackers left " + attacker.size());
			return new BattleResult(attacker.size(), attacker, new ArrayList());
			//return attacker.size();
		}
		Iterator iter = attacker.iterator();
		while(iter.hasNext() )
		{
			UnitType type = ((Unit)(iter.next())).getUnitType();
			attack = attack + UnitAttatchment.get(type).getAttack(m_id);
		}
		Iterator i2 = defender.iterator();
		while(i2.hasNext() )
		{
			
			Unit u1 = (Unit)i2.next();
			//System.out.println("I Broke" + u1.toString());
			UnitType type = u1.getUnitType();
			defend = defend + UnitAttatchment.get(type).getDefense(m_id);
			if(UnitAttatchment.get(type).isAA())
			{
				defender.remove(u1);
			}
			//value = value + Unittypes.getInt(type);
		}
		dcasualities = attack / 6;
		acasualities = defend / 6;
		if(acasualities < 1 && dcasualities < 1)
		{
			if(attack > defend)
				dcasualities = 1;
			else
				acasualities = 1;
		}
		a2 = allocatecasualitieshelper(attacker, acasualities);
		//System.out.println("Defenders " + defender.size() + " Casualities " + dcasualities);
		d2 = allocatecasualitieshelper(defender, dcasualities);
		//System.out.println("Results " + d2.size());
		
		
		try{
		m_count++;
		return Simbattlehelper(a2, d2);
		}
		//allocate casualities then call recursively
		catch (StackOverflowError e)
		{
			//System.gc();
			if(a2.size() == 1)
				System.out.println(a2.toString());
			if(d2.size() == 1)
				System.out.println(d2.toString());
			System.out.println("Bad" + a2.size() + " " + d2.size() + " # of turns " + m_count);
			//return Simbattlehelper(a2, d2, a2, d2);
		}
		return new BattleResult(0, new ArrayList(), new ArrayList());
		
	}
	
	public BattleResult Simbattle(UnitCollection attacker, UnitCollection defender)
	{
		return Simbattlehelper(attacker.getUnits(), defender.getUnits());
		//allocate casualities then call recursively
	}
	private Collection removeAA(Collection defender)
	{
	
		Collection c2 = new ArrayList();
		Iterator i2 = defender.iterator();
		while(i2.hasNext() )
		{
			
			Unit u1 = (Unit)i2.next();
			//System.out.println("I Broke" + u1.toString());
			UnitType type = u1.getUnitType();
			//defend = defend + UnitAttatchment.get(type).getDefense(m_id);
			if(!UnitAttatchment.get(type).isAA())
			{
				c2.add(u1);
			}
			//value = value + Unittypes.getInt(type);
		}
		return c2;
	}
	
	public Collection allocatecasualitieshelper(Collection a, int x)
	{
		//int y = 0;
		Unit min = null;
		Collection c = new ArrayList();
		if(x <= 0)
		{
			return a;
		}
		if(a.size() < x)
		{
			return c;
		}
		Iterator i = a.iterator();
		while(i.hasNext())
		{
			Unit current = (Unit)i.next(); ////Not Done
			if(GetCost(current) < GetCost(min))
			{
				if(min != null)
					c.add(min);
				min = current;
			}
			else
			{
				c.add(current);
			}
		}

		
		return allocatecasualitieshelper(c, x-1); 
		
	}
	
	
	public UnitCollection allocatecasualities(UnitCollection a, int x)
	{

		a.removeAllUnits(allocatecasualitieshelper(a.getUnits(), x));
		return  a;
		
	}
	
	public IntegerMap selectPurchases()
	{
		//int y = 0;
		UnitType bestattack = null;
		UnitType bestdefense = null;
		double likebestattack = 0;
		double likebestdefense = 0;
		
		IntegerMap result = new IntegerMap();
		
		Iterator i = m_data.getUnitTypeList().iterator();
		
		while(i.hasNext())
		{
			UnitType current = (UnitType)i.next(); 
			int attack = UnitAttatchment.get(current).getAttack(m_id);
			int defense = UnitAttatchment.get(current).getDefense(m_id);

			Iterator i2 = m_id.getProductionFrontier().getRules().iterator();
			while(i2.hasNext())
			{
				ProductionRule pr = (ProductionRule)i2.next();
				if(pr.getResults().getInt(current) != 0)
				{
					Resource r = m_data.getResourceList().getResource("IPCs");
					int cost = pr.getCosts().getInt(r);
					
					if((1.0 *attack) / (1.0 *cost) > likebestattack)
					{
						likebestattack = (1.0 *attack) / cost;
						bestattack = current;
					}
					if ((1.0 *defense) / cost > likebestdefense)
					{
						likebestdefense = (1.0 *defense) / cost;
						bestdefense = current;
						
					}
				}
				
			}
		}
		
		Iterator i2 = m_id.getProductionFrontier().getRules().iterator();
		while(i2.hasNext())
		{
			ProductionRule pr = (ProductionRule)i2.next();
			//int attackcost = 9999999;
			//int defensecost = 9999999;
			int ipcs = m_id.getResources().getQuantity("IPCs");
			//int aipcs = (int)(ipcs * ((1.0 * p.getAgressiveness())/((1.0 * p.getDefensiveness()) + (1.0 * p.getAgressiveness()))));
			//int dipcs = (int)(ipcs * ((1.0 * p.getDefensiveness())/((1.0 * p.getDefensiveness()) + (1.0 * p.getAgressiveness()))));
			int aipcs = (int)(ipcs * ((1.0 * m_personality.getPurchase())/10));
			int dipcs = (int)(ipcs * (1.0 - ((1.0 * m_personality.getPurchase())/10)));
			//System.out.println("" + aipcs + " " + bestattack.toString());
			//System.out.println("" + dipcs + " " + bestdefense.toString());
			if(pr.getResults().getInt(bestattack) != 0)
			{
				Resource r = m_data.getResourceList().getResource("IPCs");
				int cost = pr.getCosts().getInt(r);
				int number = aipcs / cost;
				dipcs = ipcs - (number * cost);
				result.put(pr, number);
				
					
			}
			else if(pr.getResults().getInt(bestdefense) != 0)
			{
				Resource r = m_data.getResourceList().getResource("IPCs");
				int cost = pr.getCosts().getInt(r);
				int number = dipcs / cost;
				aipcs = ipcs - (number * cost);
				result.put(pr, number);
			}
		}
		return result;
		
	}

	public Collection selectCasualities(Collection a, int x)
	{
		//int y = 0;
		Unit min = null;
		if(x <= 0)
		{
			return new ArrayList();
		}
		if(a.size() < x)
		{
			return a;
		}
		Iterator i = a.iterator();
		while(i.hasNext())
		{
			Unit current = (Unit)i.next(); ////Not Done
			
			if(GetCost(current) < GetCost(min))
			{
				min = current;
			}
		}
		Collection dead = new ArrayList();
		dead.add(min);
		a.removeAll(dead);
		
		dead.addAll(selectCasualities(a, x-1));
		return dead;
	}
	
	public int GetCost(Unit u)
	{
		if(u == null)
		{
			return 999999999;
		}
		Iterator i = m_id.getProductionFrontier().getRules().iterator();
		while(i.hasNext())
		{
			ProductionRule pr = (ProductionRule)i.next();
			if(pr.getResults().getInt(u.getType()) != 0)
			{
				Resource r = m_data.getResourceList().getResource("IPCs");
				return 	pr.getCosts().getInt(r);
			}
			
		}
		return 999999999;
	}
	
	public int getTotalValue(PlayerID p)
	{
		int value = 0;
		GameMap m = m_data.getMap();
	  	Collection t = m.getTerritoriesOwnedBy(p);
	  	Iterator i = t.iterator();
	  	while(i.hasNext())
	  	{
	  		Territory t1 = (Territory) i.next();
	  		Collection u = t1.getUnits().getUnits();
	  		Iterator i2 = u.iterator();
	  		while(i2.hasNext())
	  		{
	  			Unit u1 = (Unit)i2.next();
	  			value = value + GetCost(u1);
	  		}
	  	}
	  	
		return value;
	}
}