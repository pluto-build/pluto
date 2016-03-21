package build.pluto.test.build.cycle.fixpoint;

import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.OutputPersisted;

public class DivideByBuilder extends NumericBuilder {

  public static final BuilderFactory<FileInput, OutputPersisted<Integer>, DivideByBuilder> factory = BuilderFactoryFactory.of(DivideByBuilder.class, FileInput.class);

  public DivideByBuilder(FileInput input) {
    super(input);
  }

  @Override
  protected String description(FileInput input) {
    return "Divide by for " + input.getFile();
  }

  @Override
  protected BiFunction<Integer, Integer, Integer> getOperator() {
    return new BiFunction<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a / b;
      }
    };
  }

  @Override
  protected BiFunction<Integer, Integer, String> getPrintOperator() {
    return new BiFunction<Integer, Integer, String>() {
      @Override
      public String apply(Integer a, Integer b) {
        return a + " / " + b;
      }
    };
  }

}
