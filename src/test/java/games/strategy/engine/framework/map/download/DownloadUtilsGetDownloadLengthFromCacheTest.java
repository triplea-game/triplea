package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.DownloadUtils.DownloadLengthSupplier;

@RunWith(MockitoJUnitRunner.class)
public final class DownloadUtilsGetDownloadLengthFromCacheTest {
  private static final String URI = "some://uri";

  @Mock
  private DownloadLengthSupplier downloadLengthSupplier;

  @Before
  public void setUp() {
    DownloadUtils.downloadLengthsByUri.clear();
  }

  @After
  public void tearDown() {
    DownloadUtils.downloadLengthsByUri.clear();
  }

  @Test
  public void shouldUseSupplierWhenUriAbsentFromCache() {
    when(downloadLengthSupplier.get(URI)).thenReturn(Optional.of(42L));

    final Optional<Long> downloadLength = getDownloadLengthFromCache();

    assertThat(downloadLength, is(Optional.of(42L)));
    verify(downloadLengthSupplier).get(URI);
  }

  private Optional<Long> getDownloadLengthFromCache() {
    return DownloadUtils.getDownloadLengthFromCache(URI, downloadLengthSupplier);
  }

  @Test
  public void shouldUseCacheWhenUriPresentInCache() {
    DownloadUtils.downloadLengthsByUri.put(URI, 42L);

    final Optional<Long> downloadLength = getDownloadLengthFromCache();

    assertThat(downloadLength, is(Optional.of(42L)));
    verify(downloadLengthSupplier, never()).get(any());
  }

  @Test
  public void shouldNotUpdateCacheWhenSupplierProvidesEmptyValue() {
    when(downloadLengthSupplier.get(URI)).thenReturn(Optional.empty());

    final Optional<Long> downloadLength = getDownloadLengthFromCache();

    assertThat(downloadLength, is(Optional.empty()));
    assertThat(DownloadUtils.downloadLengthsByUri, is(anEmptyMap()));
  }
}
