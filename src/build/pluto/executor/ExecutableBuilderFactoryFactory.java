package build.pluto.executor;

import java.io.Serializable;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.output.Output;

public class ExecutableBuilderFactoryFactory {
  /**
   * Creates a BuilderFactory which creates a new builder by calling a unary
   * constructor of the builder class passing the input object. So builder B for
   * input In needs to define an accessible constructor B(In). The generated
   * builder factory implements proper serialization and equals checks.
   * 
   * @param builderClass
   *          the class of the builder to use
   * @param inputClass
   *          the class of the input
   * @return a builder factory for the both classes
   * @throws IllegalArgumentException
   *           if the constructor is not found or not accessible
   */
  public static 
//@formatter:off
  <
    In_ extends Serializable, 
    Out_ extends Output,
    B_ extends Builder<In_, Out_>
  > //@formatter:on
  ExecutableBuilderFactory<In_, Out_, B_> of(Class<? extends B_> builderClass, Class<In_> inputClass, InputParser<In_> parser) {
    return new ReflectionExecutableBuilderFactory<In_, Out_, B_>(builderClass, inputClass, parser);
  }
  
  public static class ReflectionExecutableBuilderFactory
//@formatter:off
  <
    In_ extends Serializable, 
    Out_ extends Output,
    B_ extends Builder<In_, Out_>
  > //@formatter:on
  extends BuilderFactoryFactory.ReflectionBuilderFactory<In_, Out_, B_> 
  implements ExecutableBuilderFactory<In_, Out_, B_> {
    private static final long serialVersionUID = 8772292286047291074L;
    
	private final InputParser<In_> parser;

    public ReflectionExecutableBuilderFactory(Class<? extends B_> builderClass, Class<In_> inputClass, InputParser<In_> parser) {
      super(builderClass, inputClass);
      this.parser = parser;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      return super.equals(obj);
    }

	@Override
	public InputParser<In_> inputParser() {
		return parser;
	}

  }
}
