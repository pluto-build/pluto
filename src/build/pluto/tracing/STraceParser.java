package build.pluto.tracing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by manuel on 9/6/16.
 */
public class STraceParser {

    private static String MATCH_PATTERN = ".* (.*) open\\(\"(.*)\",(.*)\\) = (.*)";
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

            return new FileDependency(mode, file);
        }

        return null;
    }

}
