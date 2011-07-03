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

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.util.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

public class UndoableMovesPanel extends AbstractUndoableMovesPanel
{
    public UndoableMovesPanel(GameData data, AbstractMovePanel movePanel)
    {
        super(data,movePanel);
    }
    
    protected final void specificViewAction(AbstractUndoableMove move)
    {
        m_movePanel.getMap().setRoute(((UndoableMove)move).getRoute());
    }
}
