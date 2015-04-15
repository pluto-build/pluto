package build.pluto.test.cli;

import static org.junit.Assert.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import build.pluto.cli.InputParser;

public class InputParserTest {

  @Rule
  public TestName name = new TestName();
  
  protected Options options;
  protected CommandLineParser parser;
  @Before
  public void setUp() {
    options = new Options();
    parser = new BasicParser();
  }
  
  protected <T> T parse(Class<T> cl, String... args) throws ParseException {
    InputParser<T> p = new InputParser<T>(cl);
    p.registerOptions(options);
    CommandLine line = parser.parse(options, args);
    return p.parseCommandLine(line);
  }
  
  public static class Primitives {
    public Primitives() {
    }
    public Primitives(byte foo1, short foo2, int foo3, long foo4, float foo5, double foo6, char foo7) {
      this.foo1 = foo1;
      this.foo2 = foo2;
      this.foo3 = foo3;
      this.foo4 = foo4;
      this.foo5 = foo5;
      this.foo6 = foo6;
      this.foo7 = foo7;
    }
    private byte foo1;
    private short foo2;
    private int foo3;
    private long foo4;
    private float foo5;
    private double foo6;
    private char foo7;
  }
  
  @Test
  public void testRegisterOptions() throws ParseException {
    Options options = new Options();
    InputParser<Primitives> p = new InputParser<>(Primitives.class);
    p.registerOptions(options);
    assertEquals(options.getOptions().size(), 7);
  }
  
  @Test
  public void testEmptyInput() throws ParseException {
    Primitives prim = parse(Primitives.class);
    assertNotNull(prim);
    Primitives empty = new Primitives();
    assertEquals(prim.foo1, empty.foo1);
    assertEquals(prim.foo2, empty.foo2);
    assertEquals(prim.foo3, empty.foo3);
    assertEquals(prim.foo4, empty.foo4);
    assertEquals(prim.foo5, empty.foo5, 0);
    assertEquals(prim.foo6, empty.foo6, 0);
    assertEquals(prim.foo7, empty.foo7);
  }


}
