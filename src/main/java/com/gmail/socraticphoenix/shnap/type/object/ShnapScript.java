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
package com.gmail.socraticphoenix.shnap.type.object;

import com.gmail.socraticphoenix.pio.ByteStream;
import com.gmail.socraticphoenix.pio.Bytes;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.run.compiler.DangerousSupplier;
import com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.run.env.ShnapTraceback;
import com.gmail.socraticphoenix.shnap.parse.ShnapParser;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstructionSequence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class ShnapScript extends ShnapObject {
    public static final ShnapScript BUILTINS = new ShnapScript("<native code>", "<native code>");

    private DangerousSupplier<String> content;

    private String name;
    private String fileName;
    private ShnapInstruction val;
    private ShnapExecution initExec;
    public boolean initialized;

    public ShnapScript(String name, String fileName) {
        super(null);
        this.loc = new ShnapLoc(0, 0, this);
        this.name = name;
        this.fileName = fileName;
        this.content = () -> "<native code>";
    }

    public ShnapExecution getInitExec() {
        return initExec;
    }

    @Override
    public String defaultToString() {
        return "script[" + this.name + "]::" + this.identityStr();
    }

    public String getContent() throws IOException {
        return content.get();
    }

    public void setContent(DangerousSupplier<String> content) {
        this.content = content;
    }

    public static ShnapScript read(DangerousSupplier<InputStream> streamMaker, String name, String fileName, String format) throws IOException {
        DangerousSupplier<byte[]> bytes = () -> {
            try (InputStream content = streamMaker.get(); ByteArrayOutputStream res = new ByteArrayOutputStream();) {
                byte[] buff = new byte[1024];
                int len;
                while ((len = content.read(buff)) != -1) {
                    res.write(buff, 0, len);
                }

                return res.toByteArray();
            }
        };

        ShnapScript script = new ShnapScript(name, fileName);
        if (format.equalsIgnoreCase("shnap")) {
            DangerousSupplier<String> content = () -> new String(bytes.get(), StandardCharsets.UTF_8);
            script.setContent(content);
            ShnapParser parser = new ShnapParser(content.get(), script);
            script.setVal(parser.parseAll());
        } else if (format.equalsIgnoreCase("cshnap")) {
            DangerousSupplier<String> content = () -> {
                ByteStream stream = null;
                try {
                    stream = ByteStream.of(bytes.get());
                    return Bytes.readString(stream);
                } finally {
                    if(stream != null) {
                        stream.close();
                    }
                }
            };
            script.setContent(content);
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes.get());
            int contLen = byteBuffer.getInt();
            byteBuffer.position(byteBuffer.position() + contLen);
            ShnapScript.readCompiled(ByteStream.of(byteBuffer), script);
        }

        return script;
    }

    public Path getCompileDstPath(Path overlord) {
        String[] pieces = this.name.split(Pattern.quote("."));
        pieces[pieces.length - 1] += ".cshnap";
        return overlord.resolve(Paths.get("", pieces));
    }

    public String deCompile() {
        if (this.val instanceof ShnapInstructionSequence) {
            StringBuilder builder = new StringBuilder();
            for (ShnapInstruction instruction : ((ShnapInstructionSequence) this.val).getSequence()) {
                builder.append(instruction.decompile(1)).append(System.lineSeparator());
            }
            return builder.toString();
        } else {
            return this.val.decompile(1);
        }
    }

    public byte[] compile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteStream stream = ByteStream.of(out);
        Bytes.writeString(stream, this.content.get());
        try {
            ShnapCompilerUtil.write(stream, this.val);
        } catch (IOException e) {
            throw new IllegalStateException("ByteArrayOutputStream threw IOException", e);
        }
        return out.toByteArray();
    }

    public static ShnapScript readCompiled(ByteStream stream, ShnapScript building) throws IOException {
        building.setVal(ShnapCompilerUtil.read(stream, building));
        return building;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void importBuiltinsTo(ShnapContext context) {
        context.set(this.name, this);
        context.setFlag(this.name, ShnapContext.Flag.DONT_IMPORT);
        for (String name : this.context.names()) {
            if (!this.context.hasFlag(name, ShnapContext.Flag.DONT_IMPORT) && !this.context.hasFlag(name, ShnapContext.Flag.PRIVATE)) {
                if(!context.hasFlag(name, ShnapContext.Flag.FINALIZED)) {
                    context.set(name, this.context.getExactly(name));
                    context.setFlag(name, ShnapContext.Flag.DONT_IMPORT);
                }
            }
        }
    }

    public ShnapExecution initScript(ShnapEnvironment tracer) {
        tracer.applyDefaults(this);
        this.setInitialized(true);
        tracer.pushTraceback(ShnapTraceback.frame(new ShnapLoc(0, 0, this), "Init " + this.defaultToString()));
        ShnapExecution ex = this.val.exec(this.context, tracer);
        if (!ex.isAbnormal()) {
            tracer.popTraceback();
        }
        this.initExec = ex;
        return ex;
    }

    public ShnapExecution runMain(ShnapEnvironment tracer) {
        if (!this.initialized) {
            ShnapExecution e = this.initScript(tracer);
            if (e.isAbnormal()) {
                return e;
            }
        } else if (this.initExec.isAbnormal()) {
            return this.initExec;
        }

        return this.get("main", tracer).mapIfNormal(e -> {
            ShnapObject main = e.getValue();
            if(main instanceof ShnapFunction && ((ShnapFunction) main).paramSizeId() == 0) {
                return ((ShnapFunction) main).invoke(tracer);
            }
            return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
        });
    }

    public String getName() {
        return this.name;
    }

    public ShnapScript setName(String name) {
        this.name = name;
        return this;
    }

    public ShnapInstruction getVal() {
        return this.val;
    }

    public ShnapScript setVal(ShnapInstruction val) {
        this.val = val;
        return this;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public ShnapScript setInitialized(boolean initialized) {
        this.initialized = initialized;
        return this;
    }

    public ShnapInstruction getBody() {
        return this.val;
    }

}
