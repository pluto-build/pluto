package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import build.pluto.stamp.Stamper;
import build.pluto.stamp.ValueStamp;

public class BibtexlikeStamper implements Stamper {

  /**
   * 
   */
  private static final long serialVersionUID = 1658120627346161279L;
  public static final BibtexlikeStamper instance = new BibtexlikeStamper();

  private BibtexlikeStamper() {
  }

  @Override
  public ValueStamp<Set<Character>> stampOf(File outFile) {
    try {
      ObjectInputStream outStream = new ObjectInputStream(new FileInputStream(outFile));
      List<Character> replacements = (List<Character>) outStream.readObject();
      outStream.close();
      return new ValueStamp<>(this, new HashSet<>(replacements));
    } catch (IOException e) {
      return new ValueStamp<>(this, null);
    } catch (ClassNotFoundException e) {
      return new ValueStamp<>(this, null);
    }
  }

}
