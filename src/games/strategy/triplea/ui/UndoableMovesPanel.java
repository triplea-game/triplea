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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.*;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import java.util.*;

public class UndoableMovesPanel extends JPanel
{
    private List<UndoableMove> m_moves;
    private final GameData m_data;
    private final MovePanel m_movePanel;

    public UndoableMovesPanel(GameData data, MovePanel movePanel)
    {
        m_data = data;
        m_movePanel = movePanel;
    }

    public void setMoves(List<UndoableMove> moves)
    {
        m_moves = moves;
        initLayout();

    }

    private void initLayout()
    {
        removeAll();
        setLayout(new BorderLayout());

        JPanel items = new JPanel();

        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));

        //we want the newest move at the top
        m_moves = new ArrayList<UndoableMove>(m_moves);
        Collections.reverse(m_moves);
        Iterator iter = m_moves.iterator();

        if(iter.hasNext())
            add(new JLabel("Moves:"), BorderLayout.NORTH);

        Dimension seperatorSize = new Dimension(150,20);
        while (iter.hasNext())
        {

            UndoableMove item = (UndoableMove)iter.next();
            items.add(createComponentForMove(item));

            if(iter.hasNext())
            {
                JSeparator seperator = new JSeparator(SwingConstants.HORIZONTAL);
                seperator.setPreferredSize(seperatorSize);
                seperator.setMaximumSize(seperatorSize);
                items.add(seperator);
            }
        }

        JScrollPane scroll = new JScrollPane(items);
        scroll.setBorder(null);

        add(scroll, BorderLayout.CENTER);
        validate();

    }

    private JComponent createComponentForMove(UndoableMove move)
    {
        Box unitsBox = new Box(BoxLayout.X_AXIS);
        unitsBox.add(new JLabel((move.getIndex() + 1) + ") "));
        Collection unitCategories = UnitSeperator.categorize(move.getUnits());
        Iterator iter = unitCategories.iterator();
        Dimension buttonSize = new Dimension(80,22);
        while (iter.hasNext())
        {
            UnitCategory category = (UnitCategory)iter.next();
            Icon icon = UnitImageFactory.instance().getIcon(category.getType(), category.getOwner(), m_data, category.getDamaged());
            JLabel label =  new JLabel("x" + category.getUnits().size() + " ",  icon , SwingConstants.LEFT );
            unitsBox.add(label);
        }

        unitsBox.add(Box.createHorizontalGlue());


        JLabel text = new JLabel(move.getRoute().getStart() + " -> " + move.getRoute().getEnd());
        Box textBox = new Box(BoxLayout.X_AXIS);
        textBox.add(text);
        textBox.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton(new UndoMoveAction(move.getIndex()));
        setSize(buttonSize, cancelButton);

        JButton viewbutton = new JButton(new ViewAction(move));
        setSize(buttonSize, viewbutton);

        Box buttonsBox = new Box(BoxLayout.X_AXIS);
        buttonsBox.add(viewbutton);
        buttonsBox.add(cancelButton);
        buttonsBox.add(Box.createHorizontalGlue());

        Box rVal = new Box(BoxLayout.Y_AXIS);
        rVal.add(unitsBox);
        rVal.add(textBox);
        rVal.add(buttonsBox);
        rVal.add(new JLabel(" "));


        return rVal;
    }

    private void setSize(Dimension buttonSize, JButton cancelButton)
    {
        cancelButton.setMinimumSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        cancelButton.setMaximumSize(buttonSize);
    }

    class UndoMoveAction extends  AbstractAction
    {
        private final int m_moveIndex;
        public UndoMoveAction(int index)
        {
            super("Undo");
            m_moveIndex = index;
        }

        public void actionPerformed(ActionEvent e)
        {
            m_movePanel.undoMove(m_moveIndex);
        }
    }

    class ViewAction extends  AbstractAction
    {
        private final UndoableMove m_move;
        public ViewAction(UndoableMove move)
        {
            super("Show");
            m_move = move;
        }

        public void actionPerformed(ActionEvent e)
        {
           m_movePanel.cancelMove();
           if(!m_movePanel.getMap().isShowing(m_move.getRoute().getEnd()))
              m_movePanel.getMap().centerOn(m_move.getRoute().getEnd());
           m_movePanel.getMap().setRoute(m_move.getRoute());
        }
    }



}

