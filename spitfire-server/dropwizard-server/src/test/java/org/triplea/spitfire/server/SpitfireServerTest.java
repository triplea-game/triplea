package org.triplea.spitfire.server;

import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({
  DBUnitExtension.class,
  SpitfireDatabaseTestSupport.class,
  SpitfireServerTestExtension.class,
})
public abstract class SpitfireServerTest {}
