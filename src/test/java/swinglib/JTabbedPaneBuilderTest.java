package swinglib;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

public class JTabbedPaneBuilderTest {
  @Test
  public void addTab() {
    final JLabel label = new JLabel("value");
    final JComponent component = new JTextField("sample component");
    final JTabbedPane pane = JTabbedPaneBuilder.builder()
        .addTab("tab", label)
        .addTab("second tab", component)
        .build();

    MatcherAssert.assertThat("we added two tabs",
        pane.getTabCount(), Is.is(2));
    MatcherAssert.assertThat("first tab we added was a label",
        pane.getTabComponentAt(0), IsInstanceOf.instanceOf(JLabel.class));
    MatcherAssert.assertThat("second tab had a component",
        pane.getTabComponentAt(1), IsInstanceOf.instanceOf(JComponent.class));
  }
}
