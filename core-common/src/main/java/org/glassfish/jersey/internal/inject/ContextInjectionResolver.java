/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
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
package org.glassfish.jersey.internal.inject;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.InjecteeImpl;

/**
 * Injection resolver for {@link Context @Context} injection annotation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
public class ContextInjectionResolver implements InjectionResolver<Context> {

    /**
     * Context injection resolver HK2 binder.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            // @Context
            bind(ContextInjectionResolver.class).to(new TypeLiteral<InjectionResolver<Context>>() {
            }).in(Singleton.class);
        }
    }

    @Inject
    private ServiceLocator serviceLocator;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        Type requiredType = injectee.getRequiredType();
        boolean isHk2Factory = ReflectionHelper.isSubClassOf(requiredType, Factory.class);
        Injectee newInjectee;

        if (isHk2Factory) {
            newInjectee = getInjectee(injectee, ReflectionHelper.getTypeArgument(requiredType, 0));
        } else {
            newInjectee = injectee;
        }

        ActiveDescriptor<?> ad = serviceLocator.getInjecteeDescriptor(newInjectee);
        if (ad != null) {
            final ServiceHandle handle = serviceLocator.getServiceHandle(ad);

            if (isHk2Factory) {
                return asFactory(handle);
            } else {
                return handle.getService();
            }
        }
        return null;
    }

    private Factory asFactory(final ServiceHandle handle) {
        return new Factory() {
            @Override
            public Object provide() {
                return handle.getService();
            }

            @Override
            public void dispose(Object instance) {
                //not used
            }
        };
    }

    private Injectee getInjectee(final Injectee injectee, final Type requiredType) {
        return new RequiredTypeOverridingInjectee(injectee, requiredType);
    }

    private static class RequiredTypeOverridingInjectee extends InjecteeImpl {

        private static final long serialVersionUID = -3740895548611880187L;

        private RequiredTypeOverridingInjectee(final Injectee injectee, final Type requiredType) {
            super(injectee);
            setRequiredType(requiredType);
        }
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
