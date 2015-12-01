package build.pluto.builder.bulk;

import java.io.File;
import java.io.Serializable;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.bulk.BetterBulkBuilder.BulkOutput;
import build.pluto.output.Output;

public class BetterBulkSubUnitBuilder<SubIn extends Serializable, SubOut extends Output> extends Builder<SubIn, SubOut> {

  public static <SubIn extends Serializable, SubOut extends Output> BuilderFactory<SubIn, SubOut, BetterBulkSubUnitBuilder<SubIn, SubOut>> 
      factory(final BuildRequest<?, BulkOutput<SubIn, SubOut>, ?, ?> bulkRequest) {
    return new BuilderFactory<SubIn, SubOut, BetterBulkSubUnitBuilder<SubIn,SubOut>>() {
      private static final long serialVersionUID = -8747965052789010750L;

      @Override
      public BetterBulkSubUnitBuilder<SubIn, SubOut> makeBuilder(SubIn input) {
        return new BetterBulkSubUnitBuilder<SubIn, SubOut>(input, bulkRequest);
      }
    };
  }
  
  private final BuildRequest<?, BulkOutput<SubIn, SubOut>, ?, ?> bulkRequest;
  
  public BetterBulkSubUnitBuilder(SubIn input, BuildRequest<?, BulkOutput<SubIn, SubOut>, ?, ?> bulkRequest) {
    super(input);
    this.bulkRequest = bulkRequest;
  }

  @Override
  protected String description(SubIn input) {
    return null;
  }

  @Override
  public File persistentPath(SubIn input) {
    return null;
  }

  @Override
  protected SubOut build(SubIn input) throws Throwable {
    BulkOutput<SubIn, SubOut> bulkOut = requireBuild(bulkRequest);
    return bulkOut.subUnits.get(input).getBuildResult();
  }

}
