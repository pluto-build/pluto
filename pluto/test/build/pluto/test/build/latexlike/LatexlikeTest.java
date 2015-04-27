package build.pluto.test.build.latexlike;

import static build.pluto.test.build.UnitValidators.unitsForPath;
import static build.pluto.test.build.Validators.validateThat;
import static build.pluto.test.build.latexlike.LatexlikeLog.CompilationParticipant.BIBLIKE;
import static build.pluto.test.build.latexlike.LatexlikeLog.CompilationParticipant.LATEXLIKE;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.Log;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.latexlike.LatexlikeLog.CompilationParticipant;

public class LatexlikeTest extends ScopedBuildTest {

  @ScopedPath("bib.biblike")
  private File bibFile;

  @ScopedPath("bib.biblike.dep")
  private File bibDep;

  @ScopedPath("doc.texlike")
  private File texFile;

  @ScopedPath("doc.texlike.dep")
  private File texDep;

  @Before
  public void cleanLog() {
    LatexlikeLog.cleanLog();
    Log.log.setLoggingLevel(Log.ALWAYS);
  }

  @Before
  public void ensureFileContents() throws IOException {
    writeTexlike("Hello X1 world X2 there X3");
    writeBiblike("1", "build", "2", "out", "3", ".");
  }

  private TrackingBuildManager build() throws IOException {
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.require(LatexlikeBuilder.factory, texFile);
    return manager;
  }

  private void assertResultCorrect() throws IOException {

    validateThat(unitsForPath(texDep).dependsOn(bibDep));
    validateThat(unitsForPath(bibDep).dependsOn(texDep));
    validateThat(unitsForPath(bibDep, texDep).areConsistent());
  }

  private void writeTexlike(String text) throws IOException {
    Files.write(texFile.toPath(), Collections.singleton(text), Charset.defaultCharset());

  }

  private void writeBiblike(String... cites) throws IOException {
    String content = "";
    assert cites.length % 2 == 0;
    for (int i = 0; i < cites.length; i = i + 2) {
      content += cites[i] + " " + cites[i + 1] + "\n";
    }
    Files.write(bibFile.toPath(), Collections.singleton(content), Charset.defaultCharset());

  }

  private void assertBuildOrder(CompilationParticipant... order) {
    assertEquals("Wrong execution order", Arrays.asList(order), LatexlikeLog.getExecutedBuilders());
  }

  @Test
  public void testCleanBuild() throws IOException {
    build();

    assertResultCorrect();

    assertBuildOrder(LATEXLIKE, BIBLIKE, LATEXLIKE, LATEXLIKE);
  }

  @Test
  public void testSimpleTextChange() throws IOException {
    build();
    cleanLog();

    System.out.println();
    System.out.println();

    // Perform a simple text change
    writeTexlike("Hello X1 earth X2 there X3");

    build();

    assertResultCorrect();

    assertBuildOrder(LATEXLIKE);

  }

  @Test
  public void testRemoveCite() throws IOException {
    build();
    cleanLog();

    System.out.println();
    System.out.println();

    // Dont cite 3 again
    writeTexlike("Hello X1 earth X2 there.");

    build();

    assertResultCorrect();

    assertBuildOrder(LATEXLIKE, BIBLIKE, LATEXLIKE, LATEXLIKE);

  }

  @Test
  public void testChangeCite() throws IOException {
    build();
    cleanLog();

    System.out.println();
    System.out.println();

    // Change a cite value
    writeBiblike("1", "build", "2", "out", "3", "!");

    build();

    assertResultCorrect();

    assertBuildOrder(BIBLIKE, LATEXLIKE, LATEXLIKE);

  }

}
