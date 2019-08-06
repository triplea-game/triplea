package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;

public class HeadedLaunchAction implements LaunchAction {

  private final Component ui;

  public HeadedLaunchAction(final Component ui) {
    this.ui = ui;
  }

  @Override
  public void handleGameInterruption(final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {
    gameSelectorModel.loadDefaultGameNewThread();
  }

  @Override
  public void onGameInterrupt() {
    SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(ui).setVisible(true));
  }

  @Override
  public void onEnd(String message) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message));
  }

  @Override
  public boolean isHeadless() {
    return false;
  }

  @Override
  public File getAutoSaveFile() {
    return getAutoSaveFileUtils().getLostConnectionAutoSaveFile(LocalDateTime.now(ZoneId.systemDefault()));
  }

  @Override
  public void onLaunch(ServerGame serverGame) {}

  @Override
  public AutoSaveFileUtils getAutoSaveFileUtils() {
    return new AutoSaveFileUtils();
  }
}
