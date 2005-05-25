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


import games.strategy.ui.*;

import javax.swing.JComponent;


public class NumberProperty extends AEditableProperty
{

  private int m_max;
  private int m_min;
  private int m_value;

  public NumberProperty(String name, int max, int min, int def)
  {
    super(name);

    if(max < min)
      throw new IllegalThreadStateException("Max must be greater than min");
    if(def > max || def < min)
        throw new IllegalThreadStateException("Default value out of range");

    m_max = max;
    m_min = min;
    m_value = def;
  }

  public Object getValue()
  {
    return "" + m_value;
  }

  public JComponent getEditorComponent()
  {
    IntTextField field = new  IntTextField(m_min, m_max);
    field.setValue(m_value);
    field.addChangeListener(new IntTextFieldChangeListener()
    {
      public void changedValue(IntTextField aField)
      {
        m_value = aField.getValue();
      }
    }
    );
    return field;
  }

}
