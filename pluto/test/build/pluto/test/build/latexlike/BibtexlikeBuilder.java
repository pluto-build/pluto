package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupport;
import build.pluto.output.Out;

public class BibtexlikeBuilder extends Builder<File, Out<File>> {

  public static final BuilderFactory<File, Out<File>, BibtexlikeBuilder> factory = BibtexlikeBuilder::new;

  public BibtexlikeBuilder(File input) {
    super(input);
  }

  @Override
  protected String description() {
    return "Bibtexlike for " + this.input.getName();
  }

  @Override
  protected File persistentPath() {
    return FileCommands.addExtension(this.input, "bibdep");
  }

  @Override
  protected CycleSupport getCycleSupport() {
    return BibtexLatexCycleSupport.instance;
  }

  @Override
  protected Out<File> build() throws Throwable {
    requireBuild(LatexlikeBuilder.factory, input);

    File outFile = FileCommands.replaceExtension(this.input, "outlike");

    if (!outFile.exists()) {
      return Out.of(null);
    }

    File bibFile = new File(this.input.getParentFile(), "bib.biblike");

    require(outFile, BibtexlikeStamper.instance);
    require(bibFile);

    ObjectInputStream outStream = new ObjectInputStream(new FileInputStream(outFile));
    String input = (String) outStream.readObject();
    outStream.close();
    char[] inputChars = input.toCharArray();

    Map<Character, String> bib = new HashMap<>();
    for (String entry : Files.readAllLines(bibFile.toPath())) {
      Character key = entry.charAt(0);
      String value = entry.substring(2, entry.length());
      bib.put(key, value);
    }

    Map<String, String> replaceTexts = new HashMap<>();

    for (int i = 0; i < inputChars.length - 1; i++) {
      if (inputChars[i] == 'X') {
        char nextChar = inputChars[i + 1];
        replaceTexts.put(Character.toString('X') + nextChar, bib.getOrDefault(nextChar, ""));
        i++;
      }
    }

    Log.log.log("BIBTEXLIKE", Log.CORE);

    File replaceFile = FileCommands.replaceExtension(this.input, "rep");

    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(replaceFile));
    stream.writeObject(replaceTexts);
    stream.close();

    provide(replaceFile);

    return Out.of(replaceFile);

  }


}
