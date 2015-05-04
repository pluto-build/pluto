package build.pluto.test.build.latexlike;

import build.pluto.builder.BuildCycle;
import build.pluto.builder.CycleSupportFactory;
import build.pluto.builder.FixpointCycleSupport;

public class BibtexLatexCycleSupport extends FixpointCycleSupport {

  public static final CycleSupportFactory factory = BibtexLatexCycleSupport::new;

  private BibtexLatexCycleSupport(BuildCycle cycle) {
    super(cycle, LatexlikeBuilder.factory, BibtexlikeBuilder.factory);
  }

}
