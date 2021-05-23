package org.triplea.spitfire.server;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Base class for http server controllers. This class is mainly to share the annotations needed for
 * enabling an http controller. All http controller classes should be 'registered' in the
 * application configuration, {@see ServerApplication}
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@SuppressWarnings("RestResourceMethodInspection")
public class HttpController {}
