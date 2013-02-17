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

package org.jboss.capedwarf.aspects.proxy;

import java.lang.annotation.Annotation;

import org.jboss.capedwarf.aspects.Aspect;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class AspectWrapper {
    private Aspect aspect;
    private Annotation annotation;

    AspectWrapper(Aspect aspect) {
        this(aspect, null);
    }

    AspectWrapper(Aspect aspect, Annotation annotation) {
        this.aspect = aspect;
        this.annotation = annotation;
    }

    public int priority() {
        return aspect.priority();
    }

    public Object invoke(AspectContext context) throws Throwable {
        return aspect.invoke(context);
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public int hashCode() {
        return aspect.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof AspectWrapper == false)
            return false;

        AspectWrapper other = (AspectWrapper) obj;
        return aspect.equals(other.aspect);
    }
}
