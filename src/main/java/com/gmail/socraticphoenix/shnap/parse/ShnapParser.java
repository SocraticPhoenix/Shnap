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
package com.gmail.socraticphoenix.shnap.parse;

import com.gmail.socraticphoenix.collect.coupling.Pair;
import com.gmail.socraticphoenix.collect.coupling.Switch;
import com.gmail.socraticphoenix.parse.CharacterStream;
import com.gmail.socraticphoenix.parse.ParserData;
import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.doc.DocNode;
import com.gmail.socraticphoenix.shnap.doc.DocTreeBuilder;
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
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapMakeResolver;
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
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;

public class ShnapParser {
    private static Predicate<Character> whitespace = Character::isWhitespace;
    private static ParserData stringData = Strings.javaEscapeFormat().quote('"');
    private static ParserData tripleQuoteData = new ParserData().escapeChar('\\').escape('"');
    private static ParserData charData = Strings.javaEscapeFormat().quote('\'');
    private static String tripleQuote = "\"\"\"";
    private static char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static String[] operatorSets;

    static {
        List<String> sets = new ArrayList<>();
        for (ShnapOperators operator : ShnapOperators.values()) {
            if (operator.isSet()) {
                sets.add(operator.getRep() + "=");
            }
        }
        operatorSets = sets.toArray(new String[sets.size()]);
    }

    private CharacterStream stream;
    private String content;
    private int[][] pts;
    private ShnapScript building;
    private String original;

    private DocTreeBuilder docBuilder;

    public ShnapParser(String content, ShnapLoc initial) {
        this.original = content;
        this.building = initial.getScript();

        StringBuilder clean = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                clean.append(line).append("\n");
            }

        } catch (IOException e) {
            throw new IllegalStateException("StringReader threw IOException", e);
        }
        content = clean.toString();
        int[][] pts = new int[content.length() + 1][];
        int row = initial.getLine();
        int col = initial.getCol();
        int k = 0;

        for (char c : content.toCharArray()) {
            pts[k++] = new int[]{row, col};
            col++;
            if (c == '\n') {
                row++;
                col = initial.getCol();
            }
        }
        pts[k] = new int[]{row, col}; //EOF location


        List<int[]> commentPts = new ArrayList<>();
        CharacterStream comments = new CharacterStream(content);
        ParserData skipQuotes = new ParserData().quote('"').escape('"');
        StringBuilder result = new StringBuilder();
        while (comments.hasNext()) {
            boolean comment = false;
            String start = null;
            String end = null;
            if (comments.isNext("/*")) {
                comment = true;
                start = "/*";
                end = "*/";
            } else if (comments.isNext("//")) {
                comment = true;
                start = "//";
                end = "\n";
            }

            if (comment) {
                int startInd = comments.index();
                comments.next(start.length());
                comments.nextUntil(end);
                comments.next(end.length());
                int endInd = comments.index();
                commentPts.add(new int[]{startInd, endInd});
            } else if (comments.isNext('"')) {
                result.append(comments.nextUntil(skipQuotes));
            } else {
                result.append(comments.next().get());
            }
        }

        int decSize = 0;
        for (int[] comment : commentPts) {
            for (int i = comment[0]; i < comment[1]; i++) {
                pts[i] = null;
                decSize++;
            }
        }

        int[][] finlPts = new int[pts.length - decSize][];
        int index = 0;
        for (int[] arr : pts) {
            if (arr != null) {
                finlPts[index++] = arr;
            }
        }

        this.docBuilder = new DocTreeBuilder();
        this.pts = finlPts;
        this.content = result.toString();
        this.stream = new CharacterStream(this.content);
    }

    public ShnapParser(String content, ShnapScript script) {
        this(content, new ShnapLoc(0, 0, script));
    }

    public int[][] getPts() {
        return this.pts;
    }

    public ShnapInstructionSequence parseAll() {
        ShnapLoc loc = this.loc();
        List<ShnapInstruction> instructions = new ArrayList<>();
        whitespace();
        String docs = null;
        if (this.isScriptDocsNext()) {
            docs = this.parseScriptDocs();
        }
        whitespace();
        this.docBuilder.pushInitialFrame(docs);
        while (stream.hasNext()) {
            whitespace();
            ShnapInstruction next = this.nextInst();
            if (next == null) {
                break;
            } else {
                instructions.add(next);
                this.prepareNextStatement();
            }
        }
        whitespace();
        if (stream.hasNext()) {
            throw err("Expected statement");
        }

        return new ShnapInstructionSequence(loc, instructions);
    }

    public void prepareNextStatement() {
        int index = stream.index();
        whitespace();
        if (stream.isNext(';')) {
            stream.consumeAll(';');
        } else {
            stream.jumpTo(index);
        }
    }

    public ShnapInstruction nextInst() {
        Pair<ShnapInstruction, List<Pair<ShnapOperators, ShnapLoc>>> simple = parsePrimaryNext();
        if(simple == null) {
            return null;
        }
        ShnapInstruction primary = simple.getA();

        List<Switch<ShnapInstruction, Pair<ShnapOperators, ShnapLoc>>> sequence = new ArrayList<>();
        sequence.add(Switch.ofA(primary));
        while (true) {
            int index = stream.index();
            ShnapLoc loc = this.loc();
            if (this.isOperatorNext()) {
                List<Pair<ShnapOperators, ShnapLoc>> prefixParts = simple.getB();
                for (int i = prefixParts.size() - 1; i >= 0; i--) {
                    Pair<ShnapOperators, ShnapLoc> prefix = prefixParts.get(i);
                    primary = new ShnapOperate(prefix.getB(), primary, prefix.getA(), null);
                    sequence.set(sequence.size() - 1, Switch.ofA(primary));
                }

                whitespace();
                ShnapOperators operator = this.nextOperator();
                whitespace();
                if (operator == ShnapOperators.NEGATIVE) { //That moment when they're the same symbol!!
                    operator = ShnapOperators.SUBTRACT;
                }
                if (operator == null || operator.getArity() == 1) {
                    stream.jumpTo(index);
                    break;
                }
                sequence.add(Switch.ofB(Pair.of(operator, loc)));
                simple = parsePrimaryNext();
                if (simple == null) {
                    throw err("Expected statement");
                }
                primary = simple.getA();
                sequence.add(Switch.ofA(primary));
            } else if (this.isAppendedInvokeNext()) {
                Pair<List<ShnapInstruction>, Map<String, ShnapInstruction>> params = parseFilledParenthesizedParams();
                primary = new ShnapInvoke(loc, primary, params.getA(), params.getB());
                sequence.set(sequence.size() - 1, Switch.ofA(primary));
            } else if (this.isAppendedSetGetSliceNext()) {
                primary = this.parseNextGetSetSlice(primary);
                sequence.set(sequence.size() - 1, Switch.ofA(primary));
            } else {
                whitespace();
                if (stream.isNext('.')) {
                    stream.next();
                    ShnapLoc subLoc = this.loc();
                    whitespace();
                    if (isAppendedSetNext()) {
                        ShnapSet set = this.parseSimpleSet();
                        set.setTarget(primary);
                        primary = set;
                        sequence.set(sequence.size() - 1, Switch.ofA(primary));
                    } else if (isAppendedGetNext()) {
                        String name = this.nextVarRef();
                        primary = new ShnapGet(subLoc, primary, name);
                        sequence.set(sequence.size() - 1, Switch.ofA(primary));
                    } else {
                        stream.jumpTo(index);
                        break;
                    }
                } else {
                    stream.jumpTo(index);
                    break;
                }
            }
        }

        List<Pair<ShnapOperators, ShnapLoc>> prefixParts = simple.getB();
        for (int i = prefixParts.size() - 1; i >= 0; i--) {
            Pair<ShnapOperators, ShnapLoc> prefix = prefixParts.get(i);
            primary = new ShnapOperate(prefix.getB(), primary, prefix.getA(), null);
            sequence.set(sequence.size() - 1, Switch.ofA(primary));
        }

        if (sequence.size() == 1) {
            return sequence.get(0).getA().get();
        } else {
            Stack<OperatorTree> operands = new Stack<>();
            Stack<ShnapOperators> operators = new Stack<>();
            Stack<ShnapLoc> operatorLocs = new Stack<>();
            operators.push(ShnapOperators.SENTINEL);

            for (int i = 0; i < sequence.size(); i++) {
                Switch<ShnapInstruction, Pair<ShnapOperators, ShnapLoc>> element = sequence.get(i);

                if (element.containsA()) {
                    operands.push(new OperatorTree(element.getA().get()));
                } else {
                    ShnapOperators operator = element.getB().get().getA();
                    if (operator.getPrecedence() > operators.peek().getPrecedence() || (!operator.isLeftAssociative() && operator.getPrecedence() == operators.peek().getPrecedence())) {
                        operators.push(operator);
                        operatorLocs.push(element.getB().get().getB());
                    } else {
                        OperatorTree right = operands.pop();
                        OperatorTree left = operands.pop();
                        ShnapOperators oper = operators.pop();
                        operands.push(new OperatorTree(operatorLocs.pop(), left, oper, right));
                        i--;
                    }
                }
            }

            while (operands.size() > 1) {
                OperatorTree right = operands.pop();
                OperatorTree left = operands.pop();
                ShnapOperators oper = operators.pop();
                operands.push(new OperatorTree(operatorLocs.pop(), left, oper, right));
            }

            if (operands.size() == 1) {
                return operands.pop().consolidate();
            }
        }

        return null;
    }

    private Pair<ShnapInstruction, List<Pair<ShnapOperators, ShnapLoc>>> parsePrimaryNext() {
        List<Pair<ShnapOperators, ShnapLoc>> prefixOps = new ArrayList<>();
        ShnapInstruction primary = null;
        int mark = stream.index();
        whitespace();

        while (this.isOperatorNext()) {
            int index = stream.index();
            ShnapLoc loc = this.loc();
            ShnapOperators op = this.nextOperator();
            if (op != null && op.getArity() == 1 && (op != ShnapOperators.NEGATIVE || !isNumberNext())) {
                prefixOps.add(Pair.of(op, loc));
                whitespace();
            } else {
                stream.jumpTo(index);
                break;
            }
        }

        whitespace();

        if (isLiteralNext()) {
            primary = nextLiteral();
        } else if (isNativeNext()) {
            primary = parseGetNative();
        } else if (isFlagNext()) {
            primary = parseNextFlag();
        } else if (isStateChangeNext()) {
            primary = parseStateChanged();
        } else if (isIfBlockNext()) {
            primary = parseNextIfBlock();
        } else if (isWhileBlockNext()) {
            primary = parseNextWhileBlock();
        } else if (isDoWhileBlockNext()) {
            primary = parseNextDoWhileBlock();
        } else if (isForBlockNext()) {
            primary = parseNextForBlock();
        } else if (isTryBlockNext()) {
            primary = parseNextTryBlock();
        } else if (isSequenceNext()) {
            primary = parseNextScopeBlock();
        } else if (isFunctionNext()) {
            primary = parseNextFunc();
        } else if (isResolverNext()) {
            primary = parseNextResolver();
        } else if (isObjNext()) {
            primary = parseNextObj();
        } else if (isClsNext()) {
            primary = parseNextCls();
        } else if (isSimpleSetNext() || isDocsNext()) {
            String docs = null;
            ShnapLoc loc = this.loc();
            if (isDocsNext()) {
                docs = this.parseDocs();
                whitespace();
                if (!isSimpleSetNext()) {
                    throw this.err("Expected variable assignment");
                }
            }
            this.docBuilder.pushDocs(docs);
            ShnapSet shnapSet = parseSimpleSet();
            if (docs != null && shnapSet.getName().startsWith("^")) {
                throw this.err(loc, "Docs cannot be applied to pushed variables");
            }

            try {
                this.docBuilder.popDocs(shnapSet.getName());
            } catch (IllegalArgumentException e) {
                throw this.err(loc, e.getMessage());
            }
            primary = shnapSet;
        } else if (isSimpleGetNext()) {
            primary = parseSimpleGet();
        } else if (stream.isNext('(')) {
            stream.next();
            ShnapInstruction inst = nextInst();
            whitespace();
            if (!stream.isNext(')')) {
                throw err("Expected )");
            }
            stream.next();
            primary = inst;
        }

        if (primary != null) {
            return Pair.of(primary, prefixOps);
        } else {
            stream.jumpTo(mark);
            return null;
        }
    }

    public DocTreeBuilder getDocBuilder() {
        return this.docBuilder;
    }

    public DocNode getDocs() {
        return this.docBuilder.build().toDocNode();
    }

    public ShnapInstruction safeNextInst() {
        ShnapInstruction inst = this.nextInst();
        if (inst == null) {
            throw err("Expected statement");
        }
        return inst;
    }

    private ShnapOperators nextOperator() {
        for (ShnapOperators op : ShnapOperators.values()) {
            if (stream.isNext(op.getRep())) {
                stream.next(op.getRep());
                return op;
            }
        }

        return null;
    }

    private boolean isOperatorNext() {
        int index = stream.index();
        boolean flag = false;
        whitespace();
        if (!stream.isNext("/<")) {
            for (ShnapOperators op : ShnapOperators.values()) {
                if (stream.isNext(op.getRep())) {
                    flag = true;
                }
            }
        }

        stream.jumpTo(index);
        return flag;
    }

    private boolean isAppendedInvokeNext() {
        return stream.isNext("(");
    }

    private boolean isAppendedSetGetSliceNext() {
        return stream.isNext("[");
    }

    public boolean isSimpleGetNext() {
        return isAppendedGetNext();
    }

    public boolean isSimpleSetNext() {
        return isAppendedSetNext();
    }

    private boolean isAppendedGetNext() {
        int index = stream.index();
        boolean flag = false;
        if (isVarRefNext()) {
            nextVarRef();
            whitespace();
            flag = true;
        }

        stream.jumpTo(index);
        return flag;
    }

    private boolean isAppendedSetNext() {
        int index = stream.index();
        boolean flag = false;
        if (isVarRefNext()) {
            nextVarRef();
            whitespace();
            if (isSetOpNext()) {
                flag = true;
            }
        }

        stream.jumpTo(index);
        return flag;
    }

    public ShnapSet parseSimpleSet() {
        String name = nextVarRef();
        ShnapOperators op = null;
        boolean noop = false;
        whitespace();
        ShnapLoc loc = this.loc();
        if (stream.isNext('=')) {
            noop = true;
            stream.next();
        } else {
            for (String test : operatorSets) {
                if (stream.isNext(test)) {
                    stream.next(test.length());
                    op = ShnapOperators.fromRep(Strings.cutLast(test));
                    if (op == ShnapOperators.NEGATIVE) {
                        op = ShnapOperators.SUBTRACT;
                    }
                    break;
                }
            }
        }

        if (op == null && !noop) {
            throw err("Expected assignment operator");
        }

        whitespace();
        ShnapInstruction val = safeNextInst();
        return new ShnapSet(loc, null, name, val, op);
    }

    public ShnapGet parseSimpleGet() {
        ShnapLoc loc = this.loc();
        String name = nextVarRef();
        return new ShnapGet(loc, null, name);
    }

    public boolean isStateChangeNext() {
        for (State state : State.values()) {
            if (this.tokenIsNext(state.getRep())) {
                return true;
            }
        }
        return false;
    }

    public ShnapInstruction parseStateChanged() {
        ShnapLoc loc = this.loc();
        State state = null;
        for (State st : State.values()) {
            if (stream.isNext(st.getRep())) {
                stream.next(st.getRep().length());
                state = st;
                break;
            }
        }
        whitespace();
        ShnapInstruction inst = null;
        if (stream.isNext(':')) {
            stream.next();
            whitespace();
            inst = safeNextInst();
        }
        return new ShnapStateChange(loc, state, inst);
    }

    public boolean isClsNext() {
        return stream.isNext("@");
    }

    public ShnapInstruction parseNextCls() {
        whitespace();
        ShnapLoc loc = this.loc();
        stream.next();
        String type = "object";
        /*if (this.isJustIdentifierNext()) {
            type = this.parseNextPathIdentifier();
        }*/
        whitespace();
        List<ShnapParameter> params = this.parseParenthesizedParams();
        whitespace();
        this.consumeArrow();
        boolean sequence = this.isSequenceNext();
        ShnapLoc loc2 = this.loc();
        List<ShnapInstruction> instructions = new ArrayList<>();
        List<ShnapInstruction> clsProperties = new ArrayList<>();
        whitespace();
        if (sequence) {
            stream.consume('{');
        }
        while (true) {
            whitespace();
            if (tokenIsNext("static")) {
                ShnapInstruction inst = parseNextClsInst();
                this.prepareNextStatement();
                clsProperties.add(inst);
            } else {
                ShnapInstruction inst = nextInst();
                if (inst == null) {
                    break;
                }
                instructions.add(inst);
                this.prepareNextStatement();
            }

            if (!sequence) {
                break;
            }
        }
        whitespace();
        if (sequence) {
            stream.consume('}');
        }
        ShnapInstruction body = new ShnapInstructionSequence(loc2, instructions);
        return ShnapFactory.makeClass(loc, params, body, clsProperties, type);
    }

    private ShnapInstruction parseNextClsInst() {
        stream.next(6);
        whitespace();
        return safeNextInst();
    }

    public boolean isObjNext() {
        return stream.isNext("#");
    }

    public ShnapMakeObj parseNextObj() {
        whitespace();
        ShnapLoc loc = this.loc();
        stream.next();
        whitespace();
        String type = "object";
        /*if (this.isJustIdentifierNext()) {
            type = this.parseNextPathIdentifier();
        }*/
        this.consumeArrow();
        ShnapInstruction body = parseNextSequence();
        return new ShnapMakeObj(loc, body, type);
    }

    public boolean isResolverNext() {
        return stream.isNext("?");
    }

    public ShnapMakeResolver parseNextResolver() {
        whitespace();
        ShnapLoc loc = this.loc();
        stream.next();
        whitespace();

        return new ShnapMakeResolver(loc, safeNextInst());
    }

    private void consumeArrow() {
        stream.next("=>");
        whitespace();
    }

    public boolean isFunctionNext() {
        return stream.isNext("$");
    }

    public ShnapMakeFunc parseNextFunc() {
        this.docBuilder.pushIgnore();
        whitespace();
        ShnapLoc loc = this.loc();
        stream.next();
        List<ShnapParameter> params = this.parseParenthesizedParams();
        whitespace();
        boolean autoReturn = stream.isNext("=>");
        this.consumeArrow();
        boolean sequence = this.isSequenceNext();
        ShnapLoc loc2 = this.loc();
        List<ShnapInstruction> instructions = new ArrayList<>();
        List<ShnapInstruction> clsProperties = new ArrayList<>();
        whitespace();
        if (sequence) {
            stream.next();
        }
        while (true) {
            whitespace();
            if (tokenIsNext("static")) {
                ShnapInstruction inst = parseNextClsInst();
                this.prepareNextStatement();
                clsProperties.add(inst);
            } else {
                ShnapInstruction inst = nextInst();
                if (inst == null) {
                    break;
                } else {
                    this.prepareNextStatement();
                }
                instructions.add(inst);
            }

            if (!sequence) {
                break;
            }
        }
        if (sequence) {
            whitespace();
            stream.consume('}');
        }
        ShnapInstruction body = new ShnapInstructionSequence(loc2, instructions);

        if (autoReturn) {
            body = new ShnapStateChange(body.getLocation(), State.RETURNING, body);
        }

        this.docBuilder.popIgnore();
        return new ShnapMakeFunc(loc, params, clsProperties, body);
    }

    public boolean isFlagNext() {
        for (ShnapContext.Flag flag : ShnapContext.Flag.values()) {
            if (this.tokenIsNext(flag.getRep())) {
                return true;
            }
        }

        return false;
    }

    public ShnapInstruction parseNextFlag() {
        whitespace();
        ShnapLoc loc = this.loc();
        ShnapContext.Flag flag = null;
        for (ShnapContext.Flag test : ShnapContext.Flag.values()) {
            if (stream.isNext(test.getRep())) {
                stream.next(test.getRep().length());
                flag = test;
                break;
            }
        }
        whitespace();
        if (!this.isSimpleGetNext()) {
            throw err("expected variable identifier");
        }
        ShnapGet res = this.parseSimpleGet();
        whitespace();
        while (stream.isNext('.')) {
            ShnapLoc loc2 = this.loc();
            stream.next();
            whitespace();
            if (!this.isSimpleGetNext()) {
                throw err("expected variable identifier");
            } else {
                res = new ShnapGet(loc2, res, this.nextVarRef());
            }
        }

        return new ShnapFlag(loc, res.getTarget(), res.getName(), flag);
    }

    public boolean isLiteralNext() {
        return isFalseTrueVoidNullNext() || isNumberNext() || isStringNext() || isCharNext() || isArrayNext();
    }


    public ShnapInstruction nextLiteral() {
        if (isFalseTrueVoidNullNext()) {
            return nextFalseTrueVoidNull();
        } else if (isNumberNext()) {
            return nextNumber();
        } else if (isStringNext()) {
            return nextString();
        } else if (isCharNext()) {
            return nextChar();
        } else if (isArrayNext()) {
            return nextArr();
        }

        return null;
    }

    public ShnapInstruction parseNextGetSetSlice(ShnapInstruction target) {
        whitespace();
        ShnapLoc loc1 = this.loc();
        stream.next();
        List<ShnapInstruction> params = new ArrayList<>();
        Map<String, ShnapInstruction> namedParams = new LinkedHashMap<>();
        boolean started = false;
        boolean sliceMode = false;
        boolean defaulting = false;
        boolean flag = true;
        whitespace();
        if (stream.isNext(']')) {
            flag = false;
            stream.next();
        }
        while (flag) {
            whitespace();
            int index = stream.index();
            boolean named = false;
            ShnapLoc loc = this.loc();
            if (isVarRefNext()) {
                String nxt = nextVarRef();
                whitespace();
                if (stream.isNext('=') && !isEqualsCompareNext()) {
                    defaulting = true;
                    named = true;
                    stream.consume('=');
                    whitespace();
                    namedParams.put(nxt, safeNextInst());
                } else if (defaulting) {
                    throw err(loc, "non-default parameter after default parameter");
                } else {
                    stream.jumpTo(index);
                }
            } else if (defaulting) {
                throw err(loc, "non-default parameter after default parameter");
            }

            if (!named) {
                params.add(safeNextInst());
            }

            whitespace();
            if (stream.isNext(',')) {
                if (sliceMode && started) {
                    throw this.err("Expected :");
                }
                started = true;
                sliceMode = false;
                stream.next();
            } else if (stream.isNext(':')) {
                if (!sliceMode && started) {
                    throw this.err("Expected ,");
                }
                started = true;
                sliceMode = true;
                stream.next();
                whitespace();
                if (stream.isNext(']')) {
                    stream.next();
                    break;
                }
            } else {
                if (!stream.isNext(']')) {
                    throw err("Expected ], :, or ,");
                }
                stream.next();
                break;
            }
        }

        whitespace();
        if (stream.isNext('=') && !isEqualsCompareNext()) {
            stream.next();
            ShnapInstruction theValue = this.safeNextInst();
            return sliceMode ? ShnapFactory.makeSliceSet(loc1, target, params, namedParams, theValue) : ShnapFactory.makeSet(loc1, target, params, namedParams, theValue);
        } else {
            return sliceMode ? ShnapFactory.makeSliceGet(loc1, target, params, namedParams) : ShnapFactory.makeGet(loc1, target, params, namedParams);
        }
    }

    public Pair<List<ShnapInstruction>, Map<String, ShnapInstruction>> parseFilledParenthesizedParams() {
        whitespace();
        if (!stream.isNext('(')) {
            return Pair.of(Collections.emptyList(), Collections.emptyMap());
        } else {
            stream.next();
            List<ShnapInstruction> params = new ArrayList<>();
            Map<String, ShnapInstruction> namedParams = new LinkedHashMap<>();
            whitespace();
            if (stream.isNext(')')) {
                stream.next();
                return Pair.of(Collections.emptyList(), Collections.emptyMap());
            }
            boolean defaulting = false;
            while (true) {
                whitespace();
                int index = stream.index();
                boolean named = false;
                ShnapLoc loc = this.loc();
                if (isVarRefNext()) {
                    String nxt = nextVarRef();
                    whitespace();
                    if (stream.isNext('=') && !isEqualsCompareNext()) {
                        defaulting = true;
                        named = true;
                        stream.consume('=');
                        whitespace();
                        namedParams.put(nxt, safeNextInst());
                    } else if (defaulting) {
                        throw err(loc, "non-default parameter after default parameter");
                    } else {
                        stream.jumpTo(index);
                    }
                } else if (defaulting) {
                    throw err(loc, "non-default parameter after default parameter");
                }

                if (!named) {
                    params.add(safeNextInst());
                }

                whitespace();
                if (stream.isNext(',')) {
                    stream.next();
                    whitespace();
                    if (stream.isNext(')')) {
                        stream.next();
                        break;
                    }
                } else {
                    if (!stream.isNext(')')) {
                        throw err("Expected ) or ,");
                    }
                    stream.next();
                    break;
                }
            }

            return Pair.of(params, namedParams);
        }
    }

    public List<ShnapParameter> parseParenthesizedParams() {
        whitespace();
        List<ShnapParameter> params = new ArrayList<>();
        whitespace();
        boolean paren = stream.isNext('(');
        if (paren) {
            stream.next();
        }
        whitespace();
        if (paren && stream.isNext(')')) {
            stream.next();
            return Collections.emptyList();
        }
        boolean wasVariable = false;
        boolean defaulting = false;
        while (true) {
            whitespace();
            ShnapLoc loc = this.loc();
            if (!isVarRefNext()) {
                whitespace();
                if (!stream.isNext(',') && !paren) {
                    break;
                }
                throw err("Expected parameter identifier");
            }
            String name = nextVarRef();
            whitespace();
            boolean variable = stream.isNext("...");
            if (variable) {
                stream.next(3);
            }
            ShnapInstruction def = null;
            if (stream.isNext("=>")) {
                params.add(new ShnapParameter(loc, name, def, variable));
                break;
            } else if (stream.isNext('=') && !stream.isNext("=>")) {
                defaulting = true;
                stream.next();
                whitespace();
                def = safeNextInst();
            } else if (defaulting) {
                throw err(loc, "non-default parameter after default parameter");
            } else if (wasVariable && variable) {
                throw err(loc, "var-args parameter not at end of function");
            }
            wasVariable = variable || wasVariable;
            whitespace();

            params.add(new ShnapParameter(loc, name, def, variable));
            if (stream.isNext(',')) {
                stream.next();
                whitespace();
                if (stream.isNext(')') && paren) {
                    stream.next();
                    break;
                }
            } else {
                if (paren) {
                    if (!stream.isNext(')')) {
                        throw err("Expected ) or ,");
                    } else {
                        stream.next();
                    }
                }
                break;
            }
        }

        return params;
    }

    public boolean isIfBlockNext() {
        return this.tokenIsNext("if");
    }

    public ShnapIfBlock parseNextIfBlock() {
        ShnapLoc loc = this.loc();
        this.docBuilder.pushIgnore();
        stream.next(2);
        whitespace();
        List<ShnapIfBlock> blocks = new ArrayList<>();
        {
            whitespace();
            boolean paren = stream.isNext("(");
            if (paren) {
                stream.consume('(');
            }
            whitespace();
            ShnapInstruction inst = safeNextInst();
            if (paren) {
                if (!stream.isNext(')')) {
                    throw err("Expected )");
                }
                stream.consume(')');
            }
            whitespace();
            Pair<ShnapLiteral, ShnapInstruction> seq = safeParseNextNamedSequence();
            blocks.add(new ShnapIfBlock(loc, seq.getA(), inst, seq.getB(), null));
        }
        while (true) {
            whitespace();
            if (this.tokenIsNext("else")) {
                loc = this.loc();
                stream.next(4);
                whitespace();
                if (this.tokenIsNext("if")) {
                    stream.next(2);
                    whitespace();
                    boolean paren = stream.isNext("(");
                    if (paren) {
                        stream.consume('(');
                    }
                    whitespace();
                    ShnapInstruction inst = safeNextInst();
                    if (paren) {
                        if (!stream.isNext(')')) {
                            throw err("Expected )");
                        }
                        stream.consume(')');
                    }
                    Pair<ShnapLiteral, ShnapInstruction> seq = safeParseNextNamedSequence();
                    blocks.add(new ShnapIfBlock(loc, seq.getA(), inst, seq.getB(), null));
                } else {
                    whitespace();
                    loc = this.loc();
                    Pair<ShnapLiteral, ShnapInstruction> seq = safeParseNextNamedSequence();
                    blocks.add(new ShnapIfBlock(loc, seq.getA(), null, seq.getB(), null));
                    break;
                }
            } else {
                break;
            }
        }
        for (int i = blocks.size() - 1; i > 0; i--) {
            blocks.get(i - 1).setElif(blocks.get(i));
        }
        this.docBuilder.popIgnore();
        return blocks.get(0);
    }

    public ShnapScopeBlock parseNextScopeBlock() {
        this.docBuilder.pushIgnore();
        ShnapLoc loc = this.loc();
        Pair<ShnapLiteral, ShnapInstruction> blck = safeParseNextNamedSequence();
        this.docBuilder.popIgnore();
        return new ShnapScopeBlock(loc, blck.getA(), blck.getB());
    }

    public ShnapTryCatchBlock parseNextTryBlock() {
        this.docBuilder.pushIgnore();
        ShnapLoc loc = this.loc();
        stream.next(3);
        whitespace();
        ShnapInstruction tryBlock = parseNextScopeBlock();
        whitespace();
        ShnapInstruction catchBlock;
        String varName = "it";
        if (this.tokenIsNext("catch")) {
            stream.next(5);
            whitespace();
            if (isVarRefNext()) {
                varName = nextVarRef();
            }
            whitespace();
            catchBlock = parseNextScopeBlock();
        } else {
            catchBlock = new ShnapNoOp(this.loc());
        }

        this.docBuilder.popIgnore();
        return new ShnapTryCatchBlock(loc, tryBlock, catchBlock, varName);
    }

    public boolean isTryBlockNext() {
        return this.tokenIsNext("try");
    }

    public ShnapDoWhileBlock parseNextDoWhileBlock() {
        this.docBuilder.pushIgnore();
        ShnapLoc loc = this.loc();
        stream.next(7);
        whitespace();
        boolean paren = stream.isNext("(");
        if (paren) {
            stream.consume('(');
        }
        whitespace();
        ShnapInstruction inst = safeNextInst();
        if (paren) {
            if (!stream.isNext(')')) {
                throw err("Expected )");
            }
            stream.consume(')');
        }
        Pair<ShnapLiteral, ShnapInstruction> seq = safeParseNextNamedSequence();
        this.docBuilder.popIgnore();
        return new ShnapDoWhileBlock(loc, seq.getA(), inst, seq.getB());
    }

    public boolean isDoWhileBlockNext() {
        return this.tokenIsNext("dowhile");
    }

    public ShnapWhileBlock parseNextWhileBlock() {
        this.docBuilder.pushIgnore();
        ShnapLoc loc = this.loc();
        stream.next(5);
        whitespace();
        boolean paren = stream.isNext("(");
        if (paren) {
            stream.consume('(');
        }
        whitespace();
        ShnapInstruction inst = safeNextInst();
        if (paren) {
            if (!stream.isNext(')')) {
                throw err("Expected )");
            }
            stream.consume(')');
        }
        Pair<ShnapLiteral, ShnapInstruction> seq = safeParseNextNamedSequence();
        this.docBuilder.popIgnore();
        return new ShnapWhileBlock(loc, seq.getA(), inst, seq.getB());
    }

    public boolean isWhileBlockNext() {
        return this.tokenIsNext("while");
    }

    public ShnapForBlock parseNextForBlock() {
        this.docBuilder.pushIgnore();
        ShnapLoc loc = this.loc();
        stream.next(3);
        whitespace();
        boolean paren = stream.isNext('(');

        if (paren) {
            stream.consume('(');
        }
        whitespace();

        String var = "it";
        boolean hasName = this.isForBlockVarColonNext();
        if (hasName) {
            var = nextVarRef();
        }
        whitespace();
        if (hasName) {
            if (!stream.isNext(":")) {
                throw err("Expected :");
            }
            stream.consume(':');
        }
        whitespace();
        ShnapInstruction inst = safeNextInst();
        if (paren) {
            if (!stream.isNext(')')) {
                throw err("Expected )");
            }
            stream.next();
        }
        whitespace();
        Pair<ShnapLiteral, ShnapInstruction> seq = safeParseNextNamedSequence();
        this.docBuilder.popIgnore();
        return new ShnapForBlock(loc, seq.getA(), var, inst, seq.getB());
    }

    private boolean isForBlockVarColonNext() {
        if (this.isVarRefNext()) {
            int index = this.stream.index();
            this.nextVarRef();
            whitespace();
            boolean flag = stream.isNext(':');
            stream.jumpTo(index);
            return flag;
        }

        return false;
    }

    public boolean isForBlockNext() {
        return this.tokenIsNext("for");
    }

    public Pair<ShnapLiteral, ShnapInstruction> safeParseNextNamedSequence() {
        whitespace();
        ShnapLoc loc = this.loc();
        ShnapStringNative name = null;
        if (this.isBlockNameNext()) {
            name = this.parseNextBlockName();
        }
        whitespace();
        return Pair.of(name == null ? null : new ShnapLiteral(loc, name), this.parseNextSequence());
    }

    public ShnapStringNative parseNextBlockName() {
        ShnapLoc loc = this.loc();
        String val = this.parseNextIdentifier();
        whitespace();
        stream.consume(':');
        return new ShnapStringNative(loc, val);
    }

    public boolean isBlockNameNext() {
        int index = stream.index();
        boolean flag = false;
        if (this.isVarRefNext()) {
            this.parseNextIdentifier();
            whitespace();
            if (stream.isNext(':')) {
                flag = true;
            }
        }

        stream.jumpTo(index);
        return flag;
    }

    public boolean isSequenceNext() {
        int index = stream.index();
        whitespace();
        boolean flag = stream.isNext('{');
        stream.jumpTo(index);
        return flag;
    }

    public ShnapInstruction parseNextSequence() {
        List<ShnapInstruction> instructions = new ArrayList<>();
        whitespace();
        ShnapLoc loc = this.loc();
        boolean seq = stream.isNext('{');
        if (seq) {
            stream.consume('{');
        }
        while (true) {
            whitespace();
            ShnapInstruction inst = nextInst();
            if (inst == null) {
                break;
            }
            instructions.add(inst);
            this.prepareNextStatement();
            if (!seq) {
                whitespace();
                if (stream.isNext("=>")) {
                    stream.next(2);
                } else {
                    break;
                }
            }
        }
        whitespace();
        if (seq) {
            stream.consume('}');
        }
        return new ShnapInstructionSequence(loc, instructions);
    }

    private boolean isSetOpNext() {
        if (stream.isNext('=') && !isEqualsCompareNext()) {
            return true;
        } else {
            for (String op : operatorSets) {
                if (stream.isNext(op)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isEqualsCompareNext() {
        return stream.isNext("==") || stream.isNext("===");
    }

    public String nextVarRef() {
        StringBuilder ref = new StringBuilder();
        while (stream.isNext('^')) {
            ref.append(stream.next().get());
        }
        if (stream.isNext(':')) {
            ref.append(stream.next().get());
        }
        ref.append(parseNextIdentifier());
        return ref.toString();
    }

    public boolean isVarRefNext() {
        int index = stream.index();
        stream.consumeAll('^');
        stream.consume(':');
        boolean flag = isIdentifierNext() && !(this.tokenIsNext("static") || isFlagNext() || isStateChangeNext() || isFalseTrueVoidNullNext() || isNativeNext() || isForBlockNext() || isTryBlockNext() || isWhileBlockNext() || isDoWhileBlockNext() || isIfBlockNext());
        stream.jumpTo(index);
        return flag;
    }

    public boolean isJustIdentifierNext() {
        int index = stream.index();
        boolean flag = isIdentifierNext() && !(this.tokenIsNext("static") || isFlagNext() || isStateChangeNext() || isFalseTrueVoidNullNext() || isNativeNext() || isForBlockNext() || isTryBlockNext() || isWhileBlockNext() || isDoWhileBlockNext() || isIfBlockNext());
        stream.jumpTo(index);
        return flag;
    }

    public ShnapGetNative parseGetNative() {
        ShnapLoc loc = this.loc();
        stream.next(6);
        whitespace();
        if (!stream.isNext("::")) {
            throw err("Expected ::");
        }
        stream.next(2);
        whitespace();
        if (!isIdentifierNext()) {
            throw err("Expected path identifier");
        }
        String id = parseNextPathIdentifier();
        return new ShnapGetNative(loc, id);
    }

    public boolean isNativeNext() {
        if (stream.isNext("native")) {
            int index = stream.index();
            stream.next("native");
            whitespace();
            boolean flag = stream.isNext("::");
            stream.jumpTo(index);
            return flag;
        }

        return false;
    }

    public String parseNextPathIdentifier() {
        StringBuilder builder = new StringBuilder();
        builder.append(stream.next().get());
        if (isDotPostWhitespaceNext()) {
            whitespace();
        }
        while (stream.isNext(Character::isJavaIdentifierPart) || stream.isNext('.')) {
            builder.append(stream.next().get());
            if (isDotPostWhitespaceNext()) {
                whitespace();
            }
        }
        return builder.toString();
    }

    private boolean isDotPostWhitespaceNext() {
        int index = stream.index();
        whitespace();
        boolean flag = stream.isNext('.');
        stream.jumpTo(index);
        return flag;
    }

    public String parseNextIdentifier() {
        StringBuilder builder = new StringBuilder();
        builder.append(stream.next().get());
        while (stream.isNext(Character::isJavaIdentifierPart)) {
            builder.append(stream.next().get());
        }
        return builder.toString();
    }

    public boolean isIdentifierNext() {
        return stream.isNext(Character::isJavaIdentifierStart);
    }

    public ShnapLiteral nextFalseTrueVoidNull() {
        ShnapLoc loc = this.loc();
        if (stream.isNext("false")) {
            stream.next(5);
            return new ShnapLiteral(loc, ShnapBooleanNative.of(false));
        } else if (stream.isNext("true")) {
            stream.next(4);
            return new ShnapLiteral(loc, ShnapBooleanNative.of(true));
        } else if (stream.isNext("null")) {
            stream.next(4);
            return new ShnapLiteral(loc, ShnapObject.getNull());
        } else {
            stream.next(4);
            return new ShnapLiteral(loc, ShnapObject.getVoid());
        }
    }

    public boolean isFalseTrueVoidNullNext() {
        return this.tokenIsNext("false") || this.tokenIsNext("true") || this.tokenIsNext("null") || this.tokenIsNext("void");
    }

    public ShnapInstruction nextArr() {
        ShnapInstruction arr;
        whitespace();
        ShnapLoc loc = this.loc();
        if (!stream.isNext('[')) {
            arr = new ShnapArrayLiteral(loc, Collections.emptyList());
        } else {
            stream.next();
            List<ShnapInstruction> params = new ArrayList<>();
            whitespace();
            if (stream.isNext(']')) {
                stream.next();
                arr = new ShnapArrayLiteral(loc, Collections.emptyList());
            } else {
                while (true) {
                    whitespace();
                    params.add(safeNextInst());
                    whitespace();
                    if (stream.isNext(',')) {
                        stream.next();
                        whitespace();
                        if (stream.isNext(']')) {
                            stream.next();
                            break;
                        }
                    } else {
                        if (!stream.isNext(']')) {
                            throw err("Expected ] or ,");
                        }
                        stream.next();
                        break;
                    }
                }

                arr = new ShnapArrayLiteral(loc, params);
            }
        }

        return arr;
    }

    public boolean isArrayNext() {
        return stream.isNext('[');
    }

    public ShnapLiteral nextChar() {
        ShnapLoc loc = this.loc();
        String val = Strings.cutFirst(Strings.cutLast(stream.nextUntil(charData.reset())));
        if (val.codePoints().count() > 1) {
            throw err(loc, "Expected single character");
        } else {
            return new ShnapLiteral(loc, new ShnapCharNative(loc, val.codePoints().toArray()[0]));
        }
    }

    public boolean isCharNext() {
        return stream.isNext('\'');
    }

    public ShnapLiteral nextString() {
        ShnapLoc loc = this.loc();
        String val;
        if (stream.isNext(tripleQuote)) {
            stream.next(3);
            StringBuilder str = new StringBuilder();
            tripleQuoteData = tripleQuoteData.reset();
            while (stream.hasNext()) {
                char z = stream.next().get();
                String s = tripleQuoteData.consider(z);
                str.append(s);
                if (tripleQuoteData.shouldConsider() && stream.isNext(tripleQuote)) {
                    break;
                }
            }
            str.append(tripleQuoteData.subTrailing());
            stream.next(3);
            val = str.toString();
        } else {
            val = Strings.cutFirst(stream.nextUntil(stringData.reset()));
            if (val.endsWith("\"")) {
                val = Strings.cutLast(val);
            }
        }

        return new ShnapLiteral(loc, new ShnapStringNative(loc, val));
    }

    public boolean isStringNext() {
        return stream.isNext('"');
    }

    public ShnapLiteral nextNumber() {
        ShnapLoc loc = this.loc();
        StringBuilder number = new StringBuilder();
        boolean decimal = false;
        parseSign(number);

        parseDigits(number);
        if (stream.isNext('.')) {
            stream.next();
            if (stream.isNext(digits)) {
                number.append(".");
                parseDigits(number);
                decimal = true;
            } else {
                stream.back();
            }
        }

        if (stream.isNext('e', 'E')) {
            decimal = true;
            number.append(stream.next().get());
            if (!isNumberNext()) {
                throw err("Expected exponent");
            }
            parseSign(number);
            parseDigits(number);
        }

        char suffix;
        if (stream.isNext('d', 'D', 'i', 'I')) {
            suffix = Character.toLowerCase(stream.next().get());
        } else {
            suffix = decimal ? 'd' : 'i';
        }

        Number num;
        if (suffix == 'd') {
            num = new BigDecimal(number.toString());
        } else if (suffix == 'i') {
            if (decimal) {
                num = new BigDecimal(number.toString()).toBigInteger();
            } else {
                num = new BigInteger(number.toString());
            }
        } else {
            throw new IllegalStateException("Unknown suffix: " + suffix);
        }

        return new ShnapLiteral(loc, ShnapNumberNative.valueOf(loc, num));
    }

    public boolean isNumberNext() {
        int index = stream.index();
        stream.consume('-', '+');
        whitespace();
        boolean flag = stream.isNext(digits);
        stream.jumpTo(index);
        return flag;
    }

    private void parseSign(StringBuilder num) {
        if (stream.isNext('+', '-')) {
            num.append(stream.next().get());
            whitespace();
        }
    }

    private void parseDigits(StringBuilder num) {
        while (stream.isNext(digits) || stream.isNext('_')) {
            if (stream.isNext('_')) {
                int index = stream.index();
                stream.next();
                if (!stream.isNext(digits)) {
                    stream.jumpTo(index);
                    return;
                }
            } else {
                num.append(stream.next().get());
            }
        }
    }

    public String parseScriptDocs() {
        this.stream.next(2);
        String docs = this.stream.nextUntil(")/");
        this.stream.next(2);
        return docs;
    }

    public String parseDocs() {
        this.stream.next(2);
        String docs = this.stream.nextUntil(">/");
        this.stream.next(2);
        return docs;
    }

    public boolean isScriptDocsNext() {
        return this.stream.isNext("/(");
    }

    public boolean isDocsNext() {
        return stream.isNext("/<");
    }

    private void whitespace() {
        this.stream.consumeAll(whitespace);
    }

    private ShnapParseError err(ShnapLoc loc, String message) {
        return new ShnapParseError(loc, message);
    }

    private ShnapParseError err(String message) {
        return err(this.loc(), message);
    }

    private ShnapLoc loc() {
        int[] pt = this.pts[this.stream.index()];
        return new ShnapLoc(pt[0], pt[1], this.building);
    }

    private boolean tokenIsNext(String token) {
        if (stream.isNext(token)) {
            int index = stream.index();
            stream.next(token.length());
            boolean flag = false;
            if (stream.hasNext()) {
                char c = stream.next().get();
                flag = !Character.isJavaIdentifierPart(c);
            } else {
                flag = true;
            }
            stream.jumpTo(index);
            return flag;
        }
        return false;
    }

}