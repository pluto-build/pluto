package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sugarj.common.FileCommands;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupportFactory;
import build.pluto.output.Out;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.IgnoreOutputStamper;
import build.pluto.stamp.Stamper;
import build.pluto.test.build.latexlike.LatexlikeLog.CompilationParticipant;

public class BibtexlikeBuilder extends Builder<File, Out<File>> {

  public static final BuilderFactory<File, Out<File>, BibtexlikeBuilder> factory = BuilderFactory.of(BibtexlikeBuilder.class, File.class);

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
  protected CycleSupportFactory getCycleSupport() {
    return BibtexLatexCycleSupport.factory;
  }

  @Override
  protected Stamper defaultStamper() {
    return FileContentStamper.instance;
  }

  @Override
  protected Out<File> build(File input) throws Throwable {
    requireBuild(new BuildRequest<>(LatexlikeBuilder.factory, input, IgnoreOutputStamper.instance));

    File outFile = FileCommands.replaceExtension(input, "outlike");
    require(outFile, BibtexlikeStamper.instance);

    if (!outFile.exists()) {
      return Out.of(null);
    }

    File bibFile = new File(input.getParentFile(), "bib.biblike");
    require(bibFile);

    LatexlikeLog.logBuilderPerformedWork(CompilationParticipant.BIBLIKE, "BIBLIKE: Do compile");

    ObjectInputStream outStream = new ObjectInputStream(new FileInputStream(outFile));
    @SuppressWarnings("unchecked")
    List<Character> replacements = (List<Character>) outStream.readObject();
    outStream.close();

    Map<Character, String> bib = new HashMap<>();
    for (String entry : Files.readAllLines(bibFile.toPath())) {
      if (entry.length() != 0) {
        Character key = entry.charAt(0);
        String value = entry.substring(2, entry.length());
        bib.put(key, value);
      }
    }

    Map<String, String> replaceTexts = new HashMap<>();

    for (Character toReplace : replacements) {
      replaceTexts.put(Character.toString('X') + toReplace, bib.getOrDefault(toReplace, ""));
    }

    File replaceFile = FileCommands.replaceExtension(input, "rep");

    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(replaceFile));
    stream.writeObject(replaceTexts);
    stream.close();

    provide(replaceFile);

    return Out.of(replaceFile);

  }

}
