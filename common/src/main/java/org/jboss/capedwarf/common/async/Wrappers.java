/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.capedwarf.common.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.google.apphosting.api.ApiProxy;
import org.jboss.capedwarf.common.apiproxy.CapedwarfDelegate;
import org.jboss.capedwarf.common.app.Application;
import org.jboss.capedwarf.common.config.CapedwarfEnvironment;
import org.jboss.capedwarf.common.threads.ExecutorFactory;

/**
 * Wrappers - env, CL, ApiProxy, ...
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class Wrappers {
    /**
     * Wrap runnable.
     *
     * @param runnable the runnable
     * @return wrapped runnable
     */
    public static Runnable wrap(Runnable runnable) {
        return new RunnableWrapper(runnable);
    }

    /**
     * Wrap callable.
     *
     * @param callable the callable
     * @return wrapped callable in future
     */
    public static <T> Future<T> wrap(final Callable<T> callable) {
        final ClassLoader appCL = Application.getAppClassloader();
        final CapedwarfEnvironment env = CapedwarfEnvironment.getThreadLocalInstance();

        return ExecutorFactory.wrap(new Callable<T>() {
            public T call() throws Exception {
                final ClassLoader old = SecurityActions.setThreadContextClassLoader(appCL);
                try {
                    CapedwarfEnvironment.setThreadLocalInstance(env);
                    try {
                        final ApiProxy.Delegate previous = ApiProxy.getDelegate();
                        ApiProxy.setDelegate(CapedwarfDelegate.INSTANCE);
                        try {
                            return callable.call();
                        } finally {
                            ApiProxy.setDelegate(previous);
                        }
                    } finally {
                        CapedwarfEnvironment.clearThreadLocalInstance();
                    }
                } finally {
                    SecurityActions.setThreadContextClassLoader(old);
                }
            }
        });
    }

    private static class RunnableWrapper implements Runnable {
        private final ClassLoader appCL;
        private final CapedwarfEnvironment env;
        private final Runnable runnable;

        private RunnableWrapper(Runnable runnable) {
            this.appCL = Application.getAppClassloader();
            this.env = CapedwarfEnvironment.getThreadLocalInstance();
            this.runnable = runnable;
        }

        public void run() {
            final ClassLoader old = SecurityActions.setThreadContextClassLoader(appCL);
            try {
                CapedwarfEnvironment.setThreadLocalInstance(env);
                try {
                    final ApiProxy.Delegate previous = ApiProxy.getDelegate();
                    ApiProxy.setDelegate(CapedwarfDelegate.INSTANCE);
                    try {
                        runnable.run();
                    } finally {
                        ApiProxy.setDelegate(previous);
                    }
                } finally {
                    CapedwarfEnvironment.clearThreadLocalInstance();
                }
            } finally {
                SecurityActions.setThreadContextClassLoader(old);
            }
        }
    }
}