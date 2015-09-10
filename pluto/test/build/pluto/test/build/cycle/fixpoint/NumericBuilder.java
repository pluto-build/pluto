package build.pluto.test.build.cycle.fixpoint;

import java.io.File;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.BuildUnit.State;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.output.OutputPersisted;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;

public abstract class NumericBuilder extends Builder<FileInput, OutputPersisted<Integer>> {

  public NumericBuilder(FileInput input) {
    super(input);
  }

  protected abstract BiFunction<Integer, Integer, Integer> getOperator();

  protected abstract BiFunction<Integer, Integer, String> getPrintOperator();

  @Override
  public
  final File persistentPath(FileInput input) {
    return FileCommands.addExtension(input.getFile().toPath(), "dep").toFile();
  }

  @Override
  protected final Stamper defaultStamper() {
    return FileContentStamper.instance;
  }

  @Override
  protected final CycleHandlerFactory getCycleSupport() {
    return NumericCycleSupport.factory;
  }

  @Override
  protected final OutputPersisted<Integer> build(FileInput input) throws Throwable {
    require(input.getFile());
    int myNumber = FileUtils.readIntFromFile(input.getFile());

    if (input.getDepsFile().exists()) {
      require(input.getDepsFile());
      List<File> depPaths = FileUtils.readPathsFromFile(input.getDepsFile(), input.getWorkingDir());

      for (File path : depPaths) {
        FileInput finput = new FileInput(input.getWorkingDir(), path);
        OutputPersisted<Integer> output = null;
        String extension = FileCommands.getExtension(path);
        if (extension.equals("modulo")) {
          output = requireBuild(ModuloBuilder.factory, finput);
        } else if (extension.equals("divide")) {
          output = requireBuild(DivideByBuilder.factory, finput);
        } else if (extension.equals("gcd")) {
          output = requireBuild(GCDBuilder.factory, finput);
        } else {
          throw new IllegalArgumentException("The extension " + extension + " is unknown.");
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
    return OutputPersisted.of(myNumber);

  }

}
