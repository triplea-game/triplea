package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.map.download.MapPreviews.Size;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class PreviewSliderTest {
  @DisplayName("A click on the border slides the picture up, leaving only the border")
  @Test
  void clickSlidesPictureUp() throws Exception {
    final PreviewSlider slider = givenSliderWithPicture();
    final int openHeight = slider.getPreferredSize().height;
    final int pictureHeight = slider.getComponent(1).getPreferredSize().height;

    clickAndSlide(slider);

    assertThat(openHeight - slider.getPreferredSize().height)
        .as("the details move up by the whole picture")
        .isEqualTo(pictureHeight);
  }

  @DisplayName("A second click on the border slides the picture back down")
  @Test
  void secondClickSlidesPictureDown() throws Exception {
    final PreviewSlider slider = givenSliderWithPicture();
    final int openHeight = slider.getPreferredSize().height;
    clickAndSlide(slider);

    clickAndSlide(slider);

    assertThat(List.of(slider.isOpen(), slider.getPreferredSize().height))
        .as("open again, as high as before")
        .containsExactly(true, openHeight);
  }

  @DisplayName("The next map's picture shows the way the last one was left")
  @Test
  void nextPictureKeepsSlide() throws Exception {
    final PreviewSlider slider = givenSliderWithPicture();
    clickAndSlide(slider);
    final int closedHeight = slider.getPreferredSize().height;

    slider.showPreview(new MapPreviewBox(new MapPreviews(Size.DETAILS), ""));

    assertThat(slider.getPreferredSize().height)
        .as("browsing maps does not open the picture again")
        .isEqualTo(closedHeight);
  }

  private static PreviewSlider givenSliderWithPicture() {
    final PreviewSlider slider = new PreviewSlider();
    slider.showPreview(new MapPreviewBox(new MapPreviews(Size.DETAILS), ""));
    return slider;
  }

  /** A click on the border and the whole slide, on the EDT, where the slide's timer runs too. */
  private static void clickAndSlide(final PreviewSlider slider) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          slider.toggle();
          IntStream.range(0, 20).forEach(step -> slider.step());
        });
  }
}
