package org.triplea.swing.jpanel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class SimpleGridBagLayoutBuilderTest {

  /** Single column case means every element will land in first column and a new row. */
  @Test
  void coordinateCalculatorSingleColumn() {
    final SimpleGridBagLayoutBuilder.CoordinateCalculator coordinateCalculator =
        new SimpleGridBagLayoutBuilder.CoordinateCalculator(1);

    for (int i = 0; i < 10; i++) {
      assertThat(
          "With one column, every element will be in the same column.",
          coordinateCalculator.calculateColumn(i),
          is(0));
      assertThat(
          "With only one column, each element should land on its own row.",
          coordinateCalculator.calculateRow(i),
          is(i));
    }
  }

  /**
   * Sets up a grid that is 'length' columns across, we'll add the same number of elements and
   * expect each element to be in the first row and in subsequent columns.
   */
  @Test
  void coordinateCalculatorSingleRow() {
    final int length = 10;
    final SimpleGridBagLayoutBuilder.CoordinateCalculator coordinateCalculator =
        new SimpleGridBagLayoutBuilder.CoordinateCalculator(length);

    for (int i = 0; i < length; i++) {
      assertThat(
          "With 'n' columns and adding 'n' elements,"
              + " each element should land in the subsequent column.",
          coordinateCalculator.calculateColumn(i),
          is(i));
      assertThat(
          "Each element should be in the same row.", coordinateCalculator.calculateRow(i), is(0));
    }
  }

  /**
   * Another special case with 2 columns, column number should alternate between 0 and 1 (modulus of
   * the element count), and the row number should be the flow of the element count divided by two.
   */
  @Test
  void twoColumns() {
    final int length = 10;
    final SimpleGridBagLayoutBuilder.CoordinateCalculator coordinateCalculator =
        new SimpleGridBagLayoutBuilder.CoordinateCalculator(2);

    for (int i = 0; i < length; i++) {
      assertThat(
          "With 2 columns, each element should land in the zero or one column, alternating",
          coordinateCalculator.calculateColumn(i),
          is(i % 2));
      assertThat(
          "With 2 columns, row count should be the flow of the element count divided by 2",
          coordinateCalculator.calculateRow(i),
          is(i / 2));
    }
  }

  /** Verify some brute force calculations. */
  @Test
  void threeColumns() {
    final SimpleGridBagLayoutBuilder.CoordinateCalculator coordinateCalculator =
        new SimpleGridBagLayoutBuilder.CoordinateCalculator(3);

    // first row
    assertThat(coordinateCalculator.calculateColumn(0), is(0));
    assertThat(coordinateCalculator.calculateRow(0), is(0));

    assertThat(coordinateCalculator.calculateColumn(1), is(1));
    assertThat(coordinateCalculator.calculateRow(1), is(0));

    assertThat(coordinateCalculator.calculateColumn(2), is(2));
    assertThat(coordinateCalculator.calculateRow(2), is(0));

    // second row
    assertThat(coordinateCalculator.calculateColumn(3), is(0));
    assertThat(coordinateCalculator.calculateRow(3), is(1));

    assertThat(coordinateCalculator.calculateColumn(4), is(1));
    assertThat(coordinateCalculator.calculateRow(4), is(1));

    assertThat(coordinateCalculator.calculateColumn(5), is(2));
    assertThat(coordinateCalculator.calculateRow(5), is(1));

    // third row
    assertThat(coordinateCalculator.calculateColumn(6), is(0));
    assertThat(coordinateCalculator.calculateRow(6), is(2));

    assertThat(coordinateCalculator.calculateColumn(7), is(1));
    assertThat(coordinateCalculator.calculateRow(7), is(2));

    assertThat(coordinateCalculator.calculateColumn(8), is(2));
    assertThat(coordinateCalculator.calculateRow(8), is(2));
  }
}
