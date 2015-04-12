package org.sugarj.cleardep.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UniteSets<T> {
  
  public class Key {
  }
  
  private Map<Key, Set<T>> repSetsMap = new HashMap<>();
  private Map<T, Key> setMembership = new HashMap<>();
  
  private Set<T> getSet(Key key) {
    return repSetsMap.get(key);
  }
  
  public Key createSet(T initial) {
    Key key = new Key();
    repSetsMap.put(key, new HashSet<T>());
    addToSet(key, initial);
    return key;
  }
  
  public Key getSet(T elem) {
    return setMembership.get(elem);
  }
  
  public Set<T> getSetMembers(Key key) {
    return getSet(key);
  }
  
  public void addToSet(Key setKey, T newElem) {
    assert getSet(newElem) == null : "Cannot add an element to the set because the element is also a member of another set";
    Set<T> set = getSet(setKey);
    set.add(newElem);
    setMembership.put(newElem, setKey);
  }
  
  public Key getOrCreateSet(T elem) {
    Key key = getSet(elem);
    if (key == null) {
      key = createSet(elem);
    }
    return key;
  }
  
  public Key uniteOrAdd(Key key, T elem) {
    Key key2 = getSet(elem);
    if (key2 == null) {
      addToSet(key, elem);
      return key;
    } else {
      return unite(key, key2);
    }
  }
  
  public Key unite(Key key1, Key key2) {
    if (key1 == key2)
      return key1;
    
    Set<T> set1 = getSet(key1);
    Set<T> set2 = getSet(key2);
    // Choose key for larger set to remain
    final Key remain;
    final Key remove;
    if (set1.size() < set2.size()) {
      remain = key2;
      remove = key1;
    } else {
      remain = key2;
      remove = key1;
    }
    
    // Move elements from one elem to other
    final Set<T> destSet =getSet(remain);
    final Set<T> srcSet = getSet(remove);
    destSet.addAll(srcSet);
    
    for (T elem : srcSet) {
      setMembership.put(elem, remain);
    }
    repSetsMap.remove(remove);
    
    return remain;
  }

}
