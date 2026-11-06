package games.strategy.engine.framework.map.download;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import games.strategy.engine.framework.map.download.MapPreviews.Size;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// the previews are loaded through the proxy of the client settings
@WireMockTest
final class MapPreviewsTest extends AbstractClientSettingTestCase {
  @DisplayName("A preview has the shape of the box of a row, whatever the shape of the picture")
  @Test
  void previewFillsRow() {
    final BufferedImage wide = MapPreviews.scale(givenPicture(1000, 300), Size.ROW, 1);
    final BufferedImage tall = MapPreviews.scale(givenPicture(300, 1000), Size.ROW, 1);

    assertThat(List.of(wide.getWidth(), wide.getHeight(), tall.getWidth(), tall.getHeight()))
        .as("all rows show 144 x 81; the picture is cut at the sides or at the top and bottom")
        .containsExactly(144, 81, 144, 81);
  }

  @DisplayName("On a scaled screen, a preview has the screen's pixels, so it is sharp")
  @Test
  void previewForScaledScreen() {
    final BufferedImage scaled = MapPreviews.scale(givenPicture(2000, 1000), Size.ROW, 2);

    assertThat(List.of(scaled.getWidth(), scaled.getHeight()))
        .as("twice the layout size at 200 percent scaling")
        .containsExactly(288, 162);
  }

  @DisplayName("A small picture is never blown up")
  @Test
  void smallPictureIsNotUpscaled() {
    final BufferedImage scaled = MapPreviews.scale(givenPicture(400, 300), Size.DETAILS, 2);

    assertThat(List.of(scaled.getWidth(), scaled.getHeight()))
        .as("only cut to the shape of the box: the full width of 400 pixels, not 1920")
        .containsExactly(400, 225);
  }

  @DisplayName("The box shows a picture at its size in the middle when it has too few pixels")
  @Test
  void smallPictureIsCentered() {
    final Rectangle box = new Rectangle(0, 0, 960, 540);

    assertThat(
            List.of(
                MapPreviewBox.pictureBounds(1920, 1080, 2, box),
                MapPreviewBox.pictureBounds(400, 225, 1, box)))
        .as("a picture for the scaled screen fills the box; a small one is not blown up")
        .containsExactly(box, new Rectangle(280, 157, 400, 225));
  }

  private static BufferedImage givenPicture(final int width, final int height) {
    return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
  }

  @DisplayName("A loaded preview is kept, so the rows of both tabs reuse it")
  @Test
  void loadedPreviewIsKept(final WireMockRuntimeInfo server) throws Exception {
    final ByteArrayOutputStream png = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1120, 520, BufferedImage.TYPE_INT_RGB), "png", png);
    stubFor(get("/preview.png").willReturn(ok().withBody(png.toByteArray())));
    final String url = server.getHttpBaseUrl() + "/preview.png";
    final MapPreviews previews = new MapPreviews(Size.ROW);

    SwingUtilities.invokeAndWait(() -> previews.load(url, icon -> {}));

    await()
        .atMost(Duration.ofSeconds(10))
        .alias("loaded in the background and kept, scaled to the row")
        .until(
            () -> {
              // the loaded preview is kept on the EDT
              SwingUtilities.invokeAndWait(() -> {});
              return previews.get(url).isPresent();
            });
  }

  @DisplayName("A preview that cannot be loaded is not tried again, eg on every repaint")
  @Test
  void failedPreviewIsNotLoadedAgain(final WireMockRuntimeInfo server) throws Exception {
    stubFor(get("/missing.png").willReturn(notFound()));
    final String missing = server.getHttpBaseUrl() + "/missing.png";
    final MapPreviews previews = new MapPreviews(Size.ROW);
    final List<Optional<ImageIcon>> results = new ArrayList<>();
    SwingUtilities.invokeAndWait(() -> previews.load(missing, results::add));
    await()
        .atMost(Duration.ofSeconds(10))
        .until(
            () -> {
              SwingUtilities.invokeAndWait(() -> {});
              return !results.isEmpty();
            });

    final List<Optional<ImageIcon>> again = new ArrayList<>();
    SwingUtilities.invokeAndWait(() -> previews.load(missing, again::add));

    assertThat(again)
        .as("answered at once from the failures, without another download")
        .containsExactly(Optional.empty());
    verify(1, getRequestedFor(urlEqualTo("/missing.png")));
  }

  @DisplayName("Previews come only from the web; other addresses are not read, not even tried")
  @Test
  void onlyWebPreviews() throws Exception {
    final MapPreviews previews = new MapPreviews(Size.ROW);
    final List<Optional<ImageIcon>> results = new ArrayList<>();

    SwingUtilities.invokeAndWait(
        () ->
            List.of(
                    "file:///C:/Windows/win.ini",
                    "jar:file:/maps/map.zip!/preview.png",
                    "ftp://example.com/preview.png",
                    "\\\\server\\share\\preview.png")
                .forEach(url -> previews.load(url, results::add)));

    assertThat(results)
        .as("the server's address could else read the player's files")
        .containsExactly(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }
}
