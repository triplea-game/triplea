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


package games.strategy.engine.history;

import javax.swing.tree.DefaultMutableTreeNode;
import games.strategy.engine.data.*;

public class Step extends IndexedHistoryNode
{
    private String m_stepName;
    private String m_delegateName;
    private PlayerID m_player;
    private String m_displayName;

    /** Creates a new instance of StepChangedMessage */

  /** Creates a new instance of StepChangedMessage */
  Step(String stepName, String delegateName, PlayerID player, int changeStartIndex,  String displayName)
  {
    super(displayName, changeStartIndex, true);

    m_delegateName = delegateName;
    m_player = player;
    m_stepName = stepName;
    m_displayName = displayName;
  }

  public PlayerID getPlayerID()
  {
    return m_player;
  }


}
