package swinglib;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.junit.Test;

public class JPanelBuilderTest {

  @Test
  public void minBuildCase() {
    assertThat(JPanelBuilder.builder().build(), notNullValue());
  }

  @Test
  public void testBorder() {
    final int borderWidth = 3;
    final JPanel panel = JPanelBuilder.builder()
        .border(JPanelBuilder.BorderType.EMPTY)
        .borderWidth(borderWidth)
        .build();

    assertThat(panel.getBorder(), instanceOf(EmptyBorder.class));

    final Insets borderInsets = ((EmptyBorder) panel.getBorder()).getBorderInsets();
    assertThat(borderInsets.top, is(borderWidth));
    assertThat(borderInsets.bottom, is(borderWidth));
    assertThat(borderInsets.left, is(borderWidth));
    assertThat(borderInsets.right, is(borderWidth));
  }

  @Test
  public void horizontalAlignmentCenter() {
    final JPanel panel = JPanelBuilder.builder()
        .horizontalAlignmentCenter()
        .build();
    assertThat(panel.getAlignmentX(), is(JComponent.CENTER_ALIGNMENT));
  }

  @Test
  public void testAddComponent() {
    final JLabel label = new JLabel("hi");

    final JPanel panel = JPanelBuilder.builder()
        .add(label)
        .build();

    assertThat("Panel children should contain the label we added.",
        Arrays.asList(panel.getComponents()), contains(label));
  }

  @Test
  public void testLayouts() {
    final GridLayout result = (GridLayout) JPanelBuilder.builder()
        .gridLayout(1, 2)
        .build()
        .getLayout();
    assertThat(result.getRows(), is(1));
    assertThat(result.getColumns(), is(2));


    assertThat(JPanelBuilder.builder()
        .gridBagLayout()
        .build()
        .getLayout(),
        instanceOf(GridBagLayout.class));

    assertThat(JPanelBuilder.builder()
        .build()
        .getLayout(),
        instanceOf(BorderLayout.class));
  }
}
