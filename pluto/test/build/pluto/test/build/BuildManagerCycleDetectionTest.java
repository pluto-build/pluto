package build.pluto.test.build;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.RelativePath;

import build.pluto.builder.BuildCycleException;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.BuildRequirement;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.EmptyBuildOutput;

public class BuildManagerCycleDetectionTest {
	
	private static AbsolutePath baseDir = new AbsolutePath(new File("testdata/CycleDetectionTest/").getAbsolutePath());
	
	@Before
	public void emptyDir() throws IOException{
		FileCommands.delete(baseDir);
		FileCommands.createDir(baseDir);
	}

	public static final BuilderFactory<Path, EmptyBuildOutput, TestBuilder> testFactory = new BuilderFactory<Path, EmptyBuildOutput, TestBuilder>() {

		private static final long serialVersionUID = 3231801709410953205L;

		@Override
		public TestBuilder makeBuilder(Path input) {
			return new TestBuilder(input);
		}

	};

	private static class TestBuilder extends Builder<Path, EmptyBuildOutput> {

		private TestBuilder(Path input) {
			super(input);
		}

		@Override
		protected String description() {
			return "Test Builder " + input.getAbsolutePath();
		}

		@Override
		protected Path persistentPath() {
			return input.replaceExtension("dep");
		}

		@Override
		protected Stamper defaultStamper() {
			return FileHashStamper.instance;
		}

		@Override
		protected EmptyBuildOutput build() throws IOException {
			AbsolutePath req;
			int number = 0;
			String inputWithoutExt = FileCommands.dropExtension(input
					.getAbsolutePath());
			char lastInputChar = inputWithoutExt.charAt(inputWithoutExt
					.length() - 1);
			if (Character.isDigit(lastInputChar)) {
				number = Integer.parseInt(new String(
						new char[] { lastInputChar })) + 1;
			} else {
				fail("Invalid file");
			}
			if (number == 10) {
				number = 0;
			}
			req = new AbsolutePath(inputWithoutExt.substring(0,
					inputWithoutExt.length() - 1)
					+ number + ".txt");

			requireBuild(testFactory, req);
			return EmptyBuildOutput.instance;
		}

	}

	private RelativePath getDepPathWithNumber(int num) {
		return new RelativePath(baseDir, "Test" + num + ".dep");
	}

	private RelativePath getPathWithNumber(int num) {
		return new RelativePath(baseDir, "Test" + num + ".txt");
	}

	@Test
	public void testCyclesDetected() throws IOException {

		try {
			BuildManager
					.build(new BuildRequest<Path, EmptyBuildOutput, TestBuilder, BuilderFactory<Path, EmptyBuildOutput, TestBuilder>>(
							testFactory, getPathWithNumber(0)));
		} catch (RequiredBuilderFailed e) {
			assertTrue("Cause is not a cycle",
					e.getCause() instanceof BuildCycleException);
			BuildCycleException cycle = (BuildCycleException) e.getCause();

			assertEquals("Wrong cause path", getDepPathWithNumber(0), cycle
					.getCycleCause().getPersistentPath());

			List<BuildRequirement<?>> cyclicUnits = cycle.getCycleComponents();
			assertEquals("Wrong number of units in cycle", 10,
					cyclicUnits.size());

			for (int i = 0; i < 10; i++) {
				BuildRequirement<?> requirement = null;
				for (BuildRequirement<?> req : cyclicUnits) {
					if (req.unit.getPersistentPath().equals(
							getDepPathWithNumber(i))) {
						requirement = req;
					}
				}
				assertTrue(requirement.unit != null);
				assertEquals("Wrong persistence path for unit",
						getDepPathWithNumber(i),
						requirement.unit.getPersistentPath());
				assertEquals("Wrong factory for unit", testFactory,
						requirement.req.factory);
				assertEquals("Wrong input for unit", getPathWithNumber(i),
						requirement.req.input);
			}
		}

	}

}
