package build.pluto.test.cli;

import static org.junit.Assert.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import build.pluto.cli.InputParser;

public class InputParserPrimitiveTest {

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
    assertEquals(empty.foo1, prim.foo1);
    assertEquals(empty.foo2, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
    assertEquals(empty.foo5, prim.foo5, 0);
    assertEquals(empty.foo6, prim.foo6, 0);
    assertEquals(empty.foo7, prim.foo7);
  }

  @Test
  public void testParse1() throws ParseException {
    Primitives prim = parse(Primitives.class, "--foo1", "100");
    assertNotNull(prim);
    Primitives empty = new Primitives();
    assertEquals(100, prim.foo1);
    assertEquals(empty.foo2, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
    assertEquals(empty.foo5, prim.foo5, 0);
    assertEquals(empty.foo6, prim.foo6, 0);
    assertEquals(empty.foo7, prim.foo7);
  }

  @Test
  public void testParse2() throws ParseException {
    Primitives prim = parse(Primitives.class, "--foo1", "100", "--foo2", "200", "--foo3", "300", "--foo4", "400", "--foo5", "1.31", "--foo6", "3.1415", "--foo7", "c");
    assertNotNull(prim);
    assertEquals(100, prim.foo1);
    assertEquals(200, prim.foo2);
    assertEquals(300, prim.foo3);
    assertEquals(400, prim.foo4);
    assertEquals(1.31, prim.foo5, 0.01);
    assertEquals(3.1415, prim.foo6, 0);
    assertEquals('c', prim.foo7);
  }

  @Test
  public void testParse3() throws ParseException {
    Primitives prim = parse(Primitives.class, "--foo6", "3.1415", "--foo7", "c", "--foo3", "300", "--foo4", "400", "--foo1", "100", "--foo2", "200", "--foo5", "1.31");
    assertNotNull(prim);
    assertEquals(100, prim.foo1);
    assertEquals(200, prim.foo2);
    assertEquals(300, prim.foo3);
    assertEquals(400, prim.foo4);
    assertEquals(1.31, prim.foo5, 0.01);
    assertEquals(3.1415, prim.foo6, 0);
    assertEquals('c', prim.foo7);
  }
  
  @Test
  public void testParseError1() throws ParseException {
    try {
      parse(Primitives.class, "--foo1", "a");
    } catch (NumberFormatException e) {
      return;
    }
    fail("Parsing should have failed");
  }
  
  @Test
  public void testParseError2() throws ParseException {
    try {
      parse(Primitives.class, "--foo1", "1000");
    } catch (NumberFormatException e) {
      return;
    }
    fail("Parsing should have failed");
  }
  
  @Test
  public void testParseError3() throws ParseException {
    try {
      parse(Primitives.class, "--fooX", "1");
    } catch (UnrecognizedOptionException e) {
      return;
    }
    fail("Parsing should have failed");
  }
  
  @Test
  public void testParseError4() throws ParseException {
    try {
      parse(Primitives.class, "--foo1", "0", "--foo1", "1");
    } catch (IllegalArgumentException e) {
      return;
    }
    fail("Parsing should have failed");
  }
}
