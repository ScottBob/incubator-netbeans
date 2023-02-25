/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.rust.grammar.ast;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.DefaultError;
import org.netbeans.modules.rust.grammar.antlr4.RustLexer;
import org.netbeans.modules.rust.grammar.antlr4.RustParser;
import org.openide.filesystems.FileObject;

/**
 * RustAST is responsible for parsing a Rust crate and detecting the major
 * components (structs, impls, etc.). While doing so it detects errors, folds,
 * and builds an AST. This is, of course, highly dependend on the grammar in
 * "RustParser.g4", because if you change the grammar the AST will also change.
 */
public final class RustAST {

    /**
     * Builds a RustAST for a given text in a fileObject.
     *
     * @param fileObject The FileObject containing the text.
     * @param text A text in the FileObject, it may or may not be the same
     * content as the file object (for instance, if the text is living in an
     * unsaved editor).
     * @return The RustAST corresponding to the text.
     */
    public static RustAST parse(FileObject fileObject, CharSequence text) {
        RustAST ast = new RustAST(fileObject);
        ast.parse(text);
        return ast;
    }

    /**
     * The list of errors.
     */
    private final List<DefaultError> errors = new ArrayList<>();

    /**
     * The visitor visiting nodes.
     */
    private RustASTVisitor visitor;

    /**
     * The crate that was last parsed.
     */
    private volatile RustASTNode crate;

    /**
     * Listens for syntax errors and keeps track of them in the "errors"
     * variable.
     */
    private final class RustANTLRErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            int errorPosition = 0;
            if (offendingSymbol instanceof Token) {
                Token offendingToken = (Token) offendingSymbol;
                errorPosition = offendingToken.getStartIndex();
                if (offendingToken.getChannel() == RustLexer.CHANNEL_COMMENT) {
                    return;
                }
            }
            errors.add(new DefaultError(null, msg, null, fileObject, errorPosition, errorPosition, Severity.ERROR));
        }
    }

    /**
     * The FileObject whose text we're parsing. We may or may not parse the
     * whole test of the FileObject.
     */
    private final FileObject fileObject;

    /**
     * Let's avoid people constructing RustASTs for now.
     *
     * @param fileObject The fileObject.
     */
    private RustAST(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    private RustASTNode parse(CharSequence text) {
        // Prepare an error listener to detect errors.
        ANTLRErrorListener errorListener = new RustANTLRErrorListener();
        // Prepare a lexer over this text
        RustLexer lexer = new RustLexer(CharStreams.fromString(String.valueOf(text)));
        lexer.addErrorListener(errorListener);
        // Prepare a parser over the lexer
        RustParser parser = new RustParser(new CommonTokenStream(lexer));
        parser.addErrorListener(errorListener);
        // Parse a crate.
        RustParser.CrateContext crate = parser.crate();
        // Now visit interesting nodes
        this.visitor = new RustASTVisitor();
        this.crate = crate.accept(visitor);
        return this.crate;
    }

    public List<DefaultError> getErrors() {
        return errors;
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public RustASTNode getCrate() {
        return crate;
    }

    public void cancel() {
        if (this.visitor != null) {
            this.visitor.cancel();
        }
    }

}
