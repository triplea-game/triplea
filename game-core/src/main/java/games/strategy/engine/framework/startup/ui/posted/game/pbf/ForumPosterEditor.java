package games.strategy.engine.framework.startup.ui.posted.game.pbf;

import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.triplea.java.ViewModelListener;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;

/** A class for selecting which Forum poster to use. */
class ForumPosterEditor extends JPanel implements ViewModelListener<ForumPosterEditorViewModel> {
  private static final long serialVersionUID = -6069315084412575053L;
  private final JButton viewPosts = new JButton("View Forum");
  private final JButton testForum = new JButton("Test Post");
  private final JLabel helpMessage = new JLabel();
  private final JButton helpButton =
      new JButtonBuilder("Help")
          .actionListener(
              () ->
                  JOptionPane.showMessageDialog(
                      this, helpMessage, "Play By Forum Help", JOptionPane.INFORMATION_MESSAGE))
          .toolTip("Click this button to show help text")
          .build();
  private final JTextField topicIdField = new JTextField();
  private final JLabel usernameLabel = new JLabel("Forum Username");
  private final JTextField usernameField = new JTextField();
  private final JLabel passwordLabel = new JLabel("Forum Password");
  private final JPasswordField passwordField = new JPasswordField();
  private final JLabel topicIdLabel = new JLabel("Topic Id:");
  private final JLabel forumLabel = new JLabel("Forums:");
  private final JCheckBox attachSaveGameToSummary = new JCheckBox("Attach save game to summary");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");
  private final JComboBox<String> forums = new JComboBox<>();

  ForumPosterEditor(final ForumPosterEditorViewModel viewModel) {
    super(new GridBagLayout());

    viewModel.setView(this);

    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;

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

    viewModel.getForumSelectionOptions().forEach(forums::addItem);
    forums.addActionListener(
        e -> {
          viewModel.setForumSelection((String) forums.getSelectedItem());

          usernameField.setText(viewModel.getForumUsername());
          passwordField.setText(String.valueOf(viewModel.getForumPassword()));
          SwingComponents.highlightLabelIfNotValid(viewModel.isForumUsernameValid(), usernameLabel);
          SwingComponents.highlightLabelIfNotValid(viewModel.isForumPasswordValid(), passwordLabel);
          testForum.setEnabled(viewModel.isTestForumPostButtonEnabled());
          helpMessage.setText(
              viewModel.getForumSelection().equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
                  ? "<html><p style='width: 400px;'>"
                      + "Posts to forums.triplea-game.org<br/>"
                      + "You can play PBEM/PBF games via forums.triplea-game.org.<br/>"
                      + "Instructions:<br/>"
                      + "Create a new Forum post in the Play by Forum category "
                      + "https://forums.triplea-game.org/category/6/<br/>"
                      + "Copy the topic id from the URL displayed in the address bar of you browser. "
                      + "If the URL is https://forums.triplea-game.org/topic/24/ put the topic number "
                      + "(24) into the Topic ID field in TripleA<br/>"
                      + "Put your username and password for the forums.triplea-game.org forum into the "
                      + "username and password fields<br/>"
                      + "Click the Test Post button, to check that TripleA can reply to your forum "
                      + "post<br/>"
                      + "Note: Your forums.triplea-game.org username and password are not stored as part "
                      + "of the save game, but they are stored encrypted in the local file system if "
                      + "you select the option to remember your credentials. You may have to enter "
                      + "your username and password again if you open the save game on another computer."
                      + "</p></html>"
                  : "<html><p style='width: 400px;'>"
                      + "Posts to www.AxisAndAllies.org<br/>"
                      + "This poster is build for PBEM games via AxisAndAllies.org. Instructions:<br/>"
                      + "Create a new Forum post in the Play Boardgames section "
                      + "http://www.axisandallies.org/forums/index.php?board=40.0<br/>"
                      + "Copy the topic id from the URL displayed in the address bar of you browser. "
                      + "If the URL is http://www.axisandallies.org/forums/index.php?topic=25252.0, "
                      + "put this 5 digit number (25252) into the Topic ID field in TripleA<br/>"
                      + "Put your username and password for the AxisAndAllies.org forum into the username "
                      + "and password fields<br/>"
                      + "Click the Test Post button, to check that TripleA can reply to your forum "
                      + "post<br/>"
                      + "Note: Your AxisAndAllies.org username and password are not stored as part of "
                      + "the save game, but they are stored encrypted in the local file system if "
                      + "you select the option to remember your credentials. You may have to enter "
                      + "your username and password again if you open the save game on another computer."
                      + "</p></html>");
        });
    forums.setSelectedItem(viewModel.getForumSelection());
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

    add(
        helpButton,
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

    new DocumentListenerBuilder(
            () -> {
              viewModel.setTopicId(topicIdField.getText().trim());
              SwingComponents.highlightLabelIfNotValid(viewModel.isTopicIdValid(), topicIdLabel);
              viewPosts.setEnabled(viewModel.isViewForumPostButtonEnabled());
              testForum.setEnabled(viewModel.isTestForumPostButtonEnabled());
            })
        .attachTo(topicIdField);
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

    viewPosts.addActionListener(e -> viewModel.viewForumButtonClicked());
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
        usernameLabel,
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

    new DocumentListenerBuilder(
            () -> {
              viewModel.setForumUsername(usernameField.getText().trim());
              SwingComponents.highlightLabelIfNotValid(
                  viewModel.isForumUsernameValid(), usernameLabel);
              testForum.setEnabled(viewModel.isTestForumPostButtonEnabled());
            })
        .attachTo(usernameField);

    add(
        usernameField,
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
        passwordLabel,
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
    new DocumentListenerBuilder(
            () -> {
              viewModel.setForumPassword(passwordField.getPassword());
              SwingComponents.highlightLabelIfNotValid(
                  viewModel.isForumPasswordValid(), passwordLabel);
              testForum.setEnabled(viewModel.isTestForumPostButtonEnabled());
            })
        .attachTo(passwordField);
    add(
        passwordField,
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

    attachSaveGameToSummary.addChangeListener(
        e -> viewModel.setAttachSaveGameToSummary(attachSaveGameToSummary.isSelected()));
    add(
        attachSaveGameToSummary,
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

    testForum.addActionListener(e -> viewModel.testPostButtonClicked());
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
    alsoPostAfterCombatMove.addActionListener(
        e -> viewModel.setAlsoPostAfterCombatMove(alsoPostAfterCombatMove.isSelected()));
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
  }

  @Override
  public void viewModelChanged(final ForumPosterEditorViewModel forumPosterEditorViewModel) {
    forums.setSelectedItem(forumPosterEditorViewModel.getForumSelection());
    topicIdField.setText(forumPosterEditorViewModel.getTopicId());
    usernameField.setText(forumPosterEditorViewModel.getForumUsername());
    passwordField.setText(String.valueOf(forumPosterEditorViewModel.getForumPassword()));
    alsoPostAfterCombatMove.setSelected(forumPosterEditorViewModel.isAlsoPostAfterCombatMove());
    attachSaveGameToSummary.setSelected(forumPosterEditorViewModel.isAttachSaveGameToSummary());
    SwingComponents.highlightLabelIfNotValid(
        forumPosterEditorViewModel.isTopicIdValid(), topicIdLabel);
    SwingComponents.highlightLabelIfNotValid(
        forumPosterEditorViewModel.isForumUsernameValid(), usernameLabel);
    SwingComponents.highlightLabelIfNotValid(
        forumPosterEditorViewModel.isForumPasswordValid(), passwordLabel);
    viewPosts.setEnabled(forumPosterEditorViewModel.isViewForumPostButtonEnabled());
    testForum.setEnabled(forumPosterEditorViewModel.isTestForumPostButtonEnabled());
  }
}
