/*
 * Copyright 2016 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This project is hosted at: https://github.com/lukeu/swing-dpi
 * Comments & collaboration are both welcome.
 */

package net.bettyluke.dpi.util;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * This class exists to work-in with functionality of the Java-internal class
 * WindowsIconFactory, which checks to see if its own inner-classes are the ones registered with
 * the UIDefaults. If they are not, it's methods delegates to the object it finds. That's a problem
 * because the object which we install there wants to delegate to IT - and then post-scale it.
 * <p>
 * So the various methods all end up in an infinite "you do it", "no you do it", "I don't want to
 * do it, you do it" argument (stack-overflow).
 * <p>
 * This is circumvented by reinstating our delegate into the UIDefault temporarily during the
 * duration of the method calls, and putting ourselves back in afterwards.
 */
public class LoopBreakingScaledIcon extends ScaledIcon {

    private Object key;

    public LoopBreakingScaledIcon(Object key, Icon icon, float scaleFactor) {
        super(icon, scaleFactor);
        this.key = key;
    }

    @Override
    public int getIconWidth() {
        Object previous = UIManager.getLookAndFeelDefaults().put(key, delegate);
        try {
            return super.getIconWidth();
        } finally {
            UIManager.getLookAndFeelDefaults().put(key, previous);
        }
    }

    @Override
    public int getIconHeight() {
        Object previous = UIManager.getLookAndFeelDefaults().put(key, delegate);
        try {
            return super.getIconHeight();
        } finally {
            UIManager.getLookAndFeelDefaults().put(key, previous);
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {

        // NOTE: if the UIManager has a bean-style property listener attached then this could
        // _potentially_ cause costs that we would not normally like during a paint operation.
        // However in typical usage there doesn't seem to ever be one installed. (Presumably it is
        // there for GUI-builder scenarios, where the builder might want to observe changes(?))
        Object previous = UIManager.getLookAndFeelDefaults().put(key, delegate);
        try {
            super.paintIcon(c, g, x, y);
        } finally {
            UIManager.getLookAndFeelDefaults().put(key, previous);
        }
    }
}
