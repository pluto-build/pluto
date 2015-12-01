package build.pluto.test.build.latexlike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.common.Log;

public class LatexlikeLog {

  public static enum CompilationParticipant {
    LATEXLIKE, BIBLIKE
  }

  private static List<CompilationParticipant> executionOrder;

  public static void cleanLog() {
    executionOrder = new ArrayList<>();
  }

  public static void logBuilderPerformedWork(CompilationParticipant work, String text) {
    Log.log.log(text, Log.ALWAYS);
    executionOrder.add(work);
  }

  public static List<CompilationParticipant> getExecutedBuilders() {
    return Collections.unmodifiableList(executionOrder);
  }

}
