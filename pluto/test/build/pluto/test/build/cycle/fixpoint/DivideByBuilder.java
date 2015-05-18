package build.pluto.test.build.cycle.fixpoint;

import java.util.function.BiFunction;

import build.pluto.builder.BuilderFactory;
import build.pluto.output.Out;

public class DivideByBuilder extends NumericBuilder {

  public static final BuilderFactory<FileInput, Out<Integer>, DivideByBuilder> factory = BuilderFactory.of(DivideByBuilder.class, FileInput.class);

  public DivideByBuilder(FileInput input) {
    super(input);
  }

  @Override
  protected String description(FileInput input) {
    return "Divide by for " + input.getFile();
  }

  @Override
  protected BiFunction<Integer, Integer, Integer> getOperator() {
    return (Integer a, Integer b) -> a / b;
  }

  @Override
  protected BiFunction<Integer, Integer, String> getPrintOperator() {
    return (Integer a, Integer b) -> a + " / " + b;
  }

}
