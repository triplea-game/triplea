package org.triplea.sound;

import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.JMenuItemCheckBoxBuilder;
import org.triplea.swing.key.binding.KeyCode;

/** Sound option window framework. */
public final class SoundOptions {
  private SoundOptions() {}

  /** Builds a "Sound Options" menu item. */
  public static JMenuItem buildSoundOptionsMenuItem() {
    final JMenuItemBuilder itemBuilder = new JMenuItemBuilder("Sound Options", KeyCode.S);

    if (ClipPlayer.hasAudio()) {
      itemBuilder.actionListener(SoundOptions::showSoundOptions);
    } else {
      itemBuilder.disabled("No audio device detected on your system");
    }

    return itemBuilder.build();
  }

  private static void showSoundOptions() {
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
    // use default frame to avoid button action dependency to itself
    JFrame defaultFrame = null;
    pane.createDialog(defaultFrame, "Sound Options").setVisible(true);
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
      ClipPlayer.setSoundClipMute(property.getClipName(), !property.getValue());
    }
    ClipPlayer.saveSoundPreferences();
  }

  /** Builds a checkbox menu item to turn sounds on or off. */
  public static JMenuItem buildGlobalSoundSwitchMenuItem() {
    final JMenuItem enableSoundMenuItem =
        new JMenuItemCheckBoxBuilder("Enable Sound", KeyCode.N)
            .bindSetting(ClientSetting.soundEnabled)
            .build();

    if (!ClipPlayer.hasAudio()) {
      enableSoundMenuItem.setEnabled(false);
      enableSoundMenuItem.setToolTipText("No audio device detected on your system");
    }
    return enableSoundMenuItem;
  }
}
