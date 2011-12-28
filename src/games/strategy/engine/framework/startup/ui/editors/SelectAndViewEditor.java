package games.strategy.engine.framework.startup.ui.editors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Allows you put multiple beans in a list and use drop down to select which bean to configure.
 * The bean's editor is displayed below the dropdown.
 * Use <code>setBeans</code> to set the beans edited by this editor, and <code>setSelectedBean</code> to select a specific bean
 * The editor automatically realigns the label of nested editors
 *
 * @author Klaus Groenbaek
 */
public class SelectAndViewEditor extends EditorPanel
{
	//-----------------------------------------------------------------------
	// instance fields 
	//-----------------------------------------------------------------------
	JComboBox m_selector = new JComboBox();
	JPanel m_view = new JPanel();
	private PropertyChangeListener _properChangeListener;
	private EditorPanel m_editor;
	private JLabel m_selectorLabel;

	//-----------------------------------------------------------------------
	// constructors 
	//-----------------------------------------------------------------------

	/**
	 * creates a new editor
	 * @param labelTitle the title in front of the combo box
	 */
	public SelectAndViewEditor(String labelTitle)
	{
		super();

		m_view.setLayout(new GridBagLayout());

		m_selectorLabel = new JLabel(labelTitle + ":");
		add(m_selectorLabel, new GridBagConstraints(0, 0, 1, 1, 0d, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 1, 2), 0, 0));
		add(m_selector, new GridBagConstraints(1, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 1, 0), 0, 0));

		add(m_view, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		m_selector.setRenderer(new DisplayNameComboBoxRender());
		m_selector.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					updateView();
					fireEditorChanged();
				}
			}
		});

		_properChangeListener = new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				fireEditorChanged();
			}
		};
	}

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------

	/**
	 * Updates the view panel below the combo box.
	 *
	 */
	private void updateView()
	{
		// todo(kg) Have the View use a card layout instead of removing all content
		// remove listeners from old editor, to avoid memory leak
		if (m_editor != null)
		{
			m_editor.removePropertyChangeListener(_properChangeListener);
		}

		m_view.removeAll();
		IBean item = (IBean) m_selector.getSelectedItem();
		m_editor = item.getEditor();
		if (m_editor != null)
		{
			// register a property change listener so we can re-notify our listeners
			m_editor.addPropertyChangeListener(_properChangeListener);
			m_view.add(m_editor, new GridBagConstraints(0, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			m_editor.isBeanValid();
		}
		revalidate();
		alignLabels();
	}

	/**
	 * Aligns label of this editor with the nested editor, by either resizing this (if it is smaller)
	 * or resizing the labels on the nested editor (if it is bigger)
	 */
	private void alignLabels()
	{
		// resize the label to align with the nested editors labels
		int height = m_selectorLabel.getPreferredSize().height;
		int width = m_selectorLabel.getPreferredSize().width;
		if (m_editor != null)
		{
			int labelWidth = m_editor.getLabelWidth();
			if (width < labelWidth)
			{
				// resize this editors label
				width = labelWidth;
			}
			else {
				// resize nested editors labels
				m_editor.setLabelWidth(width);
			}
		}
		Dimension dimension = new Dimension(width, height);
		m_selectorLabel.setPreferredSize(dimension);
		m_selectorLabel.setSize(dimension);
	}

	/**
	 * Sets the list of possible beans to choose from
	 *
	 * @param beans the list of beans
	 */
	public void setBeans(List<? extends IBean> beans)
	{
		m_selector.setModel(new DefaultComboBoxModel(beans.toArray()));
		updateView();
	}


	@Override
	public boolean isBeanValid()
	{
		return m_editor == null || m_editor.isBeanValid();
	}

	/**
	 * Returns the bean being edited
	 * @return the current bean, or null if the bean doesn't have an editor (is disabled)
	 */
	@Override
	public IBean getBean()
	{
		if (m_editor == null) {
			return null;
		}
		return m_editor.getBean();
	}

	/**
	 * Sets the bean on this editor.
	 * If an editor of the same class is found, it is selected an modified to match
	 * If no bean of this type is found, it is added to the list
	 *
	 * @param bean the bean
	 */
	public void setSelectedBean(IBean bean)
	{
		DefaultComboBoxModel model = (DefaultComboBoxModel) m_selector.getModel();
		DefaultComboBoxModel newModel = new DefaultComboBoxModel();
		boolean found = false;
		int i;
		for (i = 0; i < model.getSize(); i++)
		{
			IBean candidate = (IBean) model.getElementAt(i);
			if (candidate.sameType(bean))
			{
				found = true;
				newModel.addElement(bean);
			} else
			{
				newModel.addElement(candidate);
			}
		}

		if (found)
		{
			m_selector.setModel(newModel);
		} else
		{
			model.addElement(bean);
		}
		m_selector.setSelectedItem(bean);
		updateView();
	}
}
