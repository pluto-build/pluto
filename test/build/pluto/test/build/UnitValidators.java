package build.pluto.test.build;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;

public class UnitValidators {

  public interface Predicate<T> {
    public boolean test(T t);
  }
  
	public static class UnitValiatorBuilder {
		private Set<BuildUnit<?>> units;

		public UnitValiatorBuilder(BuildUnit<?>... units) {
			super();
			this.units = new HashSet<>(Arrays.asList(units));
		}

		public Validators.Validator dependsOn(final File... others) {
			return new Validators.Validator() {

				@Override
				public void validate() {
					for (BuildUnit<?> unit : units) {
						for (File other : others) {
							try {
							assertTrue(
									"Unit" + unit.getPersistentPath()
											+ " does not depends on "
											+ other,
									unit.dependsOn(BuildUnit.read(other)));
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
					}
				}

			};
		}
		
		public Validators.Validator areConsistent() {
			return new Validators.Validator() {
				
				@Override
				public void validate() {
					for (BuildUnit<?> unit : units) {
            InconsistenyReason locallyReason = unit.isConsistentShallowReason();
            assertTrue(unit.getPersistentPath() + " is not consistent sallowly because " + locallyReason, locallyReason == InconsistenyReason.NO_REASON);
            assertTrue(unit.getPersistentPath() + " is not consistent", unit.isConsistent());
					}
				}
			};
		}

    public Validators.Validator fullfills(final Predicate<BuildUnit<?>> pred) {
      return new Validators.Validator() {

        @Override
        public void validate() {
          for (BuildUnit<?> unit : units) {
            assertTrue("Build unit " + unit.getPersistentPath() + " does not fullfill predicate", pred.test(unit));
          }
        }
      };
    }

	}

	public static UnitValiatorBuilder unitsForPath(Path... depPath)
			throws IOException {
		BuildUnit<?>[] units = new BuildUnit[depPath.length];
		for (int i = 0; i < depPath.length; i++) {
			units[i] = BuildUnit.read(depPath[i].toFile());
 		}
		return new UnitValiatorBuilder(units);
	}
	
	public static UnitValiatorBuilder unitsForPath(File... depPath)
      throws IOException {
    BuildUnit<?>[] units = new BuildUnit[depPath.length];
    for (int i = 0; i < depPath.length; i++) {
      units[i] = BuildUnit.read(depPath[i]);
    }
    return new UnitValiatorBuilder(units);
  }
}
