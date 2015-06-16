package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;
import java.util.function.BiFunction;

import build.pluto.builder.BuilderFactory;
import build.pluto.output.OutputPersisted;

public class GCDBuilder extends NumericBuilder {

  public static final BuilderFactory<FileInput, OutputPersisted<Integer>, GCDBuilder> factory = BuilderFactory.of(GCDBuilder.class, FileInput.class);
	
	public GCDBuilder(FileInput input) {
		super(input);
	}

  @Override
  protected BiFunction<Integer, Integer, Integer> getOperator() {
    return (Integer a, Integer b) -> BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValue();
  }

  @Override
  protected BiFunction<Integer, Integer, String> getPrintOperator() {
    return (Integer a, Integer b) -> "gcd(" + a + "," + b + ")";
  }


	@Override
  protected String description(FileInput input) {
    return "GCD for " + input.getFile().getName();
	}


}
