/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 socraticphoenix@gmail.com
 * Copyright (c) 2017 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.gmail.socraticphoenix.shnap.parse.ShnapParser;
import com.gmail.socraticphoenix.shnap.run.compiler.ShnapDefaultHandlers;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompileVersusParseTest {
    /*
    Machine:
        Intel i7-7500U @ 2.7 Ghz
        12 GB RAM
        Windows 10
    Environment:
        Run in IntelliJ
    Results:
        500 lines:
            Took (on average): 1277.7 to parse
            Took (on average): 3.4 to compile
            Took (on average): 12.1 to write
            Took (on average): 85.5 to read
        5000 lines:
            Took (on average): 100346.4 to parse
            Took (on average): 18.9 to compile
            Took (on average): 8.8 to write
            Took (on average): 3037.2 to read

     */

    public static void main(String[] args) throws IOException {
        ShnapDefaultHandlers.registerDefaults();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            builder.append("native::sys.print((5 + 2 - 3 + (1 * 5 << 2)) + \"\\n\")" + System.lineSeparator());
        }

        String content = builder.toString();

        for (int i = 0; i < 10; i++) {
            ShnapScript script = new ShnapScript("false", "fake");
            script.setContent(() -> content);
            ShnapParser parser = new ShnapParser(builder.toString(), script);

            timeBegin();
            script.setVal(parser.parseAll());
            timeEnd("to parse");

            timeBegin();
            byte[] compiled = script.compile();
            timeEnd("to compile");

            Path p = Paths.get("bigTestCompiled.cshnap");
            Files.deleteIfExists(p);

            timeBegin();
            Files.write(p, compiled);
            timeEnd("to write");

            timeBegin();
            ShnapScript.read(() -> Files.newInputStream(p), "bigTestCompiled", "bigTestCompiled.shnap", "cshnap");
            timeEnd("to read");

            System.out.println("---------------------------------------------------------");
        }

        programEnd();
    }

    private static long millis = 0;
    private static Map<String, List<Long>> times = new LinkedHashMap<>();

    private static void timeBegin() {
        millis = System.currentTimeMillis();
    }

    private static void timeEnd(String action) {
        long time = System.currentTimeMillis() - millis;
        System.out.println("Took: " + time + "  " + action);
        times.computeIfAbsent(action, k -> new ArrayList<>()).add(time);
    }

    private static void programEnd() {
        for (Map.Entry<String, List<Long>> time : times.entrySet()) {
            System.out.println("Took (on average): " + time.getValue().stream().mapToLong(v -> v).average().getAsDouble() + " " + time.getKey());

        }
    }

}
