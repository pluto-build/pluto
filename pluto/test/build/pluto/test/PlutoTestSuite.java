package build.pluto.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
import build.pluto.test.cli.InputParserCollectionTest;
import build.pluto.test.cli.InputParserPrimitiveTest;

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
  InputParserPrimitiveTest.class, 
  InputParserCollectionTest.class,
  OutputPersistedTest.class,
  OutputTransientTest.class})
public class PlutoTestSuite {
}
