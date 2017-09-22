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

import com.gmail.socraticphoenix.pio.Bytes;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.ShnapParameter;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapArrayLiteral;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapFlag;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapGet;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapGetNative;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstructionSequence;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInvoke;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapLiteral;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapMakeFunc;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapMakeObj;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapNoOp;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapOperate;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapSet;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapStateChange;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapDoWhileBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapForBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapIfBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapScopeBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapTryCatchBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapWhileBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.read;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.readEnum;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.readLoc;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.readNativeVal;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.readParam;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.register;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.write;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.writeEnum;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.writeLoc;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.writeNativeVal;
import static com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil.writeParam;

public class ShnapDefaultHandlers {

    public static void registerDefaults() {
        NodeHandler[] handlers = new NodeHandler[]{
                new SimpleNodeHandler<>(ShnapGet.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    Bytes.writeString(stream, node.getName());
                    write(stream, node.getTarget());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    String name = Bytes.readString(stream);
                    ShnapInstruction target = read(stream, building);
                    return new ShnapGet(loc, target, name);
                }),
                new SimpleNodeHandler<>(ShnapSet.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    Bytes.writeString(stream, node.getName());
                    writeEnum(stream, node.getOp());
                    write(stream, node.getTarget());
                    write(stream, node.getVal());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    String name = Bytes.readString(stream);
                    ShnapOperators op = readEnum(stream, ShnapOperators.class);
                    ShnapInstruction target = read(stream, building);
                    ShnapInstruction val = read(stream, building);
                    return new ShnapSet(loc, target, name, val, op);
                }),
                new SimpleNodeHandler<>(ShnapLiteral.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    writeNativeVal(stream, node.getValue());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapLiteral(loc, readNativeVal(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapNoOp.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapNoOp(loc);
                }),
                new SimpleNodeHandler<>(ShnapOperate.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    writeEnum(stream, node.getOperator());
                    write(stream, node.getLeft());
                    write(stream, node.getRight());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    ShnapOperators op = readEnum(stream, ShnapOperators.class);
                    ShnapInstruction left = read(stream, building);
                    ShnapInstruction right = read(stream, building);
                    return new ShnapOperate(loc, left, op, right);
                }),
                new SimpleNodeHandler<>(ShnapInvoke.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    stream.putInt(node.getArgs().size());
                    stream.putInt(node.getDefArgs().size());
                    for(ShnapInstruction arg : node.getArgs()) {
                        write(stream, arg);
                    }
                    for(Map.Entry<String, ShnapInstruction> defArg : node.getDefArgs().entrySet()) {
                        Bytes.writeString(stream, defArg.getKey());
                        write(stream, defArg.getValue());
                    }
                    write(stream, node.getTarget());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    int argsSize = stream.getInt();
                    int defArgsSize = stream.getInt();
                    List<ShnapInstruction> args = new ArrayList<>();
                    Map<String, ShnapInstruction> defArgs = new LinkedHashMap<>();
                    for (int i = 0; i < argsSize; i++) {
                        args.add(read(stream, building));
                    }
                    for (int i = 0; i < defArgsSize; i++) {
                        defArgs.put(Bytes.readString(stream), read(stream, building));
                    }
                    ShnapInstruction target = read(stream, building);
                    return new ShnapInvoke(loc, target, args, defArgs);
                }),
                new SimpleNodeHandler<>(ShnapStateChange.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    stream.put((byte) node.getState().ordinal());
                    write(stream, node.getValue());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapStateChange(loc, State.values()[stream.get()], read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapInstructionSequence.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    stream.putInt(node.getSequence().size());
                    for(ShnapInstruction instruction : node.getSequence()) {
                        write(stream, instruction);
                    }
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    int size = stream.getInt();
                    List<ShnapInstruction> sequence = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        sequence.add(read(stream, building));
                    }
                    return new ShnapInstructionSequence(loc, sequence);
                }),
                new SimpleNodeHandler<>(ShnapMakeObj.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getInstruction());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapMakeObj(loc, read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapMakeFunc.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    stream.putInt(node.getParameters().size());
                    stream.putInt(node.getObjInstructions().size());
                    for(ShnapParameter param : node.getParameters()) {
                        writeParam(stream, param);
                    }
                    for(ShnapInstruction obj : node.getObjInstructions()) {
                        write(stream, obj);
                    }
                    write(stream, node.getBody());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    int paramSize = stream.getInt();
                    int objSize = stream.getInt();
                    List<ShnapParameter> parameters = new ArrayList<>();
                    for (int i = 0; i < paramSize; i++) {
                        parameters.add(readParam(stream, building));
                    }
                    List<ShnapInstruction> objInstructions = new ArrayList<>();
                    for (int i = 0; i < objSize; i++) {
                        objInstructions.add(read(stream, building));
                    }
                    ShnapInstruction body = read(stream, building);
                    return new ShnapMakeFunc(loc, parameters, objInstructions, body);
                }),
                new SimpleNodeHandler<>(ShnapDoWhileBlock.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getName());
                    write(stream, node.getVal());
                    write(stream, node.getInstruction());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapDoWhileBlock(loc, read(stream, building), read(stream, building), read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapWhileBlock.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getName());
                    write(stream, node.getVal());
                    write(stream, node.getInstruction());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapWhileBlock(loc, read(stream, building), read(stream, building), read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapForBlock.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getName());
                    Bytes.writeString(stream, node.getVarName());
                    write(stream, node.getVal());
                    write(stream, node.getInstruction());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapForBlock(loc, read(stream, building), Bytes.readString(stream), read(stream, building), read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapScopeBlock.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getName());
                    write(stream, node.getInstruction());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapScopeBlock(loc, read(stream, building), read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapTryCatchBlock.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getTryBlock());
                    write(stream, node.getCatchBlock());
                    Bytes.writeString(stream, node.getCatchName());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapTryCatchBlock(loc, read(stream, building), read(stream, building), Bytes.readString(stream));
                }),
                new SimpleNodeHandler<>(ShnapIfBlock.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    write(stream, node.getName());
                    write(stream, node.getVal());
                    write(stream, node.getInstruction());
                    write(stream, node.getElif());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapIfBlock(loc, read(stream, building), read(stream, building), read(stream, building), read(stream, building));
                }),
                new SimpleNodeHandler<>(ShnapGetNative.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    Bytes.writeString(stream, node.getName());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    return new ShnapGetNative(loc, Bytes.readString(stream));
                }),
                new SimpleNodeHandler<>(ShnapArrayLiteral.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    stream.putInt(node.getValues().size());
                    for(ShnapInstruction val : node.getValues()) {
                        write(stream, val);
                    }
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    int size = stream.getInt();
                    List<ShnapInstruction> inst = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        inst.add(read(stream, building));
                    }
                    return new ShnapArrayLiteral(loc, inst);
                }),
                new SimpleNodeHandler<>(ShnapFlag.class, (stream, node) -> {
                    writeLoc(stream, node.getLocation());
                    Bytes.writeString(stream, node.getName());
                    write(stream, node.getTarget());
                    writeEnum(stream, node.getFlag());
                }, (stream, building) -> {
                    ShnapLoc loc = readLoc(stream, building);
                    String name = Bytes.readString(stream);
                    ShnapInstruction target = read(stream, building);
                    ShnapContext.Flag flag = readEnum(stream, ShnapContext.Flag.class);
                    return new ShnapFlag(loc, target, name, flag);
                })
        };

        for (NodeHandler handler : handlers) {
            register(handler);
        }
    }

}
