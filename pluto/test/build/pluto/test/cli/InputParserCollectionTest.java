package build.pluto.test.cli;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

import build.pluto.cli.InputParser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class InputParserCollectionTest {

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
  
  public static class Collections {
    public Collections() {
    }
    public Collections(int[] foo1, Path[] foo2, ArrayList<Integer> foo3, Set<Path> foo4) {
      this.foo1 = foo1;
      this.foo2 = foo2;
      this.foo3 = foo3;
      this.foo4 = foo4;
    }
    private int[] foo1;
    private Path[] foo2;
    private ArrayList<Integer> foo3;
    private Set<Path> foo4;
  }
  
  @Test
  public void testRegisterOptions() throws ParseException {
    Options options = new Options();
    InputParser<Collections> p = new InputParser<>(Collections.class);
    p.registerOptions(options);
    assertEquals(options.getOptions().size(), 4);
  }
  
  @Test
  public void testEmptyInput() throws ParseException {
    Collections prim = parse(Collections.class);
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(empty.foo1, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }

  @Test
  public void testParse1() throws ParseException {
    Collections prim = parse(Collections.class, "--foo1", "101");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(new int[]{101}, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }
  
  @Test
  public void testParse2() throws ParseException {
    Collections prim = parse(Collections.class, "--foo1", "101", "--foo1", "102");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(new int[]{101, 102}, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }
  
  @Test
  public void testParse3() throws ParseException {
    Collections prim = parse(Collections.class, "--foo1", "101", "--foo1", "102", "--foo2", "/tmp/foobar");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(new int[]{101, 102}, prim.foo1);
    assertArrayEquals(new Path[]{new AbsolutePath("/tmp/foobar")}, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }
  
  @Test
  public void testParse4() throws ParseException {
    Collections prim = parse(Collections.class, "--foo1", "101", "--foo2", "/tmp/foobar", "--foo1", "102", "--foo2", "/tmp/foobar2");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(new int[]{101, 102}, prim.foo1);
    assertArrayEquals(new Path[]{new AbsolutePath("/tmp/foobar"), new AbsolutePath("/tmp/foobar2")}, prim.foo2);
    assertEquals(empty.foo3, prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }
  
  @Test
  public void testParse5() throws ParseException {
    Collections prim = parse(Collections.class, "--foo3", "101");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(empty.foo1, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(Lists.newArrayList(101), prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }
  
  @Test
  public void testParse6() throws ParseException {
    Collections prim = parse(Collections.class, "--foo3", "101", "--foo3", "1020");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(empty.foo1, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(Lists.newArrayList(101, 1020), prim.foo3);
    assertEquals(empty.foo4, prim.foo4);
  }
  
  @Test
  public void testParse7() throws ParseException {
    Collections prim = parse(Collections.class, "--foo3", "101", "--foo3", "1020", "--foo4", "/tmp/foobar");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(empty.foo1, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(Lists.newArrayList(101, 1020), prim.foo3);
    assertEquals(Sets.newHashSet(new AbsolutePath("/tmp/foobar")), prim.foo4);
  }
  
  @Test
  public void testParse8() throws ParseException {
    Collections prim = parse(Collections.class, "--foo3", "101", "--foo4", "/tmp/foobar", "--foo3", "1020", "--foo4", "/tmp/foobar2");
    assertNotNull(prim);
    Collections empty = new Collections();
    assertArrayEquals(empty.foo1, prim.foo1);
    assertArrayEquals(empty.foo2, prim.foo2);
    assertEquals(Lists.newArrayList(101, 1020), prim.foo3);
    assertEquals(Sets.newHashSet(new AbsolutePath("/tmp/foobar"), new AbsolutePath("/tmp/foobar2")), prim.foo4);
  }
}
