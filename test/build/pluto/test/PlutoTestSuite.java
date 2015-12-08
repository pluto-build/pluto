package build.pluto.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sugarj.common.Log;

import build.pluto.test.build.BuildFailureTest;
import build.pluto.test.build.BuildInterruptTest;
import build.pluto.test.build.BuildManagerCycleDetectionTest;
import build.pluto.test.build.RebuildInconsistentTest;
import build.pluto.test.build.cycle.fixpoint.test.FixpointCycleTestSuite;
import build.pluto.test.build.cycle.once.test.CycleAtOnceBuilderTest;
import build.pluto.test.build.cycle.once.test.NestedCycleAtOnceTest;
import build.pluto.test.build.latexlike.LatexlikeTest;
import build.pluto.test.build.output.OutputPersistedTest;
import build.pluto.test.build.output.OutputTransientTest;

@RunWith(Suite.class)
@SuiteClasses({
  CompilationUnitVisitTest.class,
  BuildFailureTest.class,
  BuildInterruptTest.class,
  BuildManagerCycleDetectionTest.class, 
  CycleAtOnceBuilderTest.class, 
  RebuildInconsistentTest.class, 
  FixpointCycleTestSuite.class,
  NestedCycleAtOnceTest.class,
 LatexlikeTest.class,
  OutputPersistedTest.class,
  OutputTransientTest.class})
public class PlutoTestSuite {
  static {
    Log.log.setLoggingLevel(Log.ALWAYS);
  }
}
