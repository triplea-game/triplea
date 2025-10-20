package tools.util;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ToolRunnableTask {
  // Avoid external instantiation by subclasses
  protected ToolRunnableTask() {}

  /**
   * Allows each subclass to run its {@link #runInternal()} in EDT.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  protected static void runTask(Class<? extends ToolRunnableTask> taskClass) {
    checkState(SwingUtilities.isEventDispatchThread());

    try {
      // Instantiate via reflection (requires a no-arg constructor)
      ToolRunnableTask task = taskClass.getDeclaredConstructor().newInstance();
      task.runInternal();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to run task: " + taskClass.getName(), e);
    } catch (final IOException e) {
      log.error("Failed to run task: {}", taskClass.getName(), e);
    }
  }

  // To be implemented by each subclass
  protected abstract void runInternal() throws IOException;
}
