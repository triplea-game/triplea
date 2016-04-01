package games.strategy.common.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.triplea.UrlConstants;

public class SwingComponents {

  /** Creates a JPanel with BorderLayout and adds a west component and an east component */
  public static JPanel horizontalJPanel(final Component westComponent, final Component eastComponent) {
    return horizontalJPanel(westComponent, Optional.empty(), eastComponent);
  }

  public static JPanel horizontalJPanel(final Component westComponent, final Component centerComponent,
      final Component eastComponent) {
    return horizontalJPanel(westComponent, Optional.of(centerComponent), eastComponent);
  }

  private static JPanel horizontalJPanel(final Component westComponent, final Optional<Component> centerComponent,
      final Component eastComponent) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(westComponent, BorderLayout.WEST);
    if (centerComponent.isPresent()) {
      panel.add(centerComponent.get(), BorderLayout.CENTER);
    }
    panel.add(eastComponent, BorderLayout.EAST);
    return panel;
  }

  public static JPanel gridPanel(final int rows, final int cols) {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(rows, cols));
    return panel;
  }

  public static JButton newJButton(final String title, final String toolTip, final ActionListener actionListener) {
    final JButton button = newJButton(title, actionListener);
    button.setToolTipText(toolTip);
    return button;
  }

  public static JButton newJButton(final String title, final ActionListener actionListener) {
    final JButton button = new JButton(title);
    button.addActionListener(actionListener);
    return button;
  }


  public static JScrollPane newJScrollPane(final Component contents) {
    final JScrollPane scroll = new JScrollPane();
    scroll.setViewportView(contents);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    return scroll;
  }

  public static void promptUser(final String title, final String message, final Runnable confirmedAction) {
    promptUser(title, message, confirmedAction, () -> {
    });
  }

  public static void promptUser(final String title, final String message, final Runnable confirmedAction,
      final Runnable cancelAction) {
    SwingUtilities.invokeLater(() -> {
      final int response = JOptionPane.showConfirmDialog(null, message, title,
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      final boolean result = response == JOptionPane.YES_OPTION;

      if (result) {
        confirmedAction.run();
      } else {
        cancelAction.run();
      }
    });
  }

  public static void newMessageDialog(final String msg) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
  }

  public static JFrame newJFrameWithCloseAction(final Runnable closeListener) {
    final JFrame frame = new JFrame();
    addWindowCloseListener(frame, closeListener);
    return frame;
  }

  public static void addWindowCloseListener(final Window window, final Runnable closeAction) {
    window.addWindowListener(new WindowListener() {
      @Override
      public void windowOpened(final WindowEvent e) {}

      @Override
      public void windowClosing(final WindowEvent e) {
        closeAction.run();
      }

      @Override
      public void windowClosed(final WindowEvent e) {}

      @Override
      public void windowIconified(final WindowEvent e) {}

      @Override
      public void windowDeiconified(final WindowEvent e) {}

      @Override
      public void windowActivated(final WindowEvent e) {}

      @Override
      public void windowDeactivated(final WindowEvent e) {}
    });
  }

  public static <T> DefaultListModel<String> newJListModel(final List<T> maps, final Function<T, String> mapper) {
    final List<String> mapList = maps.stream().map(mapper).collect(Collectors.toList());
    final DefaultListModel<String> model = new DefaultListModel();
    mapList.forEach(e -> model.addElement(e));
    return model;
  }

  public static <T> JList<String> newJList(final DefaultListModel listModel) {
    return new JList(listModel);
  }

  public static JEditorPane newHtmlJEditorPane() {
    final JEditorPane m_descriptionPane = new JEditorPane();
    m_descriptionPane.setEditable(false);
    m_descriptionPane.setContentType("text/html");
    m_descriptionPane.setBackground(new JLabel().getBackground());
    return m_descriptionPane;
  }

  public static JPanel newBorderedPanel(final int borderWidth) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setBorder(newEmptyBorder(borderWidth));
    return panel;
  }

  public static Border newEmptyBorder(final int borderWidth) {
    final int w = borderWidth;
    return new EmptyBorder(w, w, w, w);
  }

  public static void newOpenUrlConfirmationDialog(final UrlConstants url) {
    newOpenUrlConfirmationDialog(url.toString());
  }

  public static void newOpenUrlConfirmationDialog(final String url) {
    final String msg = "Okay to open URL in a web browser?\n" + url;
    SwingComponents.promptUser("Open external URL?", msg, () -> {
      DesktopUtilityBrowserLauncher.openURL(url);
    });
  }

  public static JDialog newJDialogModal(final JFrame parent, final String title, final JPanel contents) {
    final JDialog dialog = new JDialog(parent, title, true);
    dialog.getContentPane().add(contents);
    final Action closeAction = SwingAction.of("", e -> dialog.setVisible(false));
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    final String key = "dialog.close";
    dialog.getRootPane().getActionMap().put(key, closeAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    return dialog;
  }
}
