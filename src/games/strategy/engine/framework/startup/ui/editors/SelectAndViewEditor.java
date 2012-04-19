package games.strategy.engine.framework.startup.ui.editors;

import games.strategy.triplea.help.HelpSupport;
import games.strategy.triplea.ui.JButtonDialog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
	private static final long serialVersionUID = 1580648148539524876L;
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	JComboBox m_selector = new JComboBox();
	JPanel m_view = new JPanel();
	JButton m_helpButton = new JButton("Help?");
	private final PropertyChangeListener m_properChangeListener;
	private EditorPanel m_editor;
	private final JLabel m_selectorLabel;
	private final JEditorPane m_helpPanel;
	private final String m_defaultHelp;
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	/**
	 * creates a new editor
	 * 
	 * @param labelTitle
	 *            the title in front of the combo box
	 * @param defaultHelp
	 *            the name of the Help file to use when no bean is selected (when disabled)
	 */
	public SelectAndViewEditor(final String labelTitle, final String defaultHelp)
	{
		super();
		m_defaultHelp = defaultHelp;
		
		final Font oldFont = m_helpButton.getFont();
		m_helpButton.setFont(new Font(oldFont.getName(), Font.BOLD, oldFont.getSize()));
		
		m_view.setLayout(new GridBagLayout());
		
		m_selectorLabel = new JLabel(labelTitle + ":");
		add(m_selectorLabel, new GridBagConstraints(0, 0, 1, 1, 0d, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 1, 2), 0, 0));
		add(m_selector, new GridBagConstraints(1, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 1, 0), 0, 0));
		add(m_helpButton, new GridBagConstraints(2, 0, 1, 1, 0d, 0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 1, 0), 0, 0));
		
		add(m_view, new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		m_selector.setRenderer(new DisplayNameComboBoxRender());
		m_selector.addItemListener(new ItemListener()
		{
			public void itemStateChanged(final ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					updateView();
					fireEditorChanged();
				}
			}
		});
		
		m_properChangeListener = new PropertyChangeListener()
		{
			public void propertyChange(final PropertyChangeEvent evt)
			{
				fireEditorChanged();
			}
		};
		
		m_helpPanel = new JEditorPane();
		m_helpPanel.setEditable(false);
		m_helpPanel.setContentType("text/html");
		m_helpPanel.setAutoscrolls(true);
		m_helpPanel.setBackground(m_selectorLabel.getBackground());
		final Dimension preferredSize = new Dimension(500, 500);
		m_helpPanel.setPreferredSize(preferredSize);
		m_helpPanel.setSize(preferredSize);
		
		final JScrollPane notesScroll = new JScrollPane();
		notesScroll.setViewportView(m_helpPanel);
		notesScroll.setBorder(null);
		notesScroll.getViewport().setBorder(null);
		
		m_helpButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				String helpText;
				if (getBean() == null)
				{
					helpText = HelpSupport.loadHelp(m_defaultHelp);
				}
				else
				{
					helpText = getBean().getHelpText();
				}
				m_helpPanel.setText(helpText);
				JButtonDialog.showDialog(SelectAndViewEditor.this, "Help", notesScroll, "Close");
			}
		});
		
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
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
			m_editor.removePropertyChangeListener(m_properChangeListener);
		}
		
		m_view.removeAll();
		final IBean item = (IBean) m_selector.getSelectedItem();
		m_editor = item.getEditor();
		if (m_editor != null)
		{
			// register a property change listener so we can re-notify our listeners
			m_editor.addPropertyChangeListener(m_properChangeListener);
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
		final int height = m_selectorLabel.getPreferredSize().height;
		int width = m_selectorLabel.getPreferredSize().width;
		if (m_editor != null)
		{
			final int labelWidth = m_editor.getLabelWidth();
			if (width < labelWidth)
			{
				// resize this editors label
				width = labelWidth;
			}
			else
			{
				// resize nested editors labels
				m_editor.setLabelWidth(width);
			}
		}
		final Dimension dimension = new Dimension(width, height);
		m_selectorLabel.setPreferredSize(dimension);
		m_selectorLabel.setSize(dimension);
	}
	
	/**
	 * Sets the list of possible beans to choose from
	 * 
	 * @param beans
	 *            the list of beans
	 */
	public void setBeans(final List<? extends IBean> beans)
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
	 * 
	 * @return the current bean, or null if the bean doesn't have an editor (is disabled)
	 */
	@Override
	public IBean getBean()
	{
		if (m_editor == null)
		{
			return null;
		}
		return m_editor.getBean();
	}
	
	/**
	 * Sets the bean on this editor.
	 * If an editor of the same class is found, it is selected an modified to match
	 * If no bean of this type is found, it is added to the list
	 * 
	 * @param bean
	 *            the bean
	 */
	public void setSelectedBean(final IBean bean)
	{
		final DefaultComboBoxModel model = (DefaultComboBoxModel) m_selector.getModel();
		final DefaultComboBoxModel newModel = new DefaultComboBoxModel();
		boolean found = false;
		int i;
		for (i = 0; i < model.getSize(); i++)
		{
			final IBean candidate = (IBean) model.getElementAt(i);
			if (candidate.sameType(bean))
			{
				found = true;
				newModel.addElement(bean);
			}
			else
			{
				newModel.addElement(candidate);
			}
		}
		
		if (found)
		{
			m_selector.setModel(newModel);
		}
		else
		{
			model.addElement(bean);
		}
		m_selector.setSelectedItem(bean);
		updateView();
	}
}
