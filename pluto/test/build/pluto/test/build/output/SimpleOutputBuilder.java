package build.pluto.test.build.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.Output;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public abstract class SimpleOutputBuilder extends Builder<TestBuilderInput, Output> {

  public SimpleOutputBuilder(TestBuilderInput input) {
		super(input);
	}

	@Override
  protected String description(TestBuilderInput input) {
		return "Test Builder for " + input.getInputPath();
	}

	@Override
  protected File persistentPath(TestBuilderInput input) {
		return FileCommands.addExtension(input.getInputPath().toPath(), "dep").toFile();
	}

	@Override
	protected Stamper defaultStamper() {
		return FileHashStamper.instance;
	}

	@Override
  protected Output build(TestBuilderInput input) throws IOException {
		require(input.getInputPath());
		List<String> allLines = Files.readAllLines(input.getInputPath().toPath());

		if (!allLines.isEmpty() && allLines.get(0).equals("#fail"))
		  throw new RuntimeException("#fail detected in source file");
		
		List<String> contentLines = new ArrayList<String>();

		for (String line : allLines) {
			if (line.startsWith("Dep:")) {
				String depFile = line.substring(4);
				TestBuilderInput depInput = new TestBuilderInput(input.getBasePath(), new File(input.getBasePath(), depFile));
				SimpleOutputBuilderRequirement req = new SimpleOutputBuilderRequirement(this.factory(), depInput);
				Output reqOut = requireBuild(req);
				checkOutput(req, reqOut);
			} else {
				contentLines.add(line);
			}
		}

		// Write the content to a generated file
		File generatedFile = FileCommands.addExtension(input.getInputPath().toPath(), "gen").toFile();
		Files.write(generatedFile.toPath(), contentLines);
		provide(generatedFile);
		setState(BuildUnit.State.finished(true));
		return output(input);
	}

	protected abstract Output output(TestBuilderInput input);
	protected abstract void checkOutput(SimpleOutputBuilderRequirement req, Output reqOut);
	protected abstract BuilderFactory<TestBuilderInput, Output, SimpleOutputBuilder> factory();
}
