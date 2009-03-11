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

package games.strategy.kingstable.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.properties.PropertiesUI;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-06-29 12:22:06 -0500 (Fri, 29 Jun 2007) $
 */
public class KingsTableMenu extends BasicGameMenuBar<KingsTableFrame>
{

    public KingsTableMenu(KingsTableFrame frame) 
    {
        super(frame);
    }
    
    
    protected void createGameSpecificMenus (JMenuBar menuBar) 
    {
        createGameMenu(menuBar);
    }
    
    protected void addGameSpecificHelpMenus(JMenu helpMenu)
    {
        addHowToPlayHelpMenu(helpMenu);
    }
    
    /**
     * @param menuGame
     */
    private void createGameMenu(JMenuBar menuBar)
    {
        if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
        {
            JMenu menuGame = new JMenu("Game");
            menuBar.add(menuGame);
            
            AbstractAction optionsAction = new AbstractAction("View Game Options...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties(), false);
                    JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
                }
            };
            

            menuGame.add(optionsAction);

        }
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
                        "White wins by moving the king into a corner square.<br>" + 
                        "Black wins by capturing the king.</p>" +
                    
                    "<p><b>Moving:</b><br>" + 
                        "All pieces move like rooks in chess.<br>" + 
                        "Click on a piece, then drag it and drop it on an empty square.</p>" +
                        
                    "<p><b>Capturing the King</b><br>" + 
                        "The king can be captured by surrounding it on all four sides with opposing pieces, " + 
                        "or by surrounding it on three sides with opposing pieces, with the center square on the fourth side. " +
                        
                    "<p><b>Capturing a Pawn</b><br>" +    
                        "A pawn can be captured by sandwiching it between two of the opposing side's pieces, " + 
                        "or by sandwiching it between an opposing piece and a corner square.<br>" +
                        "Note: If a pawn moves into one of these two situations, that does not cause it to be captured.<br>" +
                        "Note: The king may participate in a capture.</p>" + 
                        
                    "<p><b>Restrictions</b><br>" + 
                        "Only the king may occupy the center square and the corner squares.<br>" + 
                        "Pawns may not occupy the center square or the corner squares.<br>" +
                        "However, any piece may move through the center square.</p>";
                
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
    
  
    /*
    protected void createGameSpecificMenus (JMenuBar menuBar) 
    {
        if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
        {
            JMenu menuGame = new JMenu("Game");
            menuBar.add(menuGame);
            
            AbstractAction optionsAction = new AbstractAction("View Game Options...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties(), true);
                    JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
                }
            };

            menuGame.add(optionsAction);   
        }
    }
*/
    
}
