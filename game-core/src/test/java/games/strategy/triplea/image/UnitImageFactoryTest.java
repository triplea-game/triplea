package games.strategy.triplea.image;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitImageFactoryTest {
  @Mock GameData gameData;

  @Nested
  class GetBaseImageName {

    @BeforeEach
    void setupGameData() {
      final TechnologyFrontier technologyFrontier = new TechnologyFrontier("Tech", gameData);
      when(gameData.getTechnologyFrontier()).thenReturn(technologyFrontier);
      lenient().when(gameData.getDiceSides()).thenReturn(6);
      final GameProperties gameProperties = new GameProperties(gameData);
      lenient().when(gameData.getProperties()).thenReturn(gameProperties);
    }

    @Test
    void basicUnitType() throws MutableProperty.InvalidValueException {
      givenUnitTypeAndTechnologyAssertThatImageHasSuffix(List.of(), List.of(), "");
    }

    @Test
    void basicDamagedUnitType() throws MutableProperty.InvalidValueException {
      givenUnitTypeAndTechnologyAssertThatImageHasName(
          "infantry", List.of(), List.of(), true, false, "infantry_hit");
    }

    @Test
    void basicDisabledUnitType() throws MutableProperty.InvalidValueException {
      givenUnitTypeAndTechnologyAssertThatImageHasName(
          "infantry", List.of(), List.of(), false, true, "infantry_disabled");
    }

    private void givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
        final List<String> unitAttachmentProperties,
        final List<String> techAttachmentProperties,
        final String expectedImageNameSuffix)
        throws MutableProperty.InvalidValueException {
      final String unitTypeName = "unit";
      givenUnitTypeAndTechnologyAssertThatImageHasName(
          unitTypeName,
          unitAttachmentProperties,
          techAttachmentProperties,
          unitTypeName + expectedImageNameSuffix);
    }

    private void givenUnitTypeAndTechnologyAssertThatImageHasName(
        final String unitTypeName,
        final List<String> unitAttachmentProperties,
        final List<String> techAttachmentProperties,
        final String expectedImageName)
        throws MutableProperty.InvalidValueException {
      givenUnitTypeAndTechnologyAssertThatImageHasName(
          unitTypeName,
          unitAttachmentProperties,
          techAttachmentProperties,
          false,
          false,
          expectedImageName);
    }

    private void givenUnitTypeAndTechnologyAssertThatImageHasName(
        final String unitTypeName,
        final List<String> unitAttachmentProperties,
        final List<String> techAttachmentProperties,
        final boolean damaged,
        final boolean disabled,
        final String expectedImageName)
        throws MutableProperty.InvalidValueException {

      final UnitType unitType = givenUnitTypeWithProperties(unitTypeName, unitAttachmentProperties);
      final GamePlayer player = givenPlayerWithTech(techAttachmentProperties);

      final String imageName =
          UnitImageFactory.ImageKey.builder()
              .type(unitType)
              .player(player)
              .damaged(damaged)
              .disabled(disabled)
              .build()
              .getBaseImageName();
      assertThat(imageName, is(expectedImageName));
    }

    private UnitType givenUnitTypeWithProperties(
        final String unitTypeName, final List<String> unitAttachmentProperties)
        throws MutableProperty.InvalidValueException {
      final UnitType unitType = new UnitType(unitTypeName, gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("unitAttachment", unitType, gameData);
      unitType.addAttachment(Constants.UNIT_ATTACHMENT_NAME, unitAttachment);
      for (final String unitAttachmentProperty : unitAttachmentProperties) {
        if (unitAttachmentProperty.equals("attack")) {
          unitAttachment.getPropertyOrThrow(unitAttachmentProperty).setValue(1);
        } else {
          unitAttachment.getPropertyOrThrow(unitAttachmentProperty).setValue(true);
        }
      }
      return unitType;
    }

    private GamePlayer givenPlayerWithTech(final List<String> techAttachmentProperties)
        throws MutableProperty.InvalidValueException {
      final GamePlayer player = new GamePlayer("player", gameData);
      final TechAttachment techAttachment = new TechAttachment("techAttachment", player, gameData);
      player.addAttachment(Constants.TECH_ATTACHMENT_NAME, techAttachment);
      for (final String techAttachmentProperty : techAttachmentProperties) {
        techAttachment.getPropertyOrThrow(techAttachmentProperty).setValue(true);
      }
      return player;
    }

    @Nested
    class AaGun {

      @Test
      void basic() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasName(List.of(), "aaGun");
      }

      private void givenUnitTypeAndTechnologyAssertThatImageHasName(
          final List<String> techAttachmentProperties, final String expectedImageName)
          throws MutableProperty.InvalidValueException {
        GetBaseImageName.this.givenUnitTypeAndTechnologyAssertThatImageHasName(
            Constants.UNIT_TYPE_AAGUN,
            List.of("isAA"),
            techAttachmentProperties,
            expectedImageName);
      }

      @Test
      void withRadar() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasName(List.of("aARadar"), "aaGun_r");
      }

      @Test
      void withRockets() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasName(List.of("rocket"), "rockets");
      }

      @Test
      void withRocketsAndRadar() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasName(List.of("rocket", "aARadar"), "rockets_r");
      }
    }

    @Nested
    class IsAA {

      @Test
      void basic() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(List.of(), "");
      }

      private void givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
          final List<String> techAttachmentProperties, final String expectedImageNameSuffix)
          throws MutableProperty.InvalidValueException {
        GetBaseImageName.this.givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isAA"), techAttachmentProperties, expectedImageNameSuffix);
      }

      @Test
      void withRadar() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(List.of("aARadar"), "_r");
      }

      @Test
      void withRockets() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(List.of("rocket"), "_rockets");
      }

      @Test
      void withRocketsAndRadar() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("rocket", "aARadar"), "_rockets_r");
      }
    }

    @Nested
    class IsRocket {

      @Test
      void basic() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageSuffix(List.of(), "");
      }

      private void givenUnitTypeAndTechnologyAssertThatImageSuffix(
          final List<String> techAttachmentProperties, final String expectedImageNameSuffix)
          throws MutableProperty.InvalidValueException {
        GetBaseImageName.this.givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isRocket"), techAttachmentProperties, expectedImageNameSuffix);
      }

      @Test
      void withRockets() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageSuffix(List.of("rocket"), "_rockets");
      }
    }

    @Nested
    class IsAaForAnything {

      @Test
      void forBombing() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isAAforBombingThisUnitOnly"), List.of("aARadar"), "_r");
      }

      @Test
      void forCombat() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isAAforCombatOnly"), List.of("aARadar"), "_r");
      }

      @Test
      void forFlyover() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isAAforFlyOverOnly"), List.of("aARadar"), "_r");
      }
    }

    @Nested
    class IsAir {

      @Nested
      class IsNotStrategicBomber {
        @Test
        void longRangeAir() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir"), List.of("longRangeAir"), "_lr");
        }

        @Test
        void jetFighter() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir", "attack"), List.of("jetPower"), "_jp");
        }

        @Test
        void longRangeAirAndJetFighter() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir", "attack"), List.of("longRangeAir", "jetPower"), "_lr_jp");
        }

        @Test
        void jetFighterButNoAttackOrDefense() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir", "attack"), List.of("jetPower"), "_jp");
        }
      }

      @Nested
      class IsStrategicBomber {
        @Test
        void longRangeAir() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir", "isStrategicBomber"), List.of("longRangeAir"), "_lr");
        }

        @Test
        void heavyBomber() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir", "isStrategicBomber"), List.of("heavyBomber"), "_hb");
        }

        @Test
        void longRangeAirAndHeavyBomber() throws MutableProperty.InvalidValueException {
          givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
              List.of("isAir", "isStrategicBomber"),
              List.of("longRangeAir", "heavyBomber"),
              "_lr_hb");
        }
      }
    }

    @Nested
    class IsSub {

      @Test
      void superSub() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isSub", "attack"), List.of("superSub"), "_ss");
      }

      @Test
      void superSubButNoAttackOrDefense() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isSub"), List.of("superSub"), "");
      }
    }

    @Nested
    class IsFirstStrikeAndCanEvade {

      @Test
      void superSub() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isFirstStrike", "canEvade", "attack"), List.of("superSub"), "_ss");
      }

      @Test
      void superSubButOnlyFirstStrike() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isFirstStrike", "attack"), List.of("superSub"), "");
      }

      @Test
      void superSubButOnlyCanEvade() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("canEvade", "attack"), List.of("superSub"), "");
      }
    }

    @Nested
    class IsSuicide {

      @Test
      void shouldNotNeedImageForSuperSub() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isSuicide", "attack"), List.of("superSub"), "");
      }
    }

    @Nested
    class Factory {

      @Test
      void industrialTechnology() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasName(
            Constants.UNIT_TYPE_FACTORY, List.of(), List.of("industrialTechnology"), "factory_it");
      }

      @Test
      void increasedFactoryProduction() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasName(
            Constants.UNIT_TYPE_FACTORY,
            List.of(),
            List.of("increasedFactoryProduction"),
            "factory_it");
      }
    }

    @Nested
    class IsFactory {

      @Test
      void industrialTechnology() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isFactory"), List.of("industrialTechnology"), "_it");
      }

      @Test
      void increasedFactoryProduction() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("isFactory"), List.of("increasedFactoryProduction"), "_it");
      }
    }

    @Nested
    class CanProduceUnits {

      @Test
      void industrialTechnology() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("canProduceUnits"), List.of("industrialTechnology"), "_it");
      }

      @Test
      void increasedFactoryProduction() throws MutableProperty.InvalidValueException {
        givenUnitTypeAndTechnologyAssertThatImageHasSuffix(
            List.of("canProduceUnits"), List.of("increasedFactoryProduction"), "_it");
      }
    }
  }
}
