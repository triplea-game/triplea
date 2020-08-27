package org.triplea.maps.server.http;

import com.github.database.rider.junit5.DBUnitExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

@Integration
@Slf4j
@ExtendWith(value = {MapServerExtension.class, DBUnitExtension.class})
@SuppressWarnings("PrivateConstructorForUtilityClass")
public class MapServerTest {}
