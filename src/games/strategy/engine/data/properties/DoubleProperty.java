package games.strategy.engine.data.properties;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.ui.DoubleTextField;
import games.strategy.ui.DoubleTextFieldChangeListener;

import java.io.File;
import java.math.BigDecimal;

import javax.swing.JComponent;

/**
 * 
 * @author veqryn
 * 
 */
public class DoubleProperty extends AEditableProperty
{
	private static final long serialVersionUID = 5521967819500867581L;
	private final double m_max;
	private final double m_min;
	private double m_value;
	private final int m_places;
	
	public DoubleProperty(final String name, final String description, final double max, final double min, final double def, final int numberOfPlaces)
	{
		super(name, description);
		if (max < min)
			throw new IllegalThreadStateException("Max must be greater than min");
		if (def > max || def < min)
			throw new IllegalThreadStateException("Default value out of range");
		m_max = max;
		m_min = min;
		m_places = numberOfPlaces;
		m_value = roundToPlace(def, numberOfPlaces, BigDecimal.ROUND_FLOOR);
	}
	
	public static double roundToPlace(final double number, final int places, final int BigDecimalRoundingMode)
	{
		BigDecimal bd = new BigDecimal(number);
		bd = bd.setScale(places, BigDecimalRoundingMode);
		return bd.doubleValue();
	}
	
	public Double getValue()
	{
		return m_value;
	}
	
	public void setValue(final Object value) throws ClassCastException
	{
		if (value instanceof String)
		{
			// warn developer which have run with the option cache when Number properties were stored as strings
			// todo (kg) remove at a later point
			throw new RuntimeException("Double and Number properties are no longer stored as Strings. You should delete your option cache, located at "
						+ new File(GameRunner2.getUserRootFolder(), "optionCache").toString());
		}
		else
		{
			m_value = roundToPlace((Double) value, m_places, BigDecimal.ROUND_FLOOR);
		}
	}
	
	public JComponent getEditorComponent()
	{
		final DoubleTextField field = new DoubleTextField(m_min, m_max);
		field.setValue(m_value);
		field.addChangeListener(new DoubleTextFieldChangeListener()
		{
			public void changedValue(final DoubleTextField aField)
			{
				m_value = aField.getValue();
			}
		});
		return field;
	}
}
