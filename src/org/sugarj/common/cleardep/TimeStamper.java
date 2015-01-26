package org.sugarj.common.cleardep;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 * 
 */
public class TimeStamper implements Stamper {

	private TimeStamper() {
	}

	public static final Stamper instance = new TimeStamper();

	/**
	 * @see org.sugarj.common.cleardep.Stamper#stampOf(org.sugarj.common.path.Path)
	 */
	@Override
	public Stamp stampOf(Path p) {
		if (!FileCommands.exists(p))

			return new TimeStamp(0l);

		return new TimeStamp(p.getFile().lastModified());
	}

	public static class TimeStamp extends SimpleStamp<Long> {
		private static final long serialVersionUID = 4063932604040295576L;

		public TimeStamp(Long t) {
			super(t);
		}

		@Override
		public boolean equals(Stamp o) {
			return o instanceof TimeStamp && super.equals(o);
		}

		@Override
		public Stamper getStamper() {
			return TimeStamper.instance;
		}

	}

}
