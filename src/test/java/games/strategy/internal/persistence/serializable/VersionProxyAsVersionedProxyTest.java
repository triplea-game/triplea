package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.List;

import games.strategy.persistence.serializable.AbstractVersionedProxyTestCase;
import games.strategy.util.Version;

public final class VersionProxyAsVersionedProxyTest extends AbstractVersionedProxyTestCase<Version> {
  public VersionProxyAsVersionedProxyTest() {
    super(Version.class);
  }

  @Override
  protected List<SupportedVersion<Version>> getSupportedVersions() {
    return Arrays.asList(v1());
  }

  private static SupportedVersion<Version> v1() {
    final String base16EncodedBytes = ""
        + "ACED00057372003D67616D65732E73747261746567792E696E7465726E616C2E" // ....sr.=games.strategy.internal.
        + "70657273697374656E63652E73657269616C697A61626C652E56657273696F6E" // persistence.serializable.Version
        + "50726F7879548CEF10A91330600C000078707718000000000000000100000001" // ProxyT.....0`...xpw.............
        + "00000002000000030000000478" //////////////////////////////////////// ............x
        + "";
    return new SupportedVersion<>(new Version(1, 2, 3, 4), base16EncodedBytes);
  }
}
