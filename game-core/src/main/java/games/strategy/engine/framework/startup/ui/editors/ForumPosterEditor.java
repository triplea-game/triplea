package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

import com.google.common.base.Preconditions;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.ui.ProgressWindow;
import games.strategy.util.TimeManager;

/**
 * A class for selecting which Forum poster to use.
 */
public class ForumPosterEditor extends EditorPanel {
  private static final long serialVersionUID = -6069315084412575053L;
  private final JButton viewPosts = new JButton("View Forum");
  private final JButton testForum = new JButton("Test Post");
  private final JLabel loginLabel = new JLabel("Login:");
  private final JLabel passwordLabel = new JLabel("Password:");
  private final JTextField login = new JTextField();
  private final JTextField password = new JPasswordField();
  private final JTextField topicIdField = new JTextField();
  private final JLabel topicIdLabel = new JLabel("Topic Id:");
  private final JCheckBox includeSaveGame = new JCheckBox("Attach save game to summary");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");

  public ForumPosterEditor(final GameProperties properties) {
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    add(topicIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(topicIdField, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
    topicIdField.setText(properties.get(IForumPoster.TOPIC_ID, ""));
    add(viewPosts, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    row++;
    add(loginLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(login, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(passwordLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(password, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(new JLabel(""), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    row++;
    add(includeSaveGame, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    includeSaveGame.setSelected(properties.get(IForumPoster.INCLUDE_SAVEGAME, false));
    add(testForum, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    row++;
    add(alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    alsoPostAfterCombatMove.setSelected(properties.get(IForumPoster.POST_AFTER_COMBAT, false));
    setupListeners();
  }

  /**
   * Configures the listeners for the gui components.
   */
  private void setupListeners() {
    viewPosts.addActionListener(e -> getForumPoster().viewPosted());
    testForum.addActionListener(e -> testForum());
  }

  /**
   * Tests the Forum poster.
   */
  void testForum() {
    final IForumPoster poster = getForumPoster();
    final ProgressWindow progressWindow = GameRunner.newProgressWindow(poster.getTestMessage());
    progressWindow.setVisible(true);

    new Thread(() -> {
      File f = null;
      try {
        f = File.createTempFile("123", "test");
        f.deleteOnExit();
        final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
        final Graphics g = image.getGraphics();
        g.drawString("Testing file upload", 10, 20);
        ImageIO.write(image, "jpg", f);
      } catch (final IOException e) {
        // ignore
      }
      poster.postTurnSummary(
          "Test summary from TripleA, engine version: " + ClientContext.engineVersion()
              + ", time: " + TimeManager.getLocalizedTime(),
          "Testing Forum poster", f != null ? f.toPath() : null);
      progressWindow.setVisible(false);
      // now that we have a result, marshall it back unto the swing thread
      SwingUtilities.invokeLater(() -> GameRunner.showMessageDialog(
          "", // FIXME get from turn summary bean.getTurnSummaryRef(),
          GameRunner.Title.of("Test Turn Summary Post"),
          JOptionPane.INFORMATION_MESSAGE));
    }).start();
  }

  public boolean areFieldsValid() {
    final boolean loginValid = validateTextFieldNotEmpty(login, loginLabel);
    final boolean passwordValid = validateTextFieldNotEmpty(password, passwordLabel);
    boolean idValid = validateTextFieldNotEmpty(topicIdField, topicIdLabel);
    viewPosts.setEnabled(idValid);

    final boolean allValid = loginValid && passwordValid && idValid;
    testForum.setEnabled(allValid);
    return allValid;
  }

  public IForumPoster getForumPoster() {
    // FIXME create from selection
    /*bean.setTopicId(topicIdField.getText());
    bean.setUsername(login.getText());
    bean.setPassword(password.getText());
    bean.setCredentialsSaved(credentialsSaved.isSelected());
    bean.setIncludeSaveGame(includeSaveGame.isSelected());
    bean.setAlsoPostAfterCombatMove(alsoPostAfterCombatMove.isSelected());*/
    return null;
  }
}
