package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;

import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.OutputPersisted;


public class ModuloBuilder extends NumericBuilder{
	
  public static final BuilderFactory<FileInput, OutputPersisted<Integer>, ModuloBuilder> factory = BuilderFactoryFactory.of(ModuloBuilder.class, FileInput.class);
	
	public ModuloBuilder(FileInput input) {
		super(input);
	}

	@Override
  protected String description(FileInput input) {
    return "Module Builder for " + input.getFile();
	}

  @Override
  protected BiFunction<Integer, Integer, Integer> getOperator() {
    return new BiFunction<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return BigInteger.valueOf(a).mod(BigInteger.valueOf(b)).intValue();
      }
    }; 
  }

  @Override
  protected BiFunction<Integer, Integer, String> getPrintOperator() {
    return new BiFunction<Integer, Integer, String>() {
      @Override
      public String apply(Integer a, Integer b) {
        return a + " mod " + b;
      }
    }; 
  }

}
