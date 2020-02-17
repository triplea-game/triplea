package games.strategy.engine.framework.startup.ui.pbem;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.Postconditions;
import org.triplea.java.ViewModelListener;

class ForumPosterEditorViewModel {
  private final Runnable readyCallback;

  @Setter private ViewModelListener<ForumPosterEditorViewModel> view;

  @Setter private String forumSelection = "";
  @Setter @Getter private String topicId = "";
  @Setter @Getter private boolean attachSaveGameToSummary;
  @Setter @Getter private boolean alsoPostAfterCombatMove;

  ForumPosterEditorViewModel(final Runnable readyCallback) {
    this.readyCallback = readyCallback;
  }

  ForumPosterEditorViewModel(final Runnable readyCallback, final GameProperties properties) {
    this.readyCallback = readyCallback;
    this.forumSelection = (String) properties.get(IForumPoster.NAME);
    this.topicId = properties.get(IForumPoster.TOPIC_ID, "");
    this.alsoPostAfterCombatMove = properties.get(IForumPoster.POST_AFTER_COMBAT, false);
    this.attachSaveGameToSummary = properties.get(IForumPoster.INCLUDE_SAVEGAME, false);
  }

  String getForumSelection() {
    return Optional.ofNullable(forumSelection) //
        .orElse(getForumSelectionOptions().iterator().next());
  }

  Iterable<String> getForumSelectionOptions() {
    final Iterable<String> selections = NodeBbForumPoster.availablePosters();
    Postconditions.assertState(selections.iterator().hasNext());
    return selections;
  }

  boolean areFieldsValid() {

    return false;
  }

  public void populateFromGameProperties(final GameProperties properties) {
    this.forumSelection = (String) properties.get(IForumPoster.NAME);
    this.topicId = properties.get(IForumPoster.TOPIC_ID, "");
    this.alsoPostAfterCombatMove = properties.get(IForumPoster.POST_AFTER_COMBAT, false);
    this.attachSaveGameToSummary = properties.get(IForumPoster.INCLUDE_SAVEGAME, true);
    view.viewModelChanged(this);
  }

  void viewForumButtonClicked() {
    NodeBbForumPoster.newInstanceByName(forumSelection, Integer.parseInt(topicId)).viewPosted();
  }

  void testPostButtonClicked() {
    //    final NodeBbForumPoster poster = newForumPoster();
    //    final ProgressWindow progressWindow =
    //        new ProgressWindow(JOptionPane.getFrameForComponent(this), poster.getTestMessage());
    //    progressWindow.setVisible(true);
    //
    //    new Thread(
    //            () -> {
    //              File f = null;
    //              try {
    //                f = File.createTempFile("123", ".jpg");
    //                f.deleteOnExit();
    //                final BufferedImage image = new BufferedImage(130, 40,
    // BufferedImage.TYPE_INT_RGB);
    //                final Graphics g = image.getGraphics();
    //                g.drawString("Testing file upload", 10, 20);
    //                ImageIO.write(image, "jpg", f);
    //              } catch (final IOException e) {
    //                // ignore
    //              }
    //              final CompletableFuture<String> future =
    //                  poster.postTurnSummary(
    //                      "Test summary from TripleA, engine version: "
    //                          + ClientContext.engineVersion()
    //                          + ", time: "
    //                          + TimeManager.getLocalizedTime(),
    //                      "Testing Forum poster",
    //                      f != null ? f.toPath() : null);
    //              progressWindow.setVisible(false);
    //              try {
    //                // now that we have a result, marshall it back unto the swing thread
    //                future
    //                    .thenAccept(
    //                        message ->
    //                            SwingUtilities.invokeLater(
    //                                () ->
    //                                    GameRunner.showMessageDialog(
    //                                        message,
    //                                        GameRunner.Title.of("Test Turn Summary Post"),
    //                                        JOptionPane.INFORMATION_MESSAGE)))
    //                    .exceptionally(
    //                        throwable -> {
    //                          SwingUtilities.invokeLater(
    //                              () ->
    //                                  GameRunner.showMessageDialog(
    //                                      throwable.getMessage(),
    //                                      GameRunner.Title.of("Test Turn Summary Post"),
    //                                      JOptionPane.INFORMATION_MESSAGE));
    //                          return null;
    //                        })
    //                    .get();
    //              } catch (final InterruptedException e) {
    //                Thread.currentThread().interrupt();
    //              } catch (final ExecutionException e) {
    //                log.log(Level.SEVERE, "Error while retrieving post", e);
    //              }
    //            })
    //        .start();

  }

  //  private void checkFieldsAndNotify() {
  //    areFieldsValid();
  //    readyCallback.run();
  //  }

  //  /** Configures the listeners for the gui components. */
  //  private void setupListeners() {
  ////    viewPosts.addActionListener(e -> newForumPoster().viewPosted());
  ////    testForum.addActionListener(e -> testForum());
  ////    forums.addItemListener(e -> checkFieldsAndNotify());
  ////    DocumentListenerBuilder.attachDocumentListener(topicIdField, this::checkFieldsAndNotify);
  //  }

  //  /** Checks if all fields are filled out correctly and indicates an error otherwise. */
  //  public boolean areFieldsValid() {
  //    final boolean setupValid =
  //        NodeBbForumPoster.isClientSettingSetupValidForServer((String) forums.getSelectedItem());
  //    final boolean idValid = setLabelValid(isInt(topicIdField.getText()), topicIdLabel);
  //    final boolean forumValid = setLabelValid(forums.getSelectedItem() != null, forumLabel);
  //    final boolean allValid = setupValid && idValid && forumValid;
  //    viewPosts.setEnabled(allValid);
  //    testForum.setEnabled(allValid);
  //    return allValid;
  //  }

  //  @VisibleForTesting
  //  static boolean isInt(final String string) {
  //    Preconditions.checkNotNull(string);
  //    return string.matches("^-?\\d+$");
  //  }

  //  private NodeBbForumPoster newForumPoster() {
  //    final String forumName = (String) forums.getSelectedItem();
  //    Preconditions.checkNotNull(forumName);
  //    return NodeBbForumPoster.newInstanceByName(forumName,
  // Integer.parseInt(topicIdField.getText()));
  //  }

}
