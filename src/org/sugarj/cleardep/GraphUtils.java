package org.sugarj.cleardep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.sugarj.common.util.Pair;

public class GraphUtils {

  /**
   * Calculates the strongly connected components (SCC) for the graph of all
   * compilations units reachable from the given root units. The SCCs (sets of
   * compilation units) are returned in inverse topological orders of the
   * directed acyclic graph of the SCCs. That means that, iff a SCC s1 is before
   * an SCC s2 in the result list, no compilation unit in s1 depends on any
   * compilation unit in s2.
   * 
   * @param rootUnits
   *          the compilations units to start searching with
   * @return a list of the SCCs in inverse topological order
   */
  public static List<Set<CompilationUnit>> calculateStronglyConnectedComponents(Iterable<CompilationUnit> rootUnits) {
    return new TarjanAlgorithm().calculateStronglyConnectedUnits(rootUnits);
  }

  private static class TarjanAlgorithm {

    private int index;
    // Maps for assigning index and low links values to the vertices
    // (compilation units)
    private Map<CompilationUnit, Integer> unitIndices;
    private Map<CompilationUnit, Integer> unitLowLinks;

    // Result list
    private List<Set<CompilationUnit>> stronglyConnectedComponents;

    // The stack: keep units in deque for order an in a set for tests whether
    // the stack contains a unit
    // this is necessary to have O(1) tests
    private Deque<CompilationUnit> stack;
    private Set<CompilationUnit> stackUnits;

    private void stackPush(CompilationUnit unit) {
      this.stack.push(unit);
      this.stackUnits.add(unit);
    }

    private CompilationUnit stackPop() {
      CompilationUnit u = this.stack.pop();
      // It is safe just to remove u from the set because the algorithm only
      // pushes each node once on the stack
      // so there are no duplicates
      this.stackUnits.remove(u);
      return u;
    }

    private boolean stackContains(CompilationUnit u) {
      return this.stackUnits.contains(u);
    }

    // For pseudo code see e.g. here
    // https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm

    public List<Set<CompilationUnit>> calculateStronglyConnectedUnits(Iterable<CompilationUnit> units) {
      this.index = 0;
      this.stack = new LinkedList<>();
      this.stackUnits = new HashSet<CompilationUnit>();
      this.unitIndices = new HashMap<CompilationUnit, Integer>();
      this.unitLowLinks = new HashMap<CompilationUnit, Integer>();
      this.stronglyConnectedComponents = new LinkedList<>();

      for (CompilationUnit unit : units) {
        if (!this.unitIndices.containsKey(unit)) {
          this.findStrongConnectedComponent(unit);
        }
      }
      return this.stronglyConnectedComponents;
    }

    private void findStrongConnectedComponent(CompilationUnit unit) {
      this.unitIndices.put(unit, this.index);
      this.unitLowLinks.put(unit, this.index);
      this.index++;

      this.stackPush(unit);

      for (CompilationUnit dep : unit.getCircularAndNonCircularModuleDependencies()) {
        if (this.unitIndices.containsKey(dep)) {
          if (this.stackContains(dep)) {
            this.unitLowLinks.put(unit, Math.min(this.unitLowLinks.get(unit), this.unitIndices.get(dep)));
          }
        } else {
          this.findStrongConnectedComponent(dep);
          this.unitLowLinks.put(unit, Math.min(this.unitLowLinks.get(unit), this.unitLowLinks.get(dep)));
        }
      }

      if (this.unitLowLinks.get(unit) == this.unitIndices.get(unit)) {
        Set<CompilationUnit> component = new HashSet<>();
        CompilationUnit u;
        do {
          u = this.stackPop();
          component.add(u);
        } while (u != unit);
        this.stronglyConnectedComponents.add(component);
      }

    }
  }

  /**
   * Sorts all units reachable from root on the spanning DAG in topological
   * order with respect to the DAG.
   * 
   * @param root
   *          the root unit to start at
   * @return the sorted units in topological order, that means, a unit u1 before
   *         a unit u2 in the list does not depend on u2 in the spanning DAG
   */
  public static List<CompilationUnit> sortTopologicalFrom(CompilationUnit root) {
    Objects.requireNonNull(root);
    LinkedList<CompilationUnit> sorting = new LinkedList<>();

    Map<CompilationUnit, Boolean> visitedUnits = new HashMap<>();
    Deque<Pair<CompilationUnit, Integer>> stack = new ArrayDeque<>();
    stack.push(Pair.create(root, 1));

    final Boolean NEW = null;
    final Boolean PENDING = Boolean.FALSE;
    final Boolean SORTED = Boolean.TRUE;
    while (!stack.isEmpty()) {
      Pair<CompilationUnit, Integer> p = stack.peek();
      Boolean status = visitedUnits.get(p.a);
      if (status == NEW) {
        // First visit of p.a
        visitedUnits.put(p.a, PENDING);
        boolean depAdded = false;
        for (CompilationUnit dep : p.a.getModuleDependencies()) {
          Boolean depstatus = visitedUnits.get(dep);
          if (depstatus == NEW) {
            stack.push(Pair.create(dep, 1));
            depAdded = true;
          }
          else if (depstatus == SORTED) {
            // already sorted due to another dependency
          }
          else if (depstatus == PENDING) {
            // cycle
          }
        }
        // Shorten: If no dep was pushed on the stack, we can finish p.a right
        // now and do not need
        // to push it on the stack to pop it in the next iteration again
        if (!depAdded) {
          sorting.add(p.a);
          visitedUnits.put(p.a, SORTED);
          stack.pop();
        }
      }
      else if (status == PENDING) {
        // was waiting on stack until all its deps got sorted
        sorting.add(p.a);
        visitedUnits.put(p.a, SORTED);
        stack.pop();
      }
      else if (status == SORTED) {
        // already sorted due to another dependency on it
        stack.pop();
      }
    }

    assert visitedUnits.size() == sorting.size();
    assert sorting.containsAll(visitedUnits.keySet());
    assert validateTopolocialSorting(sorting) : "Topolocial sorting is not valid " + visitedUnits;
    return sorting;

  }

  private static boolean validateTopolocialSorting(List<CompilationUnit> units) {
    for (int i = 0; i < units.size() - 1; i++) {
      for (int j = i + 1; j < units.size(); j++) {
        if (units.get(i).dependsOnTransitivelyNoncircularly(units.get(j))) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Calculates the connected components of the given graph (a set of
   * compilation units. All compilation units in a connected component are
   * connected using module dependencies.
   * 
   * @param units
   *          the graph of units to partition
   * @return a set of connected components, a partition of units
   */
  public static Set<Set<CompilationUnit>> calculateConnectedComponents(Set<CompilationUnit> units) {
    Map<CompilationUnit, Set<CompilationUnit>> components = new HashMap<>();
    Map<CompilationUnit, CompilationUnit> representants = new HashMap<>();
    Queue<CompilationUnit> unitsToVisit = new LinkedList<>(units);
    for (CompilationUnit unit : units) {
      components.put(unit, new HashSet<>(Collections.singleton(unit)));
      representants.put(unit, unit);
    }

    while (!unitsToVisit.isEmpty()) {
      CompilationUnit unit = unitsToVisit.poll();
      CompilationUnit unitRep = representants.get(unit);
      Set<CompilationUnit> unitComp = components.get(unitRep);
      for (CompilationUnit dep : unit.getCircularAndNonCircularModuleDependencies()) {
        CompilationUnit depRep = representants.get(dep);
        if (depRep == null) {
          // dep is not a member of units
          continue;
        }
        if (depRep != unitRep) {
          Set<CompilationUnit> depComp = components.get(depRep);
          unitComp.addAll(depComp);
          for (CompilationUnit u : depComp) {
            representants.put(u, unitRep);
          }
          components.remove(depRep);
        }
      }
    }

    Set<Set<CompilationUnit>> componentsSet = new HashSet<>(components.values());
    assert validateConnectedComponents(componentsSet, units) : "Connected components wrong " + componentsSet;
    return componentsSet;
  }

  private static boolean validateConnectedComponents(Set<Set<CompilationUnit>> connectComps, Set<CompilationUnit> allUnits) {
    List<Set<CompilationUnit>> components = new ArrayList<>(connectComps);

    Set<CompilationUnit> allUnitsCopy = new HashSet<>(allUnits);

    for (Set<CompilationUnit> component : components) {
      allUnitsCopy.removeAll(component);
      boolean connected = component.size() <= 1;
      for (CompilationUnit u : component) {
        if (!allUnits.contains(u)) {
          System.err.println("Contains unit which is not in allUnits");
          return false;
        }
        for (CompilationUnit u2 : component) {
          if (u == u2)
            continue;
          if (dependsOnTransitivlyUsing(u, u2, component)) {
            connected = true;
            break;
          }
        }
      }
      if (!connected) {
        System.err.println("Component is not connected");
        return false;
      }
    }

    if (allUnitsCopy.size() > 0) {
      System.err.println("Partition does not cover all units");
      return false;
    }

    for (int i = 0; i < components.size() - 1; i++) {
      for (int j = i + 1; j < components.size(); j++) {
        Set<CompilationUnit> comp1 = components.get(i);
        Set<CompilationUnit> comp2 = components.get(j);
        for (CompilationUnit u1 : comp1) {
          for (CompilationUnit u2 : comp2) {
            if (u1 == u2 || dependsOnTransitivlyUsing(u1, u2, allUnits)) {
              System.err.println("Connections between components");
              return false;
            }
          }
        }
      }
    }

    return true;
  }

  /**
   * Checks whether unit depends transitivly on other using only availableUnits
   * as intermediate units on the path from unit to other.
   * 
   * @param unit
   *          the unit the start at
   * @param other
   *          the unit to check whether start depends transitivly on
   * @param availableUnits
   *          the units which
   * @return
   */
  public static boolean dependsOnTransitivlyUsing(CompilationUnit unit, CompilationUnit other, Set<CompilationUnit> availableUnits) {
    Queue<CompilationUnit> queue = new LinkedList<CompilationUnit>();
    Set<CompilationUnit> seenUnits = new HashSet<>();
    queue.add(unit);
    seenUnits.add(unit);
    while (!queue.isEmpty()) {
      CompilationUnit u = queue.poll();
      if (u == other)
        return true;
      for (CompilationUnit dep : u.getCircularAndNonCircularModuleDependencies()) {
        if (availableUnits.contains(dep) && !seenUnits.contains(dep)) {
          seenUnits.add(dep);
          queue.add(dep);
        }
      }
    }
    return false;
  }

  /**
   * Repairs the dependency graph when e.g. dependencies has been removed. Than
   * circular module dependencies maybe need to be moved to nun circular ones.
   * This cannot be done by the {@link CompilationUnit} locally.
   * 
   * @param rootUnits
   */
  public static void repairGraph(Set<CompilationUnit> rootUnits) {
    Set<CompilationUnit> allUnits = new HashSet<>();
    for (CompilationUnit unit : rootUnits) {
      allUnits.addAll(CompilationUnitUtils.findAllUnits(unit));
    }

    // Set all dependencies to non circular an then remove circular dependencies
    for (CompilationUnit unit : allUnits) {
      HashSet<CompilationUnit> circDeps = new HashSet<>(unit.getCircularModuleDependencies());
      for (CompilationUnit dep : circDeps) {
        unit.moveCircularModulDepToNonCircular(dep);
      }
    }

    Set<CompilationUnit> seenUnits = new HashSet<>();
    Set<CompilationUnit> newUnits = new HashSet<>();
    newUnits.addAll(rootUnits);

    while (!newUnits.isEmpty()) {
      CompilationUnit unit = newUnits.iterator().next();
      newUnits.remove(unit);
      seenUnits.add(unit);

      // Need to copy the depencies because we are going to modify the
      // dependencies while iterating through them
      Set<CompilationUnit> moduleDeps = new HashSet<>(unit.getModuleDependencies());
      for (CompilationUnit dep : moduleDeps) {
        if (seenUnits.contains(dep)) {
          // This dep whould close a circle
          unit.moveModuleDepToCircular(dep);
        } else {
          newUnits.add(dep);
        }
      }
    }

    // Validate the result
    assert BuildScheduleBuilder.validateDepGraphCycleFree(rootUnits) : "The repaired graph contains cycles";
    assert BuildScheduleBuilder.validateCircDepsAreCircDeps(rootUnits) : "The graph contains circular dependencies which are not circular";
  }

}
