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

public class OCUnit {
	
	public static final int LANDUNIT = 1;
	public static final int AIRUNIT = 2;
	public static final int SEAUNIT = 3;
	public static final int AAUNIT = 4;
	
	private int cost;
	private int attackValue;
	private int defendValue;
	private int moveValue;
	private String name;
	private boolean noRetaliationHit = false; // for special units like subs
	private int unitType;
	private boolean supportShot = false;
	private boolean canHitAir = true;
	private int maxHits;
	private int maxRolls;
	private int maxHP;
	private int hp;
	private boolean blockNoRetalHit = false; // for special units like destroyers
	private boolean boostsInfAtt = false;
	private boolean boostAmphib = false;

	public boolean isBoostAmphib() {
		return boostAmphib;
	}

	public void setBoostAmphib(boolean boostAmphib) {
		this.boostAmphib = boostAmphib;
	}

	public boolean isBoostsInfAtt() {
		return boostsInfAtt;
	}

	public void setBoostsInfAtt(boolean boostsInfAtt) {
		this.boostsInfAtt = boostsInfAtt;
	}

	public boolean isBlockNoRetalHit() {
		return blockNoRetalHit;
	}

	public void setBlockNoRetalHit(boolean blockNoRetalHit) {
		this.blockNoRetalHit = blockNoRetalHit;
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}

	public int getMaxHP() {
		return maxHP;
	}

	public void setMaxHP(int maxHP) {
		this.maxHP = maxHP;
	}

	public int getMaxHits() {
		return maxHits;
	}

	public void setMaxHits(int maxHits) {
		this.maxHits = maxHits;
	}

	public int getMaxRolls() {
		return maxRolls;
	}

	public void setMaxRolls(int maxRolls) {
		this.maxRolls = maxRolls;
	}

	public boolean isCanHitAir() {
		return canHitAir;
	}

	public void setCanHitAir(boolean canHitAir) {
		this.canHitAir = canHitAir;
	}

	public static OCUnit newBB() {
		OCUnit bbUnit = null;
		bbUnit = new OCUnit(StandardUnits.BBCost, StandardUnits.BBAttack, StandardUnits.BBDefend, StandardUnits.BBMove, StandardUnits.BBName, false, OCUnit.SEAUNIT, true, true);
		return bbUnit;
	}
	
	public static OCUnit newTwoHitBB() {
		OCUnit bbUnit = null;
		bbUnit = new OCUnit(StandardUnits.BBCost, StandardUnits.BBAttack, StandardUnits.BBDefend, StandardUnits.BBMove, StandardUnits.BBName, false, OCUnit.SEAUNIT, true, true, 1, 1, 2, false, false, false);
		return bbUnit;
	}
	
	public static OCUnit newAC() {
		OCUnit acUnit = null;
		acUnit = new OCUnit(StandardUnits.ACCost, StandardUnits.ACAttack, StandardUnits.ACDefend, StandardUnits.ACMove, StandardUnits.ACName, false, OCUnit.SEAUNIT, false, true);
		return acUnit;
	}

	public static OCUnit newSub() {
		OCUnit subUnit = null;
		subUnit = new OCUnit(StandardUnits.SubCost, StandardUnits.SubAttack, StandardUnits.SubDefend, StandardUnits.SubMove, StandardUnits.SubName, true, OCUnit.SEAUNIT, false, false);
		return subUnit;
	}
	
	public static OCUnit newTrn() {
		OCUnit trnUnit = null;
		trnUnit = new OCUnit(StandardUnits.TrnCost, StandardUnits.TrnAttack, StandardUnits.TrnDefend, StandardUnits.TrnMove, StandardUnits.TrnName, false, OCUnit.SEAUNIT, false, true);
		return trnUnit;
	}

	public static OCUnit newIC() {
		OCUnit icUnit = null;
		icUnit = new OCUnit(StandardUnits.ICCost, StandardUnits.ICAttack, StandardUnits.ICDefend, StandardUnits.ICMove, StandardUnits.ICName, false, OCUnit.LANDUNIT, false, true);
		return icUnit;
	}
	
	public static OCUnit newArm() {
		OCUnit armUnit = null;
		armUnit = new OCUnit(StandardUnits.ArmCost, StandardUnits.ArmAttack, StandardUnits.ArmDefend, StandardUnits.ArmMove, StandardUnits.ArmName, false, OCUnit.LANDUNIT, false, true);
		return armUnit;
	}
	
	public static OCUnit newInf() {
		OCUnit infUnit = null;
		infUnit = new OCUnit(StandardUnits.InfCost, StandardUnits.InfAttack, StandardUnits.InfDefend, StandardUnits.InfMove, StandardUnits.InfName, false, OCUnit.LANDUNIT, false, true);
		return infUnit;
	}

	public static OCUnit newAA() {
		OCUnit aaUnit = null;
		aaUnit = new OCUnit(StandardUnits.AACost, StandardUnits.AAAttack, StandardUnits.AADefend, StandardUnits.AAMove, StandardUnits.AAName, true, OCUnit.LANDUNIT, false, true);
		return aaUnit;
	}
	
	public static OCUnit newFighter() {
		OCUnit fighter = null;
		fighter = new OCUnit(StandardUnits.FtrCost, StandardUnits.FtrAttack, StandardUnits.FtrDefend, StandardUnits.FtrMove, StandardUnits.FtrName, false, OCUnit.AIRUNIT, false, true);
		return fighter;
	}
	
	public static OCUnit newBomber() {
		OCUnit bmb = null;
		bmb = new OCUnit(StandardUnits.BmbCost, StandardUnits.BmbAttack, StandardUnits.BmbDefend, StandardUnits.BmbMove, StandardUnits.BmbName, false, OCUnit.AIRUNIT, false, true);
		return bmb;
	}
	
	public static OCUnit newHeavyBomber() {
		OCUnit bmb = null;
		bmb = new OCUnit(StandardUnits.BmbCost, StandardUnits.BmbAttack, StandardUnits.BmbDefend, StandardUnits.BmbMove, StandardUnits.BmbName, false, OCUnit.AIRUNIT, false, true, 3, 1, 1, false, false, false);
		return bmb;
	}

	public static OCUnit newRevisedHeavyBomber() {
		OCUnit bmb = null;
		bmb = new OCUnit(StandardUnits.BmbCost, StandardUnits.BmbAttack, StandardUnits.BmbDefend, StandardUnits.BmbMove, StandardUnits.BmbName, false, OCUnit.AIRUNIT, false, true, 1, 2, 1, false, false, false);
		return bmb;
	}
	
	public static OCUnit newDestroyer() {
		OCUnit des = null;
		des = new OCUnit(StandardUnits.DesCost, StandardUnits.DesAttack, StandardUnits.DesDefend, StandardUnits.DesMove, StandardUnits.DesName, false, OCUnit.SEAUNIT, false, true, 1, 1, 1, true, false, false);
		return des;
	}
	
	public static OCUnit newRtl() {
		OCUnit rtl = null;
		rtl = new OCUnit(StandardUnits.RtlCost, StandardUnits.RtlAttack, StandardUnits.RtlDefend, StandardUnits.RtlMove, StandardUnits.RtlName, false, OCUnit.LANDUNIT, false, false, 1, 1, 1, false, true, false);
		return rtl;
	}

	
	public OCUnit(int cost, int attackValue, int defendValue, int moveValue, String name, boolean noRetaliationHit, int unitType, boolean supportShot, boolean canHitAir, int maxHits, int maxRolls, int maxHp, boolean blocksNoRetHit, boolean boostsInfAtt, boolean boostAmphib) {
		super();
		this.cost = cost;
		this.attackValue = attackValue;
		this.defendValue = defendValue;
		this.moveValue = moveValue;
		this.name = name;
		this.noRetaliationHit = noRetaliationHit;
		this.unitType = unitType;
		this.supportShot = supportShot;
		this.canHitAir = canHitAir;
		this.maxHits = maxHits;
		this.maxRolls = maxRolls;
		this.maxHP = maxHp;
		this.blockNoRetalHit = blocksNoRetHit;
		this.boostsInfAtt = boostsInfAtt;
		setBoostAmphib(boostAmphib);
	}
	public OCUnit(int cost, int attackValue, int defendValue, int moveValue, String name, boolean noRetaliationHit, int unitType, boolean supportShot, boolean canHitAir) {
		super();
		this.cost = cost;
		this.attackValue = attackValue;
		this.defendValue = defendValue;
		this.moveValue = moveValue;
		this.name = name;
		this.noRetaliationHit = noRetaliationHit;
		this.unitType = unitType;
		this.supportShot = supportShot;
		this.canHitAir = canHitAir;
		this.maxHits = 1;
		this.maxRolls = 1;
		this.maxHP = 1;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getAttackValue() {
		return attackValue;
	}
	public void setAttackValue(int attackValue) {
		this.attackValue = attackValue;
	}
	public int getCost() {
		return cost;
	}
	public void setCost(int cost) {
		this.cost = cost;
	}
	public int getDefendValue() {
		return defendValue;
	}
	public void setDefendValue(int defendValue) {
		this.defendValue = defendValue;
	}
	public int getMoveValue() {
		return moveValue;
	}
	public void setMoveValue(int moveValue) {
		this.moveValue = moveValue;
	}
	public boolean isNoRetaliationHit() {
		return noRetaliationHit;
	}
	public void setNoRetaliationHit(boolean noRetaliationHit) {
		this.noRetaliationHit = noRetaliationHit;
	}
	public int getUnitType() {
		return unitType;
	}
	public void setUnitType(int unitType) {
		this.unitType = unitType;
	}
	public int roll(int hitAt) {
		//Random rand = new Random();
		//int roll = Math.abs(rand.nextInt())%6+1;
		int roll = (int)(Math.random() * 100)%6+1;
		return roll;
	}
	public boolean isSupportShot() {
		return supportShot;
	}
	public void setSupportShot(boolean supportShot) {
		this.supportShot = supportShot;
	}
	public int rollAttack(){
		return roll(attackValue);
	}
	public int rollDefend() {
		return roll(defendValue);
	}
	
	public String toString() {
		return name;
	}
	
	public static void main(String args[]) {
		OCUnit u = OCUnit.newHeavyBomber();
		for(int j=0;j<10000;j++)
		{
			System.out.println(u.rollAttack());
		}
	}

}
