package games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post;

import com.google.common.base.Preconditions;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster.SaveGameParameter;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.java.DateTimeUtil;
import org.triplea.java.ThreadRunner;

@Slf4j
@AllArgsConstructor
public class TestPostAction implements BiConsumer<String, Integer> {

  private final Supplier<TestPostProgressDisplay> testPostProgressDisplayFactory;

  @Override
  public void accept(final String forumName, final Integer topicId) {
    Preconditions.checkNotNull(forumName);
    Preconditions.checkArgument(topicId > 0);

    final TestPostProgressDisplay testPostProgressDisplay = testPostProgressDisplayFactory.get();

    ThreadRunner.runInNewThread(
        () -> {
          Path f = null;
          try {
            f = Files.createTempFile("123", ".jpg");
            f.toFile().deleteOnExit();
            final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
            final Graphics g = image.getGraphics();
            g.drawString("Testing file upload", 10, 20);
            ImageIO.write(image, "jpg", f.toFile());
          } catch (final IOException e) {
            // ignore
          }

          final NodeBbForumPoster poster = NodeBbForumPoster.newInstanceByName(forumName, topicId);
          final CompletableFuture<String> future =
              poster.postTurnSummary(
                  "Test summary from TripleA, engine version: "
                      + ProductVersionReader.getCurrentVersion()
                      + ", time: "
                      + DateTimeUtil.getLocalizedTime(),
                  "Testing Forum poster",
                  f != null
                      ? SaveGameParameter.builder().path(f).displayName("Test.jpg").build()
                      : null);
          testPostProgressDisplay.close();
          try {
            // now that we have a result, marshall it back unto the swing thread
            future
                .thenAccept(testPostProgressDisplay::showSuccess)
                .exceptionally(
                    throwable -> {
                      testPostProgressDisplay.showFailure(throwable);
                      return null;
                    })
                .get();
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (final ExecutionException e) {
            log.error("Error while retrieving post", e);
          }
        });
  }
}
