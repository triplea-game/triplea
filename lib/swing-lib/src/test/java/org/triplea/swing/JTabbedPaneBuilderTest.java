package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class JTabbedPaneBuilderTest {
  @Test
  void addTab() {
    final JLabel label = new JLabel("value");
    final JComponent component = new JTextField("sample component");
    final JTabbedPane pane =
        JTabbedPaneBuilder.builder().addTab("tab", label).addTab("second tab", component).build();

    assertThat(pane.getTabCount()).as("we added two tabs").isEqualTo(2);
    assertThat(pane.getTabComponentAt(0))
        .as("first tab we added was a label")
        .isInstanceOf(JLabel.class);
    assertThat(pane.getTabComponentAt(1))
        .as("second tab had a component")
        .isInstanceOf(JComponent.class);
  }
}
