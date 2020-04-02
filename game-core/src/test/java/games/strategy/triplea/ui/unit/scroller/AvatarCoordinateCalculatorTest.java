package games.strategy.triplea.ui.unit.scroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.awt.Point;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AvatarCoordinateCalculatorTest {

  @Nested
  class OutputCardinalityMatchesUnitCount {

    @RepeatedTest(250)
    @DisplayName("Verify output count always matches the number of units we are rendering")
    void calculatedPointCountMatchesNumberOfUnitsToDraw() {
      final int unitCount = (int) (Math.random() * 100);

      final AvatarCoordinateCalculator input =
          AvatarCoordinateCalculator.builder()
              .renderingHeight((int) (Math.random() * 100) + 20)
              .renderingWidth((int) (Math.random() * 100) + 20)
              .unitImageCount(unitCount)
              .unitImageHeight((int) (Math.random() * 5) + 5)
              .unitImageWidth((int) (Math.random() * 5) + 5)
              .build();

      final List<Point> points = input.computeDrawCoordinates();

      assertThat(
          String.format(
              "Input: %s, with %s units, should have computed as many points as there are units",
              input, unitCount),
          points,
          hasSize(unitCount));
    }
  }

  @Nested
  class XCoordinateCalculation {
    @Test
    @DisplayName("100x100 grid - Single unit is rendered in middle")
    void singleDrawUnitCoordinates0() {
      final List<Point> points =
          AvatarCoordinateCalculator.builder()
              .renderingHeight(100)
              .renderingWidth(100)
              .unitImageCount(1)
              .unitImageHeight(20)
              .unitImageWidth(20)
              .build()
              .computeDrawCoordinates();

      final int midpoint = 100 / 2;
      final int halfImageWidth = 20 / 2;
      assertThat(points.get(0).x, is(midpoint - halfImageWidth));
    }

    @Test
    @DisplayName("30x30 grid - Single unit is rendered in middle")
    void singleDrawUnitCoordinates1() {
      final List<Point> points =
          AvatarCoordinateCalculator.builder()
              .renderingHeight(30)
              .renderingWidth(30)
              .unitImageCount(1)
              .unitImageHeight(10)
              .unitImageWidth(10)
              .build()
              .computeDrawCoordinates();

      final int midpoint = 30 / 2;
      final int halfImageWidth = 10 / 2;
      assertThat(points.get(0).x, is(midpoint - halfImageWidth));
    }

    @Test
    @DisplayName("100x100 grid - 2 units rendered")
    void drawUnits2() {
      final List<Point> points =
          AvatarCoordinateCalculator.builder()
              .renderingHeight(100)
              .renderingWidth(100)
              .unitImageCount(2)
              .unitImageHeight(10)
              .unitImageWidth(10)
              .build()
              .computeDrawCoordinates();

      final int spacing = 100 / 3;
      final int halfImageWidth = (10 / 2);

      assertThat(points.get(0).x, is(spacing - halfImageWidth));
      assertThat(points.get(1).x, is((2 * spacing) - halfImageWidth));
    }

    @Test
    @DisplayName("100x100 grid - 4 units rendered")
    void drawUnits4() {
      final List<Point> points =
          AvatarCoordinateCalculator.builder()
              .renderingHeight(100)
              .renderingWidth(100)
              .unitImageCount(4)
              .unitImageHeight(10)
              .unitImageWidth(10)
              .build()
              .computeDrawCoordinates();

      final int spacing = 100 / 5;
      final int halfImageWidth = (10 / 2);

      assertThat(points.get(0).x, is(spacing - halfImageWidth));
      assertThat(points.get(1).x, is((2 * spacing) - halfImageWidth));
      assertThat(points.get(2).x, is((3 * spacing) - halfImageWidth));
      assertThat(points.get(3).x, is((4 * spacing) - halfImageWidth));
    }

    @Test
    @DisplayName("100x100 grid - 5 units rendered")
    void drawUnits5() {
      final List<Point> points =
          AvatarCoordinateCalculator.builder()
              .renderingHeight(100)
              .renderingWidth(100)
              .unitImageCount(5)
              .unitImageHeight(10)
              .unitImageWidth(10)
              .build()
              .computeDrawCoordinates();

      final int spacing = 100 / 6;
      final int halfImageWidth = (10 / 2);

      assertThat(points.get(0).x, is(spacing - halfImageWidth));
      assertThat(points.get(1).x, is((2 * spacing) - halfImageWidth));
      assertThat(points.get(2).x, is((3 * spacing) - halfImageWidth));
      assertThat(points.get(3).x, is((4 * spacing) - halfImageWidth));
      assertThat(points.get(4).x, is((5 * spacing) - halfImageWidth));
    }
  }

  @SuppressWarnings("unused")
  private static List<Arguments> verifyYCoordinate() {
    return List.of(
        Arguments.of(100, 5), //
        Arguments.of(50, 10),
        Arguments.of(80, 15),
        Arguments.of(10, 10));
  }

  @MethodSource
  @ParameterizedTest
  @DisplayName("Y value is a constant for each calculated point")
  void verifyYCoordinate(final int height, final int imageHeight) {
    final int unitCount = 10;
    final List<Point> points =
        AvatarCoordinateCalculator.builder()
            .renderingHeight(height)
            .renderingWidth(30)
            .unitImageCount(unitCount)
            .unitImageHeight(imageHeight)
            .unitImageWidth(10)
            .build()
            .computeDrawCoordinates();

    for (int i = 0; i < unitCount; i++) {
      assertThat(points.get(i).y, is(height - imageHeight));
    }
  }
}
