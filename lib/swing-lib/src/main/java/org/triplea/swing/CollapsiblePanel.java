package org.triplea.swing;

import games.strategy.engine.framework.system.SystemProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
  private String title;
  private final JButton toggleButton;

  public CollapsiblePanel(final JPanel content, final String title) {
    this.content = content;
    this.title = title;
    this.toggleButton = new JButton();
    toggleButton.addActionListener(e -> toggleState());
    setLayout(new BorderLayout());
    if (SystemProperties.isMac() && SwingComponents.isUsingNativeLookAndFeel()) {
      // We want the button to have square corners with the Mac native look and feel. Setting the
      // the preferred size achieves that. Note: The "JButton.buttonType" client property will also
      // provide a square look, but results in too much padding around the button. We actually want
      // a bit of padding, so do that using the vgap on the layout (as changing the border loses the
      // look of the button). Also, remove the focus ring, which doesn't look great.
      toggleButton.setPreferredSize(new Dimension(80, 22));
      ((BorderLayout) getLayout()).setVgap(1);
      toggleButton.setFocusPainted(false);
    }
    add(toggleButton, BorderLayout.NORTH);
    add(content, BorderLayout.CENTER);
    updateButtonText();
  }

  public void setTitle(final String title) {
    this.title = title;
    SwingUtilities.invokeLater(this::updateButtonText);
  }

  /**
   * Called when the button is clicked by the user to toggle the state. Subclasses can override to
   * monitor the toggle.
   */
  protected void toggleState() {
    setCollapsed(!isCollapsed());
  }

  public boolean isCollapsed() {
    return !content.isVisible();
  }

  public void setCollapsed(final boolean collapsed) {
    if (content.isVisible() != collapsed) {
      return;
    }
    content.setVisible(!collapsed);
    updateButtonText();
    revalidate();
  }

  private void updateButtonText() {
    final String indicator = isCollapsed() ? COLLAPSED_TEXT : EXPANDED_TEXT;
    toggleButton.setText(title + indicator);
  }
}
