package build.pluto.test.build.cycle.fixpoint;

import java.math.BigInteger;
import java.util.function.BiFunction;

import build.pluto.builder.BuilderFactory;
import build.pluto.output.Out;


public class ModuloBuilder extends NumericBuilder{
	
  public static final BuilderFactory<FileInput, Out<Integer>, ModuloBuilder> factory = ModuloBuilder::new;
	
	public ModuloBuilder(FileInput input) {
		super(input);
	}

	@Override
  protected String description(FileInput input) {
    return "Module Builder for " + input.getFile();
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
