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

/*
* UnitChooser.java
*
* Created on December 3, 2001, 7:32 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import games.strategy.ui.*;
import games.strategy.util.Util;
import games.strategy.util.IntegerMap;
import games.strategy.engine.data.*;
import games.strategy.triplea.image.*;
import games.strategy.triplea.image.*;
import games.strategy.triplea.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitChooser extends JPanel
{
  private List m_entries = new ArrayList();
  private Map m_dependents = new HashMap();
  private JTextArea m_title;
  private int m_total = -1;
  private JLabel m_leftToSelect = new JLabel();
  private GameData m_data;

  /** Creates new UnitChooser */
  public UnitChooser(Collection units, Map dependent, IntegerMap movement, GameData data)
  {
    m_dependents = dependent;
    m_data = data;
    createEntries(units, dependent, movement);
    layoutEntries();
  }

  public UnitChooser(Collection units, Map dependent, GameData data)
  {
    m_dependents = dependent;
    m_data = data;
    createEntries(units, dependent, null);
    layoutEntries();
  }

  /**
   * Set the maximum number of units that we can choose.
   */
  public void setMax(int max)
  {
    m_total = max;
    m_textFieldListener.changedValue(null);
  }

  public void setTitle(String title)
  {
    m_title.setText(title);
  }

  private void updateLeft()
  {
    if(m_total == -1)
      return;

    int selected = 0;
    Iterator iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = (ChooserEntry) iter.next();
      selected += entry.getQuantity();
    }

    m_leftToSelect.setText("Left to select:" + (m_total - selected));

    iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = (ChooserEntry) iter.next();
      entry.setLimit(m_total - selected);
    }

    m_leftToSelect.setText("Left to select:" + (m_total - selected));
  }


  private void createEntries(Collection units, Map dependent, IntegerMap movement)
  {
    Collection categories = UnitSeperator.categorize(units, dependent, movement);
    Iterator iter = categories.iterator();
    while(iter.hasNext())
    {
      UnitCategory category = (UnitCategory) iter.next();
      addEntry(category);
    }
  }

  private void addEntry(UnitCategory category)
  {
    ChooserEntry entry = new ChooserEntry(category, m_textFieldListener, m_data);
    m_entries.add(entry);

  }

  private void layoutEntries()
  {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    m_title = new JTextArea("Choose units");
    m_title.setBackground(this.getBackground());
    m_title.setEditable(false);
    m_title.setColumns(15);
    m_title.setWrapStyleWord(true);
    this.add(m_title);
    Iterator iter = m_entries.iterator();
    while(iter.hasNext())
    {
      addEntry( (ChooserEntry) iter.next());
    }
    JPanel leftToSelectPanel = new JPanel();
    leftToSelectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    leftToSelectPanel.add(m_leftToSelect);
    add(leftToSelectPanel);
  }


  private void addEntry(ChooserEntry entry)
  {
    JPanel panel = new JPanel();
    panel.add(entry);

    StringBuffer text = new StringBuffer();
    text.append("max " + entry.getCategory().getUnits().size());

    panel.add(new JLabel(text.toString()));
    this.add(panel);
  }

  public Collection getSelected()
  {
    return getSelected(true);
  }

  public Collection getSelected(boolean selectDependents)
  {
    Collection units = new ArrayList();

    Iterator entries = m_entries.iterator();
    while(entries.hasNext())
    {
      ChooserEntry current = (ChooserEntry) entries.next();
      int count = current.getQuantity();
      addToCollection(units, count, current, selectDependents);

    }
    return units;
  }

  private void addToCollection(Collection addTo, int quantity, ChooserEntry entry, boolean addDependents)
  {
    Collection possible = entry.getCategory().getUnits();
    if(possible.size() < quantity)
      throw new IllegalStateException("Not enough units");

    Iterator iter = possible.iterator();
    for(int i = 0; i < quantity; i++)
    {
      Unit current = (Unit) iter.next();
      addTo.add(current);
      if(addDependents)
      {
        Collection dependents = (Collection) m_dependents.get(current);
        if(dependents != null)
          addTo.addAll(dependents);
      }
    }
  }

  private ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener()
  {
    public void changedValue(ScrollableTextField field)
    {
      updateLeft();
    }
  };
}

class ChooserEntry extends JPanel
{
  private UnitCategory m_category;
  private ScrollableTextField m_text;
  private ScrollableTextFieldListener m_textFieldListener;
  private GameData m_data;

  ChooserEntry(UnitCategory category, ScrollableTextFieldListener listener, GameData data)
  {
    m_textFieldListener = listener;
    m_data = data;
    m_category = category;

    add(new UnitChooserEntryIcon());
    if(m_category.getMovement() != -1)
      add(new JLabel("mvt " + m_category.getMovement()));

    m_text = new ScrollableTextField(0, category.getUnits().size());
    m_text.addChangeListener(m_textFieldListener);
    add(m_text);

  }

  public UnitCategory getCategory()
  {
    return m_category;
  }


  public void setLimit(int limit)
  {
    int newMax = limit + m_text.getValue();

    m_text.setMax(Math.min(newMax, m_category.getUnits().size()));
  }


  public int getQuantity()
  {
    return m_text.getValue();
  }

  public int getMovement()
  {
    return m_category.getMovement();
  }

  class UnitChooserEntryIcon extends JComponent
  {
    public void paint(Graphics g)
    {
      super.paint(g);
      g.drawImage( UnitIconImageFactory.instance().getImage(m_category.getType(), m_category.getOwner(), m_data), 0,0,this);
      Iterator iter = m_category.getDependents().iterator();
      int index = 1;
      while(iter.hasNext())
      {
        UnitOwner holder = (UnitOwner) iter.next();
        int x = UnitIconImageFactory.UNIT_ICON_WIDTH * index;
        Image unitImg = UnitIconImageFactory.instance().getImage(holder.getType(), holder.getOwnerr(), m_data);
        g.drawImage(unitImg, x, 0, this);


        index++;
      }
    }

    public int getWidth()
    {
      //we draw a unit symbol for each dependent
      return UnitIconImageFactory.UNIT_ICON_WIDTH * (1 + m_category.getDependents().size());
    }

    public int getHeight()
    {
      return UnitIconImageFactory.UNIT_ICON_HEIGHT;
    }

    public Dimension getMaximumSize()
    {
      return getDimension();
    }

    public Dimension getMinimumSize()
    {
      return getDimension();
    }

    public Dimension getPreferredSize()
    {
      return getDimension();
    }

    public Dimension getDimension()
    {
      return new Dimension(getWidth(), getHeight());
    }
  }
}

