package org.triplea.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import lombok.Value;

/**
 * Builder for a JTabbedPane. Provides a convenient API for adding components to a JTabbedPane and
 * starts off with reasonable defaults that can be configured further as needed. <br>
 * Example usage:
 *
 * <pre>
 * <code>
 * JTabbedPaneBuilder JTabbedPaneBuilder.builder()
 *    .addTab(new JLabel("I will be tab one")
 *    .addTab(new JLabel("And I will be tab two")
 *    .build();
 * </code>
 * </pre>
 */
public class JTabbedPaneBuilder {

  private static final int DEFAULT_TAB_WIDTH = 130;
  private static final int DEFAULT_TAB_HEIGHT = 20;

  @Value
  private static class Tab {
    private final String tabName;
    private final Component contents;
  }

  private final List<Tab> components = new ArrayList<>();
  private int tabIndex = 0;

  /** Builds the swing component. */
  public JTabbedPane build() {
    final JTabbedPane tabbedPane = new JTabbedPane();

    components.forEach(
        component -> {
          final JLabel sizedLabel = new JLabel(component.getTabName());
          sizedLabel.setPreferredSize(new Dimension(DEFAULT_TAB_WIDTH, DEFAULT_TAB_HEIGHT));
          tabbedPane.addTab(
              component.getTabName(), SwingComponents.newJScrollPane(component.getContents()));
          tabbedPane.setTabComponentAt(tabIndex, sizedLabel);
          tabIndex++;
        });

    return tabbedPane;
  }

  public JTabbedPaneBuilder addTab(final String tabTitle, final Component tabContents) {
    components.add(new Tab(tabTitle, tabContents));
    return this;
  }

  public static JTabbedPaneBuilder builder() {
    return new JTabbedPaneBuilder();
  }
}
