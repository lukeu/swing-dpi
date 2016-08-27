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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class storing an observable static/global UI scaling factor, and some utility methods to scale
 * various types and metrics by that value.
 */
public class UiScaling {
    private UiScaling() {}

    /**
     * Scaling in integer percentage points; so 100 results in the 'standard' setting of 96 DPI.
     * Threading: it is assumed that all access will occur from the EDT (in which case 'volatile'
     * is somewhat superfluous and defensive).
     */
    private static volatile int s_scalingPercentage = DpiUtils.getClosestStandardScaling();

    /**
     * Threading: all access must synchronize on this final member
     */
    private static final List<ChangeListener> s_changeListeners = new ArrayList<ChangeListener>();

    /**
     * @return the currently-set scaling in percentage points.
     */
    public static int getScaling() {
        assert SwingUtilities.isEventDispatchThread();

        return s_scalingPercentage;
    }

    public static void setScaling(int scalingInPercent) {
        assert SwingUtilities.isEventDispatchThread();

        if (s_scalingPercentage != scalingInPercent) {
            s_scalingPercentage = scalingInPercent;
            notifyListeners();
        }
    }

    /**
     * Public to allow users to notify listeners of similar events (like L&F changes) without
     * needing a second listener & event to register on. (For better or worse.)
     */
    private static void notifyListeners() {
        List<ChangeListener> listeners;
        synchronized (s_changeListeners) {
            listeners = new ArrayList<ChangeListener>(s_changeListeners);
        }

        for (ChangeListener changeListener : listeners) {
            changeListener.stateChanged(new ChangeEvent(UiScaling.class));
        }
    }

    /**
     * Be notified when L&F or scaling-level has changed.
     * <p>
     * As always when listening on static objects: take care to avoid memory leaks by removing
     * listeners again, as these may continue to hold references to otherwise shorter-lived objects.
     */
    public static void addChangeListener(ChangeListener listener) {
        synchronized (s_changeListeners) {
            s_changeListeners.add(listener);
        }
    }

    /**
     * Threading note: it is possible that listeners will still be notified of events for a short
     * period after this method has exited, unless this method is called from the EDT.
     */
    public static void removeChangeListener(ChangeListener listener) {
        synchronized (s_changeListeners) {
            s_changeListeners.remove(listener);
        }
    }

    public static int scale(int i) {
        return Math.round((i * s_scalingPercentage) / 100f);
    }

    public static Dimension scale(Dimension dim) {
        return (s_scalingPercentage == 100) ? dim :
                new Dimension(scale(dim.width), scale(dim.height));
    }
}
