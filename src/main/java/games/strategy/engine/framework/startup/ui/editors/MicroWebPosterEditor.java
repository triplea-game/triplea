package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.pbem.IWebPoster;
import games.strategy.engine.pbem.TripleAWebPoster;
import games.strategy.ui.ProgressWindow;
import games.strategy.util.UrlStreams;

/**
 * A class for displaying settings for the micro web site poster.
 */
public class MicroWebPosterEditor extends EditorPanel {
  private static final long serialVersionUID = -6069315084412575053L;
  public static final String HTTP_BLANK = "http://";
  private final JButton viewSite = new JButton("View Web Site");
  private final JButton testSite = new JButton("Test Web Site");
  private final JButton initGame = new JButton("Initialize Game");
  private final JTextField id = new JTextField();
  private final JLabel hostLabel = new JLabel("Host:");
  private final JComboBox<String> hosts;
  private final JCheckBox includeSaveGame = new JCheckBox("Send emails");
  private final IWebPoster webPoster;
  private final String[] parties;
  private final JLabel gameNameLabel = new JLabel("Game Name:");
  private final JTextField gameName = new JTextField();

  public MicroWebPosterEditor(final IWebPoster bean, final String[] parties) {
    webPoster = bean;
    this.parties = parties;
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    add(hostLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    webPoster.addToAllHosts(webPoster.getHost());
    hosts = new JComboBox<>(webPoster.getAllHosts());
    hosts.setEditable(true);
    hosts.setMaximumRowCount(6);
    add(hosts, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    add(viewSite, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    row++;
    add(gameNameLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(gameName, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    gameName.setText(webPoster.getGameName());
    add(initGame, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    if ((this.parties == null) || (this.parties.length == 0)) {
      initGame.setEnabled(false);
    }
    row++;
    add(includeSaveGame, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    includeSaveGame.setSelected(webPoster.getMailSaveGame());
    add(testSite, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    setupListeners();
  }

  /**
   * Configures the listeners for the gui components.
   */
  private void setupListeners() {
    viewSite.addActionListener(e -> ((IWebPoster) getBean()).viewSite());
    testSite.addActionListener(e -> testSite());
    initGame.addActionListener(e -> initGame());
    hosts.addActionListener(e -> fireEditorChanged());
    // add a document listener which will validate input when the content of any input field is changed
    final DocumentListener docListener = new EditorChangedFiringDocumentListener();
    // hosts.getDocument().addDocumentListener(docListener);
    id.getDocument().addDocumentListener(docListener);
    gameName.getDocument().addDocumentListener(docListener);
  }

  private void initGame() {
    if (parties == null) {
      return;
    }
    final String hostUrl;
    if (!((String) hosts.getSelectedItem()).endsWith("/")) {
      hostUrl = (String) hosts.getSelectedItem();
    } else {
      hostUrl = hosts.getSelectedItem() + "/";
    }
    final ArrayList<String> players = new ArrayList<>();
    try {
      final URL url = new URL(hostUrl + "getplayers.php");
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try (InputStream stream = inputStream.get()) {
          try (InputStreamReader reader = new InputStreamReader(stream)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
              String inputLine;
              while ((inputLine = bufferedReader.readLine()) != null) {
                players.add(inputLine);
              }
            }
          }

        }
      }

      for (int i = 0; i < players.size(); i++) {
        players.set(i, players.get(i).substring(0, players.get(i).indexOf("\t")));
      }
    } catch (final Exception ex) {
      JOptionPane.showMessageDialog(MainFrame.getInstance(),
          "Retrieving players from " + hostUrl + " failed:\n" + ex.toString(), "Error",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    final JFrame window = new JFrame("Select Players");
    window.setLayout(new GridBagLayout());
    window.getContentPane().add(new JLabel("Select Players For Each Nation:"), new GridBagConstraints(0, 0, 2, 1, 0, 0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 20, 20, 20), 0, 0));
    final List<JComboBox<String>> comboBoxes = new ArrayList<>(parties.length);
    for (int i = 0; i < parties.length; i++) {
      final JLabel label = new JLabel(parties[i] + ": ");
      comboBoxes.add(i, new JComboBox<>());
      for (int p = 0; p < players.size(); p++) {
        comboBoxes.get(i).addItem(players.get((p)));
      }
      comboBoxes.get(i).setSelectedIndex(i % players.size());
      window.getContentPane().add(label, new GridBagConstraints(0, i + 1, 1, 1, 0, 0, GridBagConstraints.EAST,
          GridBagConstraints.NONE, new Insets(5, 20, 5, 5), 0, 0));
      window.getContentPane().add(comboBoxes.get(i),
          new GridBagConstraints(1, i + 1, 1, 1, 0, 0, GridBagConstraints.WEST,
              GridBagConstraints.NONE, new Insets(5, 5, 5, 20), 0, 0));
    }
    final JButton btnClose = new JButton("Cancel");
    btnClose.addActionListener(e -> {
      window.setVisible(false);
      window.dispose();
    });
    final JButton btnOk = new JButton("Initialize");
    btnOk.addActionListener(e -> {
      window.setVisible(false);
      window.dispose();
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < comboBoxes.size(); i++) {
        sb.append(parties[i]);
        sb.append(": ");
        sb.append(comboBoxes.get(i).getSelectedItem());
        sb.append("\n");
      }
      HttpEntity entity = MultipartEntityBuilder.create()
          .addTextBody("siteid", id.getText())
          .addTextBody("players", sb.toString())
          .addTextBody("gamename", gameName.getText())
          .build();
      try {
        final String response = TripleAWebPoster.executePost(hostUrl, "create.php", entity);
        if (response.toLowerCase().contains("success")) {
          JOptionPane.showMessageDialog(MainFrame.getInstance(), response, "Game initialized",
              JOptionPane.INFORMATION_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(MainFrame.getInstance(), "Game initialization failed:\n" + response, "Error",
              JOptionPane.INFORMATION_MESSAGE);
        }
      } catch (final Exception ex) {
        JOptionPane.showMessageDialog(MainFrame.getInstance(), "Game initialization failed:\n" + ex.toString(),
            "Error", JOptionPane.INFORMATION_MESSAGE);
      }
    });
    window.getContentPane().add(btnOk, new GridBagConstraints(0, parties.length + 1, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(30, 20, 20, 10), 0, 0));
    window.getContentPane().add(btnClose, new GridBagConstraints(1, parties.length + 1, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(30, 10, 20, 20), 0, 0));
    window.pack();
    window.setLocationRelativeTo(null);
    window.setVisible(true);
  }

  /**
   * Tests the Forum poster.
   */
  void testSite() {
    final IWebPoster poster = (IWebPoster) getBean();
    final ProgressWindow progressWindow = new ProgressWindow(MainFrame.getInstance(), poster.getTestMessage());
    progressWindow.setVisible(true);
    final Runnable runnable = () -> {
      Exception tmpException = null;
      try {
        final File f = File.createTempFile("123", "test");
        f.deleteOnExit();
        // For .jpg use this:
        final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
        final Graphics g = image.getGraphics();
        g.drawString("Testing file upload", 10, 20);
        ImageIO.write(image, "jpg", f);
        poster.addSaveGame(f, "Test.jpg");
        poster.postTurnSummary(null, "Test Turn Summary.", "TestPlayer", 1);
      } catch (final Exception ex) {
        tmpException = ex;
      } finally {
        progressWindow.setVisible(false);
      }
      final Exception exception = tmpException;
      // now that we have a result, marshall it back unto the swing thread
      SwingUtilities.invokeLater(() -> {
        try {
          final String message = (exception != null) ? exception.toString() : webPoster.getServerMessage();
          JOptionPane.showMessageDialog(MainFrame.getInstance(), message, "Test Turn Summary Post",
              JOptionPane.INFORMATION_MESSAGE);
        } catch (final HeadlessException e) {
          // should never happen in a GUI app
        }
      });
    };
    // start a background thread
    final Thread t = new Thread(runnable);
    t.start();
  }

  @Override
  public boolean isBeanValid() {
    final boolean hostValid = validateText((String) hosts.getSelectedItem(), hostLabel,
        text -> text != null && text.length() > 0 && !text.equalsIgnoreCase(HTTP_BLANK));
    final boolean idValid = validateTextFieldNotEmpty(gameName, gameNameLabel);
    final boolean allValid = hostValid && idValid;
    testSite.setEnabled(allValid);
    initGame.setEnabled(allValid);
    viewSite.setEnabled(hostValid);
    return allValid;
  }

  @Override
  public IBean getBean() {
    webPoster.setHost((String) hosts.getSelectedItem());
    webPoster.addToAllHosts((String) hosts.getSelectedItem());
    webPoster.getAllHosts().remove(HTTP_BLANK);
    webPoster.setSiteId(id.getText());
    webPoster.setMailSaveGame(includeSaveGame.isSelected());
    webPoster.setGameName(gameName.getText());
    return webPoster;
  }
}
