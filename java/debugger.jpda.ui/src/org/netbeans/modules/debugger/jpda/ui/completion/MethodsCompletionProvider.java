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
package org.netbeans.modules.debugger.jpda.ui.completion;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClassIndex.SearchScopeType;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Martin Entlicher
 */
@MimeRegistration(mimeType = JavaMethodNbDebugEditorKit.MIME_TYPE, service = CompletionProvider.class)
public class MethodsCompletionProvider implements CompletionProvider {
    
    //private final Set<? extends SearchScopeType> scope = Collections.singleton(new ClassSearchScopeType());

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            
            @Override
            protected void query(final CompletionResultSet resultSet, Document doc, int caretOffset) {
                if (caretOffset < 0) caretOffset = 0;
                String text;
                try {
                    text = doc.getText(0, caretOffset);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                    text = "";
                }
                String className = (String) doc.getProperty("class-name");
                if (className == null) {
                    resultSet.finish();
                    return;
                }
                String packageName;
                String simpleClassName;
                int dot = className.lastIndexOf('.');
                if (dot > 0) {
                    packageName = className.substring(0, dot + 1); // We need the dot at the end
                    simpleClassName = className.substring(dot + 1);
                } else {
                    packageName = "";
                    simpleClassName = className;
                }
                Set<? extends SearchScopeType> scope = Collections.singleton(new ClassSearchScopeType(packageName));
                int n = text.length();
                ClasspathInfo cpi = ClassCompletionProvider.getClassPathInfo();
                ClassIndex classIndex = cpi.getClassIndex();
                Set<ElementHandle<TypeElement>> declaredTypes = classIndex.getDeclaredTypes(simpleClassName, ClassIndex.NameKind.PREFIX, scope);
                ElementHandle<TypeElement> theType = null;
                for (ElementHandle<TypeElement> type : declaredTypes) {
                    if (className.equals(type.getQualifiedName())) {
                        theType = type;
                        break;
                    }
                }
                if (theType != null) {
                    final ElementHandle<TypeElement> type = theType;
                    final int caret = caretOffset;
                    try {
                        JavaSource.create(cpi, new FileObject[]{}).runUserActionTask(new Task<CompilationController>() {
                            @Override
                            public void run(CompilationController cc) throws Exception {
                                TypeElement te = type.resolve(cc);
                                List<? extends Element> enclosedElements = te.getEnclosedElements();
                                for (Element elm : enclosedElements) {
                                    ElementKind kind = elm.getKind();
                                    if (kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR) {
                                        String name = elm.getSimpleName().toString();
                                        if ("<init>".equals(name)) {    // NOI18N
                                            name = te.getSimpleName().toString();
                                        }
                                        ElementCompletionItem eci = new ElementCompletionItem(name, kind, elm.getModifiers(), caret);
                                        eci.setExecutableElement((ExecutableElement) elm);
                                        resultSet.addItem(eci);
                                    }
                                }
                            }
                        }, true);
                    } catch (IOException ex) {
                        //Exceptions.printStackTrace(ex);
                        Logger.getLogger(MethodsCompletionProvider.class.getName()).log(Level.CONFIG, className, ex);
                    }
                }
                
                resultSet.finish();
            }
        }, component);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return COMPLETION_QUERY_TYPE;
    }
    
    
    
}
