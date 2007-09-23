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

public interface IPBEMMessenger extends java.io.Serializable
{
   public String getName();

   public boolean getNeedsUsername();

   public boolean getNeedsPassword();

   public boolean getCanViewPosted();

   public String getGameId();

   public String getUsername();

   public String getPassword();

   public void setGameId(String gameId);

   public void setUsername(String username);

   public void setPassword(String password);

   public void viewPosted();

}
