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

    public String getName()
    {
        return "disabled";
    }

    public boolean getNeedsUsername()
    {
        return false;
    }

    public boolean getNeedsPassword()
    {
        return false;
    }

    public boolean getCanViewPosted()
    {
        return false;
    }

    public void setGameId(String s)
    {
    }

    public void setUsername(String s)
    {
    }

    public void setPassword(String s)
    {
    }

    public String getGameId()
    {
        return null;
    }

    public String getUsername()
    {
        return null;
    }

    public String getPassword()
    {
        return null;
    }

    public void viewPosted()
    {
    }

    public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName)
    {
    }

    public void gameDataChanged(Change change)
    {
    }

    public String toString()
    {
        return getName();
    }
}
