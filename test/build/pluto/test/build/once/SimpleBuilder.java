package build.pluto.test.build.once;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.output.None;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class SimpleBuilder extends Builder<TestBuilderInput, None> {

  public static BuilderFactory<TestBuilderInput, None, SimpleBuilder> factory = BuilderFactoryFactory.of(SimpleBuilder.class, TestBuilderInput.class);
	

	public static class TestBuilderInput implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6657909750424698658L;
		private File inputPath;
		private File basePath;

		public TestBuilderInput(File basePath, File inputPath) {
			super();
			Objects.requireNonNull(basePath);
			Objects.requireNonNull(inputPath);
			this.inputPath = inputPath;
			this.basePath = basePath;
		}

		public File getInputPath() {
			return inputPath;
		}

		public File getBasePath() {
			return basePath;
		}
		
		@Override
		public String toString() {
			return this.getInputPath().toString();
		}
	}

	
  public SimpleBuilder(TestBuilderInput input) {
		super(input);
	}

	@Override
  protected String description(TestBuilderInput input) {
		return "Test Builder for " + input.getInputPath();
	}

	@Override
  public File persistentPath(TestBuilderInput input) {
		return FileCommands.addExtension(input.inputPath.toPath(), "dep").toFile();
	}

	@Override
	protected Stamper defaultStamper() {
		return FileHashStamper.instance;
	}

	@Override
  protected None build(TestBuilderInput input) throws IOException {
		require(input.inputPath);
		List<String> allLines = FileCommands.readFileLines(input.inputPath);

		if (!allLines.isEmpty() && allLines.get(0).equals("#fail"))
		  throw new RuntimeException("#fail detected in source file");
		
		if (!allLines.isEmpty() && allLines.get(0).equals("#interrupt"))
      Thread.currentThread().interrupt();
		
		List<String> contentLines = new ArrayList<String>();

		for (String line : allLines) {
			if (line.startsWith("Dep:")) {
				String depFile = line.substring(4);
        report("Found dependency to " + depFile);
				TestBuilderInput depInput = new TestBuilderInput(
						input.basePath, new File(input.getBasePath(),
								depFile));
				SimpleRequirement req = new SimpleRequirement(factory, depInput);
				requireBuild(req);
			} else {
				contentLines.add(line);
			}
		}

		// Write the content to a generated file
		File generatedFile = FileCommands.addExtension(input.inputPath.toPath(), "gen").toFile();
		FileCommands.writeLinesFile(generatedFile, contentLines);
		provide(generatedFile);
		setState(BuildUnit.State.finished(true));
		return None.val;
	}

}
