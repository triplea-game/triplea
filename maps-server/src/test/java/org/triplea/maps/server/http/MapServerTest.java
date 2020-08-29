package org.triplea.maps.server.http;

import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

@Integration
@ExtendWith(value = {MapServerExtension.class, DBUnitExtension.class})
@SuppressWarnings("PrivateConstructorForUtilityClass")
public class MapServerTest {}
