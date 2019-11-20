package org.triplea.swing.jpanel;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.junit.jupiter.api.Test;

class JPanelBuilderTest {

  @Test
  void horizontalAlignmentCenter() {
    final JPanel panel =
        new JPanelBuilder().horizontalAlignmentCenter().add(new JLabel("")).build();
    assertThat(panel.getAlignmentX(), is(JComponent.CENTER_ALIGNMENT));
  }

  @Test
  void testAddComponent() {
    final JLabel label = new JLabel("hi");

    final JPanel panel = new JPanelBuilder().add(label).build();

    assertThat(
        "Panel children should contain the label we added.",
        List.of(panel.getComponents()),
        contains(label));
  }

  @Test
  void defaultLayoutIsFlowLayout() {
    assertThat(
        new JPanelBuilder().add(new JLabel()).build().getLayout(), instanceOf(FlowLayout.class));
  }

  @Test
  void testLayouts() {
    final GridLayout result = (GridLayout) new JPanelBuilder().gridLayout(1, 2).build().getLayout();
    assertThat(result.getRows(), is(1));
    assertThat(result.getColumns(), is(2));

    assertThat(
        new JPanelBuilder().gridBagLayout().build().getLayout(), instanceOf(GridBagLayout.class));

    assertThat(new JPanelBuilder().flowLayout().build().getLayout(), instanceOf(FlowLayout.class));

    assertThat(
        new JPanelBuilder().borderLayout().build().getLayout(), instanceOf(BorderLayout.class));
  }

  @Test
  void emptyBorderWithSingleWidth() {
    final int borderWidth = 100;
    final JPanel panel = new JPanelBuilder().border(borderWidth).add(new JLabel()).build();
    assertThat(panel.getBorder(), instanceOf(EmptyBorder.class));
    final Insets insets = panel.getBorder().getBorderInsets(panel);
    assertThat(insets.top, is(borderWidth));
    assertThat(insets.bottom, is(borderWidth));
    assertThat(insets.left, is(borderWidth));
    assertThat(insets.right, is(borderWidth));
  }

  @Test
  void emptyBorderWithIndependentWidths() {
    final JPanel panel = new JPanelBuilder().border(1, 2, 3, 4).add(new JLabel()).build();

    assertThat(panel.getBorder(), instanceOf(EmptyBorder.class));

    final Insets insets = panel.getBorder().getBorderInsets(panel);
    assertThat(insets.top, is(1));
    assertThat(insets.left, is(2));
    assertThat(insets.bottom, is(3));
    assertThat(insets.right, is(4));
  }

  @Test
  void addLabel() {
    final String labelText = "abc";

    final JPanel panel = new JPanelBuilder().add(new JLabel(labelText)).build();

    assertThat(panel.getComponents().length, is(1));
    assertThat(panel.getComponents()[0], instanceOf(JLabel.class));
    assertThat(((JLabel) panel.getComponents()[0]).getText(), is(labelText));
  }
}
