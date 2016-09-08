package build.pluto.test.build.once;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import build.pluto.executor.InputParser;
import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.None;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class SimpleBuilder extends Builder<TestBuilderInput, None> {

  public static BuilderFactory<TestBuilderInput, None, SimpleBuilder> factory = BuilderFactoryFactory.of(SimpleBuilder.class, TestBuilderInput.class);
	public static BuilderFactory<TestBuilderInput, None, SimpleBuilder> factoryFileDepDiscovery = new BuilderFactory<TestBuilderInput, None, SimpleBuilder>() {
		@Override
		public SimpleBuilder makeBuilder(TestBuilderInput input) {
			SimpleBuilder builder = new SimpleBuilder(input);
			builder.setFileDiscovery(true);
			return builder;
		}

		@Override
		public boolean isOverlappingGeneratedFileCompatible(File overlap, Serializable input, BuilderFactory<?, ?, ?> otherFactory, Serializable otherInput) {
			return false;
		}

		@Override
		public InputParser<TestBuilderInput> inputParser() {
			return null;
		}
	};


	private boolean fileDiscovery = false;

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
		return "Test Builder for " + input.getInputPath() + "(File Discovery: " + fileDiscovery + ")";
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
  		if (!this.useFileDependencyDiscovery())
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
				if (useFileDependencyDiscovery())
					requireBuild(factoryFileDepDiscovery, depInput);
				else
					requireBuild(factory, depInput);
			} else {
				contentLines.add(line);
			}
		}

		// Write the content to a generated file
		File generatedFile = FileCommands.addExtension(input.inputPath.toPath(), "gen").toFile();
		FileCommands.writeLinesFile(generatedFile, contentLines);
		if (!this.useFileDependencyDiscovery())
			provide(generatedFile);
		setState(BuildUnit.State.finished(true));
		return None.val;
	}

	@Override
	protected boolean useFileDependencyDiscovery() {
		return fileDiscovery;
	}

	public void setFileDiscovery(boolean fileDiscovery) {
		this.fileDiscovery = fileDiscovery;
	}
}
