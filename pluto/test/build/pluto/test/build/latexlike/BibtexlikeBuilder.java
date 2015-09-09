package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sugarj.common.FileCommands;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.output.IgnoreOutputStamper;
import build.pluto.output.OutputPersisted;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.latexlike.LatexlikeLog.CompilationParticipant;

public class BibtexlikeBuilder extends Builder<File, OutputPersisted<File>> {

  public static final BuilderFactory<File, OutputPersisted<File>, BibtexlikeBuilder> factory = BuilderFactoryFactory.of(BibtexlikeBuilder.class, File.class);

  public BibtexlikeBuilder(File input) {
    super(input);
  }

  @Override
  protected String description(File input) {
    return "Bibtexlike for " + input.getName();
  }

  @Override
  protected File persistentPath(File input) {
    return FileCommands.addExtension(new File(input.getParentFile(), "bib.biblike"), "dep");
  }

  @Override
  protected CycleHandlerFactory getCycleSupport() {
    return BibtexLatexCycleSupport.factory;
  }

  @Override
  protected Stamper defaultStamper() {
    return FileContentStamper.instance;
  }

  @Override
  protected OutputPersisted<File> build(File input) throws Throwable {
    requireBuild(new BuildRequest<>(LatexlikeBuilder.factory, input, IgnoreOutputStamper.instance));

    File outFile = FileCommands.replaceExtension(input, "outlike");
    require(outFile, BibtexlikeStamper.instance);

    if (!outFile.exists()) {
      return OutputPersisted.of(null);
    }

    File bibFile = new File(input.getParentFile(), "bib.biblike");
    require(bibFile);

    LatexlikeLog.logBuilderPerformedWork(CompilationParticipant.BIBLIKE, "BIBLIKE: Do compile");

    ObjectInputStream outStream = new ObjectInputStream(new FileInputStream(outFile));
    @SuppressWarnings("unchecked")
    List<Character> replacements = (List<Character>) outStream.readObject();
    outStream.close();

    Map<Character, String> bib = new HashMap<>();
    for (String entry : FileCommands.readFileLines(bibFile)) {
      if (entry.length() != 0) {
        Character key = entry.charAt(0);
        String value = entry.substring(2, entry.length());
        bib.put(key, value);
      }
    }

    Map<String, String> replaceTexts = new HashMap<>();

    for (Character toReplace : replacements) {
      String replace = bib.get(toReplace);
      if (replace == null)
        replace = "";
      replaceTexts.put(Character.toString('X') + toReplace, replace);
    }

    File replaceFile = FileCommands.replaceExtension(input, "rep");

    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(replaceFile));
    stream.writeObject(replaceTexts);
    stream.close();

    provide(replaceFile);

    return OutputPersisted.of(replaceFile);

  }

}
