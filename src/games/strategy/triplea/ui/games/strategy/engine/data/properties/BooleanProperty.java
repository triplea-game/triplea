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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class BooleanProperty extends AEditableProperty
{
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = -7265501762343216435L;
  
  private boolean mValue;

  public BooleanProperty(String name, boolean defaultValue)
  {
    super(name);
    mValue = defaultValue;
  }


  public Object getValue()
  {
    return mValue ? Boolean.TRUE : Boolean.FALSE;
  }

  public void setValue(boolean aValue)
  {
      mValue = aValue;
  }
  
  /**
   *
   * @return component used to edit this property
   */
  public JComponent getEditorComponent()
  {
    final JCheckBox box = new JCheckBox("");
    box.setSelected(mValue);
    box.addActionListener(new ActionListener()
    {
                          public void actionPerformed(ActionEvent e)
                          {
                            mValue = box.isSelected();
                          }
    }

                          );

    return box;
  }


}
