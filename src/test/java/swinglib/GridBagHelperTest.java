package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

public class GridBagHelperTest {


  @Test
  public void gridBagHelperTest() {
    final JComponent component = new JPanel();
    final GridBagHelper helper = new GridBagHelper(component, 3);

    assertThat(helper.nextConstraint().gridx, is(0));
    assertThat(helper.nextConstraint().gridy, is(0));

    assertThat("verify that values do not change until we start adding components.",
        helper.nextConstraint().gridx, is(0));
    assertThat(helper.nextConstraint().gridy, is(0));


    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(1));
    assertThat(helper.nextConstraint().gridy, is(0));


    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(2));
    assertThat(helper.nextConstraint().gridy, is(0));

    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(0));
    assertThat(helper.nextConstraint().gridy, is(1));

    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(1));
    assertThat(helper.nextConstraint().gridy, is(1));

    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(2));
    assertThat(helper.nextConstraint().gridy, is(1));

    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(0));
    assertThat(helper.nextConstraint().gridy, is(2));

    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(1));
    assertThat(helper.nextConstraint().gridy, is(2));

    helper.add(buildComponent());
    assertThat(helper.nextConstraint().gridx, is(2));
    assertThat(helper.nextConstraint().gridy, is(2));

    helper.addAll(buildComponent(), buildComponent(), buildComponent());
    assertThat(helper.nextConstraint().gridx, is(2));
    assertThat(helper.nextConstraint().gridy, is(3));
  }

  private static JComponent buildComponent() {
    return new JLabel();
  }
}
