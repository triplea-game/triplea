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

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.message.*;
import games.strategy.engine.data.events.*;
import games.strategy.triplea.ui.TripleAFrame;

import games.strategy.triplea.delegate.message.*;


/**
 *
 * @author  Troy Graber
 * @version 1.0
 */
public class Personality
{
	private int Agressiveness = 5;
	private int Defensiveness = 5;
	private int Purchase = 2;
	private int Selfishness = 2;
	private int Attrition = 5;
	public Personality ()
	{
	}
	public Personality (int ag, int d, int p, int s, int at)
	{
		Agressiveness = ag;
		Defensiveness = d;
		Purchase = p;
		Selfishness = s;
		Attrition = at;
	}
	public int getAgressiveness()
	{
		return Agressiveness;
	}
	public int getDefensiveness()
	{
		return Defensiveness;
	}
	public int getPurchase()
	{
		return Purchase;
	}
	public int getSelfishness()
	{
		return Selfishness;
	}
	public int getAttrition()
	{
		return Attrition;
	}
}
	
