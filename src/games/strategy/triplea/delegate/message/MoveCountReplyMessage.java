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

import games.strategy.engine.message.*;
import java.util.List;

/**
 * A reply from the server telling how many moves have been made.
 */

public class MoveCountReplyMessage implements Message
{
  //A list of UndoableMoves
  final List  m_moves;

  /**
   *
   * @param moves List - a list of undoable moves
   */
  public MoveCountReplyMessage(List moves)
  {
    m_moves = moves;
  }

  public List getMoves()
  {
    return m_moves;
  }

  public int getMoveCount()
  {
    return m_moves.size();
  }

}
