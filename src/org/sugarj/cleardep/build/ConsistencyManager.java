package org.sugarj.cleardep.build;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;

public class ConsistencyManager {

  private Set<BuildUnit<?>> consistentUnits;

  private Set<BuildUnit<?>> checkingUnits;

  private Map<BuildUnit<?>, Set<BuildUnit<?>>> uncheckedCyclicDepencies;
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

  public boolean canCheckUnit(BuildUnit<?> current, BuildRequirement<?> next) {
    if (this.checkingUnits.contains(next.unit)) {
      Set<BuildUnit<?>> discoveredCyclicDependencies = uncheckedCyclicDepencies.get(next);
      if (discoveredCyclicDependencies == null) {
        discoveredCyclicDependencies = new HashSet<>();
        uncheckedCyclicDepencies.put(next.unit, discoveredCyclicDependencies);
      }
      discoveredCyclicDependencies.add(current);
      rememberedRequirements.put(next.unit, next);

      return false;
    } else {
      return true;
    }
  }

  public Set<BuildRequirement<?>> stopCheckProgress(BuildUnit<?> unit, boolean isConsistent) {
    this.checkingUnits.remove(unit);
    if (isConsistent) {
      consistentUnits.add(unit);
    }

    Set<BuildRequirement<?>> inconsistentMissingUnits = null;
    Iterator<Entry<BuildUnit<?>, Set<BuildUnit<?>>>> dependenciesIter = this.uncheckedCyclicDepencies.entrySet().iterator();
    while (dependenciesIter.hasNext()) {
      Entry<BuildUnit<?>, Set<BuildUnit<?>>> dependencies = dependenciesIter.next();
      boolean hasContained = dependencies.getValue().remove(unit);

      if (hasContained) {
        if (!isConsistent) {
          if (inconsistentMissingUnits == null) {
            inconsistentMissingUnits = new HashSet<>();
          }
          inconsistentMissingUnits.add(rememberedRequirements.get(dependencies.getKey()));
        }
        if (!isConsistent || dependencies.getValue().isEmpty()) {
          dependenciesIter.remove();
        }
      }
    }

    if (inconsistentMissingUnits == null) {
      return Collections.emptySet();
    } else {
      return inconsistentMissingUnits;
    }
  }

}
