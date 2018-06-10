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

package net.bettyluke.dpi.plaf;

import java.awt.Font;

import javax.swing.plaf.FontUIResource;

/**
 * Purely a 'tagging class' - so we can detect instances of this to validate that we never
 * scale an font twice.
 */
public class ScaledFontUIResource extends FontUIResource {
    public ScaledFontUIResource(String name, int style, int newSize) {
        super(name, style, newSize);
    }

    public ScaledFontUIResource(Font font) {
        super(font);
    }
}
