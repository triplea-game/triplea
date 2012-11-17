package util.image;

import games.strategy.engine.data.properties.AEditableProperty;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.CollectionProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.ComboProperty;
import games.strategy.engine.data.properties.DoubleProperty;
import games.strategy.engine.data.properties.FileProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.MapProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.util.PropertyUtil;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * This will take ANY object, and then look at every method that begins with 'set[name]' and if there also exists a method 'get[name]'
 * and a field '[name]' which is public, then it will take these and create an editable UI component
 * for each of these based on the games.strategy.engine.data.properties classes.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 * @param <T>
 *            parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MapPropertyWrapper<T> extends AEditableProperty
{
	private static final long serialVersionUID = 6406798101396215624L;
	private IEditableProperty m_property;
	// private final Class m_clazz;
	// private final T m_defaultValue;
	private final Method m_setter;
	private final Method m_getter;
	
	public MapPropertyWrapper(final String name, final String description, final T defaultValue, final Method setter, final Method getter)
	{
		super(name, description);
		// m_clazz = clazz;
		m_setter = setter;
		m_getter = getter;
		// m_defaultValue = defaultValue;
		if (defaultValue instanceof Boolean)
			m_property = new BooleanProperty(name, description, ((Boolean) defaultValue));
		else if (defaultValue instanceof Color)
			m_property = new ColorProperty(name, description, ((Color) defaultValue));
		else if (defaultValue instanceof File)
			m_property = new FileProperty(name, description, ((File) defaultValue));
		else if (defaultValue instanceof String)
			m_property = new StringProperty(name, description, ((String) defaultValue));
		else if (defaultValue instanceof Collection || defaultValue instanceof List || defaultValue instanceof Set)
			m_property = new CollectionProperty(name, description, ((Collection) defaultValue));
		else if (defaultValue instanceof Map || defaultValue instanceof HashMap)
		{
			m_property = new MapProperty(name, description, ((Map) defaultValue));
		}
		else if (defaultValue instanceof Integer)
			m_property = new NumberProperty(name, description, Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) defaultValue));
		else if (defaultValue instanceof Double)
			m_property = new DoubleProperty(name, description, Double.MAX_VALUE, Double.MIN_VALUE, ((Double) defaultValue), 5);
		else
			throw new IllegalArgumentException("Can not instantiate PropertyWrapper with: " + defaultValue.getClass().getCanonicalName());
	}
	
	public MapPropertyWrapper(final String name, final String description, final T defaultValue, final Collection<T> possibleValues, final Method setter, final Method getter)
	{
		super(name, description);
		// m_clazz = clazz;
		m_setter = setter;
		m_getter = getter;
		// m_defaultValue = defaultValue;
		if (defaultValue instanceof Collection)
			m_property = new ComboProperty<T>(name, description, defaultValue, possibleValues);
		else
			throw new IllegalArgumentException("Can not instantiate PropertyWrapper with: " + defaultValue.getClass().getCanonicalName());
	}
	
	public MapPropertyWrapper(final String name, final String description, final int max, final int min, final int defaultValue, final Method setter, final Method getter)
	{
		super(name, description);
		// m_clazz = clazz;
		m_setter = setter;
		m_getter = getter;
		// m_defaultValue = defaultValue;
		m_property = new NumberProperty(name, description, max, min, defaultValue);
	}
	
	public MapPropertyWrapper(final String name, final String description, final double max, final double min, final double defaultValue, final int places, final Method setter, final Method getter)
	{
		super(name, description);
		// m_clazz = clazz;
		m_setter = setter;
		m_getter = getter;
		// m_defaultValue = defaultValue;
		m_property = new DoubleProperty(name, description, max, min, defaultValue, places);
	}
	
	public MapPropertyWrapper(final String name, final String description, final File defaultValue, final String[] acceptableSuffixes, final Method setter, final Method getter)
	{
		super(name, description);
		// m_clazz = clazz;
		m_setter = setter;
		m_getter = getter;
		// m_defaultValue = defaultValue;
		m_property = new FileProperty(name, description, defaultValue, acceptableSuffixes);
	}
	
	@Override
	public int getRowsNeeded()
	{
		return m_property.getRowsNeeded();
	}
	
	public Object getValue()
	{
		return m_property.getValue();
	}
	
	public T getValueT()
	{
		return (T) m_property.getValue();
	}
	
	public void setValue(final Object value) throws ClassCastException
	{
		m_property.setValue(value);
	}
	
	public void setValueT(final T value)
	{
		m_property.setValue(value);
	}
	
	public JComponent getEditorComponent()
	{
		return m_property.getEditorComponent();
	}
	
	public T getFromObject(final Object object)
	{
		try
		{
			return (T) m_getter.invoke(object);
		} catch (final IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (final IllegalAccessException e)
		{
			e.printStackTrace();
		} catch (final InvocationTargetException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public void setToObject(final Object object)
	{
		final Object value = getValue();
		final Object[] args = { value };
		try
		{
			System.out.println(m_setter + "   to   " + value);
			m_setter.invoke(object, args);
		} catch (final IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (final IllegalAccessException e)
		{
			e.printStackTrace();
		} catch (final InvocationTargetException e)
		{
			e.printStackTrace();
		}
	}
	
	public static List<MapPropertyWrapper> createProperties(final Object object)
	{
		final List<MapPropertyWrapper> properties = new ArrayList<MapPropertyWrapper>();
		for (final Method setter : object.getClass().getMethods())
		{
			final boolean startsWithSet = setter.getName().startsWith("set");
			if (!startsWithSet)
				continue;
			final String propertyName = setter.getName().substring(Math.min(3, setter.getName().length()), setter.getName().length());
			final Method getter;
			try
			{
				getter = object.getClass().getMethod("get" + propertyName);
			} catch (final SecurityException e)
			{
				e.printStackTrace();
				continue;
			} catch (final NoSuchMethodException e)
			{
				e.printStackTrace();
				continue;
			}
			final Field field = PropertyUtil.getFieldIncludingFromSuperClasses(object.getClass(), propertyName, false);
			final Object currentValue;
			// final Type type;
			try
			{
				// type = field.getGenericType();
				currentValue = field.get(object);
			} catch (final IllegalArgumentException e)
			{
				e.printStackTrace();
				continue;
			} catch (final IllegalAccessException e)
			{
				e.printStackTrace();
				continue;
			}
			try
			{
				final MapPropertyWrapper wrapper = new MapPropertyWrapper(propertyName, null, currentValue, setter, getter);
				properties.add(wrapper);
			} catch (final Exception e)
			{
				e.printStackTrace();
				continue;
			}
		}
		return properties;
	}
	
	public static void writePropertiesToObject(final Object object, final List<MapPropertyWrapper> properties)
	{
		for (final MapPropertyWrapper p : properties)
		{
			p.setToObject(object);
		}
	}
	
	public static PropertiesUI createPropertiesUI(final List<? extends IEditableProperty> properties, final boolean editable)
	{
		return new PropertiesUI(properties, editable);
	}
	
	public static Tuple<PropertiesUI, List<MapPropertyWrapper>> createPropertiesUI(final Object object, final boolean editable)
	{
		final List<MapPropertyWrapper> properties = createProperties(object);
		final PropertiesUI ui = new PropertiesUI(properties, editable);
		return new Tuple<PropertiesUI, List<MapPropertyWrapper>>(ui, properties);
	}
	
	public static void main(final String[] args)
	{
		final MapProperties mapProperties = new MapProperties();
		final List<MapPropertyWrapper> properties = createProperties(mapProperties);
		final PropertiesUI ui = createPropertiesUI(properties, true);
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(ui);
		frame.pack();
		frame.setVisible(true);
	}
}
