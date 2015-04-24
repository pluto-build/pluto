package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;
import java.util.function.BiFunction;

import build.pluto.builder.BuilderFactory;


public class ModuloBuilder extends NumericBuilder{
	
	public static final BuilderFactory<FileInput, IntegerOutput, ModuloBuilder> factory = new BuilderFactory<FileInput, IntegerOutput, ModuloBuilder>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4153011353141009760L;

		@Override
		public ModuloBuilder makeBuilder(FileInput input) {
			return new ModuloBuilder(input);
		}
	};
	
	public ModuloBuilder(FileInput input) {
		super(input);
	}

	@Override
	protected String description() {
		return "Module Builder for " + this.input.getFile();
	}

  @Override
  protected BiFunction<Integer, Integer, Integer> getOperator() {
    return (Integer a, Integer b) -> BigInteger.valueOf(a).mod(BigInteger.valueOf(b)).intValue();
  }

  @Override
  protected BiFunction<Integer, Integer, String> getPrintOperator() {
    return (Integer a, Integer b) -> a + " mod " + b;
  }

}
