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
 * NOPUEndTurnDelegate.java
 * 
 * Created on August 11, 205, 2:16 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;

import java.util.Collection;

/**
 * 
 * @author Adam Jette
 * @version 1.0
 * 
 *          At the end of the turn collect NO income.
 */
@AutoSave(afterStepEnd = true)
public class NoPUEndTurnDelegate extends EndTurnDelegate
{
	@Override
	protected int getProduction(final Collection<Territory> territories)
	{
		return 0;
	}
	
	@Override
	protected void showEndTurnReport(final String endTurnReport)
	{
		// show nothing on purpose
	}
	
	/**
	 * Default behavior for this delegate is that we do not collect PU/resource income from territories, but we do collect and do any national objectives and triggers.
	 */
	@Override
	protected String doNationalObjectivesAndOtherEndTurnEffects(final IDelegateBridge bridge)
	{
		// TODO: add a step properties boolean for this (default = do this)
		return super.doNationalObjectivesAndOtherEndTurnEffects(bridge);
	}
	
	@Override
	protected String addOtherResources(final IDelegateBridge aBridge)
	{
		return "";
	}
}
