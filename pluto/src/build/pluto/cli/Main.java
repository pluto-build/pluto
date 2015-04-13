package build.pluto.cli;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;

public class Main {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalAccessException, IOException {
    String command;
    BuilderFactory<Serializable, Serializable, Builder<Serializable, Serializable>> factory;
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
      
      BuildRequest<?, ?, ?, ?> req = new BuildRequest<Serializable, Serializable, Builder<Serializable, Serializable>, BuilderFactory<Serializable, Serializable, Builder<Serializable, Serializable>>>(factory, input);
      BuildManager.build(req);
      BuildUnit<?> unit = BuildManager.readResult(req);
      System.exit(unit.hasFailed() ? 1 : 0);
    } catch (ParseException e) {
      showUsage(command, options);
    }
  }

  @SuppressWarnings("unchecked")
  private static BuilderFactory<Serializable, Serializable, Builder<Serializable, Serializable>> findFactory(String factoryField) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    String factoryFieldContainer = factoryField.substring(0, factoryField.lastIndexOf('.'));
    String factoryFieldName = factoryField.substring(factoryField.lastIndexOf('.') + 1);
    Class<?> containerClass = Class.forName(factoryFieldContainer);
    Field f = containerClass.getDeclaredField(factoryFieldName);
    f.setAccessible(true);
    Object fval = f.get(null);
    return (BuilderFactory<Serializable, Serializable, Builder<Serializable, Serializable>>) fval;
  }
  

  private static void showUsage(String command, Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(command, options);
  }
  

}
