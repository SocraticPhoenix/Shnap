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
package com.gmail.socraticphoenix.shnap.run.compiler;

import com.gmail.socraticphoenix.collect.coupling.Pair;
import com.gmail.socraticphoenix.pio.ByteStream;
import com.gmail.socraticphoenix.pio.Bytes;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.ShnapParameter;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapAbsentNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigDecimalNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigIntegerNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ShnapCompilerUtil {
    private static List<NodeHandler> indexedHandlers = new ArrayList<>();
    private static Map<Class, Pair<NodeHandler, Integer>> handlerMap = new HashMap<>();

    public static void write(ByteStream stream, ShnapInstruction instruction) throws IOException {
        if(instruction == null) {
            stream.put((byte) -1);
        } else {
            Pair<NodeHandler, Integer> handlerPair = handlerMap.get(instruction.getClass());
            if(handlerPair == null) {
                throw new IllegalArgumentException("No compilation handler for type: " + instruction.getClass());
            }
            stream.put((byte) handlerPair.getB().intValue());
            handlerPair.getA().write(stream, instruction);
        }
    }

    public static void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Stream<Path> children = Files.list(path);
            List<Path> collectedChildren = children.collect(Collectors.toList());;
            children.close();
            for(Path child : collectedChildren) {
                deleteDirectory(child);
            }
        }

        Files.deleteIfExists(path);
    }

    public static ShnapInstruction read(ByteStream stream, ShnapScript building) throws IOException {
        int index = stream.get();
        if(index == -1) {
            return null;
        }
        NodeHandler handler = indexedHandlers.get(index);
        return handler.read(stream, building);
    }

    public static void register(NodeHandler handler) {
        if(handlerMap.containsKey(handler.type())) {
            throw new IllegalArgumentException("Duplicate handler for type: " + handler.type());
        }

        int index = indexedHandlers.size();
        indexedHandlers.add(handler);
        handlerMap.put(handler.type(), Pair.of(handler, index));
    }

    public static void writeLoc(ByteStream stream, ShnapLoc loc) throws IOException {
        stream.putInt(loc.getLine());
        stream.putInt(loc.getCol());
    }

    public static ShnapLoc readLoc(ByteStream stream, ShnapScript building) throws IOException {
        return new ShnapLoc(stream.getInt(), stream.getInt(), building);
    }

    public static void writeEnum(ByteStream stream, Enum enu) throws IOException {
        byte ord = (byte) (enu == null ? -1 : enu.ordinal());
        stream.put(ord);
    }

    public static <T extends Enum> T readEnum(ByteStream stream, Class<T> enu) throws IOException {
        byte ord = stream.get();
        if(ord == -1){
            return null;
        } else {
            return enu.getEnumConstants()[ord];
        }
    }

    public static void writeParam(ByteStream stream, ShnapParameter parameter) throws IOException {
        writeLoc(stream, parameter.getLocation());
        Bytes.writeString(stream, parameter.getName());
        write(stream, parameter.getValue());
    }

    public static ShnapParameter readParam(ByteStream stream, ShnapScript building) throws IOException {
        ShnapLoc loc = readLoc(stream, building);
        String name = Bytes.readString(stream);
        return new ShnapParameter(loc, name, read(stream, building));
    }

    public static void writeNativeVal(ByteStream stream, ShnapObject object) throws IOException {
        writeLoc(stream, object.getLocation());
        if (object instanceof ShnapAbsentNative) {
            stream.put((byte) 0);
            if (object == ShnapObject.getNull()) {
                stream.put((byte) 0);
            } else if (object == ShnapObject.getVoid()) {
                stream.put((byte) 1);
            } else {
                throw new IllegalArgumentException("Unknown native: " + object);
            }
        } else if (object instanceof ShnapBooleanNative) {
            stream.put((byte) 1);
            stream.put((byte) (((ShnapBooleanNative) object).getValue() ? 1 : 0));
        } else if (object instanceof ShnapBigIntegerNative) {
            stream.put((byte) 2);
            Bytes.writeBigInt(stream, ((ShnapBigIntegerNative) object).getNumber());
        } else if (object instanceof ShnapBigDecimalNative) {
            stream.put((byte) 3);
            Bytes.writeBigDecimal(stream, ((ShnapBigDecimalNative) object).getNumber().stripTrailingZeros());
        } else if (object instanceof ShnapStringNative) {
            stream.put((byte) 4);
            Bytes.writeString(stream, ((ShnapStringNative) object).getValue());
        } else if (object instanceof ShnapCharNative) {
            stream.put((byte) 5);
            stream.putInt(((ShnapCharNative) object).getNumber());
        } else {
            throw new IllegalArgumentException("Unknown native: " + object);
        }
    }

    public static ShnapObject readNativeVal(ByteStream stream, ShnapScript building) throws IOException {
        ShnapLoc loc = readLoc(stream, building);
        byte id = stream.get();
        switch (id) {
            case 0:
                return stream.get() == 0 ? ShnapObject.getNull() : ShnapObject.getVoid();
            case 1:
                return ShnapBooleanNative.of(stream.get() != 0);
            case 2:
                return ShnapNumberNative.valueOf(loc, Bytes.readBigInt(stream));
            case 3:
                return ShnapNumberNative.valueOf(loc, Bytes.readBigDecimal(stream));
            case 4:
                return new ShnapStringNative(loc, Bytes.readString(stream));
            case 5:
                return new ShnapCharNative(loc, stream.getInt());
            default:
                throw new IllegalArgumentException("Unknown native: " + id);
        }
    }

    public static void zipFile(File source, File targetZip) throws IOException {
        targetZip.mkdirs();
        targetZip.delete();
        targetZip.createNewFile();
        FileOutputStream fos = new FileOutputStream(targetZip);
        ZipOutputStream zos = new ZipOutputStream(fos);
        recurse(zos, source, source);
        zos.close();
    }

    private static void recurse(ZipOutputStream stream, File file, File source) throws IOException {
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) {
                recurse(stream, sub, source);
            }
        } else {
            byte[] buffer = new byte[1024];
            ZipEntry entry = new ZipEntry(name(file, source));
            stream.putNextEntry(entry);
            FileInputStream in = new FileInputStream(file);
            int len;
            while ((len = in.read(buffer)) > 0) {
                stream.write(buffer, 0, len);
            }
            in.close();
            stream.closeEntry();
        }
    }

    public static void unZipFile(File zip, File dir)
            throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        // Open the zip file
        ZipFile zipFile = new ZipFile(zip);
        Enumeration<?> enu = zipFile.entries();
        while (enu.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enu.nextElement();
            String name = zipEntry.getName();
            // Do we need to create a directory ?
            File file = new File(dir, name);
            if (name.endsWith("/")) {
                file.mkdirs();
                continue;
            }

            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            if(file.exists()) {
                Files.deleteIfExists(file.toPath());
            }

            // Extract the file
            InputStream is = zipFile.getInputStream(zipEntry);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = is.read(bytes)) >= 0) {
                fos.write(bytes, 0, length);

            }
            is.close();
            fos.close();

        }
        zipFile.close();
    }

    private static String name(File file, File source) {
        return file.getAbsolutePath().substring(source.getAbsolutePath().length() + 1, file.getAbsolutePath().length()).replace(File.separator, "/").replace("\\", "/");
    }

}
