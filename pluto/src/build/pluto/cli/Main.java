package build.pluto.cli;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.RequiredBuilderFailed;

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
      BuildRequest<?, ?, ?, ?> req = new BuildRequest<Serializable, Serializable, Builder<Serializable, Serializable>, BuilderFactory<Serializable, Serializable, Builder<Serializable, Serializable>>>(factory, input);
      
      if (line.hasOption(cleanOption.getLongOpt()) || line.hasOption(dryCleanOption.getLongOpt())) {
        clean(line.hasOption(dryCleanOption.getLongOpt()), req);
        return;
      }
      if (line.hasOption(cleanBuildOption.getLongOpt()))
        clean(false, req);
      
      BuildManager.build(req);
      BuildUnit<?> unit = BuildManager.readResult(req);
      System.exit(unit.hasFailed() ? 1 : 0);
    } catch (RequiredBuilderFailed e) {
      e.printStackTrace();
      System.exit(1);
    } catch (ParseException e) {
      showUsage(command, options);
      System.exit(1);
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
  
  private static void clean(boolean dryRun, BuildRequest<?, ?, ?, ?> req) throws IOException {
    BuildUnit<?> unit = BuildManager.readResult(req);
    Set<BuildUnit<?>> allUnits = unit.getTransitiveModuleDependencies();
    for (BuildUnit<?> next : allUnits) {
      for (Path p : next.getGeneratedFiles())
        deleteFile(p, dryRun);
      deleteFile(next.getPersistentPath(), dryRun);
    }
  }

  private static void deleteFile(Path p, boolean dryRun) throws IOException {
    Log.log.log("Delete " + p + (dryRun ? " (dry run)" : ""), Log.CORE);
    if (!dryRun)
      if (!p.getFile().isDirectory() || p.getFile().list().length == 0)
        FileCommands.delete(p);
  }


}
