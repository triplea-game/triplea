package games.strategy.ui;

import static games.strategy.ui.SwingComponents.appendExtensionIfAbsent;
import static games.strategy.ui.SwingComponents.extensionWithLeadingPeriod;
import static games.strategy.ui.SwingComponents.extensionWithoutLeadingPeriod;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

public final class SwingComponentsTest {
  @Test
  public void testAppendExtensionIfAbsent_ShouldAppendExtensionWhenExtensionAbsent() {
    assertThat(appendExtensionIfAbsent(new File("path/file.aaa"), "bbb"), is(new File("path/file.aaa.bbb")));
    assertThat(appendExtensionIfAbsent(new File("path/filebbb"), "bbb"), is(new File("path/filebbb.bbb")));
  }

  @Test
  public void testAppendExtensionIfAbsent_ShouldNotAppendExtensionWhenExtensionPresent() {
    assertThat(appendExtensionIfAbsent(new File("path/file.bbb"), "bbb"), is(new File("path/file.bbb")));
  }

  @Test
  public void testAppendExtensionIfAbsent_ShouldHandleExtensionThatStartsWithPeriod() {
    assertThat(appendExtensionIfAbsent(new File("path/file.aaa"), ".bbb"), is(new File("path/file.aaa.bbb")));
  }

  @Test
  public void testAppendExtensionIfAbsent_ShouldUseCaseInsensitiveComparisonForExtension() {
    assertThat(appendExtensionIfAbsent(new File("path/file.bBb"), "BbB"), is(new File("path/file.bBb")));
  }

  @Test
  public void testExtensionWithLeadingPeriod() {
    assertThat(extensionWithLeadingPeriod(""), is(""));

    assertThat(extensionWithLeadingPeriod("a"), is(".a"));
    assertThat(extensionWithLeadingPeriod(".a"), is(".a"));

    assertThat(extensionWithLeadingPeriod("aa"), is(".aa"));
    assertThat(extensionWithLeadingPeriod(".aa"), is(".aa"));

    assertThat(extensionWithLeadingPeriod("aaa"), is(".aaa"));
    assertThat(extensionWithLeadingPeriod(".aaa"), is(".aaa"));

    assertThat(extensionWithLeadingPeriod("aaa.aaa"), is(".aaa.aaa"));
    assertThat(extensionWithLeadingPeriod(".aaa.aaa"), is(".aaa.aaa"));
  }

  @Test
  public void testExtensionWithoutLeadingPeriod() {
    assertThat(extensionWithoutLeadingPeriod(""), is(""));

    assertThat(extensionWithoutLeadingPeriod("a"), is("a"));
    assertThat(extensionWithoutLeadingPeriod(".a"), is("a"));

    assertThat(extensionWithoutLeadingPeriod("aa"), is("aa"));
    assertThat(extensionWithoutLeadingPeriod(".aa"), is("aa"));

    assertThat(extensionWithoutLeadingPeriod("aaa"), is("aaa"));
    assertThat(extensionWithoutLeadingPeriod(".aaa"), is("aaa"));

    assertThat(extensionWithoutLeadingPeriod("aaa.aaa"), is("aaa.aaa"));
    assertThat(extensionWithoutLeadingPeriod(".aaa.aaa"), is("aaa.aaa"));
  }
}
