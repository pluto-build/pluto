package build.pluto.tracing;

import org.fusesource.jansi.Ansi;
import org.sugarj.common.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by manuel on 9/6/16.
 */
public class STraceParser {

    private static String MATCH_PATTERN = ".* (.*) open\\(\"(.*)\",(.*)\\) = (.*)";
    private static String MATCH_PATTERN_PARTIAL = ".* (.*) open\\(\"(.*)\",(.*) \\<unfinished \\.\\.\\.\\>";
    private String[] lines;

    public STraceParser(String[] lines) {
        this.lines = lines;
    }

    public List<FileDependency> readDependencies() {
        List<FileDependency> deps = new ArrayList<>();

        for (String line : lines) {
            FileDependency dependency = tryParseLine(line);
            if (dependency != null)
                deps.add(dependency);
        }

        return deps;
    }

    private FileDependency tryParseLine(String line) {
        if (line == null)
            return null;

        Pattern r = Pattern.compile(MATCH_PATTERN);

        Matcher m = r.matcher(line);
        if (m.find()) {
            FileAccessMode mode = null;
            if (m.group(3).contains("O_RDONLY")) mode = FileAccessMode.READ_MODE;
            if (m.group(3).contains("O_WRONLY")) mode = FileAccessMode.WRITE_MODE;
            if (m.group(3).contains("O_RDWR")) mode = FileAccessMode.WRITE_MODE;

            File file = new File(m.group(2));

            long mils = (long) (Float.parseFloat(m.group(1)) * 1000);

            FileDependency dep = new FileDependency(mode, file, new Date(mils));
            if (m.group(4).contains("ENO"))
                dep.setFileExisted(false);


            return dep;
        } else {
            Pattern rp = Pattern.compile(MATCH_PATTERN_PARTIAL);

            Matcher mp = rp.matcher(line);

            if (mp.find()) {
                FileAccessMode mode = null;
                if (mp.group(3).contains("O_RDONLY")) mode = FileAccessMode.READ_MODE;
                if (mp.group(3).contains("O_WRONLY")) mode = FileAccessMode.WRITE_MODE;
                if (mp.group(3).contains("O_RDWR")) mode = FileAccessMode.WRITE_MODE;

                File file = new File(mp.group(2));

                long mils = (long) (Float.parseFloat(mp.group(1)) * 1000);

                FileDependency dep = new FileDependency(mode, file, new Date(mils));

                // TODO: This is not accurate...
                dep.setFileExisted(true);

                Log.log.log("[STRACE] Found partial match: " + line, Log.DETAIL, Ansi.Color.RED);

                return dep;
            } else if (!line.isEmpty()) {
                Log.log.log("[STRACE] Not parsed: " + line, Log.DETAIL, Ansi.Color.YELLOW);
            }
        }

        return null;
    }

}
