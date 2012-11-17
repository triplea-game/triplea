package games.strategy.engine.data.properties;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;

/**
 * 
 * @author veqryn
 * 
 * @param <T>
 */
public class CollectionProperty<T> extends AEditableProperty
{
	private static final long serialVersionUID = 5338055034530377261L;
	private List<T> m_values;
	
	/**
	 * 
	 * @param name
	 *            name of the property
	 * @param defaultValue
	 *            default string value
	 * @param possibleValues
	 *            collection of Strings
	 */
	public CollectionProperty(final String name, final String description, final Collection<T> values)
	{
		super(name, description);
		m_values = new ArrayList<T>(values);
	}
	
	public Object getValue()
	{
		return m_values;
	}
	
	public List<T> getValueT()
	{
		return m_values;
	}
	
	@SuppressWarnings("unchecked")
	public void setValue(final Object value) throws ClassCastException
	{
		m_values = (List<T>) value;
	}
	
	public void setValueT(final List<T> value)
	{
		m_values = value;
	}
	
	@Override
	public int getRowsNeeded()
	{
		return (m_values == null ? 1 : Math.max(1, m_values.size()));
	}
	
	public JComponent getEditorComponent()
	{
		if (m_values == null)
			return new JTable();
		final Object[][] tableD = new Object[m_values.size()][1];
		for (int i = 0; i < m_values.size(); i++)
		{
			tableD[i][0] = m_values.get(i);
		}
		final JTable table = new JTable(tableD, new Object[] { "Values: " });
		table.addFocusListener(new FocusListener()
		{
			public void focusGained(final FocusEvent e)
			{
			}
			
			public void focusLost(final FocusEvent e)
			{
				// TODO: change m_values
			}
		});
		return table;
	}
}
