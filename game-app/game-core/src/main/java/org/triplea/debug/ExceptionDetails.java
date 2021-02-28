package org.triplea.debug;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ExceptionDetails {
  @Nonnull private final String exceptionClassName;
  @Nullable private final String exceptionMessage;
  @Nullable private final StackTraceElement[] stackTraceElements;
}
