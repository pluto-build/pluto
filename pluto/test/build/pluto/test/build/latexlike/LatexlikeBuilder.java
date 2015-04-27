package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupport;
import build.pluto.output.Out;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;

public class LatexlikeBuilder extends Builder<File, Out<File>> {

  public static final BuilderFactory<File, Out<File>, LatexlikeBuilder> factory = LatexlikeBuilder::new;

  private LatexlikeBuilder(File input) {
    super(input);
  }

  @Override
  protected String description() {
    return "Latexlike for " + this.input.getName();
  }

  @Override
  protected File persistentPath() {
    return FileCommands.addExtension(this.input, "dep");
  }

  @Override
  protected CycleSupport getCycleSupport() {
    return BibtexLatexCycleSupport.instance;
  }

  @Override
  protected Stamper defaultStamper() {
    return FileContentStamper.instance;
  }

  @Override
  protected Out<File> build() throws Throwable {
    Out<File> replFile = requireBuild(BibtexlikeBuilder.factory, this.input);
    
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

    File outFile = FileCommands.replaceExtension(this.input, "outlike");
    require(outFile);


    String text;
    String textBefore = FileCommands.readFileAsString(input);
    if (outFile.exists()) {
      ObjectInputStream outInStream = new ObjectInputStream(new FileInputStream(outFile));
      outInStream.readObject();
      text = (String) outInStream.readObject();
      outInStream.close();

    } else {
      text = textBefore;
    }


    for (String toReplace : replacements.keySet()) {
      text = text.replaceAll(toReplace, replacements.get(toReplace));
    }

    ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(outFile));
    outStream.writeObject(textBefore);
    outStream.writeObject(text);
    outStream.close();
    provide(outFile);

    Log.log.log("LATEXLIKE", Log.CORE);

    String pdf = text.replaceAll(" ", "\n");
    File pdfFile = FileCommands.replaceExtension(input, "pdflike");
    Files.write(pdfFile.toPath(), Collections.singleton(pdf));
    provide(pdfFile);

    return Out.of(pdfFile);

  }

}
