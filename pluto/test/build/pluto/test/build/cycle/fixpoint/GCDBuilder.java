package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;
import java.util.function.BiFunction;

import build.pluto.builder.BuilderFactory;
import build.pluto.output.Out;

public class GCDBuilder extends NumericBuilder {

  public static final BuilderFactory<FileInput, Out<Integer>, GCDBuilder> factory = GCDBuilder::new;
	
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
	protected String description() {
		return "GCD for " + this.input.getFile().getName();
	}


}
