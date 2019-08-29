package games.strategy.sound;

import games.strategy.engine.data.properties.PropertiesUi;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/** Sound option window framework. */
public final class SoundOptions {
  private SoundOptions() {}

  /** Builds a "Sound Options" menu item. */
  public static JMenuItem buildSoundOptionsMenuItem() {
    final JMenuItem soundOptions = new JMenuItem("Sound Options");
    soundOptions.setMnemonic(KeyEvent.VK_S);
    soundOptions.addActionListener(e -> showSoundOptions(soundOptions));
    return soundOptions;
  }

  private static void showSoundOptions(final JComponent parent) {
    final ClipPlayer clipPlayer = ClipPlayer.getInstance();
    final String ok = "OK";
    final String cancel = "Cancel";
    final String selectAll = "All";
    final String selectNone = "None";

    final List<SoundOptionCheckBox> properties = SoundPath.getSoundOptions();
    final JScrollPane scroll = new JScrollPane(new PropertiesUi(properties, true));
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    final JOptionPane pane =
        new JOptionPane(
            scroll,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            new Object[] {ok, selectAll, selectNone, cancel});
    pane.createDialog(parent, "Sound Options").setVisible(true);
    final Object pressedButton = pane.getValue();
    if (pressedButton == null || pressedButton.equals(cancel)) {
      return;
    }
    if (pressedButton.equals(selectAll)) {
      for (final SoundOptionCheckBox property : properties) {
        property.setValue(true);
      }
    } else if (pressedButton.equals(selectNone)) {
      for (final SoundOptionCheckBox property : properties) {
        property.setValue(false);
      }
    }
    for (final SoundOptionCheckBox property : properties) {
      clipPlayer.setMute(property.getClipName(), !property.getValue());
    }
    clipPlayer.saveSoundPreferences();
  }

  /** Builds a checkbox menu item to turn sounds on or off. */
  public static JMenuItem buildGlobalSoundSwitchMenuItem() {
    final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");
    soundCheckBox.setMnemonic(KeyEvent.VK_N);
    soundCheckBox.setSelected(!ClipPlayer.getBeSilent());
    soundCheckBox.addActionListener(e -> ClipPlayer.setBeSilent(!soundCheckBox.isSelected()));
    return soundCheckBox;
  }
}
