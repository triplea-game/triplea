package org.triplea.swing;

import games.strategy.engine.framework.system.SystemProperties;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A panel that can be collapsed and expanded via a button at the top of it.
 *
 * <p>The panel supports a main content component that will either be shown or hidden, depending on
 * whether the panel is expanded or collapsed and a a title that will be shown on the toggle button.
 */
public class CollapsiblePanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private static final char COLLAPSED_INDICATOR = '►';
  private static final String COLLAPSED_TEXT =
      SwingComponents.canDisplayCharacter(COLLAPSED_INDICATOR) ? " " + COLLAPSED_INDICATOR : "";

  private static final char EXPANDED_INDICATOR = '▼';
  private static final String EXPANDED_TEXT =
      SwingComponents.canDisplayCharacter(EXPANDED_INDICATOR) ? " " + EXPANDED_INDICATOR : "";

  private final JPanel content;
  private final JButton toggleButton;

  private String title;
  private String currentToggleIndicator = EXPANDED_TEXT;

  public CollapsiblePanel(final JPanel content, final String title) {
    super();
    this.title = title;
    this.content = content;
    this.toggleButton = new JButton();
    // We want the button to have square edges. On Mac, there's a special property for that.
    if (SystemProperties.isMac()) {
      toggleButton.putClientProperty("JButton.buttonType", "gradient");
    }

    toggleButton.addActionListener(
        e -> {
          if (content.isVisible()) {
            collapse();
          } else {
            expand();
          }
        });
    setLayout(new BorderLayout());
    add(toggleButton, BorderLayout.NORTH);
    add(content, BorderLayout.CENTER);
    expand();
  }

  public void collapse() {
    currentToggleIndicator = COLLAPSED_TEXT;
    toggleButton.setText(title + currentToggleIndicator);
    content.setVisible(false);
    revalidate();
  }

  public void expand() {
    currentToggleIndicator = EXPANDED_TEXT;
    toggleButton.setText(title + currentToggleIndicator);
    content.setVisible(true);
    revalidate();
  }

  public void setTitle(final String title) {
    this.title = title;
    SwingUtilities.invokeLater(() -> toggleButton.setText(title + currentToggleIndicator));
  }
}
