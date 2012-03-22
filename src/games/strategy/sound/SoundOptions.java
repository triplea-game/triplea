/**
 * Created on 12.03.2012
 */
package games.strategy.sound;

import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.ui.PropertiesSelector;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 * Sound option window framework.
 * 
 * @author Frigoref
 * 
 */
public final class SoundOptions
{
	final ClipPlayer m_clipPlayer;
	
	/**
	 * @param parentMenu
	 *            menu where to add the menu item "Sound Options..."
	 */
	public static void addToMenu(final JMenu parentMenu)
	{
		final JMenuItem soundOptions = new JMenuItem("Sound Options...");
		soundOptions.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				new SoundOptions(parentMenu);
			}
		});
		parentMenu.add(soundOptions);
	}
	
	public static void addToPanel(final JPanel parentPanel)
	{
		final JButton soundOptions = new JButton("Sound Options...");
		soundOptions.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				new SoundOptions(parentPanel);
			}
		});
		parentPanel.add(soundOptions);
	}
	
	public SoundOptions(final JComponent parent)
	{
		m_clipPlayer = ClipPlayer.getInstance();
		final String ok = "OK";
		final String cancel = "Cancel";
		final String selectAll = "All";
		final String selectNone = "None";
		final ArrayList<IEditableProperty> properties = m_clipPlayer.getSoundOptions(SoundPath.SoundType.TRIPLEA);
		final Object pressedButton = PropertiesSelector.getButton(parent, properties, new Object[] { ok, selectAll, selectNone, cancel });
		if (pressedButton.equals(ok))
		{
			for (final IEditableProperty property : properties)
			{
				m_clipPlayer.setMute(((SoundOptionCheckBox) property).getClipName(), !(Boolean) property.getValue());
			}
		}
		else if (pressedButton.equals(cancel))
		{
		}
		else if (pressedButton.equals(selectAll))
		{
			for (final IEditableProperty property : properties)
			{
				property.setValue(true);
				m_clipPlayer.setMute(((SoundOptionCheckBox) property).getClipName(), false);
			}
		}
		else if (pressedButton.equals(selectNone))
		{
			for (final IEditableProperty property : properties)
			{
				property.setValue(false);
				m_clipPlayer.setMute(((SoundOptionCheckBox) property).getClipName(), true);
			}
		}
	}
	
	public static void addGlobalSoundSwitchMenu(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");
		soundCheckBox.setSelected(!ClipPlayer.getBeSilent());
		soundCheckBox.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent e)
				{
					ClipPlayer.setBeSilent(!soundCheckBox.isSelected());
				}
			});
		parentMenu.add(soundCheckBox);
	}
	
	public static void addGlobalSoundSwitchCheckbox(final JPanel parentPanel)
	{
		final JCheckBox soundCheckBox = new JCheckBox("Enable Sound");
		soundCheckBox.setSelected(!ClipPlayer.getBeSilent());
		soundCheckBox.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent e)
				{
					ClipPlayer.setBeSilent(!soundCheckBox.isSelected());
				}
			});
		parentPanel.add(soundCheckBox);
	}
}
