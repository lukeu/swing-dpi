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

package net.bettyluke.dpi;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Collections;
import java.util.IdentityHashMap;

import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import net.bettyluke.dpi.plaf.BasicTweaker;
import net.bettyluke.dpi.plaf.MetalTweaker;
import net.bettyluke.dpi.plaf.NimbusTweaker;
import net.bettyluke.dpi.plaf.Tweaker;
import net.bettyluke.dpi.plaf.WindowsTweaker;

/**
 * Applies changes to Swing's {@code UIDefaults} to perform a best-effort default scaling for many
 * parts of the UI, according to a specified scaling percentage.
 * <p>
 * A few other tweaks are made at the same time, to adjust the UI metrics to reflect more modern
 * practices - like table & tree row sizing on all platforms and changing the Windows UI font to be
 * more like Windows 7, less like Windows 95.
 * <p>
 * For example, see internal comments of JDK class {@code BasicTableUI#installDefaults}
 * about how Sun realised that table-row heights were incorrect on each platform, but it became
 * too risky to change them after release.
 */
public class UiDefaultsScaler {

    private final Tweaker delegate;

    private UiDefaultsScaler(Tweaker delegate) {
        this.delegate = delegate;
    }

    public static void updateAndApplyGlobalScaling(int scalingInPercent, boolean alsoTweak) {
        float scaleFactor = scalingInPercent / 100f;

        BasicTweaker tweaker = createTweakerForCurrentLook(scaleFactor);
        tweaker.setDoExtraTweaks(alsoTweak);

        UiDefaultsScaler scaler = new UiDefaultsScaler(tweaker);
        scaler.applyScalingAndTweaks();

        // Updates the global constant, which can be used for apply scaling to UI elements not
        // covered by the UIDefaults. This also fires a notification event to anyone interested.
        UiScaling.setScaling(scalingInPercent);
    }

    private void applyScalingAndTweaks() {
        delegate.initialTweaks();
        modifyDefaults(delegate);
        delegate.finalTweaks();
    }

    private static BasicTweaker createTweakerForCurrentLook(float dpiScaling) {
        String testString = UIManager.getLookAndFeel().getName().toLowerCase();
        if (testString.contains("windows")) {
            return new WindowsTweaker(dpiScaling, testString.contains("classic"));
        }
        if (testString.contains("metal")) {
            return new MetalTweaker(dpiScaling);
        }
        if (testString.contains("nimbus")) {
            return new NimbusTweaker(dpiScaling);
        }
        return new BasicTweaker(dpiScaling);
    }

    private void modifyDefaults(Tweaker tweaker) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        // Used to replicate aliased-references to the same object wherever the original did this.
        IdentityHashMap<Object, Object> identityMap = new IdentityHashMap<Object, Object>();

        for (Object key: Collections.list(defaults.keys())) {
            Object original = defaults.get(key);
            if (identityMap.keySet().contains(original)) {
                continue;
            }
            Object newValue = modifyValueUsingDelegate(tweaker, key, original);
            if (newValue != null && newValue != original) {
                defaults.put(key, newValue);
            }
        }
    }

    /**
     * @return {@code null} if the value was not of an type known to possibly need modification,
     *         {@code value} if delegated but no modification is made, otherwise a modified value.
     */
    private Object modifyValueUsingDelegate(Tweaker tweaker, Object key, Object original) {
        if (original instanceof Font) {
            return tweaker.modifyFont(key, (Font) original);
        }
        if (original instanceof Icon) {
            return tweaker.modifyIcon(key, (Icon) original);
        }
        if (original instanceof Dimension) {
            return tweaker.modifyDimension(key, (Dimension) original);
        }
        if (original instanceof Integer) {
            return tweaker.modifyInteger(key, (Integer) original);
        }
        if (original instanceof Insets) {
            return tweaker.modifyInsets(key, (Insets) original);
        }
        return null;
    }
}
