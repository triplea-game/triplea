package swinglib;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import org.apache.commons.math3.util.Pair;

import games.strategy.ui.SwingComponents;

/**
 * Builder for a JTabbedPane. Provides a convenient API for adding components
 * to a JTabbedPane and starts off with reasonable defaults that can be configured
 * further as needed.
 * <br />
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
  private final List<Pair<String, Component>> components = new ArrayList<>();
  private int tabIndex = 0;

  private JTabbedPaneBuilder() {

  }

  /**
   * Builds the swing component.
   */
  public JTabbedPane build() {
    final JTabbedPane tabbedPane = new JTabbedPane();

    components.forEach(component -> {
      final JLabel sizedLabel = new JLabel(component.getKey());
      sizedLabel.setPreferredSize(new Dimension(DEFAULT_TAB_WIDTH, DEFAULT_TAB_HEIGHT));
      tabbedPane.addTab(component.getKey(), SwingComponents.newJScrollPane(component.getValue()));
      tabbedPane.setTabComponentAt(tabIndex, sizedLabel);
      tabIndex++;
    });

    return tabbedPane;
  }

  public JTabbedPaneBuilder addTab(final String tabTitle, final Component tabContents) {
    components.add(new Pair<>(tabTitle, tabContents));
    return this;
  }


  public static JTabbedPaneBuilder builder() {
    return new JTabbedPaneBuilder();
  }
}
