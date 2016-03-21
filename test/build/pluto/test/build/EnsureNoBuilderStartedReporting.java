package build.pluto.test.build;

import java.util.Set;

import junit.framework.AssertionFailedError;
import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.output.Output;
import build.pluto.util.NoReporting;

public class EnsureNoBuilderStartedReporting extends NoReporting {
  @Override
  public <O extends Output> void startedBuilder(BuildRequest<?, O, ?, ?> req, Builder<?, ?> b, BuildUnit<O> oldUnit, Set<BuildReason> reasons) {
    throw new AssertionFailedError("Builder was started " + req);
  }
}
