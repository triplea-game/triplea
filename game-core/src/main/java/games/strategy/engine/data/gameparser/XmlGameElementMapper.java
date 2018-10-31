package games.strategy.engine.data.gameparser;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.xml.TestAttachment;
import games.strategy.engine.xml.TestDelegate;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.BidPlaceDelegate;
import games.strategy.triplea.delegate.BidPurchaseDelegate;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.EndTurnDelegate;
import games.strategy.triplea.delegate.InitializationDelegate;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.NoAirCheckPlaceDelegate;
import games.strategy.triplea.delegate.NoPuEndTurnDelegate;
import games.strategy.triplea.delegate.NoPuPurchaseDelegate;
import games.strategy.triplea.delegate.PlaceDelegate;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.delegate.RandomStartDelegate;
import games.strategy.triplea.delegate.SpecialMoveDelegate;
import games.strategy.triplea.delegate.TechActivationDelegate;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.TwoIfBySeaEndTurnDelegate;
import games.strategy.triplea.delegate.TwoIfBySeaInitDelegate;
import games.strategy.triplea.delegate.TwoIfBySeaPlaceDelegate;
import games.strategy.triplea.delegate.UserActionDelegate;
import lombok.extern.java.Log;

/**
 * This class creates objects referred to by game XMLs via the 'javaClass' property. For example:
 *
 * <pre>
 * &lt;attachment name="territoryAttachment" attachTo="Rason"
 * javaClass="games.strategy.triplea.attachments.TerritoryAttachment" type="territory">
 * </pre>
 *
 * <p>
 * In the above example, we are going to map the String value "games.strategy.triplea.attachments.TerritoryAttachment"
 * to a class constructor.
 * </p>
 *
 * <p>
 * Note: attachments and delegates are initialized slightly differently, one is is no-arg the other has initialization
 * parameters.
 * </p>
 */
@Log
public class XmlGameElementMapper {
  /* Maps a name (given as an XML attribute value) to a supplier function that creates the corresponding delegate */
  private final ImmutableMap<String, Supplier<IDelegate>> delegateMap =
      ImmutableMap.<String, Supplier<IDelegate>>builder()
          .put("BattleDelegate", BattleDelegate::new)
          .put("BidPlaceDelegate", BidPlaceDelegate::new)
          .put("BidPurchaseDelegate", BidPurchaseDelegate::new)
          .put("EndRoundDelegate", EndRoundDelegate::new)
          .put("EndTurnDelegate", EndTurnDelegate::new)
          .put("InitializationDelegate", InitializationDelegate::new)
          .put("MoveDelegate", MoveDelegate::new)
          .put("NoAirCheckPlaceDelegate", NoAirCheckPlaceDelegate::new)
          .put("NoPUEndTurnDelegate", NoPuEndTurnDelegate::new)
          .put("NoPUPurchaseDelegate", NoPuPurchaseDelegate::new)
          .put("PlaceDelegate", PlaceDelegate::new)
          .put("PoliticsDelegate", PoliticsDelegate::new)
          .put("PurchaseDelegate", PurchaseDelegate::new)
          .put("RandomStartDelegate", RandomStartDelegate::new)
          .put("SpecialMoveDelegate", SpecialMoveDelegate::new)
          .put("TechActivationDelegate", TechActivationDelegate::new)
          .put("TechnologyDelegate", TechnologyDelegate::new)
          .put("UserActionDelegate", UserActionDelegate::new)
          .put("games.strategy.twoIfBySea.delegate.EndTurnDelegate", TwoIfBySeaEndTurnDelegate::new)
          .put("games.strategy.twoIfBySea.delegate.InitDelegate", TwoIfBySeaInitDelegate::new)
          .put("games.strategy.twoIfBySea.delegate.PlaceDelegate", TwoIfBySeaPlaceDelegate::new)
          .put("games.strategy.engine.xml.TestDelegate", TestDelegate::new)
          .build();

  /*
   * Maps a name (given as an XML attribute value) to a function that can create attachment objects.
   */
  private final ImmutableMap<String, Function<AttachmentData, IAttachment>> attachmentMap =
      ImmutableMap.<String, Function<AttachmentData, IAttachment>>builder()
          .put("CanalAttachment", attachmentData -> new CanalAttachment(attachmentData.name,
              attachmentData.attachable, attachmentData.gameData))
          .put("PlayerAttachment",
              attachmentData -> new PlayerAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("PoliticalActionAttachment",
              attachmentData -> new PoliticalActionAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("RelationshipTypeAttachment",
              attachmentData -> new RelationshipTypeAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("RulesAttachment",
              attachmentData -> new RulesAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("TechAbilityAttachment",
              attachmentData -> new TechAbilityAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("TechAttachment",
              attachmentData -> new TechAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("TerritoryAttachment",
              attachmentData -> new TerritoryAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("TerritoryEffectAttachment",
              attachmentData -> new TerritoryEffectAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("TriggerAttachment",
              attachmentData -> new TriggerAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("UnitAttachment",
              attachmentData -> new UnitAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("UnitSupportAttachment",
              attachmentData -> new UnitSupportAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("UserActionAttachment",
              attachmentData -> new UserActionAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.engine.xml.TestAttachment", attachmentData -> new TestAttachment(attachmentData.name,
              attachmentData.attachable, attachmentData.gameData))
          .build();

  /** Small data holder class. */
  private static class AttachmentData {
    private final String name;
    private final Attachable attachable;
    private final GameData gameData;

    AttachmentData(final String name, final Attachable attachable, final GameData gameData) {
      this.name = name;
      this.attachable = attachable;
      this.gameData = gameData;
    }
  }

  /**
   * Loads a new instance of the given class.
   * Assumes a zero argument constructor.
   */
  public Optional<IDelegate> getDelegate(final String className) {
    final String bareName = className.replaceAll("^games\\.strategy\\.triplea\\.delegate\\.", "");
    if (!delegateMap.containsKey(bareName)) {
      handleMissingObjectError("delegate", className);
      return Optional.empty();
    }

    return Optional.of(delegateMap.get(bareName).get());
  }

  private static void handleMissingObjectError(final String typeLabel, final String value) {
    log.log(Level.SEVERE, "Could not find " + typeLabel + " '" + value + "'. This is can be a map configuration"
        + " problem, and would need to be fixed in the map XML. Or, the map XML is using a feature from a newer game"
        + " engine version, and you will need to install the latest TripleA for it to be enabled. Meanwhile, the"
        + " functionality provided by this " + typeLabel + " will not available.");
  }

  public Optional<IAttachment> getAttachment(final String javaClass, final String name, final Attachable attachable,
      final GameData data) {
    final String bareName = javaClass.replaceAll("^games\\.strategy\\.triplea\\.attachments\\.", "");
    if (!attachmentMap.containsKey(bareName)) {
      handleMissingObjectError("attachment", javaClass);
      return Optional.empty();
    }
    final Function<AttachmentData, IAttachment> attachmentFactoryFunction = attachmentMap.get(bareName);
    return Optional.of(attachmentFactoryFunction.apply(new AttachmentData(name, attachable, data)));
  }
}
