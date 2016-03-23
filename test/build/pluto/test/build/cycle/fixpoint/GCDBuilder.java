package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;

import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.OutputPersisted;

public class GCDBuilder extends NumericBuilder {

  public static final BuilderFactory<FileInput, OutputPersisted<Integer>, GCDBuilder> factory = BuilderFactoryFactory.of(GCDBuilder.class, FileInput.class);
	
	public GCDBuilder(FileInput input) {
		super(input);
	}

  @Override
  protected BiFunction<Integer, Integer, Integer> getOperator() {
    return new BiFunction<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValue();
      }
    };
  }

  @Override
  protected BiFunction<Integer, Integer, String> getPrintOperator() {
    return new BiFunction<Integer, Integer, String>() {
      @Override
      public String apply(Integer a, Integer b) {
        return "gcd(" + a + "," + b + ")";
      }
    };
  }


	@Override
  protected String description(FileInput input) {
    return "GCD for " + input.getFile().getName();
	}


}
