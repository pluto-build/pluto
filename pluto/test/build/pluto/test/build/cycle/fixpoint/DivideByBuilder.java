package build.pluto.test.build.cycle.fixpoint;

import build.pluto.builder.BuilderFactory;

public class DivideByBuilder extends NumericBuilder {
	

	public static final BuilderFactory<FileInput, IntegerOutput, DivideByBuilder> factory = new BuilderFactory<FileInput, IntegerOutput, DivideByBuilder>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = -9080859579168714780L;

		@Override
		public DivideByBuilder makeBuilder(FileInput input) {
			return new DivideByBuilder(input);
		}
	};


	public DivideByBuilder(FileInput input) {
		super(input);
	}
	
	@Override
	protected Operator getOperator() {
		return new Operator() {
			
			@Override
			public int apply(int a, int b) {
				return a/ b;
			}
		};
	}

	@Override
	protected String description() {
		return "Dividy by for " + this.input.getFile().getRelativePath();
	}
	
	

}
