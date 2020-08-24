package org.triplea.maps.server.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;

class MapsConfig extends Configuration {

  @Valid @NotNull @JsonProperty @Getter
  private final DataSourceFactory database = new DataSourceFactory();
}
