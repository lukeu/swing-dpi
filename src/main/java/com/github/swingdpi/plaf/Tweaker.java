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

package com.github.swingdpi.plaf;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Icon;

/**
 * A tweaking delegate class which should be created with a short lifetime - to apply one round
 * of tweaking only.
 */
public interface Tweaker {

    void initialTweaks();

    Font modifyFont(Object key, Font original);

    Icon modifyIcon(Object key, Icon original);

    Dimension modifyDimension(Object key, Dimension original);

    Integer modifyInteger(Object key, Integer original);

    Insets modifyInsets(Object key, Insets original);

    void finalTweaks();
}
