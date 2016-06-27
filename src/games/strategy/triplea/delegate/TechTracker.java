package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * Tracks which players have which technology advances.
 */
public class TechTracker implements java.io.Serializable {
  private static final long serialVersionUID = 4705039229340373735L;

  /** Creates new TechTracker */
  public TechTracker() {}

  /**
   * Returns what tech advances this player already has successfully researched (including ones that may not be in their
   * tech frontier).
   *
   * @param id
   * @param data
   */
  public static Collection<TechAdvance> getCurrentTechAdvances(final PlayerID id, final GameData data) {
    final Collection<TechAdvance> rVal = new ArrayList<>();
    final TechAttachment attachment = TechAttachment.get(id);
    // search all techs
    for (final TechAdvance ta : TechAdvance.getTechAdvances(data)) {
      if (ta.hasTech(attachment)) {
        rVal.add(ta);
      }
    }
    return rVal;
  }

  /**
   * Returns what tech categories are no longer available for this player, because all techs in them have been
   * successfully researched
   * already.
   *
   * @param data
   * @param id
   */
  public static Collection<TechnologyFrontier> getFullyResearchedPlayerTechCategories(final GameData data,
      final PlayerID id) {
    final Collection<TechnologyFrontier> rVal = new ArrayList<>();
    final TechAttachment attachment = TechAttachment.get(id);
    for (final TechnologyFrontier tf : TechAdvance.getPlayerTechCategories(data, id)) {
      boolean has = true;
      for (final TechAdvance t : tf.getTechs()) {
        has = t.hasTech(attachment);
        if (!has) {
          break;
        }
      }
      if (has) {
        rVal.add(tf);
      }
    }
    return rVal;
  }

  public static synchronized void addAdvance(final PlayerID player, final IDelegateBridge bridge,
      final TechAdvance advance) {
    Change attachmentChange;
    if (advance instanceof GenericTechAdvance && ((GenericTechAdvance) advance).getAdvance() == null) {
        attachmentChange = ChangeFactory.genericTechChange(TechAttachment.get(player), true, advance.getProperty());
    } else {
      attachmentChange =
          ChangeFactory.attachmentPropertyChange(TechAttachment.get(player), "true", advance.getProperty());
    }
    bridge.addChange(attachmentChange);
    advance.perform(player, bridge);
  }

  public static synchronized void removeAdvance(final PlayerID player, final IDelegateBridge bridge,
      final TechAdvance advance) {
    Change attachmentChange;
    if (advance instanceof GenericTechAdvance) {
      if (((GenericTechAdvance) advance).getAdvance() == null) {
        attachmentChange = ChangeFactory.genericTechChange(TechAttachment.get(player), false, advance.getProperty());
      } else {
        attachmentChange =
            ChangeFactory.attachmentPropertyChange(TechAttachment.get(player), "false", advance.getProperty());
      }
    } else {
      attachmentChange =
          ChangeFactory.attachmentPropertyChange(TechAttachment.get(player), "false", advance.getProperty());
    }
    bridge.addChange(attachmentChange);
    // advance.perform(player, bridge);
  }

  public static int getTechCost(final PlayerID id) {
    final TechAttachment ta = TechAttachment.get(id);
    return ta.getTechCost();
  }

  public static boolean hasLongRangeAir(final PlayerID player) {
    return TechAttachment.get(player).getLongRangeAir();
  }

  public static boolean hasHeavyBomber(final PlayerID player) {
    return TechAttachment.get(player).getHeavyBomber();
  }

  public static boolean hasSuperSubs(final PlayerID player) {
    return TechAttachment.get(player).getSuperSub();
  }

  public static boolean hasJetFighter(final PlayerID player) {
    return TechAttachment.get(player).getJetPower();
  }

  public static boolean hasRocket(final PlayerID player) {
    return TechAttachment.get(player).getRocket();
  }

  public static boolean hasIndustrialTechnology(final PlayerID player) {
    return TechAttachment.get(player).getIndustrialTechnology();
  }

  public static boolean hasImprovedArtillerySupport(final PlayerID player) {
    return TechAttachment.get(player).getImprovedArtillerySupport();
  }

  public static boolean hasParatroopers(final PlayerID player) {
    return TechAttachment.get(player).getParatroopers();
  }

  public static boolean hasIncreasedFactoryProduction(final PlayerID player) {
    return TechAttachment.get(player).getIncreasedFactoryProduction();
  }

  public static boolean hasAARadar(final PlayerID player) {
    return TechAttachment.get(player).getAARadar();
  }
}
