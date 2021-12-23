package org.triplea.java.concurrency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
final class CompletableFutureUtilsTest {
  @Nested
  final class LogExceptionWhenCompleteTest {
    private Throwable consumedObject;
    private final Consumer<Throwable> exceptionConsumer = throwable -> consumedObject = throwable;

    @Test
    void shouldNotWriteLogWhenFutureCompletesNormally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      CompletableFutureUtils.logExceptionWhenComplete(future, exceptionConsumer);
      future.complete(new Object());

      assertThat(consumedObject, nullValue());
    }

    @Test
    void shouldWriteLogWhenFutureCompletesExceptionally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      CompletableFutureUtils.logExceptionWhenComplete(future, exceptionConsumer);
      final Exception ex = new Exception();
      future.completeExceptionally(ex);

      assertThat(consumedObject, is(sameInstance(ex)));
    }
  }
}
