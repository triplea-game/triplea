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

import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class ListProperty extends AEditableProperty
{

  private List m_possibleValues;
  private String m_value;

  /**
   *
   * @param values A collection of Strings
   */
  public ListProperty(String name, String defaultValue, Collection possibleValues)
  {
    super(name);

    if(!possibleValues.contains(defaultValue))
      throw new IllegalStateException("possible values does not contain default");

    m_possibleValues = new ArrayList(possibleValues);
    m_value = defaultValue;
  }

  public String getValue()
  {
    return m_value;
  }

  public JComponent getEditorComponent()
  {
    final JComboBox box =  new JComboBox(new Vector(m_possibleValues));
    box.setSelectedItem(m_value);
    box.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_value = (String) box.getSelectedItem();
      }
    }
        );

    return box;

  }

}