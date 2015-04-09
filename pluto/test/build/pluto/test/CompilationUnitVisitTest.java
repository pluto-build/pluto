package build.pluto.test;

import static build.pluto.test.CompilationUnitTestUtils.makeEdgeFrom;
import static build.pluto.test.CompilationUnitTestUtils.makeNode;
import static build.pluto.test.CompilationUnitTestUtils.set;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import build.pluto.BuildUnit;
import build.pluto.test.CompilationUnitTestUtils.NodeOutput;

public class CompilationUnitVisitTest {
	
	BuildUnit.ModuleVisitor<List<BuildUnit<NodeOutput>>> collectVisitedNodesVisitor = new BuildUnit.ModuleVisitor<List<BuildUnit<NodeOutput>>> () {

		@Override
		public List<BuildUnit<NodeOutput>> visit(BuildUnit<?> mod) {
			List<BuildUnit<NodeOutput>> singleton = new ArrayList<>();
			singleton.add((BuildUnit<NodeOutput>) mod);
			return singleton;
		}

		@Override
		public List<BuildUnit<NodeOutput>> combine(List<BuildUnit<NodeOutput>> t1, List<BuildUnit<NodeOutput>> t2) {
			t1.addAll(t2);
			return t1;
		}

		@Override
		public List<BuildUnit<NodeOutput>> init() {
			return new ArrayList<>();
		}

		@Override
		public boolean cancel(List<BuildUnit<NodeOutput>> t) {
			return false;
		}
		
	};
	
	@Test
	public void testVisitTreeGraph() {
		BuildUnit<NodeOutput> root = makeNode("root");
		
		BuildUnit<NodeOutput> c1 = makeNode("c1");
		makeEdgeFrom(root).to(c1);
		BuildUnit<NodeOutput> c11 = makeNode("c11");
		BuildUnit<NodeOutput> c12 = makeNode("c12");
		makeEdgeFrom(c1).to(c11).and(c12);
		
		BuildUnit<NodeOutput> c2 = makeNode("c2");
		makeEdgeFrom(root).to(c2);
		
		
		List<BuildUnit<NodeOutput>> visitedUnits = root.visit(collectVisitedNodesVisitor);
		
		//Check that we did visit the correct number if modules
		assertEquals("Visited wrong number of visited nodes", 5, visitedUnits.size());
		
		// Check that all units has been visited
		assertEquals("Visited not all units", set(root, c1, c11, c12, c2), set(visitedUnits));
	}
	
	@Test
	public void testVisitDAGGraph() {
		BuildUnit<NodeOutput> root1 = makeNode("root1");
		BuildUnit<NodeOutput> root2 = makeNode("root2");
		
		BuildUnit<NodeOutput> cx1 = makeNode("cx1");
		makeEdgeFrom(root1).and(root2).to(cx1);
		BuildUnit<NodeOutput> cx11 = makeNode("cx11");
		BuildUnit<NodeOutput> cx12 = makeNode("cx12");
		makeEdgeFrom(cx1).to(cx11).and(cx12);
		BuildUnit<NodeOutput> cx1x1 = makeNode("cx1x1");
		BuildUnit<NodeOutput> cx121 = makeNode("cx121");
		makeEdgeFrom(cx11).and(cx12).to(cx1x1);
		makeEdgeFrom(cx12).to(cx121);
		
		BuildUnit<NodeOutput> c21 = makeNode("c21");
		makeEdgeFrom(root2).to(c21);
		
		List<BuildUnit<NodeOutput>> visitedUnitsRoot1 = root1.visit(collectVisitedNodesVisitor);
		assertEquals("Wrong number of visited nodes from root1", 6, visitedUnitsRoot1.size());
		assertEquals("Wrong visited nodes from root1", set(root1, cx1, cx11, cx12, cx1x1, cx121), set(visitedUnitsRoot1));
		
		List<BuildUnit<NodeOutput>> visitedUnitsRoot2 = root2.visit(collectVisitedNodesVisitor);
		assertEquals("Wrong number of visited nodes from root2", 7, visitedUnitsRoot2.size());
		assertEquals("Wrong visited nodes from root2", set(root2,  cx1, cx11,cx12,cx1x1,cx121,c21), set(visitedUnitsRoot2));
		
	}
	
	@Test
	public void testVisitCycleGraph() {
		BuildUnit<NodeOutput> n1 = makeNode("n1");
		BuildUnit<NodeOutput> n2 = makeNode("n2");
		makeEdgeFrom(n1).to(n2);
		makeEdgeFrom(n2).to(n1);
		
		List<BuildUnit<NodeOutput>> visitedUnitsN1 = n1.visit(collectVisitedNodesVisitor);
		List<BuildUnit<NodeOutput>> visitedUnitsN2 = n2.visit(collectVisitedNodesVisitor);
		assertEquals("Wrong number of visited nodes from n1", 2, visitedUnitsN1.size());
		assertEquals("Wrong visited nodes from n1", set(n1, n2), set(visitedUnitsN1));
		assertEquals("Wrong number of visited nodes from n2", 2, visitedUnitsN2.size());
		assertEquals("Wrong visited nodes from n2", set(n1, n2), set(visitedUnitsN2));
		
		BuildUnit<NodeOutput> root = makeNode("root");
		makeEdgeFrom(root).to(n1);
		List<BuildUnit<NodeOutput>> visitedUnitsRoot = root.visit(collectVisitedNodesVisitor);
		assertEquals("Wrong number of visited nodes from root", 3, visitedUnitsRoot.size());
		assertEquals("Wrong visited nodes from root", set(n1, n2, root), set(visitedUnitsRoot));
	}

}
