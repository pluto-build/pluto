package build.pluto.builder.factory;

import java.io.Serializable;

import build.pluto.builder.Builder;
import build.pluto.executor.InputParser;
import build.pluto.output.Output;

public class BuilderFactoryFactory {
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
   * @param parser
   *          the input parser for the input
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
  BuilderFactory<In_, Out_, B_> of(Class<? extends B_> builderClass, Class<In_> inputClass, InputParser<In_> parser) {
    return new ReflectionBuilderFactory<In_, Out_, B_>(builderClass, inputClass, parser);
  }

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
  BuilderFactory<In_, Out_, B_> of(Class<? extends B_> builderClass, Class<In_> inputClass) {
    return new ReflectionBuilderFactory<In_, Out_, B_>(builderClass, inputClass, new ReflectionInputParser(inputClass));
  }
  
  
}
