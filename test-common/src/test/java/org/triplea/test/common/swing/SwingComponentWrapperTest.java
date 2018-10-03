package org.triplea.test.common.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.triplea.test.common.assertions.Optionals.isMissing;
import static org.triplea.test.common.assertions.Optionals.isPresent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class SwingComponentWrapperTest {

  private static final String CHILD_NAME = "child_name";


  @Test
  void findChildByName() {
    assertThat(
        withChildComponentName(CHILD_NAME).findChildByNameNonThrowing(CHILD_NAME, JLabel.class),
        isPresent());

    assertThat(
        withChildComponentName("other").findChildByNameNonThrowing(CHILD_NAME, JLabel.class),
        isMissing());
  }

  /**
   * Creates a slightly complex child/parent swing component tree with a component at the 'bottom' of that tree
   * having the name supplied by parameter.
   */
  private static SwingComponentWrapper withChildComponentName(final String childName) {
    final JLabel label = new JLabel("text");
    label.setName(childName);


    final JPanel innerPanel = new JPanel();
    innerPanel.add(new JLabel("other"));
    innerPanel.add(label);

    final JPanel panel = new JPanel();
    panel.add(innerPanel);
    panel.add(new JButton("not text type"));

    final JFrame frame = new JFrame();
    frame.add(panel);
    return SwingComponentWrapper.of(frame);
  }
}
