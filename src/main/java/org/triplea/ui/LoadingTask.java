package org.triplea.ui;

import java.util.function.Function;

import javafx.application.Platform;

public class LoadingTask<T> {

  private Function<LoadingTask<T>, T> function;

  public LoadingTask(Function<LoadingTask<T>, T> function) {
    if (TripleA.instance == null) {
      throw new IllegalStateException("The TripleA frame MUST be initialised.");
    }
    this.function = function;
  }

  public T run() {
    if (Platform.isFxApplicationThread()) {
      throw new IllegalStateException("This method must not be called on the FX Application Thread!");
    }
    try {
      Platform.runLater(() -> TripleA.instance.displayLoadingScreen(true));
      return function.apply(this);
    } finally {
      Platform.runLater(() -> {
        TripleA.instance.displayLoadingScreen(false);
        TripleA.instance.setLoadingMessage("");
      });
    }
  }

  public void setLoadingMesage(String message) {
    Platform.runLater(() -> TripleA.instance.setLoadingMessage(message));
  }
}
