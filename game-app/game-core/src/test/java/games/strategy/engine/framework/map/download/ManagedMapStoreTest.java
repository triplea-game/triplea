package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ManagedMapStoreTest {
  @Test
  void disposedStoreNoLongerListensToDownloadCoordinator() {
    final ManagedMapStore store = new ManagedMapStore();
    assertThat(DownloadCoordinator.instance.hasDownloadListener(store)).isTrue();

    store.dispose();

    assertThat(DownloadCoordinator.instance.hasDownloadListener(store)).isFalse();
  }
}
