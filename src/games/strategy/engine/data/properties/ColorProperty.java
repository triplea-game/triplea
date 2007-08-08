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
package games.strategy.engine.data.properties;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * User editable property representing a color.
 * <p>
 * Presents a clickable label with the currently selected color, 
 * through which a color swatch panel is accessable to change the color.
 * 
 * @author Lane O.B. Schwartz
 * @version $LastChangedDate$
 */
public class ColorProperty extends AEditableProperty
{
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = 6826763550643504789L;

  private final int m_max = 0xFFFFFF;
  private final int m_min = 0x000000;
  private Color m_color;
  
  public ColorProperty(String name, int def)
  {
    super(name);

    if(def > m_max || def < m_min)
        throw new IllegalThreadStateException("Default value out of range");

    m_color = new Color(def);
  }

  public Object getValue()
  {
    return m_color;
  }

  public JComponent getEditorComponent()
  {
      final JLabel label = new JLabel(" ") {
          public void paintComponent(Graphics g) 
          {
              Graphics2D g2 = (Graphics2D)g;
              g2.setColor(m_color);
              g2.fill(g2.getClip());
          }
      };
      
      label.addMouseListener(new MouseListener() {
          public void mouseClicked(MouseEvent e) {
              System.out.println(m_color);
              m_color = JColorChooser.showDialog(label, "Choose color", m_color);
              
              // Ask Swing to repaint this label when it's convenient
              SwingUtilities.invokeLater(new Runnable()
              {
                  public void run()
                  {
                      label.repaint();
                  }
              });
          }
          public void mouseEntered(MouseEvent e) {}
          public void mouseExited(MouseEvent e) {}
          public void mousePressed(MouseEvent e) {}
          public void mouseReleased(MouseEvent e) {}
      });
      
      return label;

  }

}
