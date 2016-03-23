package build.pluto.dependency;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.output.Output;
import build.pluto.output.OutputStamper;

/**
 * Collection of BuildRequests from which other files originate.
 * 
 * @author seba
 */
public class Origin implements Serializable {

  private static final long serialVersionUID = 4754106394706480006L;
  
  private final Set<? extends BuildRequest<?, ?, ?, ?>> reqs;
  
  private Origin(Set<? extends BuildRequest<?, ?, ?, ?>> reqs) {
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
  
  public static 
//@formatter:off
  <In_ extends Serializable, 
   Out_ extends Output, 
   B_ extends build.pluto.builder.Builder<In_, Out_>, 
   F_ extends BuilderFactory<In_, Out_, B_>, 
   SubIn_ extends In_>
//@formatter:on
  Origin from(F_ factory, In_ input) {
    return Builder().add(new BuildRequest<>(factory, input)).get();
  }

  
  public static Builder Builder() { return new Builder(); }
  public static class Builder {
    private Set<BuildRequest<?, ?, ?, ?>> reqs = new HashSet<>();
    private Origin result = null;
    
    public Origin get() {
      if (result != null)
        return result;
      if (reqs.isEmpty())
        return null;
      return new Origin(reqs);
    }
    
    public 
  //@formatter:off
    <In_ extends Serializable, 
     Out_ extends Output, 
     B_ extends build.pluto.builder.Builder<In_, Out_>, 
     F_ extends BuilderFactory<In_, Out_, B_>, 
     SubIn_ extends In_>
  //@formatter:on
    Builder add(F_ factory, In_ input) {
      add(new BuildRequest<>(factory, input));
      return this;
    }

    public 
  //@formatter:off
    <In_ extends Serializable, 
     Out_ extends Output, 
     B_ extends build.pluto.builder.Builder<In_, Out_>, 
     F_ extends BuilderFactory<In_, Out_, B_>, 
     SubIn_ extends In_>
  //@formatter:on
    Builder add(F_ factory, In_ input, OutputStamper<Out_> stamper) {
      add(new BuildRequest<>(factory, input, stamper));
      return this;
    }

    public Builder add(BuildRequest<?, ?, ?, ?>... reqs) {
      result = null;
      for (BuildRequest<?, ?, ?, ?> req : reqs)
        this.reqs.add(req);
      return this;
    }
    
    public Builder add(Origin origin) {
      result = null;
      if (origin != null)
        this.reqs.addAll(origin.reqs);
      return this;
    }
  }
}
