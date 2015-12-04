package build.pluto.dependency;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import build.pluto.builder.BuildRequest;

/**
 * Collection of BuildRequests from which other files originate.
 * 
 * @author seba
 */
public class Origin implements Serializable {

  private static final long serialVersionUID = 4754106394706480006L;
  
  private final Set<? extends BuildRequest<?, ?, ?, ?>> reqs;
  
  public Origin(Set<? extends BuildRequest<?, ?, ?, ?>> reqs) {
    if (reqs.isEmpty())
      throw new IllegalArgumentException("Origin requirements must be non-empty. Use a null object instead of the empty Origin.");
    this.reqs = Collections.unmodifiableSet(reqs);
  }
  
  public Collection<? extends BuildRequest<?, ?, ?, ?>> getReqs() {
    return reqs;
  }
  
  @Override
  public int hashCode() {
    return reqs.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    return other == null || other instanceof Origin && ((Origin) other).reqs.equals(reqs);
  }
  
  @Override
  public String toString() {
    return "Origin(" + reqs.toString() + ")";
  }
  
  public static Origin from(BuildRequest<?, ?, ?, ?>... reqs) {
    return Builder().add(reqs).get();
  }
  
  public static Builder Builder() { return new Builder(); }
  public static class Builder {
    private Set<BuildRequest<?, ?, ?, ?>> reqs = new TreeSet<>();
    private Origin result = null;
    
    public Origin get() {
      if (result != null)
        return result;
      if (reqs.isEmpty())
        return null;
      return new Origin(reqs);
    }
    
    public Builder add(BuildRequest<?, ?, ?, ?>... reqs) {
      result = null;
      for (BuildRequest<?, ?, ?, ?> req : reqs)
        this.reqs.add(req);
      return this;
    }
    
    public Builder from(Origin origin) {
      result = null;
      if (origin != null)
        this.reqs.addAll(origin.reqs);
      return this;
    }
  }
}
