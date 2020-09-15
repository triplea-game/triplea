package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Font;
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
            .build();
    final JButton startLocal =
        new JButtonBuilder("Start Local Game")
            .toolTipText(
                "<html>Start a game on this computer. <br>"
                    + "You can play against a friend sitting besides you (hotseat mode), <br>"
                    + "or against one of the AIs.</html>")
            .build();
    final JButton startPbf =
        new JButtonBuilder("Play By Forum")
            .toolTipText(
                "<html>Starts a game which will be posted to an online forum or message board.</html>")
            .build();
    final JButton startPbem =
        new JButtonBuilder("Play By Email")
            .toolTipText(
                "<html>Starts a game which will be emailed back and forth between all players.</html>")
            .build();
    final JButton hostGame =
        new JButtonBuilder("Host Networked Game")
            .toolTipText(
                "<html>Hosts a network game, which people can connect to. <br>"
                    + "Anyone on a LAN will be able to connect. <br>"
                    + "Anyone from the internet can connect as well, but only if the host has "
                    + "configured port forwarding correctly.</html>")
            .build();
    final JButton connectToHostedGame =
        new JButtonBuilder("Connect to Networked Game")
            .toolTipText(
                "<html>Connects to someone's hosted game, <br>"
                    + "so long as you know their IP address.</html>")
            .build();
    final JButton enginePreferences =
        new JButtonBuilder("Engine Preferences")
            .toolTipText("<html>Configure certain options related to the engine.")
            .build();
    final JButton userGuideButton = new JButton("User Guide & Help");

    setLayout(new GridBagLayout());
    // top space
    int row = 0;
    add(
        new JPanel(),
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            1,
            1,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));
    row++;
    add(
        connectToLobby,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    row++;
    add(
        startLocal,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    row++;
    add(
        startPbf,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    row++;
    add(
        startPbem,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    row++;
    add(
        hostGame,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    row++;
    add(
        connectToHostedGame,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    row++;
    add(
        enginePreferences,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));

    final JButton mapCreator =
        new JButtonBuilder()
            .title("Run the Map Creator")
            .actionListener(MapCreator::openMapCreatorWindow)
            .build();

    row++;
    add(
        mapCreator,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));

    row++;
    add(
        userGuideButton,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));

    // top space
    add(
        new JPanel(),
        new GridBagConstraints(
            0,
            100,
            1,
            1,
            1,
            1,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));

    startLocal.addActionListener(e -> model.showLocal());
    startPbf.addActionListener(e -> model.showPbf());
    startPbem.addActionListener(e -> model.showPbem());
    hostGame.addActionListener(e -> new Thread(model::showServer).start());
    connectToHostedGame.addActionListener(e -> new Thread(model::showClient).start());
    connectToLobby.addActionListener(e -> model.login());
    enginePreferences.addActionListener(
        e -> ClientSetting.showSettingsWindow(JOptionPane.getFrameForComponent(this)));
    userGuideButton.addActionListener(e -> userGuidePage());
  }

  private static void userGuidePage() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE);
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
