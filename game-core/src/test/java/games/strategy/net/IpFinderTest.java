package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class IpFinderTest {
  // 172.217.3.132
  private static final InetAddress INET4_PUBLIC_ADDRESS =
      inetAddressOf(new byte[] {(byte) 0xAC, (byte) 0xD9, (byte) 0x03, (byte) 0x84});

  // 192.168.2.1
  private static final InetAddress INET4_SITE_LOCAL_ADDRESS =
      inetAddressOf(new byte[] {(byte) 0xC0, (byte) 0xA8, (byte) 0x02, (byte) 0x01});

  // 169.254.2.1
  private static final InetAddress INET4_LINK_LOCAL_ADDRESS =
      inetAddressOf(new byte[] {(byte) 0xA9, (byte) 0xFE, (byte) 0x02, (byte) 0x01});

  // 2607:f8b0:4008:80c::2004
  private static final InetAddress INET6_PUBLIC_ADDRESS =
      inetAddressOf(
          new byte[] {
            (byte) 0x26, (byte) 0x07, (byte) 0xF8, (byte) 0xB0, (byte) 0x40, (byte) 0x08,
                (byte) 0x08, (byte) 0x0C,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x20, (byte) 0x04
          });

  // fec0::1ff:fe23:4567:890a
  private static final InetAddress INET6_SITE_LOCAL_ADDRESS =
      inetAddressOf(
          new byte[] {
            (byte) 0xFE, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0xFF, (byte) 0xFE, (byte) 0x23, (byte) 0x45, (byte) 0x67,
                (byte) 0x89, (byte) 0x0A
          });

  // fe80::1ff:fe23:4567:890a
  private static final InetAddress INET6_LINK_LOCAL_ADDRESS =
      inetAddressOf(
          new byte[] {
            (byte) 0xFE, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0xFF, (byte) 0xFE, (byte) 0x23, (byte) 0x45, (byte) 0x67,
                (byte) 0x89, (byte) 0x0A
          });

  private static InetAddress inetAddressOf(final byte[] addr) {
    try {
      return InetAddress.getByAddress(addr);
    } catch (final UnknownHostException e) {
      throw new AssertionError("should not happen", e);
    }
  }

  @BeforeAll
  static void checkInvariants() {
    assertThat(INET4_LINK_LOCAL_ADDRESS.isLinkLocalAddress(), is(true));
    assertThat(INET4_PUBLIC_ADDRESS.isLinkLocalAddress(), is(false));
    assertThat(INET4_PUBLIC_ADDRESS.isSiteLocalAddress(), is(false));
    assertThat(INET4_SITE_LOCAL_ADDRESS.isSiteLocalAddress(), is(true));

    assertThat(INET6_LINK_LOCAL_ADDRESS.isLinkLocalAddress(), is(true));
    assertThat(INET6_PUBLIC_ADDRESS.isLinkLocalAddress(), is(false));
    assertThat(INET6_PUBLIC_ADDRESS.isSiteLocalAddress(), is(false));
    assertThat(INET6_SITE_LOCAL_ADDRESS.isSiteLocalAddress(), is(true));
  }

  @Nested
  final class GetInetAddressComparatorTest {
    private InetAddress selectInetAddress(final InetAddress... inetAddresses) {
      return Stream.of(inetAddresses)
          .min(IpFinder.getInetAddressComparator())
          .orElseThrow(() -> new AssertionError("failed to select an InetAddress"));
    }

    @Test
    void shouldSelectInet4PublicAddressBeforeInet6PublicAddress() {
      assertThat(
          selectInetAddress(INET6_PUBLIC_ADDRESS, INET4_PUBLIC_ADDRESS), is(INET4_PUBLIC_ADDRESS));
      assertThat(
          selectInetAddress(INET4_PUBLIC_ADDRESS, INET6_PUBLIC_ADDRESS), is(INET4_PUBLIC_ADDRESS));
    }

    @Test
    void shouldSelectInet6PublicAddressBeforeInet4LinkLocalAddress() {
      assertThat(
          selectInetAddress(INET4_LINK_LOCAL_ADDRESS, INET6_PUBLIC_ADDRESS),
          is(INET6_PUBLIC_ADDRESS));
      assertThat(
          selectInetAddress(INET6_PUBLIC_ADDRESS, INET4_LINK_LOCAL_ADDRESS),
          is(INET6_PUBLIC_ADDRESS));
    }

    @Test
    void shouldSelectInet6PublicAddressBeforeInet4SiteLocalAddress() {
      assertThat(
          selectInetAddress(INET4_SITE_LOCAL_ADDRESS, INET6_PUBLIC_ADDRESS),
          is(INET6_PUBLIC_ADDRESS));
      assertThat(
          selectInetAddress(INET6_PUBLIC_ADDRESS, INET4_SITE_LOCAL_ADDRESS),
          is(INET6_PUBLIC_ADDRESS));
    }

    @Test
    void shouldSelectInet4SiteLocalAddressBeforeInet4LinkLocalAddress() {
      assertThat(
          selectInetAddress(INET4_LINK_LOCAL_ADDRESS, INET4_SITE_LOCAL_ADDRESS),
          is(INET4_SITE_LOCAL_ADDRESS));
      assertThat(
          selectInetAddress(INET4_SITE_LOCAL_ADDRESS, INET4_LINK_LOCAL_ADDRESS),
          is(INET4_SITE_LOCAL_ADDRESS));
    }

    @Test
    void shouldSelectInet6SiteLocalAddressBeforeInet6LinkLocalAddress() {
      assertThat(
          selectInetAddress(INET6_LINK_LOCAL_ADDRESS, INET6_SITE_LOCAL_ADDRESS),
          is(INET6_SITE_LOCAL_ADDRESS));
      assertThat(
          selectInetAddress(INET6_SITE_LOCAL_ADDRESS, INET6_LINK_LOCAL_ADDRESS),
          is(INET6_SITE_LOCAL_ADDRESS));
    }
  }
}
