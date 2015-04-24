package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;
import java.util.function.BiFunction;

import build.pluto.builder.BuilderFactory;

public class GCDBuilder extends NumericBuilder {

	public static final BuilderFactory<FileInput, IntegerOutput, GCDBuilder> factory = new BuilderFactory<FileInput, IntegerOutput, GCDBuilder>() {

	
		/**
		 * 
		 */
		private static final long serialVersionUID = -8446980413610510702L;

		@Override
		public GCDBuilder makeBuilder(FileInput input) {
			return new GCDBuilder(input);
		}
	};
	
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
