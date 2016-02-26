package build.pluto.builder;

import java.io.File;
import java.io.Serializable;

import build.pluto.output.Output;

/**
 * The BuilderFactory creates builder for inputs. It needs to be serializable in a way that
 * the a BuilderFactory object b is equal to the deserialized object of the serializaed b.
 * Note that lambdas (including method references) do not fulfill this property.
 * 
 * @author moritzlichter
 *
 * @param <In> the input type of the builder
 * @param <Out> the output type of the builder
 * @param <B> the type of builder, which this factory creates.
 */
public interface BuilderFactory
//@formatter:off
<
  In extends Serializable, 
  Out extends Output, 
  B extends Builder<In, Out>
>
//@formatter:on
extends Serializable {

  /**
   * Creates a new builder for the given input. It required that for b =
   * makeInput(i) it is b.getInput(i).
   * 
   * @param input
   *          the input to create a builder for
   * @return the new builder
   */
  public B makeBuilder(In input);

  /**
   * In case two or more builders generate the same file, using this method, a builder
   * can declare that such an overlap is unproblematic.
   */
  public boolean isOverlappingGeneratedFileCompatible(File overlap, Serializable input, BuilderFactory<?, ?, ?> otherFactory, Serializable otherInput);
}
