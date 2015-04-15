package build.pluto.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import build.pluto.test.build.BuildManagerCycleDetectionTest;
import build.pluto.test.build.CycleAtOnceBuilderTest;
import build.pluto.test.build.RebuildInconsistentTest;
import build.pluto.test.build.cycle.fixpoint.test.FixpointCycleTestSuite;
import build.pluto.test.cli.InputParserTest;

@RunWith(Suite.class)
@SuiteClasses({CompilationUnitVisitTest.class, BuildManagerCycleDetectionTest.class, CycleAtOnceBuilderTest.class, RebuildInconsistentTest.class, FixpointCycleTestSuite.class, InputParserTest.class})
public class PlutoTestSuite {

}
