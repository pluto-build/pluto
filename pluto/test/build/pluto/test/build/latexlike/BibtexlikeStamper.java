package build.pluto.test.build.latexlike;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.sugarj.common.FileCommands;

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
  public ValueStamp<Set<Character>> stampOf(File p) {
    try {
      char[] inputChars = FileCommands.readFileAsString(p).toCharArray();
      Set<Character> charsToReplace = new HashSet<>();
      for (int i = 0; i < inputChars.length - 1; i++) {
        if (inputChars[i] == 'X') {
          char nextChar = inputChars[i + 1];
          charsToReplace.add(nextChar);
          i++;
        }
      }
      return new ValueStamp<>(this, charsToReplace);
    } catch (IOException e) {
      return new ValueStamp<>(this, null);
    }
  }

}
