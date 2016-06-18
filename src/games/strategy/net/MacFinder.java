package games.strategy.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.primitives.Bytes;

import games.strategy.debug.ClientLogger;
import games.strategy.util.MD5Crypt;

public class MacFinder {

  /**
   * Should result in something like this: $1$MH$345ntXD4G3AKpAeHZdaGe3
   */
  public static String getHashedMacAddress() {
    final String mac = getMacAddress();
    if (mac == null) {
      throw new IllegalArgumentException(
          "You have an invalid MAC address!");
    }
    return MD5Crypt.crypt(mac, "MH");
  }

  private static String getMacAddress() {
    try {
      final InetAddress address = InetAddress.getLocalHost();
      final NetworkInterface localHostNI = NetworkInterface.getByInetAddress(address);
      if (localHostNI != null) {
        byte[] rawMac = localHostNI.getHardwareAddress();
        String mac = convertMacBytesToString(rawMac);
        if (isMacValid(mac)) {
          return mac;
        } else {
          for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            mac = convertMacBytesToString(networkInterface.getHardwareAddress());
            if (isMacValid(mac)) {
              return mac;
            }
          }
        }
      }
    } catch (SocketException | UnknownHostException e) {
      ClientLogger.logError("Couldn't find a valid MAC Adress", e);
    }
    return null;
  }

  private static String convertMacBytesToString(final byte[] mac) {
    if (mac == null) {
      return null;
    }
    // Extract each array of mac address and convert it to the following format 00.1E.F3.C8.FC.E6
    return Joiner.on('.').join(
        FluentIterable.from(Bytes.asList(mac)).transform(macbyte -> String.format("%02X", macbyte)));
  }

  private static boolean isMacValid(final String mac) {
    if (mac == null || mac.length() != 17 || !mac.contains(".") || !mac.matches("[0-9A-Fa-f.]+")) {
      return false;
    }
    final char[] chars = mac.toCharArray();
    int periodCount = 0;
    int nonZeroNumberCount = 0;
    int i = 1;
    for (final char ch : chars) {
      if (ch == '.' && (i % 3 != 0)) {
        return false;
      }
      if (ch == '.') {
        periodCount++;
      }
      if (ch != '.' && ch != '0') {
        nonZeroNumberCount++;
      }
      i++;
    }
    if (periodCount != 5 || nonZeroNumberCount == 0 || mac.equals("00.00.00.00.00.E0")) {
      return false;
    }
    return true;
  }
}
