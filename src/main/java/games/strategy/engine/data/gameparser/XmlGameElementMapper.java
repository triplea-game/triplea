package games.strategy.engine.data.gameparser;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import games.strategy.debug.ClientLogger;
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
import games.strategy.triplea.delegate.NoPUEndTurnDelegate;
import games.strategy.triplea.delegate.NoPUPurchaseDelegate;
import games.strategy.triplea.delegate.PlaceDelegate;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.delegate.RandomStartDelegate;
import games.strategy.triplea.delegate.SpecialMoveDelegate;
import games.strategy.triplea.delegate.TechActivationDelegate;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.UserActionDelegate;
import games.strategy.twoIfBySea.delegate.InitDelegate;

/**
 * This class creates objects referred to by game XMLs via the 'javaClass' property, eg:
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
public class XmlGameElementMapper {

  // these keys are package protected to allow test to have access to known good keys
  @VisibleForTesting
  static final String BATTLE_DELEGATE_NAME = "games.strategy.triplea.delegate.BattleDelegate";
  @VisibleForTesting
  static final String CANAL_ATTACHMENT_NAME = "games.strategy.triplea.attachments.CanalAttachment";

  /* Maps a name (given as an XML attribute value) to a supplier function that creates the corresponding delegate */
  private final ImmutableMap<String, Supplier<IDelegate>> delegateMap =
      ImmutableMap.<String, Supplier<IDelegate>>builder()
          .put(BATTLE_DELEGATE_NAME, BattleDelegate::new)
          .put("games.strategy.triplea.delegate.BidPlaceDelegate", BidPlaceDelegate::new)
          .put("games.strategy.triplea.delegate.BidPurchaseDelegate", BidPurchaseDelegate::new)
          .put("games.strategy.triplea.delegate.EndRoundDelegate", EndRoundDelegate::new)
          .put("games.strategy.triplea.delegate.EndTurnDelegate", EndTurnDelegate::new)
          .put("games.strategy.triplea.delegate.InitializationDelegate", InitializationDelegate::new)
          .put("games.strategy.triplea.delegate.MoveDelegate", MoveDelegate::new)
          .put("games.strategy.triplea.delegate.NoAirCheckPlaceDelegate", NoAirCheckPlaceDelegate::new)
          .put("games.strategy.triplea.delegate.NoPUEndTurnDelegate", NoPUEndTurnDelegate::new)
          .put("games.strategy.triplea.delegate.NoPUPurchaseDelegate", NoPUPurchaseDelegate::new)
          .put("games.strategy.triplea.delegate.PlaceDelegate", PlaceDelegate::new)
          .put("games.strategy.triplea.delegate.PoliticsDelegate", PoliticsDelegate::new)
          .put("games.strategy.triplea.delegate.PurchaseDelegate", PurchaseDelegate::new)
          .put("games.strategy.triplea.delegate.RandomStartDelegate", RandomStartDelegate::new)
          .put("games.strategy.triplea.delegate.SpecialMoveDelegate", SpecialMoveDelegate::new)
          .put("games.strategy.triplea.delegate.TechActivationDelegate", TechActivationDelegate::new)
          .put("games.strategy.triplea.delegate.TechnologyDelegate", TechnologyDelegate::new)
          .put("games.strategy.triplea.delegate.UserActionDelegate", UserActionDelegate::new)
          .put("games.strategy.twoIfBySea.delegate.EndTurnDelegate",
              games.strategy.twoIfBySea.delegate.EndTurnDelegate::new)
          .put("games.strategy.twoIfBySea.delegate.InitDelegate", InitDelegate::new)
          .put("games.strategy.twoIfBySea.delegate.PlaceDelegate",
              games.strategy.twoIfBySea.delegate.PlaceDelegate::new)
          .put("games.strategy.engine.xml.TestDelegate", TestDelegate::new)
          .build();

  /*
   * Maps a name (given as an XML attribute value) to a function that can create attachment objects.
   */
  private final ImmutableMap<String, Function<AttachmentData, IAttachment>> attachmentMap =
      ImmutableMap.<String, Function<AttachmentData, IAttachment>>builder()
          .put(CANAL_ATTACHMENT_NAME, attachmentData -> new CanalAttachment(attachmentData.name,
              attachmentData.attachable, attachmentData.gameData))
          .put("games.strategy.triplea.attachments.PlayerAttachment",
              attachmentData -> new PlayerAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.PoliticalActionAttachment",
              attachmentData -> new PoliticalActionAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.RelationshipTypeAttachment",
              attachmentData -> new RelationshipTypeAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.RulesAttachment",
              attachmentData -> new RulesAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.TechAbilityAttachment",
              attachmentData -> new TechAbilityAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.TechAttachment", attachmentData ->
              new TechAttachment(attachmentData.name, attachmentData.attachable, attachmentData.gameData))
          .put("games.strategy.triplea.attachments.TerritoryAttachment",
              attachmentData -> new TerritoryAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.TerritoryEffectAttachment",
              attachmentData -> new TerritoryEffectAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.TriggerAttachment",
              attachmentData -> new TriggerAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.UnitAttachment",
              attachmentData -> new UnitAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.UnitSupportAttachment",
              attachmentData -> new UnitSupportAttachment(attachmentData.name, attachmentData.attachable,
                  attachmentData.gameData))
          .put("games.strategy.triplea.attachments.UserActionAttachment",
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

    AttachmentData(String name, Attachable attachable, GameData gameData) {
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
    if (!delegateMap.containsKey(className)) {
      handleMissingObjectError("delegate", className);
      return Optional.empty();
    }

    Supplier<IDelegate> delegateFactory = delegateMap.get(className);
    return Optional.of(delegateFactory.get());
  }

  private static void handleMissingObjectError(String typeLabel, String value) {
    ClientLogger.logError("Could not find " + typeLabel + " '" + value + "'. This is can be a map configuration"
        + " problem, and would need to be fixed in the map XML. Or, the map XML is using a feature from a newer game"
        + " engine version, and you will need to install the latest TripleA for it to be enabled. Meanwhile, the"
        + " functionality provided by this " + typeLabel + " will not available.");
  }

  public Optional<IAttachment> getAttachment(String javaClass, String name, Attachable attachable, GameData data) {
    if (!attachmentMap.containsKey(javaClass)) {
      handleMissingObjectError("attachment", javaClass);
      return Optional.empty();
    }
    final Function<AttachmentData, IAttachment> attachmentFactoryFunction = attachmentMap.get(javaClass);
    return Optional.of(attachmentFactoryFunction.apply(new AttachmentData(name, attachable, data)));
  }
}
