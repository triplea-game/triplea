package games.strategy.common.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import com.google.common.collect.Sets;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.triplea.UrlConstants;

public class SwingComponents {

  private static Set<String> visiblePrompts = Sets.newHashSet();

  /** Creates a JPanel with BorderLayout and adds a west component and an east component */
  public static JPanel horizontalJPanel(Component westComponent, Component eastComponent) {
    return horizontalJPanel(westComponent, Optional.empty(), eastComponent);
  }

  public static JPanel horizontalJPanel(Component westComponent, Component centerComponent, Component eastComponent) {
    return horizontalJPanel(westComponent, Optional.of(centerComponent), eastComponent);
  }

  private static JPanel horizontalJPanel(Component westComponent, Optional<Component> centerComponent,
      Component eastComponent) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(westComponent, BorderLayout.WEST);
    if (centerComponent.isPresent()) {
      panel.add(centerComponent.get(), BorderLayout.CENTER);
    }
    panel.add(eastComponent, BorderLayout.EAST);
    return panel;
  }

  public static JPanel gridPanel(int rows, int cols) {
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(rows, cols));
    return panel;
  }

  public static JButton newJButton(String title, String toolTip, Runnable actionListener) {
    return newJButton(title, toolTip, SwingAction.of(e->actionListener.run()));
  }

  public static JButton newJButton(String title, String toolTip, ActionListener actionListener) {
    JButton button = newJButton(title, actionListener);
    button.setToolTipText(toolTip);
    return button;
  }

  public static JButton newJButton(String title, ActionListener actionListener) {
    JButton button = new JButton(title);
    button.addActionListener(actionListener);
    return button;
  }


  public static JScrollPane newJScrollPane(Component contents) {
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

    boolean showMessage = false;
    synchronized (visiblePrompts) {
      if (!visiblePrompts.contains(message)) {
        visiblePrompts.add(message);
        showMessage = true;
      }
    }

    if (showMessage) {
      SwingUtilities.invokeLater(() -> {
        // blocks until the user responds to the modal dialog
        int response = JOptionPane.showConfirmDialog(null, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        // dialog is now closed
        visiblePrompts.remove(message);
        if( response == JOptionPane.YES_OPTION ) {
          confirmedAction.run();
        } else {
          cancelAction.run();
        }
      });
    }

  }

  public static void newMessageDialog(String msg) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
  }

  public static JFrame newJFrameWithCloseAction(final Runnable closeListener) {
    JFrame frame = new JFrame();
    addWindowCloseListener(frame, closeListener);
    return frame;
  }

  public static void addWindowCloseListener(Window window, Runnable closeAction) {
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        closeAction.run();
      }
    });
  }

  public static <T> DefaultListModel<String> newJListModel(List<T> maps, Function<T, String> mapper) {
    List<String> mapList = maps.stream().map(mapper).collect(Collectors.toList());
    DefaultListModel<String> model = new DefaultListModel();
    mapList.forEach(e -> model.addElement(e));
    return model;
  }

  public static <T> JList<String> newJList(DefaultListModel listModel) {
    return new JList(listModel);
  }

  public static JEditorPane newHtmlJEditorPane() {
    JEditorPane m_descriptionPane = new JEditorPane();
    m_descriptionPane.setEditable(false);
    m_descriptionPane.setContentType("text/html");
    m_descriptionPane.setBackground(new JLabel().getBackground());
    return m_descriptionPane;
  }

  public static JPanel newBorderedPanel(int borderWidth) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setBorder(newEmptyBorder(borderWidth));
    return panel;
  }

  public static Border newEmptyBorder(int borderWidth) {
    int w = borderWidth;
    return new EmptyBorder(w, w, w, w);
  }

  public static void newOpenUrlConfirmationDialog(UrlConstants url) {
    newOpenUrlConfirmationDialog(url.toString());
  }

  public static void newOpenUrlConfirmationDialog(String url) {
    final String msg = "Okay to open URL in a web browser?\n" + url;
    SwingComponents.promptUser("Open external URL?", msg, () -> {
      DesktopUtilityBrowserLauncher.openURL(url);
    });
  }
}
