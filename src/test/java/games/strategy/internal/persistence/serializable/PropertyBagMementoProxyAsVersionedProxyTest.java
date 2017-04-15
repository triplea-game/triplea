package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import games.strategy.persistence.serializable.AbstractVersionedProxyTestCase;
import games.strategy.util.memento.PropertyBagMemento;

public final class PropertyBagMementoProxyAsVersionedProxyTest
    extends AbstractVersionedProxyTestCase<PropertyBagMemento> {
  public PropertyBagMementoProxyAsVersionedProxyTest() {
    super(PropertyBagMemento.class);
  }

  @Override
  protected List<SupportedVersion<PropertyBagMemento>> getSupportedVersions() {
    return Arrays.asList(v1());
  }

  private static SupportedVersion<PropertyBagMemento> v1() {
    final String base16EncodedBytes = ""
        + "ACED00057372004867616D65732E73747261746567792E696E7465726E616C2E" // ....sr.Hgames.strategy.internal.
        + "70657273697374656E63652E73657269616C697A61626C652E50726F70657274" // persistence.serializable.Propert
        + "794261674D656D656E746F50726F78796C6EA60E0C4A88670C00007870771B00" // yBagMementoProxyln...J.g...xpw..
        + "000000000000010009736368656D612D69640000000000000008737200116A61" // .........schema-id........sr..ja
        + "76612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F61" // va.util.HashMap......`....F..loa
        + "64466163746F724900097468726573686F6C6478703F40000000000003770800" // dFactorI..thresholdxp?@......w..
        + "0000040000000274000970726F7065727479327400043231313274000970726F" // .......t..property2t..2112t..pro
        + "7065727479317372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF" // perty1sr..java.lang.Long;.....#.
        + "0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286" // ...J..valuexr..java.lang.Number.
        + "AC951D0B94E08B0200007870000000000000002A7878" ////////////////////// ..........xp.......*xx
        + "";
    final PropertyBagMemento expected = new PropertyBagMemento("schema-id", 8L, ImmutableMap.<String, Object>of(
        "property1", 42L,
        "property2", "2112"));
    return new SupportedVersion<>(expected, base16EncodedBytes);
  }
}
