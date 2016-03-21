package build.pluto.builder.factory;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import build.pluto.executor.InputParser;
import build.pluto.executor.config.yaml.NullYamlObject;
import build.pluto.executor.config.yaml.YamlObject;

public class ReflectionInputParser<In extends Serializable> implements InputParser<In> {

  private static final long serialVersionUID = -5569654237072622142L;

  private final Class<In> inputClass;
  
  public ReflectionInputParser(Class<In> inputClass) {
    this.inputClass = inputClass;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public In parse(YamlObject yinput, String target, File workingDir) throws Throwable {
    try {
      // is there a builder class and a constructor that accepts the builder?
      Class<?> inputBuilderClass = Class.forName(inputClass.getName() + "$Builder");
      for (Constructor<?> cons : inputClass.getDeclaredConstructors())
        if (cons.getParameterTypes().length == 1 && cons.getParameterTypes()[0].isAssignableFrom(inputBuilderClass)) {
          cons.setAccessible(true);
          Object inputBuilder = parseReflective(inputBuilderClass, yinput, workingDir);
          return (In) cons.newInstance(inputBuilder);
        }
    } catch (ClassNotFoundException e) {
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
    
    return parseReflective(inputClass, yinput, workingDir);
  }
  
  private <T> T parseReflective(Class<T> cl, YamlObject yinput, File workingDir) throws IllegalArgumentException, IllegalAccessException {
    Objenesis objenesis = new ObjenesisStd();
    T obj = objenesis.newInstance(cl);
    
    Map<String, YamlObject> map = yinput.asMap();
    for (Field field : cl.getDeclaredFields()) {
      YamlObject yval = map.get(field.getName());
      if (yval != NullYamlObject.instance) {
        Object val = cast(yval, field.getGenericType(), workingDir);
        field.setAccessible(true);
        field.set(obj, val);
      }
    }
    
    return obj;
  }

  private Object cast(YamlObject yval, Type type, File workingDir) {
    if (type == boolean.class)
      return yval.asBoolean();
    else if (type == char.class)
      return yval.asChar();
    else if (type == byte.class || type == short.class || type == int.class)
      return yval.asInt();
    else if (type == long.class)
      return yval.asLong();
    else if (type == float.class || type == double.class)
      return yval.asDouble();
    else if (type == String.class)
      return yval.asString();
    
    if (yval == NullYamlObject.instance)
      return null;
    
    if (type == File.class) {
      File val = new File(yval.asString());
      if (!val.isAbsolute())
        val = new File(workingDir, yval.asString());
      return val;
    }
    
    if (type instanceof GenericArrayType) {
      GenericArrayType arType = (GenericArrayType) type;
      Type component = arType.getGenericComponentType();
      List<YamlObject> ylist = yval.asList();
      Object[] ar = (Object[]) Array.newInstance(classOfType(component), ylist.size());
      for (int i = 0; i < ar.length; i++)
        ar[i] = cast(ylist.get(i), component, workingDir);
      return ar;
    }
    
    if (type instanceof Class<?> && ((Class<?>) type).isArray()) {
      Class<?> component = ((Class<In>) type).getComponentType();
      List<YamlObject> ylist = yval.asList();
      Object[] ar = (Object[]) Array.newInstance(component, ylist.size());
      for (int i = 0; i < ar.length; i++)
        ar[i] = cast(ylist.get(i), component, workingDir);
      return ar;
    }
    
    if (type instanceof ParameterizedType) {
      Class<?> colClass = classOfType(((ParameterizedType) type).getRawType());
      Type[] targs = ((ParameterizedType) type).getActualTypeArguments();
      if (Collection.class.isAssignableFrom(colClass) && targs.length == 1) {
        Type component = targs[0];
        List<YamlObject> ylist = yval.asList();
        Collection<Object> col = constructCollection(colClass, ylist.size());
        if (col != null) {
          for (YamlObject o : ylist)
            col.add(cast(o, component, workingDir));
          
          return col;
        }
      }
    }
    
    throw new UnsupportedOperationException("Cannot parse " + yval + " as " + type);
  }

  private Class<?> classOfType(Type type) {
    if (type instanceof Class<?>)
      return (Class<?>) type;
    if (type instanceof GenericArrayType)
      throw new UnsupportedOperationException();
    if (type instanceof ParameterizedType)
      return classOfType(((ParameterizedType) type).getRawType());
    if (type instanceof TypeVariable<?>) {
      Type[] bounds = ((TypeVariable) type).getBounds();
      if (bounds.length == 0)
        return Object.class;
      if (bounds.length == 1)
        return classOfType(bounds[0]);
      throw new UnsupportedOperationException();
    }
    if (type instanceof WildcardType)
      throw new UnsupportedOperationException();
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> constructCollection(Class<?> type, int size) {
    if (type == List.class)
      return new ArrayList<>(size);
    if (type == Set.class)
      return new HashSet<>(size);
      
    try {
      Constructor<?> cons0 = type.getConstructor();
      if (cons0 != null)
        return (Collection<Object>) cons0.newInstance();
      
      Constructor<?> cons1 = type.getConstructor(int.class);
      if (cons1 != null)
        return (Collection<Object>) cons1.newInstance(size);
    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
    }
    return null;
  }

}
