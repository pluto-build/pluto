package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.sugarj.common.Log;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;

public class LatexlikeTest extends ScopedBuildTest {

  @ScopedPath("bib.biblike")
  private File bibFile;

  @ScopedPath("doc.texlike")
  private File texFile;

  @Test
  public void testCleanBuild() throws IOException {
    Log.log.setLoggingLevel(Log.CORE);
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.require(LatexlikeBuilder.factory, texFile);
  }

}
