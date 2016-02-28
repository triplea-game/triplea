package util.triplea.MapXMLCreator;

import games.strategy.util.Triple;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
class GameSettingsRow extends DynamicRow
{	
	private boolean m_isBoolean;
	private JComboBox<String> m_tSettingName;
	private JTextField m_tValue;
	private JComboBox<String> m_tEditable;
	public static String[] selectionTrueFalse = {"false","true"};
	private JTextField m_tMinNumber;
	private JTextField m_tMaxNumber;
	
	public GameSettingsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String settingName, final String[] settingNames, final String value, final String editable,
				final int minNumber, final int maxNumber)
	{
		super(settingName, parentRowPanel, stepActionPanel);
		
		m_isBoolean = GameSettingsPanel.isBoolean(settingName);
		m_tSettingName = new JComboBox<String>(settingNames);
		m_tEditable = new JComboBox<String>(selectionTrueFalse);
		m_tValue = new JTextField(value);
		final Integer minCountInteger = Integer.valueOf(minNumber);
		m_tMinNumber = new JTextField(minCountInteger == null ? "0" : Integer.toString(minCountInteger));
		final Integer maxCountInteger = Integer.valueOf(maxNumber);
		m_tMaxNumber = new JTextField(maxCountInteger == null ? "0" : Integer.toString(maxCountInteger));
		
		Dimension dimension = m_tSettingName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_LARGE;
		m_tSettingName.setPreferredSize(dimension);
		try
		{
			m_tSettingName.setSelectedIndex(Arrays.binarySearch(settingNames, settingName));
		} catch (IllegalArgumentException e)
		{
			System.out.println(settingName + " is not known (yet)!");
		}
		m_tSettingName.addFocusListener(new FocusListener()
		{
			int prevSelectedIndex = m_tSettingName.getSelectedIndex();
			@Override
			public void focusLost(FocusEvent arg0)
			{
				if (prevSelectedIndex == m_tSettingName.getSelectedIndex())
					return;
				final String curr_settingName = (String) m_tSettingName.getSelectedItem();
				if (m_currentRowName.equals(curr_settingName))
					return;
				if (MapXMLHelper.s_gameSettings.containsKey(curr_settingName))
				{
					JOptionPane.showMessageDialog(stepActionPanel, "Game setting '" + curr_settingName + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tSettingName.setSelectedIndex(prevSelectedIndex);
							m_tSettingName.requestFocus();
						}
					});
					return;
				}
				// everything is okay with the new value name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_gameSettings.get(m_currentRowName);
				MapXMLHelper.s_gameSettings.remove(m_currentRowName);
				final boolean newIsBoolean = GameSettingsPanel.isBoolean(curr_settingName);
				if (newIsBoolean != m_isBoolean)
				{
					if (newIsBoolean)
					{
						newValues.set(0, "false");
						m_tValue.setText("false");
						m_tMinNumber.setEnabled(false);
						m_tMaxNumber.setEnabled(false);
					}
					else
					{
						newValues.set(0, "0");
						m_tValue.setText("0");
						m_tMinNumber.setEnabled(true);
						m_tMaxNumber.setEnabled(true);
					}
					newValues.set(2, "0");
					m_tMinNumber.setText("0");
					newValues.set(3, "0");
					m_tMaxNumber.setText("0");
					m_isBoolean = newIsBoolean;
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tValue.updateUI();
							m_tMinNumber.updateUI();
							m_tMaxNumber.updateUI();
							m_tValue.requestFocus();
							m_tValue.selectAll();
						}
					});
				}
				MapXMLHelper.s_gameSettings.put(curr_settingName, newValues);
				m_currentRowName = curr_settingName;
				prevSelectedIndex = m_tSettingName.getSelectedIndex();
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});


		dimension = m_tValue.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tValue.setPreferredSize(dimension);
		m_tValue.addFocusListener(new FocusListener()
		{
			String prevValue = value;
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tValue.getText().trim().toLowerCase();
				boolean isInputOkay = true;
				if (m_isBoolean)
				{
						final String parsedInputText = Boolean.toString(Boolean.parseBoolean(inputText));
					isInputOkay = parsedInputText.equals(inputText);
					if (!isInputOkay)
						JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is not a boolean value.", "Input error", JOptionPane.ERROR_MESSAGE);
				}
				else
				{
					try
					{
						Integer.parseInt(inputText);
					} catch (NumberFormatException e)
					{
						JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
						isInputOkay = false;
					}
				}
				if (!isInputOkay)
				{
					m_tValue.setText(prevValue);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tValue.updateUI();
							m_tValue.requestFocus();
							m_tValue.selectAll();
						}
					});
					return;
				}
				else
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tValue.updateUI();
						}
					});
				}
				prevValue = inputText;

				// everything is okay with the new value name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_gameSettings.get(m_currentRowName);
				newValues.set(0, inputText);
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tValue.selectAll();
			}
		});

		dimension = m_tEditable.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tEditable.setPreferredSize(dimension);
		m_tEditable.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, editable));
		m_tEditable.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent arg0)
			{
				// everything is okay with the new value name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_gameSettings.get(m_currentRowName);
				newValues.set(1, (String) m_tEditable.getSelectedItem());
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});
		
		dimension = m_tMinNumber.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tMinNumber.setPreferredSize(dimension);
		m_tMinNumber.setEnabled(!m_isBoolean);
		m_tMinNumber.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tMinNumber.getText().trim();
				try
				{
					final Integer newValue = Integer.parseInt(inputText);
					if (newValue < 0)
						throw new NumberFormatException();
					final Triple<String, String, Integer> oldTriple = MapXMLHelper.s_playerSequence.get(m_currentRowName);
					MapXMLHelper.s_playerSequence.put(m_currentRowName, new Triple<String, String, Integer>(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
				} catch (NumberFormatException e)
				{
					m_tMinNumber.setText("0");
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tMinNumber.updateUI();
							m_tMinNumber.requestFocus();
							m_tMinNumber.selectAll();
						}
					});
					return;
				}
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tMinNumber.selectAll();
			}
		});
		
		dimension = m_tMaxNumber.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tMaxNumber.setPreferredSize(dimension);
		m_tMaxNumber.setEnabled(!m_isBoolean);
		m_tMaxNumber.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tMaxNumber.getText().trim();
				try
				{
					final Integer newValue = Integer.parseInt(inputText);
					if (newValue < 0)
						throw new NumberFormatException();
					final Triple<String, String, Integer> oldTriple = MapXMLHelper.s_playerSequence.get(m_currentRowName);
					MapXMLHelper.s_playerSequence.put(m_currentRowName, new Triple<String, String, Integer>(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
				} catch (NumberFormatException e)
				{
					m_tMaxNumber.setText("0");
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tMaxNumber.updateUI();
							m_tMaxNumber.requestFocus();
							m_tMaxNumber.selectAll();
						}
					});
					return;
				}
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tMaxNumber.selectAll();
			}
		});
	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tValue);
		componentList.add(m_tSettingName);
		componentList.add(m_tEditable);
		componentList.add(m_tMinNumber);
		componentList.add(m_tMaxNumber);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tSettingName, gbc_template);
		
		final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
		gbc_tValue.gridx = 1;
		parent.add(m_tValue, gbc_tValue);
		
		final GridBagConstraints gbc_tEditable = (GridBagConstraints) gbc_template.clone();
		gbc_tEditable.gridx = 2;
		parent.add(m_tEditable, gbc_tEditable);
		
		final GridBagConstraints gbc_tMinNumber = (GridBagConstraints) gbc_template.clone();
		gbc_tMinNumber.gridx = 3;
		parent.add(m_tMinNumber, gbc_tMinNumber);
		
		final GridBagConstraints gbc_tMaxNumber = (GridBagConstraints) gbc_template.clone();
		gbc_tMaxNumber.gridx = 4;
		parent.add(m_tMaxNumber, gbc_tMaxNumber);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 5;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final GameSettingsRow newGameSettingsRow = (GameSettingsRow) newRow;
		this.m_tSettingName.setSelectedIndex(newGameSettingsRow.m_tSettingName.getSelectedIndex());
		this.m_tValue.setText(newGameSettingsRow.m_tValue.getText());
		this.m_tEditable.setSelectedIndex(newGameSettingsRow.m_tEditable.getSelectedIndex());
		this.m_tMinNumber.setText(newGameSettingsRow.m_tMinNumber.getText());
		this.m_tMaxNumber.setText(newGameSettingsRow.m_tMaxNumber.getText());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_gameSettings.remove(m_currentRowName);
	}
}
