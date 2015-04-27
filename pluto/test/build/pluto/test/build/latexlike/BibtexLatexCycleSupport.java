package build.pluto.test.build.latexlike;

import build.pluto.builder.FixpointCycleSupport;

public class BibtexLatexCycleSupport extends FixpointCycleSupport {

  public static final BibtexLatexCycleSupport instance = new BibtexLatexCycleSupport();

  private BibtexLatexCycleSupport() {
    super(LatexlikeBuilder.factory, BibtexlikeBuilder.factory);
  }

}
