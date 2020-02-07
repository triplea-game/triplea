package games.strategy.engine.framework.startup.ui.editors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.pbem.IForumPoster;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import org.triplea.java.TimeManager;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.ProgressWindow;

/** A class for selecting which Forum poster to use. */
@Log
public class ForumPosterEditor extends EditorPanel {
  private static final long serialVersionUID = -6069315084412575053L;
  private final JButton viewPosts = new JButton("View Forum");
  private final JButton testForum = new JButton("Test Post");
  private final JTextField topicIdField = new JTextField();
  private final JLabel topicIdLabel = new JLabel("Topic Id:");
  private final JLabel forumLabel = new JLabel("Forums:");
  private final JCheckBox includeSaveGame = new JCheckBox("Attach save game to summary");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");
  private final JComboBox<String> forums = new JComboBox<>();
  private final Runnable readyCallback;

  public ForumPosterEditor(final Runnable readyCallback) {
    this.readyCallback = readyCallback;
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    IForumPoster.availablePosters().forEach(forums::addItem);
    add(
        forumLabel,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, bottomSpace, labelSpace),
            0,
            0));
    add(
        forums,
        new GridBagConstraints(
            1,
            row,
            1,
            1,
            1.0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));
    row++;
    add(
        topicIdLabel,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, bottomSpace, labelSpace),
            0,
            0));
    add(
        topicIdField,
        new GridBagConstraints(
            1,
            row,
            1,
            1,
            1.0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));
    add(
        viewPosts,
        new GridBagConstraints(
            2,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 2, bottomSpace, 0),
            0,
            0));
    row++;
    add(
        new JLabel(""),
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, bottomSpace, labelSpace),
            0,
            0));
    row++;
    add(
        includeSaveGame,
        new GridBagConstraints(
            0,
            row,
            2,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    add(
        testForum,
        new GridBagConstraints(
            2,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    row++;
    add(
        alsoPostAfterCombatMove,
        new GridBagConstraints(
            0,
            row,
            2,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    setupListeners();
  }

  private void checkFieldsAndNotify() {
    areFieldsValid();
    readyCallback.run();
  }

  /** Configures the listeners for the gui components. */
  private void setupListeners() {
    viewPosts.addActionListener(e -> newForumPoster().viewPosted());
    testForum.addActionListener(e -> testForum());
    forums.addItemListener(e -> checkFieldsAndNotify());
    DocumentListenerBuilder.attachDocumentListener(topicIdField, this::checkFieldsAndNotify);
  }

  /** Tests the Forum poster. */
  private void testForum() {
    final IForumPoster poster = newForumPoster();
    final ProgressWindow progressWindow =
        new ProgressWindow(JOptionPane.getFrameForComponent(this), poster.getTestMessage());
    progressWindow.setVisible(true);

    new Thread(
            () -> {
              File f = null;
              try {
                f = File.createTempFile("123", ".jpg");
                f.deleteOnExit();
                final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
                final Graphics g = image.getGraphics();
                g.drawString("Testing file upload", 10, 20);
                ImageIO.write(image, "jpg", f);
              } catch (final IOException e) {
                // ignore
              }
              final CompletableFuture<String> future =
                  poster.postTurnSummary(
                      "Test summary from TripleA, engine version: "
                          + ClientContext.engineVersion()
                          + ", time: "
                          + TimeManager.getLocalizedTime(),
                      "Testing Forum poster",
                      f != null ? f.toPath() : null);
              progressWindow.setVisible(false);
              try {
                // now that we have a result, marshall it back unto the swing thread
                future
                    .thenAccept(
                        message ->
                            SwingUtilities.invokeLater(
                                () ->
                                    GameRunner.showMessageDialog(
                                        message,
                                        GameRunner.Title.of("Test Turn Summary Post"),
                                        JOptionPane.INFORMATION_MESSAGE)))
                    .exceptionally(
                        throwable -> {
                          SwingUtilities.invokeLater(
                              () ->
                                  GameRunner.showMessageDialog(
                                      throwable.getMessage(),
                                      GameRunner.Title.of("Test Turn Summary Post"),
                                      JOptionPane.INFORMATION_MESSAGE));
                          return null;
                        })
                    .get();
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (final ExecutionException e) {
                log.log(Level.SEVERE, "Error while retrieving post", e);
              }
            })
        .start();
  }

  /** Checks if all fields are filled out correctly and indicates an error otherwise. */
  public boolean areFieldsValid() {
    final boolean setupValid =
        IForumPoster.isClientSettingSetupValidForServer((String) forums.getSelectedItem());
    final boolean idValid = setLabelValid(isInt(topicIdField.getText()), topicIdLabel);
    final boolean forumValid = setLabelValid(forums.getSelectedItem() != null, forumLabel);
    final boolean allValid = setupValid && idValid && forumValid;
    viewPosts.setEnabled(allValid);
    testForum.setEnabled(allValid);
    return allValid;
  }

  @VisibleForTesting
  static boolean isInt(final String string) {
    Preconditions.checkNotNull(string);
    return string.matches("^-?\\d+$");
  }

  private IForumPoster newForumPoster() {
    final String forumName = (String) forums.getSelectedItem();
    Preconditions.checkNotNull(forumName);
    return IForumPoster.newInstanceByName(forumName, Integer.parseInt(topicIdField.getText()));
  }

  public void applyToGameProperties(final GameProperties properties) {
    properties.set(IForumPoster.NAME, forums.getSelectedItem());
    properties.set(IForumPoster.TOPIC_ID, Integer.parseInt(topicIdField.getText()));
    properties.set(IForumPoster.POST_AFTER_COMBAT, alsoPostAfterCombatMove.isSelected());
    properties.set(IForumPoster.INCLUDE_SAVEGAME, includeSaveGame.isSelected());
  }

  public void populateFromGameProperties(final GameProperties properties) {
    forums.setSelectedItem(properties.get(IForumPoster.NAME));
    final Object topicId = properties.get(IForumPoster.TOPIC_ID);
    topicIdField.setText(topicId == null ? "" : String.valueOf(topicId));
    alsoPostAfterCombatMove.setSelected(properties.get(IForumPoster.POST_AFTER_COMBAT, false));
    includeSaveGame.setSelected(properties.get(IForumPoster.INCLUDE_SAVEGAME, true));
  }
}
