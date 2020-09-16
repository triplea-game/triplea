package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.ui.panels.main.SetupPanelModel;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import tools.map.making.MapCreator;

/**
 * This is the main welcome panel with 'play online' button. This panel is just the upper right of
 * the main screen, it does not include the map information nor the 'play' and 'quit' buttons.
 */
public class MetaSetupPanel extends SetupPanel {

  private static final long serialVersionUID = 3926503672972937677L;

  public MetaSetupPanel(final SetupPanelModel model) {
    final JButton connectToLobby =
        new JButtonBuilder("Play Online")
            .biggerFont()
            .toolTipText(
                "<html>Find Games Online on the Lobby Server. <br>"
                    + "TripleA is MEANT to be played Online against other humans. <br>"
                    + "Any other way is not as fun!</html>")
            .actionListener(model::login)
            .build();
    final JButton startLocal =
        new JButtonBuilder("Start Local Game")
            .toolTipText(
                "<html>Start a game on this computer. <br>"
                    + "You can play against a friend sitting besides you (hotseat mode), <br>"
                    + "or against one of the AIs.</html>")
            .actionListener(model::showLocal)
            .build();

    final JButton startPbf =
        new JButtonBuilder("Play By Forum")
            .toolTipText(
                "<html>"
                    + "Starts a game which will be posted to an online forum or message board."
                    + "</html>")
            .actionListener(model::showPbf)
            .build();
    final JButton startPbem =
        new JButtonBuilder("Play By Email")
            .toolTipText(
                "<html>"
                    + "Starts a game which will be emailed back and forth between all players."
                    + "</html>")
            .actionListener(model::showPbem)
            .build();
    final JButton hostGame =
        new JButtonBuilder("Host Networked Game")
            .toolTipText(
                "<html>Hosts a network game, which people can connect to. <br>"
                    + "Anyone on a LAN will be able to connect. <br>"
                    + "Anyone from the internet can connect as well, but only if the host has "
                    + "configured port forwarding correctly.</html>")
            .actionListener(() -> new Thread(model::showServer).start())
            .build();
    final JButton connectToHostedGame =
        new JButtonBuilder("Connect to Networked Game")
            .toolTipText(
                "<html>Connects to someone's hosted game, <br>"
                    + "so long as you know their IP address.</html>")
            .actionListener(() -> new Thread(model::showClient).start())
            .build();
    final JButton enginePreferences =
        new JButtonBuilder("Engine Preferences")
            .toolTipText("<html>Configure certain options related to the engine.")
            .actionListener(
                () -> ClientSetting.showSettingsWindow(JOptionPane.getFrameForComponent(this)))
            .build();
    final JButton userGuideButton =
        new JButtonBuilder("User Guide & Help")
            .actionListener(
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE))
            .build();
    final JButton mapCreator =
        new JButtonBuilder()
            .title("Map Creator Tools")
            .actionListener(MapCreator::openMapCreatorWindow)
            .build();

    setLayout(new BorderLayout());
    final JPanel mainContents = new JPanel();
    add(mainContents);
    mainContents.setLayout(new GridBagLayout());
    int row = 0;
    mainContents.add(connectToLobby, buildConstraintForRow(row));
    row++;
    mainContents.add(startLocal, buildConstraintForRow(row));
    row++;
    mainContents.add(startPbf, buildConstraintForRow(row));
    row++;
    mainContents.add(startPbem, buildConstraintForRow(row));
    row++;
    mainContents.add(hostGame, buildConstraintForRow(row));
    row++;
    mainContents.add(connectToHostedGame, buildConstraintForRow(row));
    row++;
    mainContents.add(enginePreferences, buildConstraintForRow(row));
    row++;
    mainContents.add(mapCreator, buildConstraintForRow(row));
    row++;
    mainContents.add(userGuideButton, buildConstraintForRow(row));
  }

  private GridBagConstraints buildConstraintForRow(final int rowNumber) {
    return new GridBagConstraintsBuilder(0, rowNumber)
        .anchor(GridBagConstraintsAnchor.CENTER)
        .fill(GridBagConstraintsFill.NONE)
        .insets(new Insets(10, 0, 0, 0))
        .build();
  }

  @Override
  public boolean canGameStart() {
    return false;
  }

  @Override
  public List<Action> getUserActions() {
    return List.of();
  }

  @Override
  public boolean isCancelButtonVisible() {
    return false;
  }

  @Override
  public void cancel() {}

  @Override
  public Optional<ILauncher> getLauncher() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void postStartGame() {}
}
