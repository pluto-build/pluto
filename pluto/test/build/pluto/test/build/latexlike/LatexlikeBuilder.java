package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sugarj.common.FileCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.output.OutputPersisted;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.latexlike.LatexlikeLog.CompilationParticipant;

public class LatexlikeBuilder extends Builder<File, OutputPersisted<File>> {

  public static final BuilderFactory<File, OutputPersisted<File>, LatexlikeBuilder> factory = BuilderFactoryFactory.of(LatexlikeBuilder.class, File.class);

  public LatexlikeBuilder(File input) {
    super(input);
  }

  @Override
  protected String description(File input) {
    return "Latexlike for " + input.getName();
  }

  @Override
  protected File persistentPath(File input) {
    return FileCommands.addExtension(input, "dep");
  }

  @Override
  protected CycleHandlerFactory getCycleSupport() {
    return BibtexLatexCycleSupport.factory;
  }

  @Override
  protected Stamper defaultStamper() {
    return FileContentStamper.instance;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected OutputPersisted<File> build(File input) throws Throwable {
    OutputPersisted<File> replFile = requireBuild(BibtexlikeBuilder.factory, input);
    
    LatexlikeLog.logBuilderPerformedWork(CompilationParticipant.LATEXLIKE, "LATEXLIKE: Do compilation");

    Map<String, String> replacements;
    if (replFile.val != null) {
      require(replFile.val);
      ObjectInputStream stream = new ObjectInputStream(new FileInputStream(replFile.val));
      replacements = (Map<String, String>) stream.readObject();
      stream.close();
    } else {
      replacements = Collections.emptyMap();
    }

    require(input);

    File outFile = FileCommands.replaceExtension(input, "outlike");
    require(outFile);


    String text = FileCommands.readFileAsString(input);

    List<Character> replaceChars = new ArrayList<>();
    char[] textChars = text.toCharArray();
    for (int i = 0; i < textChars.length - 1; i++) {
      if (textChars[i] == 'X') {
        replaceChars.add(textChars[i + 1]);
        i++;
      }
    }

    for (String toReplace : replacements.keySet()) {
      text = text.replaceAll(toReplace, replacements.get(toReplace));
    }

    ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(outFile));
    outStream.writeObject(replaceChars);
    outStream.writeObject(replacements);
    outStream.close();
    provide(outFile);

    String pdf = text.replaceAll(" ", "\n");
    File pdfFile = FileCommands.replaceExtension(input, "pdflike");
    FileCommands.writeToFile(pdfFile, pdf);
    provide(pdfFile);

    return OutputPersisted.of(pdfFile);

  }

}
