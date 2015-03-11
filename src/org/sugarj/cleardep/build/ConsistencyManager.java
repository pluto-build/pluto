package org.sugarj.cleardep.build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;

public class ConsistencyManager {

  /**
   * Holds all {@link BuildUnit}s for which the manager is told, that they are
   * consistent
   */
  private Set<BuildUnit<?>> consistentUnits;

  /**
   * Holds all {@link BuildUnit} which are currently checked for consistency but
   * have not finished yet (because of recursive checks to dependencies)
   */
  private Set<BuildUnit<?>> checkingUnits;

  /**
   * CycleSupport: Maps from a {@link BuildUnit} u to a set of all
   * {@link BuildUnit}s S that tried to check the consistency of u recursively
   * but could not because of a cycle. That means that after the consistency of
   * u is checked, all units in S are influenced by the result of u.
   */
  private Map<BuildUnit<?>, Set<BuildUnit<?>>> uncheckedCyclicDepencies;

  /**
   * Helper map to remember from which {@link BuildRequirement} a given
   * {@link BuildUnit} was
   */
  private Map<BuildUnit<?>, BuildRequirement<?>> rememberedRequirements;

  public ConsistencyManager() {
    this.consistentUnits = new HashSet<>();
    this.checkingUnits = new HashSet<>();
    this.uncheckedCyclicDepencies = new HashMap<>();
    this.rememberedRequirements = new HashMap<>();
  }

  public boolean isConsistent(BuildUnit<?> unit) {
    return consistentUnits.contains(unit);
  }

  public void startCheckProgress(BuildUnit<?> unit) {
    this.checkingUnits.add(unit);
  }

  /**
   * Checks whether a consistency check for current can check the unit in next
   * recursively.
   * 
   * @param current
   *          the units which requires a recursive consistency check of another
   *          one
   * @param next
   *          the holder of the unit that should be checked
   * @return true if a recursive check is possible, false otherwise because of a
   *         cycle
   */
  public boolean canCheckUnit(BuildUnit<?> current, BuildRequirement<?> next) {
    if (this.checkingUnits.contains(next.unit)) {
      // next.unit is already in progress, so there is a cycle.
      // Need to remember that for next
      Set<BuildUnit<?>> discoveredCyclicDependencies = uncheckedCyclicDepencies.get(next);
      // Fill the map lazily (so no empty sets) because they are only needed in
      // cycles
      if (discoveredCyclicDependencies == null) {
        discoveredCyclicDependencies = new HashSet<>();
        uncheckedCyclicDepencies.put(next.unit, discoveredCyclicDependencies);
      }
      discoveredCyclicDependencies.add(current);
      // Remember the requirement to give back later
      rememberedRequirements.put(next.unit, next);

      return false;
    } else {
      return true;
    }
  }

  /**
   * Marks the given unit as finished in the consistency check progress. That
   * means, that all external checks and checks to units, which could be done
   * recursively, had been made. The unit will be marked as consistent if there
   * are no cyclic units, on which this unit depends, which are not consistent.
   * Otherwise this unit will be marked consistent, if these checks has been
   * finished
   * 
   * @param unit
   *          the unit to mark as consistent
   */
  public void stopCheckProgress(BuildUnit<?> unit) {

    // Check that there are no units that prohibit marking this unit as
    // consistent
    final Set<BuildUnit<?>> missingUnits = this.uncheckedCyclicDepencies.get(unit);
    boolean isConsistent = missingUnits == null;
    if (!isConsistent) {
      return;
    }
    
    this.checkingUnits.remove(unit);
    this.consistentUnits.add(unit);

    // Now remove unit from all sets of units on which other depends
    Iterator<Entry<BuildUnit<?>, Set<BuildUnit<?>>>> dependenciesIter = this.uncheckedCyclicDepencies.entrySet().iterator();

    Set<BuildUnit<?>> newConsistentUnits = new HashSet<>();
    while (dependenciesIter.hasNext()) {

      Entry<BuildUnit<?>, Set<BuildUnit<?>>> dependencies = dependenciesIter.next();
      // Try to remove the unit from the set
      boolean hasContained = dependencies.getValue().remove(unit);

      if (hasContained && dependencies.getValue().isEmpty()) {
        // Hey, a new unit has been finished, because it does not depend on
        // other units anymore
        // Remember it
        dependenciesIter.remove();
        newConsistentUnits.add(dependencies.getKey());
      }
    }
    // Now finish all units, which has been completed
    for (BuildUnit<?> newComplete : newConsistentUnits) {
      // This marks it as consistent and may finish more units, which depended
      // on this unit
      startCheckProgress(newComplete);
    }
  }

}
