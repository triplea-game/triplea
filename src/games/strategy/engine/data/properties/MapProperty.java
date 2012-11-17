package games.strategy.engine.data.properties;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComponent;

/**
 * Basically creates a map of other properties.
 * 
 * @author veqryn
 * 
 * @param <T>
 *            String or something with a valid toString()
 * @param <U>
 *            parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
public class MapProperty<T, U> extends AEditableProperty
{
	private static final long serialVersionUID = -8021039503574228146L;
	private Map<T, U> m_map;
	final List<IEditableProperty> m_properties = new ArrayList<IEditableProperty>();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MapProperty(final String name, final String description, final Map<T, U> map)
	{
		super(name, description);
		m_map = map;
		for (final Entry<T, U> entry : map.entrySet())
		{
			final String key = (String) entry.getKey();
			final U value = entry.getValue();
			if (value instanceof Boolean)
				m_properties.add(new BooleanProperty(key, description, ((Boolean) value)));
			else if (value instanceof Color)
				m_properties.add(new ColorProperty(key, description, ((Color) value)));
			else if (value instanceof File)
				m_properties.add(new FileProperty(key, description, ((File) value)));
			else if (value instanceof String)
				m_properties.add(new StringProperty(key, description, ((String) value)));
			else if (value instanceof Collection || value instanceof List || value instanceof Set)
				m_properties.add(new CollectionProperty(name, description, ((Collection) value)));
			else if (value instanceof Integer)
				m_properties.add(new NumberProperty(key, description, Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) value)));
			else if (value instanceof Double)
				m_properties.add(new DoubleProperty(key, description, Double.MAX_VALUE, Double.MIN_VALUE, ((Double) value), 5));
			else
				throw new IllegalArgumentException("Can not instantiate MapProperty with: " + value.getClass().getCanonicalName());
		}
	}
	
	@Override
	public int getRowsNeeded()
	{
		return Math.max(1, m_properties.size());
	}
	
	public Object getValue()
	{
		return m_map;
	}
	
	public Map<T, U> getValueT()
	{
		return m_map;
	}
	
	@SuppressWarnings("unchecked")
	public void setValue(final Object value) throws ClassCastException
	{
		m_map = (Map<T, U>) value;
	}
	
	public void setValueT(final Map<T, U> value)
	{
		m_map = value;
	}
	
	public JComponent getEditorComponent()
	{
		final PropertiesUI ui = new PropertiesUI(m_properties, true);
		return ui;
	}
	
	@Override
	public JComponent getViewComponent()
	{
		final PropertiesUI ui = new PropertiesUI(m_properties, false);
		return ui;
	}
	
}
