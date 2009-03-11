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

import java.awt.*;
import java.util.*;

import javax.swing.*;

public class PropertiesUI extends JPanel
{

  private final GameProperties m_properties;
  private int m_nextRow;


  public static void main(String[] args)
  {
    GameProperties properties = new GameProperties(null);

    properties.addEditableProperty(new BooleanProperty("bool1 default false", false));
    properties.addEditableProperty(new BooleanProperty("bool2 default true", true));
    properties.addEditableProperty(new StringProperty("String", "default") );
    properties.addEditableProperty(new NumberProperty("Number [10,20]", 20, 12, 15) );

    Collection<String> listValues = new ArrayList<String>();
    listValues.add("apples");
    listValues.add("oranges");
    listValues.add("bananas");
    properties.addEditableProperty(new ListProperty("List", "apples", listValues) );


    PropertiesUI ui = new PropertiesUI(properties, true);

    JFrame frame = new JFrame();
    frame.getContentPane().add(ui);
    frame.pack();
    frame.setVisible(true);


  }

  public PropertiesUI(GameProperties properties, boolean editable)
  {
    init();

    m_properties = properties;
    Iterator iter = m_properties.getEditableProperties().iterator();

    while(iter.hasNext())
    {
      IEditableProperty property = (IEditableProperty) iter.next();
      if(editable)
        addItem(property.getName(), property.getEditorComponent());
      else
        addItem(property.getName(), property.getViewComponent());
    }
  }

  private void init()
  {
      setLayout(new GridBagLayout());

      // Create a blank label to use as a vertical fill so that the
      // label/item pairs are aligned to the top of the panel and are not
      // grouped in the centre if the parent component is taller than
      // the preferred size of the panel.

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridx   = 0;
      constraints.gridy   = 99;
      constraints.insets  = new Insets(10, 0, 0, 0);
      constraints.weighty = 1.0;
      constraints.fill    = GridBagConstraints.VERTICAL;

      JLabel verticalFillLabel = new JLabel();

      add(verticalFillLabel, constraints);
  }



  private void addItem(String labelText, JComponent item)
  {
      // Create the label and its constraints

      JLabel label = new JLabel(labelText);

      GridBagConstraints labelConstraints = new GridBagConstraints();

      labelConstraints.gridx   = 0;
      labelConstraints.gridy   = m_nextRow;
      labelConstraints.insets  = new Insets(10, 10, 0, 0);
      labelConstraints.anchor  = GridBagConstraints.NORTHEAST;
      labelConstraints.fill    = GridBagConstraints.NONE;

      add(label, labelConstraints);

      // Add the component with its constraints

      GridBagConstraints itemConstraints = new GridBagConstraints();

      itemConstraints.gridx   = 1;
      itemConstraints.gridy   = m_nextRow;
      itemConstraints.insets  = new Insets(10, 10, 0, 10);
      itemConstraints.weightx = 1.0;
      itemConstraints.anchor  = GridBagConstraints.WEST;
      itemConstraints.fill    = GridBagConstraints.HORIZONTAL;

      add(item, itemConstraints);

      m_nextRow++;
    }



}
