package org.triplea.live.servers;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.triplea.util.Version;

public class ServerYamlParser implements Function<InputStream, LiveServers> {

  @SuppressWarnings("unchecked")
  @Override
  public LiveServers apply(final InputStream inputStream) {
    final Load load = new Load(LoadSettings.builder().build());
    final Map<?, ?> yamlProps;
    try {
      yamlProps = (Map<?, ?>) load.loadFromInputStream(inputStream);
    } catch (final ClassCastException e) {
      throw new IllegalArgumentException("Invalid yaml format");
    }

    if (yamlProps == null) {
      throw new IllegalArgumentException("Invalid yaml file");
    }

    final String latest =
        Optional.ofNullable((String) yamlProps.get("latest"))
            .orElseThrow(
                () -> new IllegalArgumentException("No 'latest' property found: " + yamlProps));

    final List<ServerProperties> serverProperties =
        ((List<Map<String, String>>) yamlProps.get("servers"))
            .stream().map(this::mapToServerProperties).collect(Collectors.toList());

    return LiveServers.builder()
        .latestEngineVersion(new Version(latest))
        .servers(serverProperties)
        .build();
  }

  private ServerProperties mapToServerProperties(final Map<String, ?> props) {
    return ServerProperties.builder()
        .minEngineVersion(
            new Version(Preconditions.checkNotNull(String.valueOf(props.get("version")))))
        .message(Strings.nullToEmpty((String) props.get("message")))
        .uri(Optional.ofNullable((String) props.get("lobby_uri")).map(URI::create).orElse(null))
        .mapsServerUri(Optional.ofNullable((String) props.get("maps_uri")).map(URI::create).orElse(null))
        .inactive(Optional.ofNullable((Boolean) props.get("inactive")).orElse(false))
        .build();
  }
}
