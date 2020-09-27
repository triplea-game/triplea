package tools.map.making.ui.upload.map.validators;

import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.GameParsingValidation;
import games.strategy.engine.data.gameparser.XmlGameElementMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
class Validators {
  public List<MapValidator> getValidators() {
    return List.of(
        hasGameXml(), //
        gameXmlsCanBeParsed());
  }

  private MapValidator hasGameXml() {
    return new MapValidator(path -> findXmls(path).isEmpty() ? List.of("No Game XMLs") : List.of());
  }

  private static Collection<Path> findXmls(final Path mapPath) throws IOException {
    return Files.find(
            mapPath,
            6,
            (filePath, fileAttributes) ->
                filePath.toFile().getName().endsWith(".xml") && fileAttributes.isRegularFile())
        .collect(Collectors.toList());
  }

  private MapValidator gameXmlsCanBeParsed() {
    return new MapValidator(
        path ->
            findXmls(path).stream()
                .map(
                    xml ->
                        GameParser.parse(xml.toUri(), new XmlGameElementMapper())
                            .map(gameData -> new GameParsingValidation(gameData).validate())
                            .orElse(List.of()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
  }
}
