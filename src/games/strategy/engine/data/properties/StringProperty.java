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

import javax.swing.*;
import java.awt.event.*;

public class StringProperty extends AEditableProperty
{

  private String m_value;

  public StringProperty(String name, String defaultValue)
  {
    super(name);

    m_value = defaultValue;
  }

  public JComponent getEditorComponent()
  {
    final JTextField text =  new JTextField(m_value);
    text.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_value = text.getText();
      }
    }
    );

    return text;
  }

  public Object getValue()
  {
    return m_value;
  }
}
