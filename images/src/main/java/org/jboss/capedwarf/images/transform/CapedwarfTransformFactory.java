/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.capedwarf.images.transform;

import com.google.appengine.api.images.Transform;

/**
 * Creates a CapedwarfTransform based on the supplied GAE Transform.
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public class CapedwarfTransformFactory {

    public static CapedwarfTransform createJBossTransform(Transform transform) {
        String transformClassName = transform.getClass().getName();
        if ("com.google.appengine.api.images.Resize".equals(transformClassName)) {
            return new ResizeTransform(transform);
        } else if ("com.google.appengine.api.images.HorizontalFlip".equals(transformClassName)) {
            return new HorizontalFlipTransform(transform);
        } else if ("com.google.appengine.api.images.VerticalFlip".equals(transformClassName)) {
            return new VerticalFlipTransform(transform);
        } else if ("com.google.appengine.api.images.Rotate".equals(transformClassName)) {
            return new RotateTransform(transform);
        } else if ("com.google.appengine.api.images.Crop".equals(transformClassName)) {
            return new CropTransform(transform);
        } else if ("com.google.appengine.api.images.ImFeelingLucky".equals(transformClassName)) {
            return new ImFeelingLuckyTransform(transform);
        } else if ("com.google.appengine.api.images.CompositeTransform".equals(transformClassName)) {
            return new CompositeTransform(transform);
        }
        throw new RuntimeException("Unsupported transform " + transform);
    }
}
