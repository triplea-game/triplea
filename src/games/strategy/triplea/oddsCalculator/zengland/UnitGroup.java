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
package games.strategy.triplea.oddsCalculator.zengland;

public class UnitGroup implements Cloneable {
	
	private OCUnit unit;
	private int numUnits;
	private int totalHp;
	
	public UnitGroup(OCUnit unit, int numUnits) {
		super();
		setUnit(unit);
		setNumUnits(numUnits);
		setTotalHp(numUnits*unit.getMaxHP());
	}
	public int getNumUnits() {
		return numUnits;
	}
	public void setNumUnits(int numUnits) {
		this.numUnits = numUnits;
		if(this.numUnits<0)
			this.numUnits = 0;
	}
	public OCUnit getUnit() {
		return unit;
	}
	public void setUnit(OCUnit unit) {
		this.unit = unit;
	}
	public void removeExtraHp() {
		this.totalHp--;
		if(this.totalHp<0)
			this.totalHp = 0;
	}
	public void removeUnit() {
		this.totalHp--;
		if(this.totalHp<0)
			this.totalHp = 0;
		setNumUnits(this.numUnits-1);
		if(this.numUnits<0)
			this.numUnits = 0;
	}
	
	public int rollUnitsAttack(int numBoosted) {
		int hits = 0;
		int maxHits = unit.getMaxHits();
		int maxRolls = unit.getMaxRolls();
		int curAttValue = unit.getAttackValue()+1;
		for(int i=0;i<numUnits;i++)
		{
			if(i==numBoosted)
				curAttValue=unit.getAttackValue();
			for(int j=0;j<maxHits;j++)
			{
				boolean gotAHit=false;
				for(int k=0;k<maxRolls&&!gotAHit;k++)
				{
					int roll = unit.rollAttack();
					if(roll<=curAttValue)
					{
						gotAHit=true;
					}
				}
				if(gotAHit)
					hits++;
			}
		}
		return hits;
	}
	
	public int rollUnitsAttack() {
		int hits = 0;
		int maxHits = unit.getMaxHits();
		int maxRolls = unit.getMaxRolls();
		for(int i=0;i<numUnits;i++)
		{
			for(int j=0;j<maxHits;j++)
			{
				boolean gotAHit=false;
				for(int k=0;k<maxRolls&&!gotAHit;k++)
				{
					int roll = unit.rollAttack();
					if(roll<=unit.getAttackValue())
					{
						gotAHit=true;
					}
				}
				if(gotAHit)
					hits++;
			}
		}
		return hits;
	}
	public int rollUnitsDefend() {
		int hits = 0;
		for(int i=0;i<numUnits;i++)
		{
			int roll = unit.rollDefend();
			if(roll<=unit.getDefendValue())
				hits++;
		}
		return hits;
	}
	
	public String toString() {
		String unitGroup = "";
		unitGroup += this.getUnit().toString();
		unitGroup += " " + this.getNumUnits();
		return unitGroup;
	}
	
	public static void main(String args[]) {
		//UnitGroup ug = new UnitGroup(Unit.newRevisedHeavyBomber(), 1);
		UnitGroup ug = new UnitGroup(OCUnit.newInf(), 10);
		System.out.println(ug);
		int defHits = 0;
		int attHits = 0;
		int rounds = 1;
		for(int i=0;i<rounds;i++)
		{
		defHits += ug.rollUnitsDefend();
		attHits += ug.rollUnitsAttack(5);
//		System.out.println("Attack hits " + attHits);
//		System.out.println("Defense hits " + defHits);
		}
		System.out.println("Avg Attack hits " + (float)attHits/(float)rounds);
		System.out.println("Avg Defense hits " + (float)defHits/(float)rounds);

	}
	public int getTotalHp() {
		return totalHp;
	}
	public void setTotalHp(int totalHp) {
		this.totalHp = totalHp;
	}
	
	public Object clone() {
		Object c = null;
		c = new UnitGroup(this.getUnit(), this.getNumUnits());
		return c;
	}

}
