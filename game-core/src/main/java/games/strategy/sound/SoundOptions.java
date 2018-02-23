package games.strategy.sound;

import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.ui.PropertiesSelector;

/**
 * Sound option window framework.
 */
public final class SoundOptions {
  private final ClipPlayer clipPlayer;

  /**
   * @param parentMenu
   *        menu where to add the menu item "Sound Options".
   */
  public static void addToMenu(final JMenu parentMenu) {
    final JMenuItem soundOptions = new JMenuItem("Sound Options");
    soundOptions.setMnemonic(KeyEvent.VK_S);
    soundOptions.addActionListener(e -> new SoundOptions(parentMenu));
    parentMenu.add(soundOptions);
  }

  private SoundOptions(final JComponent parent) {
    clipPlayer = ClipPlayer.getInstance();
    final String ok = "OK";
    final String cancel = "Cancel";
    final String selectAll = "All";
    final String selectNone = "None";

    final List<IEditableProperty> properties = SoundPath.getSoundOptions();
    final Object pressedButton = PropertiesSelector.getButton(parent, "Sound Options", properties,
        ok, selectAll, selectNone, cancel);
    if ((pressedButton == null) || pressedButton.equals(cancel)) {
      // do nothing
    } else if (pressedButton.equals(ok)) {
      for (final IEditableProperty property : properties) {
        clipPlayer.setMute(((SoundOptionCheckBox) property).getClipName(), !(Boolean) property.getValue());
      }
      clipPlayer.saveSoundPreferences();
    } else if (pressedButton.equals(selectAll)) {
      for (final IEditableProperty property : properties) {
        property.setValue(true);
        clipPlayer.setMute(((SoundOptionCheckBox) property).getClipName(), false);
      }
      clipPlayer.saveSoundPreferences();
    } else if (pressedButton.equals(selectNone)) {
      for (final IEditableProperty property : properties) {
        property.setValue(false);
        clipPlayer.setMute(((SoundOptionCheckBox) property).getClipName(), true);
      }
      clipPlayer.saveSoundPreferences();
    }
  }

  public static void addGlobalSoundSwitchMenu(final JMenu parentMenu) {
    final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");
    soundCheckBox.setMnemonic(KeyEvent.VK_N);
    soundCheckBox.setSelected(!ClipPlayer.getBeSilent());
    soundCheckBox.addActionListener(e -> ClipPlayer.setBeSilent(!soundCheckBox.isSelected()));
    parentMenu.add(soundCheckBox);
  }
}
