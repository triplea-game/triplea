package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

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

    assertThat("we added two tabs", pane.getTabCount(), is(2));
    assertThat(
        "first tab we added was a label", pane.getTabComponentAt(0), instanceOf(JLabel.class));
    assertThat(
        "second tab had a component", pane.getTabComponentAt(1), instanceOf(JComponent.class));
  }
}
