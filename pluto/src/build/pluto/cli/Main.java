package build.pluto.cli;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.output.Output;

public class Main {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalAccessException, IOException {
    String command;
    BuilderFactory<Serializable, Output, Builder<Serializable, Output>> factory;
    Class<Serializable> inputClass;
    try {
      command = args[0];
      String factoryField = args[1];
      String inputClassName = args[2];
      
      factory = findFactory(factoryField);
      inputClass = (Class<Serializable>) Class.forName(inputClassName);
    } catch (Exception e) {
      System.err.println("Usage: <cmd-name> <builder-factory-field> <input-class>" );
      System.err.println(" * cmd-name\t\t\tarbitrary string, used for logging only");
      System.err.println(" * builder-factory-field\tfully qualified path to static field that contains the builder factory");
      System.err.println(" * input-class\t\t\tfully qualified path to class of the builder factory");
      System.err.print("Error was: ");
      e.printStackTrace();
      
      throw e;
    }
    
    
    Options options = new Options();
    Option helpOption = new Option("h", "help", false, "Display usage information");
    options.addOption(helpOption);
    Option cleanOption = new Option(null, "clean", false, "Clean all generated artifacts");
    options.addOption(cleanOption);
    Option dryCleanOption = new Option(null, "dry-clean", false, "Clean all generated artifacts (dry run)");
    options.addOption(dryCleanOption);
    Option cleanBuildOption = new Option(null, "clean-build", false, "Clean all generated artifacts and build afterwards");
    options.addOption(cleanBuildOption);
    
    
    InputParser<Serializable> inputParser = new InputParser<Serializable>(inputClass);
    inputParser.registerOptions(options);
    
    CommandLineParser parser = new BasicParser();
    
    try {
      String[] optArgs = Arrays.copyOfRange(args, 3, args.length);
      CommandLine line = parser.parse(options, optArgs);
      if (line.hasOption(helpOption.getOpt())) {
        showUsage(command, options);
        return;
      }
        
      Serializable input = inputParser.parseCommandLine(line);
      if (requiresListOfInputs(factory)) 
        input = new ArrayList<Serializable>(Collections.singletonList(input));
      BuildRequest<?, ?, ?, ?> req = new BuildRequest<Serializable, Output, Builder<Serializable, Output>, BuilderFactory<Serializable, Output, Builder<Serializable, Output>>>(factory, input);
      
      if (line.hasOption(cleanOption.getLongOpt()) || line.hasOption(dryCleanOption.getLongOpt())) {
        BuildManagers.clean(line.hasOption(dryCleanOption.getLongOpt()), req);
        return;
      }
      if (line.hasOption(cleanBuildOption.getLongOpt()))
        BuildManagers.clean(false, req);
      
      BuildManagers.build(req);
      BuildUnit<?> unit = BuildManagers.readResult(req);
      System.exit(unit.hasFailed() ? 1 : 0);
    } catch (RequiredBuilderFailed e) {
      e.printStackTrace();
      System.exit(1);
    } catch (ParseException e) {
      Log.log.logErr(e.getMessage(), Log.CORE);
      showUsage(command, options);
      System.exit(1);
    }
  }

  private static boolean requiresListOfInputs(BuilderFactory<Serializable, Output, Builder<Serializable, Output>> factory) {
    try {
      for (Method m : factory.getClass().getMethods())
          if ("makeBuilder".equals(m.getName()) && m.getParameterCount() == 1)
            return List.class.isAssignableFrom(m.getParameters()[0].getType());
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private static BuilderFactory<Serializable, Output, Builder<Serializable, Output>> findFactory(String factoryField) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    String factoryFieldContainer = factoryField.substring(0, factoryField.lastIndexOf('.'));
    String factoryFieldName = factoryField.substring(factoryField.lastIndexOf('.') + 1);
    Class<?> containerClass = Class.forName(factoryFieldContainer);
    Field f = containerClass.getDeclaredField(factoryFieldName);
    f.setAccessible(true);
    Object fval = f.get(null);
    return (BuilderFactory<Serializable, Output, Builder<Serializable, Output>>) fval;
  }
  

  private static void showUsage(String command, Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(command, options);
  }
}
