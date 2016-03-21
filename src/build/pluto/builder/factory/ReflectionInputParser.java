package build.pluto.builder.factory;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
  public In parse(YamlObject yinput, String target, File workingDir) throws Throwable {
    Objenesis objenesis = new ObjenesisStd();
    In input = objenesis.newInstance(inputClass);
    
    Map<String, YamlObject> map = yinput.asMap();
    for (Field field : inputClass.getFields()) {
      YamlObject yval = map.get(field.getName());
      if (yval != NullYamlObject.instance) {
        Object val = cast(yval, field.getType(), workingDir);
        field.setAccessible(true);
        field.set(input, val);
      }
    }
    
    return input;
  }

  private Object cast(YamlObject yval, Class<?> type, File workingDir) {
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
    
    if (type.isArray()) {
      Class<?> component = type.getComponentType();
      List<YamlObject> ylist = yval.asList();
      Object[] ar = (Object[]) Array.newInstance(component, ylist.size());
      for (int i = 0; i < ar.length; i++)
        ar[i] = cast(ylist.get(i), component, workingDir);
      return ar;
    }
    
    if (Collection.class.isAssignableFrom(type)) {
      List<YamlObject> ylist = yval.asList();
      Collection<Object> col = constructCollection(type, ylist.size());
      if (col != null) {
        for (YamlObject o : ylist)
          col.add(o);
        
        return col;
      }
    }
    
    return yval.asObject();
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> constructCollection(Class<?> type, int size) {
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
