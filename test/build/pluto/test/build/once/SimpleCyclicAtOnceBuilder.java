package build.pluto.test.build.once;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.executor.InputParser;
import build.pluto.output.None;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class SimpleCyclicAtOnceBuilder extends
		BuildCycleAtOnceBuilder<TestBuilderInput, None> {

  public static BuilderFactory<ArrayList<TestBuilderInput>, None, SimpleCyclicAtOnceBuilder> factory = SimpleCyclicAtOnceBuilderFactor.instance;

  private static class SimpleCyclicAtOnceBuilderFactor implements BuilderFactory<ArrayList<TestBuilderInput>, None, SimpleCyclicAtOnceBuilder> {

		private static final long serialVersionUID = 1L;

    private static final SimpleCyclicAtOnceBuilderFactor instance = new SimpleCyclicAtOnceBuilderFactor();

		@Override
		public SimpleCyclicAtOnceBuilder makeBuilder(
				ArrayList<TestBuilderInput> input) {
			return new SimpleCyclicAtOnceBuilder(input);
		}

    private Object readResolve() {
      return instance;
    }

    @Override
    public boolean isOverlappingGeneratedFileCompatible(File overlap, Serializable input, BuilderFactory<?, ?, ?> otherFactory, Serializable otherInput) {
      return false;
    }

    @Override
    public InputParser<ArrayList<TestBuilderInput>> inputParser() {
      return null;
    }

	};

	public SimpleCyclicAtOnceBuilder(ArrayList<TestBuilderInput> input) {
		super(input, factory);
	}

	@Override
	protected File singletonPersistencePath(TestBuilderInput input) {
		return FileCommands.addExtension(input.getInputPath().toPath(), "dep").toFile();
	}
	
	@Override
  protected List<None> buildAll(ArrayList<TestBuilderInput> inputs) throws Throwable {

    List<None> outputs = new ArrayList<>(inputs.size());

		Set<File> cyclicDependencies = new HashSet<>();
    for (TestBuilderInput input : inputs) {
			// System.out.println(input.getInputPath().getRelativePath());
			cyclicDependencies.add(input.getInputPath());
			require(input.getInputPath());
		}

		List<String> contentLines = new ArrayList<>();

    for (TestBuilderInput input : inputs) {
			List<String> allLines = FileCommands.readFileLines(input.getInputPath());

			for (String line : allLines) {
				if (line.startsWith("Dep:")) {
					String depFile = line.substring(4);
					File depPath = new File(
							input.getBasePath(), depFile);
					if (!cyclicDependencies.contains(depPath)) {
						TestBuilderInput depInput = new TestBuilderInput(
								input.getBasePath(), depPath);
            requireBuild(factory, BuildCycleAtOnceBuilder.singletonArrayList(depInput));
					}
				} else {
					contentLines.add(line);
				}
			}
		}

    for (TestBuilderInput input : inputs) {

			// Write the content to a generated file
			File generatedFile = FileCommands.addExtension(
					input.getInputPath().toPath(), "gen").toFile();
			FileCommands.writeLinesFile(generatedFile, contentLines);
      provide(input, generatedFile);
			outputs.add(None.val);
		}
		setState(BuildUnit.State.finished(true));
		return outputs;
	}

	@Override
  protected String description(ArrayList<TestBuilderInput> inputs) {
		String descr = "Cyclic SimpleBuilder for ";
    for (TestBuilderInput input : inputs) {
			descr += input.getInputPath().getName() + ", ";
		}
		return descr;
	}

	@Override
	protected Stamper defaultStamper() {
		return FileContentStamper.instance;
	}

}
