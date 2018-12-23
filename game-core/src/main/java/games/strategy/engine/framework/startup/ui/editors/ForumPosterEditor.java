package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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
  private final JTextField topicIdField = new JTextField();
  private final JLabel topicIdLabel = new JLabel("Topic Id:");
  private final JCheckBox includeSaveGame = new JCheckBox("Attach save game to summary");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");

  public ForumPosterEditor() {
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    add(topicIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(topicIdField, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
    add(viewPosts, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    row++;
    add(new JLabel(""), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    row++;
    add(includeSaveGame, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    add(testForum, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    row++;
    add(alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
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
      final CompletableFuture<String> future = poster.postTurnSummary(
          "Test summary from TripleA, engine version: " + ClientContext.engineVersion()
              + ", time: " + TimeManager.getLocalizedTime(),
          "Testing Forum poster", f != null ? f.toPath() : null);
      progressWindow.setVisible(false);
      // now that we have a result, marshall it back unto the swing thread
      future.thenAccept(message -> SwingUtilities.invokeLater(() -> GameRunner.showMessageDialog(
          message,
          GameRunner.Title.of("Test Turn Summary Post"),
          JOptionPane.INFORMATION_MESSAGE)))
        .exceptionally(throwable -> {
          SwingUtilities.invokeLater(() -> GameRunner.showMessageDialog(
              throwable.getMessage(),
              GameRunner.Title.of("Test Turn Summary Post"),
              JOptionPane.INFORMATION_MESSAGE));
          return null;
        });
    }).start();
  }

  public boolean areFieldsValid() {
    boolean idValid = validateTextFieldNotEmpty(topicIdField, topicIdLabel);
    viewPosts.setEnabled(idValid);

    testForum.setEnabled(idValid);
    return idValid;
  }

  private IForumPoster getForumPoster() {
    // FIXME apply values of other fields to the gamedata somehow.
    // also fall back to stored values
    return null; // function.apply(Integer.parseInt(topicIdField.getText()), login.getText(), password.getText());
  }

  public void applyToGameProperties(final GameProperties properties) {
    properties.set(IForumPoster.NAME, ""/* FIXME put actual name here*/);
    properties.set(IForumPoster.TOPIC_ID, topicIdField.getText());
    properties.set(IForumPoster.POST_AFTER_COMBAT, alsoPostAfterCombatMove.isSelected());
    properties.set(IForumPoster.INCLUDE_SAVEGAME, includeSaveGame.isSelected());
  }

  public void populateFromGameProperties(final GameProperties properties) {
    // FIXME add name combobox
    topicIdField.setText(properties.get(IForumPoster.TOPIC_ID, ""));
    alsoPostAfterCombatMove.setSelected(properties.get(IForumPoster.POST_AFTER_COMBAT, false));
    includeSaveGame.setSelected(properties.get(IForumPoster.INCLUDE_SAVEGAME, true));
  }
}
