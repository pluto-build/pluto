package build.pluto.test.build.cycle.fixpoint;

import java.util.function.BiFunction;

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
	protected String description() {
		return "Dividy by for " + this.input.getFile();
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
