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
import games.strategy.engine.data.Unit;

import java.util.ArrayList;
import java.util.List;

public class ProBattleResultData
{
	private double winPercentage;
	private double TUVSwing;
	private boolean hasLandUnitRemaining;
	private List<Unit> averageUnitsRemaining;
	
	public ProBattleResultData()
	{
		winPercentage = 0;
		TUVSwing = 0;
		hasLandUnitRemaining = false;
		averageUnitsRemaining = new ArrayList<Unit>();
	}
	
	public ProBattleResultData(double winPercentage, double TUVSwing, boolean hasLandUnitRemaining, List<Unit> averageUnitsRemaining)
	{
		this.winPercentage = winPercentage;
		this.TUVSwing = TUVSwing;
		this.hasLandUnitRemaining = hasLandUnitRemaining;
		this.averageUnitsRemaining = averageUnitsRemaining;
	}
	
	public double getWinPercentage()
	{
		return winPercentage;
	}
	
	public void setWinPercentage(double winPercentage)
	{
		this.winPercentage = winPercentage;
	}
	
	public double getTUVSwing()
	{
		return TUVSwing;
	}
	
	public void setTUVSwing(double tUVSwing)
	{
		TUVSwing = tUVSwing;
	}
	
	public boolean isHasLandUnitRemaining()
	{
		return hasLandUnitRemaining;
	}
	
	public void setHasLandUnitRemaining(boolean hasLandUnitRemaining)
	{
		this.hasLandUnitRemaining = hasLandUnitRemaining;
	}
	
	public void setAverageUnitsRemaining(List<Unit> averageUnitsRemaining)
	{
		this.averageUnitsRemaining = averageUnitsRemaining;
	}
	
	public List<Unit> getAverageUnitsRemaining()
	{
		return averageUnitsRemaining;
	}
	
}
