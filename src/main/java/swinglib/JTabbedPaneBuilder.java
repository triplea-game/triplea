package swinglib;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import org.apache.commons.math3.util.Pair;

import games.strategy.ui.SwingComponents;

public class JTabbedPaneBuilder {

  private static final int DEFAULT_TAB_WIDTH = 130;
  private static final int DEFAULT_TAB_HEIGHT = 20;
  private final List<Pair<String, Component>> components = new ArrayList<>();
  private int tabIndex = 0;


  private JTabbedPaneBuilder() {

  }

  public JTabbedPane build() {

    //
    // super.addTab(tab, contents);
    // final JLabel sizedLabel = new JLabel(tab);
    // sizedLabel.setPreferredSize(tabDimension);
    // super.setTabComponentAt(tabIndex, sizedLabel);
    // tabIndex++;

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
