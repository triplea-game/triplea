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

import javax.swing.*;

import games.strategy.ui.*;
import games.strategy.util.Util;
import games.strategy.util.IntegerMap;

import games.strategy.engine.data.*;

import games.strategy.triplea.image.*;
import games.strategy.triplea.image.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitChooser extends JPanel
{
	/** 
	 * Maps ChooserEntry -> Collection of units
	 */
	private Map m_entries = new HashMap();
	private Map m_dependents = new HashMap();
	private JTextArea m_title;
	private int m_total = -1;
	private JLabel m_leftToSelect = new JLabel();
	
	/** Creates new UnitChooser */
    public UnitChooser(Collection units, Map dependent, IntegerMap movement) 
	{	
		createEntries(units, dependent, movement);
		layoutEntries();
    }

	public UnitChooser(Collection units, Map dependent) 
	{	
		createEntries(units, dependent, null);
		layoutEntries();
    }

	
	public void setTitle(String title)
	{
		m_title.setText(title);
	}
	
	public void setMax(int max)
	{
		if(m_total == -1)
			add(m_leftToSelect);
		m_total = max;
		updateLeft();
	}
	
	private void updateLeft()
	{
		if(m_total == -1)
			return;
		
		int selected = 0;
		Iterator iter = m_entries.keySet().iterator();
		while(iter.hasNext())
		{
			ChooserEntry entry = (ChooserEntry) iter.next();
			selected += entry.getQuantity();
		}
		m_leftToSelect.setText("Left to select:" + (m_total - selected));
		
		iter = m_entries.keySet().iterator();
		while(iter.hasNext())
		{
			ChooserEntry entry = (ChooserEntry) iter.next();
			entry.setLimit(m_total - selected);
		}
		
		m_leftToSelect.setText("Left to select:" + (m_total - selected));
	}

	
	private void createEntries(Collection units, Map dependent, IntegerMap movement)
	{
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit current = (Unit) iter.next();
			
			
			int unitMovement = -1;
			if(movement != null)
				unitMovement = movement.getInt(current);
			
			
			Collection currentDependents = (Collection) dependent.get(current);
			ChooserEntry entry = new ChooserEntry(current, currentDependents,unitMovement , m_textFieldListener);
			addEntry(entry, current);
			addDependent(current, (Collection) dependent.get(current));
		}
	}
	
	private void addEntry(ChooserEntry entry, Unit unit)
	{
		Collection units = (Collection) m_entries.get(entry);
		if(units == null)
		{
			units = new ArrayList();
			m_entries.put(entry, units);
		}
		units.add(unit);
	}
	
	private void addDependent(Unit unit, Collection dependent)
	{
		if(dependent != null)
			m_dependents.put(unit, dependent);
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
		Iterator iter = m_entries.keySet().iterator();
		while(iter.hasNext())
		{
			addEntry( (ChooserEntry) iter.next());
		}
	}
	
	
	private void addEntry(ChooserEntry entry)
	{
		JPanel panel = new JPanel();
		int max = ( (Collection) m_entries.get(entry)).size();
		entry.setMax(max);
		panel.add(entry);
		
		StringBuffer text = new StringBuffer();
		text.append("max " + max);
		
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
		
		Iterator entries = m_entries.keySet().iterator();
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
		Collection possible = (Collection) m_entries.get(entry);
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
	private UnitType m_type;
	//Collection of UnitOwners
	private Collection m_dependents;
	private ScrollableTextField m_text;
	private ScrollableTextFieldListener m_textFieldListener;
	private int m_max; //max we can select
	private int m_movement; //movement of the unit
	
	ChooserEntry(Unit unit, Collection dependents, int movement, ScrollableTextFieldListener listener)
	{
		m_textFieldListener = listener;
		m_type = unit.getType();
		m_dependents = new ArrayList();
		m_movement = movement;
	
		createDependents(dependents);
		
		add(new UnitChooserEntryIcon());
		if(m_movement != -1)
			add(new JLabel("mvt " + m_movement));
	}
	
	public void setMax(int max)
	{
		m_max = max;
		m_text = new ScrollableTextField(0, m_max);
		m_text.addChangeListener(m_textFieldListener);
		add(m_text);
	}
	
	public void setLimit(int limit)
	{
		int newMax = limit + m_text.getValue();
		
		m_text.setMax(Math.min(newMax, m_max));
	}
		
	private void createDependents(Collection dependents)
	{
		if(dependents == null)
			return;
		
		Iterator iter = dependents.iterator();
		
		while(iter.hasNext())
		{
			Unit current = (Unit) iter.next();
			m_dependents.add(new UnitOwner(current));
		}	
	}
	
	public boolean equals(Object o)
	{
		ChooserEntry other = (ChooserEntry) o;
		
		return other.m_type.equals(this.m_type) &&  other.m_movement == this.m_movement &&
		       Util.equals(this.m_dependents, other.m_dependents);
	}
	
	public int hashCode()
	{
		return m_type.hashCode();
	}
	
	public String toString()
	{
		return "Entry type:" + m_type.getName() + " dependenents:" + m_dependents;
	}
	
	public int getQuantity()
	{
		return m_text.getValue();
	}

	public int getMovement()
	{
		return m_movement;
	}
	
	class UnitChooserEntryIcon extends JComponent
	{
		public void paint(Graphics g)
		{
			super.paint(g);
			g.drawImage( UnitIconImageFactory.instance().getImage(m_type), 0,0,this);
			Iterator iter = m_dependents.iterator();
			int index = 1;
			while(iter.hasNext())
			{
				UnitOwner holder = (UnitOwner) iter.next();
				holder.paint(index, g, this);
				index++;
			}
		}
		
		public int getWidth()
		{
			return UnitIconImageFactory.UNIT_ICON_WIDTH * (1 + m_dependents.size()); 
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

class UnitOwner
{
	private UnitType type;
	private PlayerID owner;
	
	UnitOwner(Unit unit)
	{
		type = unit.getType();
		owner = unit.getOwner();
	}
	
	public boolean equals(Object o)
	{
		UnitOwner other = (UnitOwner) o;
		return other.type.equals(this.type) &&
			other.owner.equals(this.owner);
	}
	
	public int hashCode()
	{
		return type.hashCode() ^ owner.hashCode();
	}
	
	public void paint(int index, Graphics g, JComponent parent)
	{
		int x = UnitIconImageFactory.UNIT_ICON_WIDTH * index;
		Image unitImg = UnitIconImageFactory.instance().getImage(type);
		g.drawImage(unitImg, x, 0, parent);
		
		Image flagImage = FlagIconImageFactory.instance().getSmallFlag(owner);
		g.drawImage(flagImage, x, 0, parent);
	}
	
	public String toString()
	{
		return "Unit owner:" + owner + " type:" + type;
	}
}
