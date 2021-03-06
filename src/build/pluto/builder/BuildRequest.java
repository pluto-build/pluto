package build.pluto.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

import build.pluto.builder.factory.BuilderFactory;
import build.pluto.output.Output;
import build.pluto.output.OutputEqualStamper;
import build.pluto.output.OutputStamper;

import com.cedarsoftware.util.DeepEquals;

public class BuildRequest<
  In extends Serializable, 
  Out extends Output, 
  B extends Builder<In, Out>,
  F extends BuilderFactory<In, Out, B>
> implements Serializable {
  private static final long serialVersionUID = 6839071650576011805L;
  
  public final F factory;
  public final In input;
  public final OutputStamper<? super Out> stamper;

  public BuildRequest(F factory, In input) {
    this(factory, input, OutputEqualStamper.instance());
  }
  
  public BuildRequest(F factory, In input, OutputStamper<? super Out> stamper) {
    Objects.requireNonNull(factory);
    Objects.requireNonNull(input);
    Objects.requireNonNull(stamper);
    if (BuildManager.ASSERT_SERIALIZABLE && !assertFactorySerializable(factory))
      throw new IllegalArgumentException("The given BuilderFactory does not fullfil its contract: its serialized and deseriablized object is not equal to itself");
    if (BuildManager.ASSERT_SERIALIZABLE && !assertInputSerializable(input))
      throw new IllegalArgumentException("The given Input does not fullfil its contract: its serialized and deseriablized object is not equal to itself: " + input);

    this.factory = factory;
    this.input = input;
    this.stamper = stamper;
  }

  public B createBuilder() {
    return factory.makeBuilder(input);
  }
  
  public boolean deepEquals(Object o) {
    return DeepEquals.deepEquals(this, o);
  }
  
  public int deepHashCode() {
    return DeepEquals.deepHashCode(this);
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof BuildRequest<?, ?, ?, ?>) {
      BuildRequest<?, ?, ?, ?> other = (BuildRequest<?, ?, ?, ?>) o;
      return DeepEquals.deepEquals(factory, other.factory) && DeepEquals.deepEquals(input, other.input);
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + DeepEquals.deepHashCode(factory);
    result = prime * result + DeepEquals.deepHashCode(input);
    return result;
  }

  @Override
  public String toString() {
    return "BuildReq(" + factory.toString() + ")";
  }

  private boolean assertFactorySerializable(F factory) {
    try {
      ByteArrayOutputStream memBufferOutput = new ByteArrayOutputStream();
      ObjectOutputStream oStream = new ObjectOutputStream(memBufferOutput);
      oStream.writeObject(factory);
      ByteArrayInputStream memBufferInput = new ByteArrayInputStream(memBufferOutput.toByteArray());
      ObjectInputStream iStream = new ObjectInputStream(memBufferInput);
      @SuppressWarnings("unchecked")
      F deserializedFactory = (F) iStream.readObject();
      return factory.equals(deserializedFactory) && deserializedFactory.equals(factory);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private boolean assertInputSerializable(In input) {
    try {
      ByteArrayOutputStream memBufferOutput = new ByteArrayOutputStream();
      ObjectOutputStream oStream = new ObjectOutputStream(memBufferOutput);
      oStream.writeObject(input);
      ByteArrayInputStream memBufferInput = new ByteArrayInputStream(memBufferOutput.toByteArray());
      ObjectInputStream iStream = new ObjectInputStream(memBufferInput);
      @SuppressWarnings("unchecked")
      In deserializedInput = (In) iStream.readObject();
      return DeepEquals.deepEquals(input, deserializedInput) && DeepEquals.deepEquals(deserializedInput, input);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
