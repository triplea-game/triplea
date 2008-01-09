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

package games.puzzle.slidingtiles.ui;

import games.strategy.common.ui.BasicGameMenuBar;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * Represents the menu bar for an n-puzzle game.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class NPuzzleMenu extends BasicGameMenuBar<NPuzzleFrame>
{

    public NPuzzleMenu(NPuzzleFrame frame) 
    {
        super(frame);
    }
    
    
    protected void addGameSpecificHelpMenus(JMenu helpMenu)
    {
        addHowToPlayHelpMenu(helpMenu);
    }
    
    
    /**
     * @param parentMenu
     */
    private void addHowToPlayHelpMenu(JMenu parentMenu)
    {
        parentMenu.add(new AbstractAction("How to play...")
        {
            public void actionPerformed(ActionEvent e)
            {
                //html formatted string
                String hints = 
                    
                    "<p><b>Winning</b><br>" + 
                        "Rearrange the tiles into numerical order, with the blank square in the upper left corner.</p>" +
                    
                    "<p><b>Moving:</b><br>" + 
                        "Any square which is horizontally or vertically adjacent to the blank square may be moved into the blank square</p>";
                
                JEditorPane editorPane = new JEditorPane();
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(hints);
                editorPane.setPreferredSize(new Dimension(550,380));
                
                JScrollPane scroll = new JScrollPane(editorPane);

                JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
            }
        });
    }
    
}
