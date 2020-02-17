package games.strategy.engine.framework.startup.ui.pbem;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.triplea.java.ViewModelListener;
import org.triplea.swing.DocumentListenerBuilder;

/** A class for selecting which Forum poster to use. */
class ForumPosterEditor extends EditorPanel
    implements ViewModelListener<ForumPosterEditorViewModel> {
  private static final long serialVersionUID = -6069315084412575053L;
  private final JButton viewPosts = new JButton("View Forum");
  private final JButton testForum = new JButton("Test Post");
  private final JTextField topicIdField = new JTextField();
  private final JLabel topicIdLabel = new JLabel("Topic Id:");
  private final JLabel forumLabel = new JLabel("Forums:");
  private final JCheckBox attachSaveGameToSummary = new JCheckBox("Attach save game to summary");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");
  private final JComboBox<String> forums = new JComboBox<>();

  ForumPosterEditor(final ForumPosterEditorViewModel viewModel) {
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
    forums.addActionListener(e -> viewModel.setForumSelection((String) forums.getSelectedItem()));
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

    DocumentListenerBuilder.attachDocumentListener(
        topicIdField, () -> viewModel.setTopicId(topicIdField.getText().trim()));
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
    alsoPostAfterCombatMove.setSelected(forumPosterEditorViewModel.isAlsoPostAfterCombatMove());
    attachSaveGameToSummary.setSelected(forumPosterEditorViewModel.isAttachSaveGameToSummary());

    final boolean isValid = forumPosterEditorViewModel.areFieldsValid();
    setLabelValid(isValid, topicIdLabel);
    setLabelValid(isValid, forumLabel);
    viewPosts.setEnabled(isValid);
    testForum.setEnabled(isValid);
  }
}
