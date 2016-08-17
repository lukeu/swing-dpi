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

package net.bettyluke.dpi.plaf;

import java.awt.Font;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.plaf.IconUIResource;

import net.bettyluke.dpi.DpiUtils;
import net.bettyluke.dpi.util.LoopBreakingScaledIcon;

public class WindowsTweaker extends BasicTweaker {

    /**
     * Certain fonts and metrics are pre-scaled, because the PLaF implementation asks Windows to
     * provide them. Therefore we need to 'undo' Window's scaling before applying our desired
     * scale-factor. This value holds the resulting scale-factor to apply to these UI elements.
     */
    protected final float alternateScaleFactor;

    /**
     * UI Defaults starting with these keys are handled by the WindowsIconFactory. The do an
     * instanceof check against some specific private inner classes (such as VistaMenuItemCheckIcon)
     * (rather than something public like IconUIResource) so that limits us: we simply can't
     * override them. It causes infinite loops from methods like getIconWidth().
     * <p>
     * Anyway, it shouldn't be too bad. They're also automatically scaled to the System-default
     * scaling. So assuming a typical user is trying to match that, they should be the correct
     * size already, or thereabouts. (NB: The user must log off & back in if they have changed the
     * system default DPI.)
     */
    private static final String[] PREFIX_TO_NOT_SCALE_ICONS = {
            "InternalFrame."//, "CheckBox", "Menu.", "MenuItem.", "RadioButton"
    };

    /**
     * UIDefaults starting with these strings are 'prescaled' and therefore require being scaled by
     * {@link #alternateScaleFactor}.
     */
    private static final String[] PRESCALED_INTEGER_PREFIXES = {
            "ScrollBar.", "InternalFrame.", "Menu.",  "MenuBar.", "MenuItem.",
            "CheckBoxMenuItem.", "RadioButtonMenuItem."
    };

    public WindowsTweaker(float scaleFactor) {

        // Windows already scales fonts, scrollbar sizes (etc) according to the system DPI settings.
        // (the same things which hopefully the heuristics in BasicTweaker manages to locate).
        super(scaleFactor);
        alternateScaleFactor = 100f * scaleFactor / DpiUtils.getCurrentScaling();
    }

    @Override
    public void finalTweaks() {
        super.finalTweaks();
    }

    @Override
    public Font modifyFont(Object key, Font original) {
        String keyString = key.toString();
        if (keyString.endsWith(".acceleratorFont") && !keyString.startsWith("MenuItem.") ||
                keyString.equals("ColorChooser.font") ||
                keyString.equals("TextArea.font")) {
            return super.modifyFont(key, original);
        }
        return (alternateScaleFactor == 1f) ? original :
                newScaledFontUIResource(original, alternateScaleFactor);
    }

    @Override
    public Integer modifyInteger(Object key, Integer original) {
        for (String prefix : PRESCALED_INTEGER_PREFIXES) {
            if (String.valueOf(key).startsWith(prefix)) {
                return scaleIntegerIfMetric(key, original, alternateScaleFactor);
            }
        }
        return super.modifyInteger(key, original);
    }

    @Override
    public Icon modifyIcon(Object key, Icon original) {

        // InternalFrame icons appear to choose their size programatically. (Possibly based on the
        // title font size?) They should not be rescaled at all.
        for (String prefix : PREFIX_TO_NOT_SCALE_ICONS) {
            if (String.valueOf(key).startsWith(prefix)) {
                return original;
            }
        }

        // WindowsIconFactory private icon implementations yield to other icons installed in the
        // UIDefaults. We need to use a special delegate that tricks them into believing that
        // they are actually still installed in the UIDefaults to get them to paint correctly.
        //
        // Examples: CheckBox.icon, Menu.arrowIcon, RadioButtonMenuItem.checkIcon
        //
        if (original.getClass().getName().contains("WindowsIconFactory")) {
            return newLoopBreakingScaledIcon(key, original, alternateScaleFactor);
        }


        // These icons appear to NOT include Windows-default scaling applied. Just scale using
        // the desired scale-factor directly.
        //
        // Examples: FileChooser.newFolderIcon, Tree.openIcon
        if (original instanceof IconUIResource || original instanceof ImageIcon) {
            return super.modifyIcon(key, original);
        }

        // Other icons appear to include Windows-default scaling already. That must be taken into
        // account to arrive at the desired scale-factor.
        //
        // Examples: RadioButtonMenuItem.arrowIcon, Table.ascendingSortIcon, Tree.expandedIcon
        return newScaledIconUIResource(original, alternateScaleFactor);
    }

    protected static Icon newLoopBreakingScaledIcon(Object key, Icon original, float scale) {
        if (scale == 1f && original instanceof IconUIResource) {
            return original;
        }
        return new ScaledIconUIResource(new LoopBreakingScaledIcon(key, original, scale));
    }
}
