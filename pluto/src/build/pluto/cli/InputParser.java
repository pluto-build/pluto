package build.pluto.cli;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

public class InputParser<In> {
  
  private final Class<In> inputClass;
  private Constructor<In> inputConstructor;
  private Option[] inputOptions;
  
  public InputParser(Class<In> inputClass) {
    this.inputClass = inputClass;
  }
  
  @SuppressWarnings("unchecked")
  private Constructor<In> getInputConstructor() {
    if (inputConstructor != null)
      return inputConstructor;

    for (Constructor<?> c : inputClass.getConstructors()) {
      if (c.isAccessible())
        continue;
      else if (inputConstructor == null)
        inputConstructor = (Constructor<In>) c;
      else if (inputConstructor.getParameters().length < c.getParameters().length)
        inputConstructor = (Constructor<In>) c;
    }
    
    if (inputConstructor == null)
      try { // try to use default constructor
        inputConstructor = inputClass.getConstructor();
      } catch (NoSuchMethodException | SecurityException e) {
      }
    
    if (inputConstructor == null)
      throw new IllegalArgumentException("Could not find callable constructor in class " + inputClass);
    
    return inputConstructor;
  }
  
  public void registerOptions(Options options) {
    Constructor<In> c = getInputConstructor();
    inputOptions = new Option[c.getParameters().length];
    
    for (int i = 0; i < c.getParameters().length; i++) {
      String name = c.getParameters()[i].getName();
      Class<?> paramClass = c.getParameters()[i].getType();
      Option opt = makeOption(name, paramClass);
      options.addOption(opt);
      inputOptions[i] = opt;
    }
  }

  private Option makeOption(String name, Class<?> paramClass) {
    String optName = makeOptionName(name);
    String desc = "Value for constructor parameter " + name + " of type " + pretty(paramClass);
    
    if (paramClass.equals(boolean.class) || paramClass.equals(Boolean.class))
      return new Option(null, optName, false, desc);
    
    Option opt = new Option(null, optName, true, desc);
    if (paramClass.isArray() || Collection.class.isAssignableFrom(paramClass)) {
      opt.setArgs(Option.UNLIMITED_VALUES);
      opt.setValueSeparator(' ');
//      opt.setDescription(desc + ", separate values by '" + File.pathSeparatorChar + "'");
    }
    return opt;
  }

  private String makeOptionName(String s) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isUpperCase(c))
        builder.append('-').append(Character.toLowerCase(c));
      else
        builder.append(c);
    }
    return builder.toString();
  }
  
  public In parseCommandLine(CommandLine line) {
    Objects.requireNonNull(inputConstructor);
    Objects.requireNonNull(inputOptions);
    
    Object[] inputs = new Object[inputOptions.length];
    for (int i = 0; i < inputs.length; i++) {
      Option opt = inputOptions[i];
      Class<?> paramClass = inputConstructor.getParameters()[i].getType();
      if (line.hasOption(opt.getLongOpt())) {
        Object paramVal = parseParamFromCommandLine(opt.getLongOpt(), paramClass, line.getOptionValues(opt.getLongOpt()));
        inputs[i] = paramVal;
      }
      else
        inputs[i] = defaultParamValue(paramClass);
    }
    
    try {
      return inputConstructor.newInstance(inputs);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private Object parseParamFromCommandLine(String opt, Class<?> paramClass, String[] vals) {
    if (vals == null)
      vals = new String[0];
    
    Object collection = parseCollectionParamFromCommandLine(opt, paramClass, vals);
    if (collection != null)
      return collection;
    
    if (vals.length > 1)
      throw new IllegalArgumentException("Too many values for option " + opt + " of type " + pretty(paramClass));
    String val = vals[0];
    
    // primitive types
    if (paramClass.equals(boolean.class) || paramClass.equals(Boolean.class))
      return true;
    if (paramClass.equals(byte.class) || paramClass.equals(Byte.class))
      return Byte.parseByte(val);
    if (paramClass.equals(short.class) || paramClass.equals(Short.class))
      return Short.parseShort(val);
    if (paramClass.equals(int.class) || paramClass.equals(Integer.class))
      return Integer.parseInt(val);
    if (paramClass.equals(long.class) || paramClass.equals(Long.class))
      return Long.parseLong(val);
    if (paramClass.equals(float.class) || paramClass.equals(Float.class))
      return Float.parseFloat(val);
    if (paramClass.equals(double.class) || paramClass.equals(Double.class))
      return Double.parseDouble(val);
    if (paramClass.equals(char.class) || paramClass.equals(Character.class)) {
      if (val.length() == 1)
        return val.charAt(0);
      throw new IllegalArgumentException("Expected single character but found " + val + " for option " + opt);
    }
    
    // selected class types
    if (paramClass.equals(String.class))
      return val;
    if (paramClass.equals(AbsolutePath.class))
      return new AbsolutePath(val);
    if (paramClass.equals(Path.class) && AbsolutePath.acceptable(val))
      return new AbsolutePath(val);
    if (paramClass.equals(Path.class) || paramClass.equals(RelativePath.class))
      return new RelativePath(new AbsolutePath("."), val);
    
    throw new UnsupportedOperationException("Cannot parse value of type " + pretty(paramClass) + " for option " + opt);
  }

  @SuppressWarnings("unchecked")
  private Object parseCollectionParamFromCommandLine(String opt, Class<?> paramClass, String[] vals) {
    if (paramClass.isArray()) {
      Object arObj = Array.newInstance(paramClass.getComponentType(), vals.length);
      for (int i = 0; i < vals.length; i++)
        Array.set(arObj, i, parseParamFromCommandLine(opt, paramClass.getComponentType(), new String[] {vals[i]}));
      return arObj;
    }
    
    if (Collection.class.isAssignableFrom(paramClass)) {
      Collection<Object> col;
      
      try {
        if (paramClass.equals(Collection.class) || paramClass.equals(List.class))
          col = new ArrayList<>();
        else if (paramClass.equals(Set.class))
          col = new HashSet<>();
        else
          col = (Collection<Object>) paramClass.getConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        try {
          col = (Collection<Object>) paramClass.getConstructor(int.class).newInstance(vals.length);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
          try {
            col = (Collection<Object>) paramClass.getConstructor(Integer.class).newInstance(vals.length);
          } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e2) {
            throw new UnsupportedOperationException("Cannot instantiate collection of type " + pretty(paramClass));
          }
        }
      }
      
      if (vals.length == 0)
        return col;
      
      List<Pair<Object, Class<?>>> elems = new ArrayList<>();
      for (String s : vals) 
        elems.add(parseParamWithInferredType(s));
      Class<?> elemType = findCommonSupertype(elems);
      
      for (int i = 0; i < vals.length; i++)
        col.add(parseParamFromCommandLine(opt, elemType, new String[] {vals[i]}));
      
      return col;
    }
    
    return null;
  }

  private Pair<Object, Class<?>> parseParamWithInferredType(String s) {
    // primitive types
    if ("true".equals(s.toLowerCase()) || "false".equals(s.toLowerCase()))
      return Pair.create(Boolean.parseBoolean(s), Boolean.class);
    try {
      return Pair.create(Byte.parseByte(s), Byte.class);
    } catch (NumberFormatException e) { }
    try {
      return Pair.create(Short.parseShort(s), Short.class);
    } catch (NumberFormatException e) { }
    try {
      return Pair.create(Integer.parseInt(s), Integer.class);
    } catch (NumberFormatException e) { }
    try {
      return Pair.create(Long.parseLong(s), Long.class);
    } catch (NumberFormatException e) { }
    try {
      return Pair.create(Float.parseFloat(s), Float.class);
    } catch (NumberFormatException e) { }
    try {
      return Pair.create(Double.parseDouble(s), Double.class);
    } catch (NumberFormatException e) { }
    
    
    // selected class types
    if (AbsolutePath.acceptable(s))
      return Pair.create(new AbsolutePath(s), AbsolutePath.class);
    if (new File(s).exists())
      return Pair.create(new RelativePath(new AbsolutePath("."), s), RelativePath.class);
    if (s.matches("^([\\w\\.\\-\\$]+[/\\])*[\\w\\.\\-\\$]+$"))
      return Pair.create(new RelativePath(new AbsolutePath("."), s), RelativePath.class);
    
    // character types
    if (s.length() == 1)
      return Pair.create(s.charAt(0), Character.class);
    return Pair.create(s, String.class);
  }
  
  private Class<?> findCommonSupertype(List<Pair<Object, Class<?>>> elems) {
    if (elems.isEmpty())
      return null;
    
    Class<?> cl = elems.get(0).b;
    if (cl.equals(Byte.class) || cl.equals(Short.class))
      cl = Integer.class;
    else if (cl.equals(Float.class))
      cl = Double.class;
    
    for (int i = 1; i < elems.size(); i++) {
      Class<?> next = elems.get(i).b;
      cl = meetTypes(cl, next);
    }
    
    return cl;
  }

  private Class<?> meetTypes(Class<?> cl1, Class<?> cl2) {
    if (cl1.equals(cl2))
      return cl1;
    
    if (cl1.isAssignableFrom(cl2))
      return cl1;
    if (cl2.isAssignableFrom(cl1))
      return cl2;
    
    Class<?> prim = meetPrimitiveTypes(cl1, cl2);
    if (prim == null)
      prim = meetPrimitiveTypes(cl2, cl1);
    if (prim != null)
      return prim;
    
    return String.class;
  }

  private Class<?> meetPrimitiveTypes(Class<?> cl1, Class<?> cl2) {
    if (cl1.equals(Short.class) && cl2.equals(Byte.class))
      return Integer.class;
    if (cl1.equals(Integer.class) && (cl2.equals(Byte.class) || cl2.equals(Short.class)))
        return Integer.class;
    if (cl1.equals(Long.class) && (cl2.equals(Byte.class) || cl2.equals(Short.class) || cl2.equals(Integer.class)))
      return Long.class;
    if (cl1.equals(Float.class) && (cl2.equals(Byte.class) || cl2.equals(Short.class) || cl2.equals(Integer.class) || cl2.equals(Long.class)))
      return Double.class;
    if (cl1.equals(Double.class) && (cl2.equals(Byte.class) || cl2.equals(Short.class) || cl2.equals(Integer.class) || cl2.equals(Long.class) || cl2.equals(Float.class)))
      return Double.class;
    
    return null;
  }
  
  private String pretty(Class<?> cl) {
    if (cl.isArray())
      return cl.getComponentType().getName() + "[]";
    return cl.getName();
  }
  
  private Object defaultParamValue(Class<?> paramClass) {
    if (paramClass.equals(boolean.class) || paramClass.equals(Boolean.class))
      return false;
    if (paramClass.equals(byte.class) || paramClass.equals(Byte.class))
      return (byte) 0;
    if (paramClass.equals(short.class) || paramClass.equals(Short.class))
      return (short) 0;
    if (paramClass.equals(int.class) || paramClass.equals(Integer.class))
      return (int) 0;
    if (paramClass.equals(long.class) || paramClass.equals(Long.class))
      return (long) 0;
    if (paramClass.equals(float.class) || paramClass.equals(Float.class))
      return (float) 0;
    if (paramClass.equals(double.class) || paramClass.equals(Double.class))
      return (double) 0;
    if (paramClass.equals(char.class) || paramClass.equals(Character.class))
      return (char) 0;
    return null;
  }
}
