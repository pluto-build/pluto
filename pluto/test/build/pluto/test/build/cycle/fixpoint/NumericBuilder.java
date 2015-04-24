package build.pluto.test.build.cycle.fixpoint;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.BuildUnit.State;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleSupport;
import build.pluto.output.Out;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;

public abstract class NumericBuilder extends Builder<FileInput, Out<Integer>> {

	public NumericBuilder(FileInput input) {
		super(input);
	}

  protected abstract BiFunction<Integer, Integer, Integer> getOperator();

  protected abstract BiFunction<Integer, Integer, String> getPrintOperator();

	@Override
	protected final File persistentPath() {
		return FileCommands.addExtension(this.input.getFile().toPath(), "dep").toFile();
	}

	@Override
	protected final Stamper defaultStamper() {
		return FileContentStamper.instance;
	}
	
	@Override
	protected final CycleSupport getCycleSupport() {
		return new NumericCycleSupport();
	}

	@Override
  protected final Out<Integer> build() throws Throwable {
		require(this.input.getFile());
		int myNumber = FileUtils.readIntFromFile(this.input.getFile());

		
		if (this.input.getDepsFile().exists()) {
			require(this.input.getDepsFile());
			List<File> depPaths = FileUtils.readPathsFromFile(
					this.input.getDepsFile(), this.input.getWorkingDir());

			for (File path : depPaths) {
				FileInput input = new FileInput(this.input.getWorkingDir(),
						path);
        Out<Integer> output = null;
				String extension = FileCommands.getExtension(path);
				if (extension.equals("modulo")) {
					output = requireBuild(ModuloBuilder.factory, input);
        } else if (extension.equals("divide")) {
          output = requireBuild(DivideByBuilder.factory, input);
				} else if (extension.equals("gcd")) {
					output = requireBuild(GCDBuilder.factory, input);
				} else {
					throw new IllegalArgumentException("The extension "
							+ extension + " is unknown.");
				}
				// Cycle support: if there is no output currently, ignore the dependency
				if (output != null) {
            int otherNumber = output.val;
            int result = this.getOperator().apply(myNumber, otherNumber);
            Log.log.log(this.getPrintOperator().apply(myNumber, otherNumber) + " = " + result, Log.CORE);
            myNumber = result;
				}
			}
		}
		setState(State.finished(true));
    return Out.of(myNumber);

	}

}
