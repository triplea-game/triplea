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

package games.strategy.triplea.delegate.message;

/**
 * Sent from the delegate to the player.
 * Indicates the results of a bombing raid.
 */

public class BombingResults extends MultiDestinationMessage
{

  private int[] m_dice;
  private int m_cost;

  public BombingResults(int [] dice, int cost)
  {
    m_dice = dice;
    m_cost = cost;
  }

  public int[] getDice()
  {
    return m_dice;
  }

  public int getCost()
  {
    return m_cost;
  }
}