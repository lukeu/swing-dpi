/*
 * Copyright 2016 Luke Usherwood.
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

package net.bettyluke.dpi;

import java.awt.Toolkit;

public class DpiUtils {

    public static final int[] STANDARD_SCALINGS = new int[] { 100, 125, 150, 200, 250, 300 };

    // This looks to never change (on Windows it requires logging out, plus there is no event to
    // monitor). It invokes a native call, and we don't know how expensive that call might be
    // on all platforms. This might be read in performance-critical areas, like painting.
    // Hence, we just cache this value.
    private static final int SCREEN_RESOLUTION_IN_DPI =
            Toolkit.getDefaultToolkit().getScreenResolution();

    /**
     * A scaling level of 100% represents 96 DPI on a 'typical' (2000-2010 era) monitor. (This value
     * is assumed by Windows. As far as screens go, DPI doesn't really represent inches at all.)
     */
    private static final int UNSCALED_DPI = 96;

    public static int getClosestStandardScaling() {
        return closest(getCurrentScaling(), STANDARD_SCALINGS);
    }

    public static int getClosestStandardScaling(int scaling) {
        return closest(scaling, STANDARD_SCALINGS);
    }

    /**
     * Returns the default scaling level of the primary monitor at the point the user logged in.
     * Although per-monitor scaling can be changed dynamically in Windows 8.1 and 10, this
     * value will not change until the user logs out.
     * <p>
     * So this value seems designed for "System scale factor" apps, as described here:
     *
     *   https://blogs.technet.microsoft.com/askcore/2015/12/08/display-scaling-in-windows-10/
     */
    public static int getCurrentScaling() {
        return Math.round((SCREEN_RESOLUTION_IN_DPI * 100f) / UNSCALED_DPI);
    }

    private static int closest(int of, int[] in) {
        int min = Integer.MAX_VALUE;
        int closest = of;

        for (int v : in) {
            int diff = Math.abs(v - of);
            if (diff < min) {
                min = diff;
                closest = v;
            }
        }
        return closest;
    }
}
