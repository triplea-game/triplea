package games.strategy.engine.framework.map.download;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.io.Files;

@RunWith(MockitoJUnitRunner.class)
public final class DownloadUtilsDownloadToFileTest {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private CloseableHttpClient client;

  @Mock
  private HttpEntity entity;

  private File file;

  private FileOutputStream os;

  @Mock
  private CloseableHttpResponse response;

  @Mock
  private StatusLine statusLine;

  /**
   * Sets up the test fixture.
   */
  @Before
  public void setUp() throws Exception {
    file = temporaryFolder.newFile();
    os = new FileOutputStream(file);

    when(client.execute(any())).thenReturn(response);
    when(response.getEntity()).thenReturn(entity);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
  }

  @Test
  public void shouldCopyEntityToFileWhenHappyPath() throws Exception {
    final byte[] bytes = givenEntityContentIs(new byte[] {42, 43, 44, 45});

    downloadToFile();

    assertThat(fileContent(), is(bytes));
  }

  private byte[] givenEntityContentIs(final byte[] bytes) throws Exception {
    when(entity.getContent()).thenReturn(new ByteArrayInputStream(bytes));
    return bytes;
  }

  private void downloadToFile() throws Exception {
    DownloadUtils.downloadToFile("some://uri", os, client);
  }

  private byte[] fileContent() throws Exception {
    return Files.toByteArray(file);
  }

  @Test
  public void shouldThrowExceptionWhenStatusCodeIsNotOk() {
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

    catchException(() -> downloadToFile());

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("status code"))));
  }

  @Test
  public void shouldThrowExceptionWhenEntityIsAbsent() {
    when(response.getEntity()).thenReturn(null);

    catchException(() -> downloadToFile());

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("entity is missing"))));
  }
}
