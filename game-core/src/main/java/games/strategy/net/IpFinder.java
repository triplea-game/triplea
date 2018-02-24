package games.strategy.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import games.strategy.util.Util;

/**
 * nekromancer@users.sourceforge.net
 * Utility class for finding the local ip address of a machine with multiple network interfaces.
 * This class will discard any InetAddress whose isLoobackAddresS() returns true.
 * It will also discard any InetAddress whose isLinkLocalAddress() returns true.
 * On most systems the IP addres it uses for internet communication will NOT be
 * a LinkLocalAddress. Even if your system goes through a gateway, the standard
 * 192.168.0.1 address will be valid (not link local and not loopback). It is up
 * to the user to tell his/her opponents the IP address of his/her gateway to
 * connect to. And it is their responsibility to make sure they have port forwarding
 * and IP masquarading set properly. TripleA will be bound to their local address
 * and all packets will be routed through the gateway. Opponents will be bound to the
 * gateway address. In essence it should all work.
 * IF the game is run on the system that is acting as the dedicated gateway, many IPs
 * will be found as valid. The 1st IP that will be detected will be used. According to
 * some tests, the 1st ip tends to be the IP used by the gateway to connect to the net.
 * This means that TripleA will still work.
 */
public class IpFinder {
  /**
   * We iterate through an enumeration of network interfaces on the machine
   * and picks the first IP that is not a loopback and not a link local and not private.
   * In the case of IRIX computers connected on a LAN through a central
   * gateway running java off a telnet session will result in a null
   * network interface (patched below).
   *
   * @exception java.net.SocketException
   *            required by InetAddress
   * @exception java.net.UnknownHostException
   *            required for getLocalHost()
   * @return java.net.InetAddress the ip address to use
   */
  public static InetAddress findInetAddress() throws SocketException, UnknownHostException {
    final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    if (interfaces == null) {
      return InetAddress.getLocalHost();
    }
    final List<InetAddress> allButLoopback = Collections.list(interfaces).stream()
        .map(NetworkInterface::getInetAddresses)
        .map(Collections::list)
        .flatMap(Collection::stream)
        .filter(Util.not(InetAddress::isLoopbackAddress))
        .collect(Collectors.toList());
    // try to find one that is not private and ip4
    for (final InetAddress address : allButLoopback) {
      if (address.getAddress().length == 4 && isPublic(address)) {
        return address;
      }
    }
    // try to find one that is not private
    for (final InetAddress address : allButLoopback) {
      if (isPublic(address)) {
        return address;
      }
    }
    // try to find one that is not link local
    for (final InetAddress address : allButLoopback) {
      if (!address.isLinkLocalAddress()) {
        return address;
      }
    }
    // all else fails, return localhost
    return InetAddress.getLocalHost();
  }

  private static boolean isPublic(final InetAddress address) {
    return !(address.isSiteLocalAddress() || address.isLinkLocalAddress());
  }
}
