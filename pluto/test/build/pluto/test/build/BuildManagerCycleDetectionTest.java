package build.pluto.test.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import build.pluto.builder.BuildCycleException;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.EmptyBuildOutput;
import build.pluto.util.AbsoluteComparedFile;

public class BuildManagerCycleDetectionTest {
	
	private static File baseDir = new File("testdata/CycleDetectionTest/");
	
	@Before
	public void emptyDir() throws IOException{
		FileCommands.delete(baseDir.toPath());
		FileCommands.createDir(baseDir.toPath());
	}

  public static final BuilderFactory<AbsoluteComparedFile, EmptyBuildOutput, TestBuilder> testFactory = BuilderFactoryFactory.of(TestBuilder.class, AbsoluteComparedFile.class);

  public static class TestBuilder extends Builder<AbsoluteComparedFile, EmptyBuildOutput> {

    public TestBuilder(AbsoluteComparedFile input) {
			super(input);
		}

		@Override
    protected String description(AbsoluteComparedFile input) {
      return "Test Builder " + input.getFile();
		}

		@Override
    protected File persistentPath(AbsoluteComparedFile input) {
      return FileCommands.replaceExtension(input.getFile().toPath(), "dep").toFile();
		}

		@Override
		protected Stamper defaultStamper() {
			return FileHashStamper.instance;
		}

		@Override
    protected EmptyBuildOutput build(AbsoluteComparedFile input) throws IOException {
			File req;
			int number = 0;
      String inputWithoutExt = FileCommands.dropExtension(input.getFile().getPath());
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
			req = new File(inputWithoutExt.substring(0,
					inputWithoutExt.length() - 1)
					+ number + ".txt");

      requireBuild(testFactory, AbsoluteComparedFile.absolute(req));
			return EmptyBuildOutput.instance;
		}

	}

  private AbsoluteComparedFile getPathWithNumber(int num) {
    return AbsoluteComparedFile.absolute(new File(baseDir, "Test" + num + ".txt"));
	}

	@Test
	public void testCyclesDetected() throws IOException {

		try {
      BuildManagers.build(new BuildRequest<>(testFactory, getPathWithNumber(0)));
		} catch (RequiredBuilderFailed e) {
			assertTrue("Cause is not a cycle",
					e.getCause() instanceof BuildCycleException);
			BuildCycleException cycle = (BuildCycleException) e.getCause();

      assertEquals("Wrong cause path", getPathWithNumber(0), cycle.getCycleCause().input);

      List<BuildRequest<?, ?, ?, ?>> cyclicUnits = cycle.getCycle().getCycleComponents();
			assertEquals("Wrong number of units in cycle", 10,
					cyclicUnits.size());

			for (int i = 0; i < 10; i++) {
        BuildRequest<?, ?, ?, ?> req = cyclicUnits.get(i);
        assertTrue("Wrong request at " + i, req.input.equals(getPathWithNumber(i)));
        assertTrue("No requirement for " + i, req != null);
        assertEquals("Wrong factory for unit", testFactory, req.factory);
			}
		}

	}

}
