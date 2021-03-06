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
package org.netbeans.modules.websvc.core.dev.wizard.nodes;

/** Service children (Port elements)
 *
 * @author mkuchtiak
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.modules.websvc.api.jaxws.wsdlmodel.WsdlPort;
import org.netbeans.modules.websvc.api.jaxws.wsdlmodel.WsdlService;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class ServiceChildren extends Children.Keys<WsdlPort> {
    WsdlService wsdlService;
    
    public ServiceChildren(WsdlService wsdlService) {
        this.wsdlService=wsdlService;
    }
    
    @Override
    protected void addNotify() {
        super.addNotify();
        updateKeys();
    }
    
    @Override
    protected void removeNotify() {
        setKeys(Collections.<WsdlPort>emptySet());
        super.removeNotify();
    }
       
    private void updateKeys() {
        List<WsdlPort> keys =  wsdlService.getPorts();
        setKeys(keys==null ? new ArrayList<WsdlPort>() : keys);
    }

    protected Node[] createNodes(WsdlPort key) {
        return new Node[] {new PortNode((WsdlPort)key)};
    }

}
