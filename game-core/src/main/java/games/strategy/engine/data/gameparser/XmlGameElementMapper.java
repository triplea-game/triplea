package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.delegate.IDelegate;
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
import games.strategy.triplea.delegate.UserActionDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * This class creates objects referred to by game XMLs via the 'javaClass' property. For example:
 *
 * <pre>
 * &lt;attachment name="territoryAttachment" attachTo="Reason"
 * javaClass="games.strategy.triplea.attachments.TerritoryAttachment" type="territory">
 * </pre>
 *
 * <p>In the above example, we are going to map the String value
 * "games.strategy.triplea.attachments.TerritoryAttachment" to a class constructor.
 *
 * <p>Note: attachments and delegates are initialized slightly differently, one is is no-arg the
 * other has initialization parameters.
 */
@Slf4j
public final class XmlGameElementMapper {
  private final ImmutableMap<String, Supplier<IDelegate>> delegateFactoriesByTypeName;
  private final ImmutableMap<String, AttachmentFactory> attachmentFactoriesByTypeName;

  public XmlGameElementMapper() {
    this(Map.of(), Map.of());
  }

  @VisibleForTesting
  public XmlGameElementMapper(
      final Map<String, Supplier<IDelegate>> auxiliaryDelegateFactoriesByTypeName,
      final Map<String, AttachmentFactory> auxiliaryAttachmentFactoriesByTypeName) {
    checkNotNull(auxiliaryDelegateFactoriesByTypeName);
    checkNotNull(auxiliaryAttachmentFactoriesByTypeName);

    delegateFactoriesByTypeName = newDelegateFactories(auxiliaryDelegateFactoriesByTypeName);
    attachmentFactoriesByTypeName = newAttachmentFactories(auxiliaryAttachmentFactoriesByTypeName);
  }

  private static ImmutableMap<String, Supplier<IDelegate>> newDelegateFactories(
      final Map<String, Supplier<IDelegate>> auxiliaryDelegateFactoriesByTypeName) {
    return ImmutableMap.<String, Supplier<IDelegate>>builder()
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
        .putAll(newTwoIfBySeaDelegateFactories())
        .putAll(auxiliaryDelegateFactoriesByTypeName)
        .build();
  }

  @SuppressWarnings(
      "deprecation") // required for map compatibility; remove upon next map-incompatible release
  private static ImmutableMap<String, Supplier<IDelegate>> newTwoIfBySeaDelegateFactories() {
    return ImmutableMap.<String, Supplier<IDelegate>>builder()
        .put(
            "games.strategy.twoIfBySea.delegate.EndTurnDelegate",
            games.strategy.triplea.delegate.TwoIfBySeaEndTurnDelegate::new)
        .put("games.strategy.twoIfBySea.delegate.InitDelegate", InitializationDelegate::new)
        .put("games.strategy.twoIfBySea.delegate.PlaceDelegate", PlaceDelegate::new)
        .build();
  }

  private static ImmutableMap<String, AttachmentFactory> newAttachmentFactories(
      final Map<String, AttachmentFactory> auxiliaryAttachmentFactoriesByTypeName) {
    return ImmutableMap.<String, AttachmentFactory>builder()
        .put("CanalAttachment", CanalAttachment::new)
        .put("PlayerAttachment", PlayerAttachment::new)
        .put("PoliticalActionAttachment", PoliticalActionAttachment::new)
        .put("RelationshipTypeAttachment", RelationshipTypeAttachment::new)
        .put("RulesAttachment", RulesAttachment::new)
        .put("TechAbilityAttachment", TechAbilityAttachment::new)
        .put("TechAttachment", TechAttachment::new)
        .put("TerritoryAttachment", TerritoryAttachment::new)
        .put("TerritoryEffectAttachment", TerritoryEffectAttachment::new)
        .put("TriggerAttachment", TriggerAttachment::new)
        .put("UnitAttachment", UnitAttachment::new)
        .put("UnitSupportAttachment", UnitSupportAttachment::new)
        .put("UserActionAttachment", UserActionAttachment::new)
        .putAll(auxiliaryAttachmentFactoriesByTypeName)
        .build();
  }

  /** Returns a new delegate of the type associated with the specified name. */
  public Optional<IDelegate> newDelegate(final String typeName) {
    checkNotNull(typeName);

    final String normalizedTypeName =
        typeName.replaceAll("^games\\.strategy\\.triplea\\.delegate\\.", "");
    final @Nullable Supplier<IDelegate> delegateFactory =
        delegateFactoriesByTypeName.get(normalizedTypeName);
    if (delegateFactory != null) {
      return Optional.of(delegateFactory.get());
    }

    handleMissingObject("delegate", typeName);
    return Optional.empty();
  }

  private static void handleMissingObject(final String objectTypeName, final String objectName) {
    log.error(
        "Could not find "
            + objectTypeName
            + " '"
            + objectName
            + "'. This can be a map configuration problem, and would need to be fixed in the "
            + "map XML. Or the map XML is using a feature from a newer game engine version, "
            + "and you will need to install the latest TripleA for it to be enabled. Meanwhile, "
            + "the functionality provided by this "
            + objectTypeName
            + " will not available.");
  }

  /** Returns a new attachment of the type associated with the specified name. */
  public Optional<IAttachment> newAttachment(
      final String typeName,
      final String name,
      final Attachable attachable,
      final GameData gameData) {
    checkNotNull(typeName);

    final String normalizedTypeName = typeName.replaceAll("^.*\\.", "");
    final @Nullable AttachmentFactory attachmentFactory =
        attachmentFactoriesByTypeName.get(normalizedTypeName);
    if (attachmentFactory != null) {
      return Optional.of(attachmentFactory.newAttachment(name, attachable, gameData));
    }

    handleMissingObject("attachment", typeName);
    return Optional.empty();
  }

  /** A factory for creating instances of {@link IAttachment}. */
  @FunctionalInterface
  @VisibleForTesting
  public interface AttachmentFactory {
    IAttachment newAttachment(String name, Attachable attachable, GameData gameData);
  }
}
