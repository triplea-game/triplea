package games.strategy.engine.framework.startup.ui.editors;

import games.strategy.engine.framework.startup.ui.editors.validators.NonEmptyValidator;
import games.strategy.engine.framework.startup.ui.editors.validators.IValidator;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * Base class for editors, contains a number of utility methods, suce a validation
 *
 * @author Klaus Groenbaek
 */
public abstract class EditorPanel extends JPanel
{
	public static final String EDITOR_CHANGE = "EditorChange";
	//-----------------------------------------------------------------------
	// instance fields 
	//-----------------------------------------------------------------------
	protected final Color m_labelColor;

	//-----------------------------------------------------------------------
	// constructor
	//-----------------------------------------------------------------------
	public EditorPanel()
	{
		super(new GridBagLayout());
		m_labelColor = new JLabel().getForeground();

	}

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		super.addPropertyChangeListener(EDITOR_CHANGE, listener);
	}

	protected boolean validateTextField(JTextField field, JLabel label)
	{
		return validateTextField(field, label, new NonEmptyValidator());
	}

	protected boolean validateTextField(JTextField field, JLabel label, IValidator IValidator)
	{
	  boolean valid = true;
		if (!IValidator.isValid(field.getText()))
		{
			valid = false;
			label.setForeground(Color.RED);
		} else
		{
			label.setForeground(m_labelColor);
		}
		return valid;

	}


	public abstract boolean isInputValid();

	public abstract IBean getBean();


	/**
	 * Returns the Label width, this can be used by wrapping editors to try to align label sizes
	 * @return the size of the largest label in the first column
	 */
	public int getLabelWidth() {
		int width = 0;
		GridBagLayout layout = (GridBagLayout) getLayout();
		Component[] components = getComponents();
		for (Component component : components)
		{
			// label in first column
			if (component instanceof JLabel && layout.getConstraints(component).gridx == 0)
			{
				if (component.getPreferredSize().width > width) {
					width = component.getPreferredSize().width;
				}
			}
		}
		return width;
	}

	protected boolean emptyOrNull(String str)
	{
		return str == null || str.trim().equals("");
	}

	public void setLabelWidth(int width)
	{
		GridBagLayout layout = (GridBagLayout) getLayout();
		Component[] components = getComponents();
		for (Component component : components)
		{
			// label in first column
			if (component instanceof JLabel && layout.getConstraints(component).gridx == 0)
			{
			    int height = component.getPreferredSize().height;
				Dimension dimension = new Dimension(width, height);
				component.setPreferredSize(dimension);
				component.setSize(dimension);
			}
		}
	}
}
