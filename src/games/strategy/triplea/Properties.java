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

package games.strategy.triplea;

import games.strategy.engine.data.*;

/**
 * <p>Title: TripleA</p>
 * @author Sean Bridges
 *
 */

public class Properties implements Constants
{

	public static int getNeutralCharge(GameData data)
	{
        try
        {
          return Integer.parseInt((String) data.getProperties().get(NEUTRAL_CHARGE_PROPERTY) );
        } catch(Exception e)
        {
          return 3;
        }
	}


    public static int getFactoriesPerCountry(GameData data)
    {
      try
      {
        return Integer.parseInt((String) data.getProperties().get(FACTORIES_PER_COUNTRY_PROPERTY) );
      } catch(Exception e)
      {
        return 1;
      }

    }

    private Properties()
    {
    }
}