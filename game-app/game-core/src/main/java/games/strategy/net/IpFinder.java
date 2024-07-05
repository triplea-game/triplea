package games.strategy.net;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * nekromancer@users.sourceforge.net Utility class for finding the local ip address of a machine
 * with multiple network interfaces. This class will discard any InetAddress whose
 * isLoopbackAddress() returns true. It will also discard any InetAddress whose isLinkLocalAddress()
 * returns true. On most systems the IP address it uses for internet communication will NOT be a
 * LinkLocalAddress. Even if your system goes through a gateway, the standard 192.168.0.1 address
 * will be valid (not link local and not loopback). It is up to the user to tell his/her opponents
 * the IP address of his/her gateway to connect to. And it is their responsibility to make sure they
 * have port forwarding and IP masquerading set properly. TripleA will be bound to their local
 * address and all packets will be routed through the gateway. Opponents will be bound to the
 * gateway address. In essence it should all work. IF the game is run on the system that is acting
 * as the dedicated gateway, many IPs will be found as valid. The 1st IP that will be detected will
 * be used. According to some tests, the 1st ip tends to be the IP used by the gateway to connect to
 * the net. This means that TripleA will still work.
 */
public final class IpFinder {
  private IpFinder() {}

  /**
   * We iterate through an enumeration of network interfaces on the machine and picks the first IP
   * that is not a loopback and not a link local and not private. In the case of IRIX computers
   * connected on a LAN through a central gateway running java off a telnet session will result in a
   * null network interface (patched below).
   *
   * @exception java.net.SocketException required by InetAddress
   * @exception java.net.UnknownHostException required for getLocalHost()
   * @return java.net.InetAddress the ip address to use
   */
  public static InetAddress findInetAddress() throws SocketException, UnknownHostException {
    return NetworkInterface.networkInterfaces()
        .map(NetworkInterface::getInetAddresses)
        .map(Collections::list)
        .flatMap(Collection::stream)
        .filter(not(InetAddress::isLoopbackAddress))
        .min(getInetAddressComparator())
        .orElse(Node.getLocalHost());
  }

  @VisibleForTesting
  static Comparator<InetAddress> getInetAddressComparator() {
    return Comparator.comparing(IpFinder::isPublic)
        .thenComparing(Inet4Address.class::isInstance)
        .thenComparing(InetAddress::isLinkLocalAddress, Comparator.reverseOrder())
        .reversed();
  }

  private static boolean isPublic(final InetAddress address) {
    return !(address.isSiteLocalAddress() || address.isLinkLocalAddress());
  }
}
