package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;

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
		return "Module Builder for " + this.input.getFile().getRelativePath();
	}

	@Override
	protected Operator getOperator() {
		return new Operator() {
			
			@Override
			public int apply(int a, int b) {
				return new BigInteger(Integer.toString(a)).mod(new BigInteger(Integer.toString(b))).intValue();
			}
		};
	}

}
