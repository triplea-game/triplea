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

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.NullForumPoster;
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
  private final JCheckBox credentialsSaved = new JCheckBox("Remember me");
  private final IForumPoster bean;

  public ForumPosterEditor(final IForumPoster bean) {
    this.bean = bean;
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    if (bean.getCanViewPosted()) {
      add(topicIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
      add(topicIdField, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
      topicIdField.setText(bean.getTopicId());
      add(viewPosts, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 2, bottomSpace, 0), 0, 0));
      row++;
    }
    add(loginLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(login, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    login.setText(bean.getUsername());
    row++;
    add(passwordLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(password, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    password.setText(bean.getPassword());
    row++;
    add(new JLabel(""), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(credentialsSaved, new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    credentialsSaved.setSelected(bean.areCredentialsSaved());
    row++;
    if (bean.supportsSaveGame()) {
      add(includeSaveGame, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      includeSaveGame.setSelected(bean.getIncludeSaveGame());
      add(testForum, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0));
    } else {
      add(testForum, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0));
    }
    row++;
    add(alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    alsoPostAfterCombatMove.setSelected(bean.getAlsoPostAfterCombatMove());
    setupListeners();
  }

  /**
   * Configures the listeners for the gui components.
   */
  private void setupListeners() {
    viewPosts.addActionListener(e -> ((IForumPoster) getBean()).viewPosted());
    credentialsSaved.addActionListener(e -> fireEditorChanged());
    testForum.addActionListener(e -> testForum());
    // add a document listener which will validate input when the content of any input field is changed
    final DocumentListener docListener = new EditorChangedFiringDocumentListener();
    login.getDocument().addDocumentListener(docListener);
    password.getDocument().addDocumentListener(docListener);
    topicIdField.getDocument().addDocumentListener(docListener);
  }

  /**
   * Tests the Forum poster.
   */
  void testForum() {
    final IForumPoster poster = (IForumPoster) getBean();
    final ProgressWindow progressWindow = GameRunner.newProgressWindow(poster.getTestMessage());
    progressWindow.setVisible(true);
    // start a background thread
    new Thread(() -> {
      if (poster.getIncludeSaveGame()) {
        try {
          final File f = File.createTempFile("123", "test");
          f.deleteOnExit();
          /*
           * For .txt use this:
           * final FileOutputStream fout = new FileOutputStream(f);
           * fout.write("Test upload".getBytes());
           * fout.close();
           * poster.addSaveGame(f, "test.txt");
           */
          // For .jpg use this:
          final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
          final Graphics g = image.getGraphics();
          g.drawString("Testing file upload", 10, 20);
          try {
            ImageIO.write(image, "jpg", f);
          } catch (final IOException e) {
            // ignore
          }
          poster.addSaveGame(f, "Test.jpg");
        } catch (final IOException e) {
          // ignore
        }
      }
      poster.postTurnSummary(
          "Test summary from TripleA, engine version: " + ClientContext.engineVersion()
              + ", time: " + TimeManager.getLocalizedTime(),
          "Testing Forum poster");
      progressWindow.setVisible(false);
      // now that we have a result, marshall it back unto the swing thread
      SwingUtilities.invokeLater(() -> {
        GameRunner.showMessageDialog(
            bean.getTurnSummaryRef(),
            GameRunner.Title.of("Test Turn Summary Post"),
            JOptionPane.INFORMATION_MESSAGE);
      });
    }).start();
  }

  @Override
  public boolean isBeanValid() {
    if (bean instanceof NullForumPoster) {
      return true;
    }
    final boolean loginValid = validateTextFieldNotEmpty(login, loginLabel);
    final boolean passwordValid = validateTextFieldNotEmpty(password, passwordLabel);
    boolean idValid = true;
    if (bean.getCanViewPosted()) {
      idValid = validateTextFieldNotEmpty(topicIdField, topicIdLabel);
      viewPosts.setEnabled(idValid);
    } else {
      topicIdLabel.setForeground(labelColor);
      viewPosts.setEnabled(false);
    }
    final boolean allValid = loginValid && passwordValid && idValid;
    testForum.setEnabled(allValid);
    return allValid;
  }

  @Override
  public IBean getBean() {
    bean.setTopicId(topicIdField.getText());
    bean.setUsername(login.getText());
    bean.setPassword(password.getText());
    bean.setCredentialsSaved(credentialsSaved.isSelected());
    bean.setIncludeSaveGame(includeSaveGame.isSelected());
    bean.setAlsoPostAfterCombatMove(alsoPostAfterCombatMove.isSelected());
    return bean;
  }
}
