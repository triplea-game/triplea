package games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post;

import com.google.common.base.Preconditions;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster.SaveGameParameter;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.injection.Injections;
import org.triplea.java.DateTimeUtil;

@Log
@AllArgsConstructor
public class TestPostAction implements BiConsumer<String, Integer> {

  private final Supplier<TestPostProgressDisplay> testPostProgressDisplayFactory;

  @Override
  public void accept(final String forumName, final Integer topicId) {
    Preconditions.checkNotNull(forumName);
    Preconditions.checkArgument(topicId > 0);

    final TestPostProgressDisplay testPostProgressDisplay = testPostProgressDisplayFactory.get();

    new Thread(
            () -> {
              File f = null;
              try {
                f = File.createTempFile("123", ".jpg");
                f.deleteOnExit();
                final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
                final Graphics g = image.getGraphics();
                g.drawString("Testing file upload", 10, 20);
                ImageIO.write(image, "jpg", f);
              } catch (final IOException e) {
                // ignore
              }

              final NodeBbForumPoster poster =
                  NodeBbForumPoster.newInstanceByName(forumName, topicId);
              final CompletableFuture<String> future =
                  poster.postTurnSummary(
                      "Test summary from TripleA, engine version: "
                          + Injections.getInstance().engineVersion()
                          + ", time: "
                          + DateTimeUtil.getLocalizedTime(),
                      "Testing Forum poster",
                      f != null
                          ? SaveGameParameter.builder()
                              .path(f.toPath())
                              .displayName("Test.jpg")
                              .build()
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
                log.log(Level.SEVERE, "Error while retrieving post", e);
              }
            })
        .start();
  }
}
