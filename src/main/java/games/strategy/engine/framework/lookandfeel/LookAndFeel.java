package games.strategy.engine.framework.lookandfeel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;

public class LookAndFeel {

  public static List<String> getLookAndFeelAvailableList() {
    return Arrays.stream(UIManager.getInstalledLookAndFeels())
        .map(UIManager.LookAndFeelInfo::getClassName)
        .collect(Collectors.toList());
  }

  public static void setupLookAndFeel() {
    SwingAction.invokeAndWait(() -> {
      try {
        UIManager.setLookAndFeel(ClientSetting.LOOK_AND_FEEL_PREF.value());
      } catch (final Throwable t) {
        if (!SystemProperties.isMac()) {
          try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          } catch (final Exception e) {
            ClientLogger.logQuietly(e);
          }
        }
      }
    });
  }

}
