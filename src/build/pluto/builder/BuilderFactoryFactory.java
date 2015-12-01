package build.pluto.builder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

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


    class ReflectionBuilderFactory implements BuilderFactory<In_, Out_, B_> {
      private static final long serialVersionUID = -7269299134693061223L;

      private Class<? extends B_> builderClass;
      private Class<In_> inputClass;
      private Constructor<? extends B_> builderConstructor;

      public ReflectionBuilderFactory(Class<? extends B_> builderClass, Class<In_> inputClass) {
        this.builderClass = builderClass;
        this.inputClass = inputClass;
        this.initContructor();
      }

      private void initContructor() {
        try {
          builderConstructor = builderClass.getConstructor(inputClass);
          if (!Modifier.isPublic(builderConstructor.getModifiers())) {
            throw new IllegalArgumentException("Cannot call the constructor of " + builderClass + " because it is not public.");
          }
        } catch (NoSuchMethodException | SecurityException e) {
          throw new IllegalArgumentException("No constructor " + builderClass.getSimpleName() + "(" + inputClass.getSimpleName() + ") found", e);
        }
      }

      @Override
      public B_ makeBuilder(In_ input) {
        try {
          return builderConstructor.newInstance(input);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new RuntimeException("Failed to call the constructor for " + builderClass.getSimpleName(), e);
        }
      }

      @SuppressWarnings("unchecked")
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        builderClass = (Class<B_>) stream.readObject();
        inputClass = (Class<In_>) stream.readObject();
        initContructor();
      }

      private void writeObject(ObjectOutputStream stream) throws IOException, ClassNotFoundException {
        stream.writeObject(builderClass);
        stream.writeObject(inputClass);
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((builderClass == null) ? 0 : builderClass.hashCode());
        result = prime * result + ((inputClass == null) ? 0 : inputClass.hashCode());
        return result;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj)
          return true;
        if (obj == null)
          return false;
        if (getClass() != obj.getClass())
          return false;
        ReflectionBuilderFactory other = (ReflectionBuilderFactory) obj;
        if (builderClass == null) {
          if (other.builderClass != null)
            return false;
        } else if (!builderClass.equals(other.builderClass))
          return false;
        if (inputClass == null) {
          if (other.inputClass != null)
            return false;
        } else if (!inputClass.equals(other.inputClass))
          return false;
        return true;
      }

      @Override
      public String toString() {
        return builderClass.toString() + "(" + inputClass.toString() + ")";
      }

    }
    return new ReflectionBuilderFactory(builderClass, inputClass);

  }
}
