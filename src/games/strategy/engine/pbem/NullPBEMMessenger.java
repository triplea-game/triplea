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


package games.strategy.engine.pbem;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;

public class NullPBEMMessenger implements IPBEMMessenger
{

    public NullPBEMMessenger()
    {
    }

    @Override
	public String getName()
    {
        return "disabled";
    }

    @Override
	public boolean getNeedsUsername()
    {
        return false;
    }

    @Override
	public boolean getNeedsPassword()
    {
        return false;
    }

    @Override
	public boolean getCanViewPosted()
    {
        return false;
    }

    @Override
	public void setGameId(String s)
    {
    }

    @Override
	public void setUsername(String s)
    {
    }

    @Override
	public void setPassword(String s)
    {
    }

    @Override
	public String getGameId()
    {
        return null;
    }

    @Override
	public String getUsername()
    {
        return null;
    }

    @Override
	public String getPassword()
    {
        return null;
    }

    @Override
	public void viewPosted()
    {
    }

    public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName)
    {
    }

    public void gameDataChanged(Change change)
    {
    }

    @Override
	public String toString()
    {
        return getName();
    }
}
