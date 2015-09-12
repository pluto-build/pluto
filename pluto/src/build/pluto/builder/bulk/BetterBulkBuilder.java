package build.pluto.builder.bulk;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.FileRequirement;
import build.pluto.output.Output;

public abstract class BetterBulkBuilder<In extends Serializable, SubIn extends Serializable, SubOut extends Output>
  extends Builder<In, BetterBulkBuilder.BulkOutput<SubIn, SubOut>> {
  
  public BetterBulkBuilder(In input) {
    super(input);
  }

  public static class BulkOutput<SubIn extends Serializable, SubOut extends Output> implements Output, Iterable<SubOut> {
    private static final long serialVersionUID = 399869276147783309L;
    
    public final Map<SubIn, BuildUnit<SubOut>> subUnits;
    
    private BulkOutput(Map<SubIn, BuildUnit<SubOut>> subUnits) {
      this.subUnits = Collections.unmodifiableMap(subUnits);
    }

    @Override
    public Iterator<SubOut> iterator() {
      return new Iterator<SubOut>() {
        private Iterator<BuildUnit<SubOut>> it = subUnits.values().iterator();
        @Override
        public boolean hasNext() { return it.hasNext(); }
        @Override
        public SubOut next() { return it.next().getBuildResult(); }
        @Override
        public void remove() { throw new UnsupportedOperationException(); }
      };
    }
  }

  private Map<SubIn, BuildUnit<SubOut>> units;
  
  private BuildUnit<SubOut> unit(SubIn source) throws IOException {
    BuildUnit<SubOut> unit = units.get(source);
    if (unit != null)
      return unit;
    
    BuilderFactory<SubIn, SubOut, BetterBulkSubUnitBuilder<SubIn, SubOut>> subFactory = BetterBulkSubUnitBuilder.factory(this.getBuildUnit().getGeneratedBy());
    BuildRequest<?, SubOut, ?, ?> subRequest = new BuildRequest<>(subFactory, source);
    unit = BuildUnit.create(null, subRequest);
    return unit;
  }
  
  protected void require(SubIn source, FileRequirement req) throws IOException {
    unit(source).requireOther(req);
    if (!this.getBuildUnit().getRequiredFiles().contains(req))
      this.requireOther(req);
  }
  
  protected void require(SubIn source, SubIn other) throws IOException {
    BuildUnit<SubOut> sourceUnit = unit(source);
    BuildUnit<SubOut> otherUnit = unit(other);
    sourceUnit.requires(new BuildRequirement<>(otherUnit, otherUnit.getGeneratedBy()));
  }

  protected void provide(SubIn source, FileRequirement req) throws IOException {
    unit(source).generates(req);
  }

  protected void output(SubIn source, SubOut out) throws IOException {
    unit(source).setBuildResult(out);
  }

  protected abstract void buildInconsistentSubUnits(Collection<SubIn> inconsistent);
  
  @Override
  protected BulkOutput<SubIn, SubOut> build(In input) throws Throwable {
    if (getPreviousBuildUnit() == null) {
      buildInconsistentSubUnits(null);
    }
    else {
      Collection<SubIn> inconsistent = new HashSet<>();
      for (Entry<SubIn, BuildUnit<SubOut>> e : getPreviousBuildUnit().getBuildResult().subUnits.entrySet())
        if (!e.getValue().isConsistentShallow())
          inconsistent.add(e.getKey());
      
      buildInconsistentSubUnits(inconsistent);
    }
    return new BulkOutput<>(units);
  }
}
