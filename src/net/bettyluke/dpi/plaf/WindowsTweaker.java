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
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

import net.bettyluke.dpi.DpiUtils;
import net.bettyluke.dpi.util.LoopBreakingScaledIcon;

public class WindowsTweaker extends BasicTweaker {

    /**
     * Certain fonts and metrics are pre-scaled, because the PLaF implementation asks Windows to
     * provide them. Therefore we need to 'undo' Window's scaling before applying our desired
     * scale-factor. This value holds the resulting scale-factor to apply to these UI elements.
     */
    protected final float alternateScaleFactor;

    protected final Font optionPaneFont;

    protected final boolean windowsClassic;

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

    private static final String BUTTON_DASHED_RECT_PREFIX = "Button.dashedRectGap";

    public WindowsTweaker(float scaleFactor, boolean classic) {

        // Windows already scales fonts, scrollbar sizes (etc) according to the system DPI settings.
        // (the same things which hopefully the heuristics in BasicTweaker manages to locate).
        super(scaleFactor);
        alternateScaleFactor = 100f * scaleFactor / DpiUtils.getSystemScaling();
        optionPaneFont = UIManager.getFont("OptionPane.font");
        windowsClassic = classic;
    }

    @Override
    public void initialTweaks() {
        super.initialTweaks();
    }

    @Override
    public void finalTweaks() {
        super.finalTweaks();
        try {
            int x = (Integer) UIManager.get(BUTTON_DASHED_RECT_PREFIX + "X");
            int y = (Integer) UIManager.get(BUTTON_DASHED_RECT_PREFIX + "Y");
            UIManager.put(BUTTON_DASHED_RECT_PREFIX + "Width", x * 2);
            UIManager.put(BUTTON_DASHED_RECT_PREFIX + "Height", y * 2);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Font modifyFont(Object key, Font original) {
        String keyString = key.toString();
        Font font = maybeSubstituteFont(keyString, original);
        if (isFontUnscaled(keyString)) {
            return super.modifyFont(key, font);
        }
        return isUnscaled(alternateScaleFactor) ? font :
                newScaledFontUIResource(font, alternateScaleFactor);
    }

    private Font maybeSubstituteFont(String key, Font original) {
        if (makeModern() && "Tahoma".equals(original.getFamily()) && !key.equals("Panel.font")) {
            return optionPaneFont;
        }
        return original;
    }

    private boolean isFontUnscaled(String keyString) {
        return keyString.endsWith(".acceleratorFont") && !keyString.startsWith("MenuItem.") ||
                keyString.equals("ColorChooser.font") ||
                keyString.equals("TextArea.font");
    }

    @Override
    public Integer modifyInteger(Object key, Integer original) {
        if (key.toString().startsWith(BUTTON_DASHED_RECT_PREFIX)) {

            // Reduce how far into the button the keyboard selection moves. Otherwise it starts
            // crossing the text at higher scaling levels.
            return Math.round(original * (1f + scaleFactor) * 0.5f);
        }
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
        String className = original.getClass().getName();
        if (className.contains("WindowsIconFactory")) {

            // Deep inside some sun-private UI implementation code, there's some sanity checks
            // calling "instanceof VistaMenuItemCheckIcon" to ensure the Vista icons are
            // "compatible" (whatever that means). If we change them, they're not "compatible",
            // and the menu layout falls back to the default (like Metal) and looks weird.
            //
            // Not much we can do. :-( Still, if the user's scaling is between +/- 25% of the
            // primary monitor then this will probably look all right. This is bound to cover
            // most typical scenarios. If not, the size of Check and Radio buttons in menus will be
            // very mismatched.
            if (className.endsWith("VistaMenuItemCheckIcon")) {
                return original;
            }
            return newLoopBreakingScaledIcon(key, original, alternateScaleFactor);
        }

        // These icons appear to NOT include Windows-default scaling applied. Just scale using
        // the desired scale-factor directly.
        //
        // Examples: FileChooser.newFolderIcon, Tree.openIcon
        if (original instanceof UIResource || original instanceof ImageIcon) {
            return super.modifyIcon(key, original);
        }

        // Other icons appear to include Windows-default scaling already. That must be taken into
        // account to arrive at the desired scale-factor.
        //
        // Examples: RadioButtonMenuItem.arrowIcon, Table.ascendingSortIcon, Tree.expandedIcon
        return newScaledIconUIResource(original, alternateScaleFactor);
    }

    /**
     * Stick with Java's Windows 95/XP styling if 'classic' Windows is specified or if tweaking
     * UI has been globally disabled. Otherwise try and catch up a bit with the current decade.
     */
    private boolean makeModern() {
        return doExtraTweaks && !windowsClassic;
    }

    protected static Icon newLoopBreakingScaledIcon(Object key, Icon original, float scale) {
        if (isUnscaled(scale) && original instanceof UIResource) {
            return original;
        }
        return new ScaledIconUIResource(new LoopBreakingScaledIcon(key, original, scale));
    }
}
