/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.transaction.startup;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.glassfish.api.Startup;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.logging.LogDomains;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;

/**
 * Service wrapper to only lookup the transaction recovery when there
 * are applications deployed since the actual service has ORB dependency.
 */
@Service
public class TransactionRecoveryWrapper implements Startup, PostConstruct {

    @Inject
    Habitat habitat;

    @Inject
    Events events;

    private static Logger _logger = LogDomains.getLogger(TransactionRecoveryWrapper.class, LogDomains.JTA_LOGGER);

    private JavaEETransactionManager tm = null;

    @Override
    public void postConstruct() {
        EventListener glassfishEventListener = new EventListener() {
            @Override
            public void event(Event event) {
                if (event.is(EventTypes.SERVER_READY)) {
                    _logger.fine("TM RECOVERY WRAPPER - ON READY");
                    onReady();
                } else if (event.is(EventTypes.PREPARE_SHUTDOWN)) {  
                    _logger.fine("TM RECOVERY WRAPPER - ON SHUTDOWN");
                    onShutdown();
                }
            }
        };
        events.register(glassfishEventListener);
    }

    public void onReady() {
        _logger.fine("TM RECOVERY WRAPPER - ON READY STARTED");

        tm = habitat.getByContract(JavaEETransactionManager.class);
        tm.initRecovery(false);

        _logger.fine("TM RECOVERY WRAPPER - ON READY FINISHED");
    }

    public void onShutdown() {
        // Cleanup
        _logger.fine("ON TM SHUTDOWN STARTED");
        if (tm == null) {
            tm = habitat.getByContract(JavaEETransactionManager.class);
        }
        tm.shutdown();
        _logger.fine("ON TM SHUTDOWN FINISHED");

    }

    public Startup.Lifecycle getLifecycle() {
        return Startup.Lifecycle.SERVER;
    }

}
