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

package net.bettyluke.dpi.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.UIManager;

public class ScaledIcon implements Icon {
    protected final Icon delegate;
    protected final float scaleFactor;
    private final AffineTransformOp scaleOperation;
    private SoftReference<ImageIcon> enabledIcon = new SoftReference<ImageIcon>(null);
    private SoftReference<ImageIcon> disabledIcon = new SoftReference<ImageIcon>(null);

    public ScaledIcon(Icon icon, float scaleFactor) {

        // Ensure we don't repeatedly scale icons. Callers must have reset the L&F before
        // calling and take care not to scale an icon multiple times, e.g. when iterating the
        // UI-defaults. This can be tricky since UI-defaults may cross-reference each other.
        assert !(icon instanceof ScaledIcon) &&
                !icon.getClass().getSimpleName().contains("ScaledIconUIResource") :
                        "Icon is a:  " + icon.getClass().getName();

        delegate = icon;
        this.scaleFactor = scaleFactor;
        scaleOperation = new AffineTransformOp(
                AffineTransform.getScaleInstance(scaleFactor, scaleFactor),
                AffineTransformOp.TYPE_BICUBIC);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {

        // Weird, this actually happens in Motif. (* roll-eyes *)
        if (getIconWidth() <= 0 || getIconHeight() <= 0) {
            delegate.paintIcon(c, g, x, y);
            return;
        }

        boolean renderEnabled = !(c instanceof AbstractButton) || c.isEnabled();
        ImageIcon icon = renderEnabled ? enabledIcon.get() : disabledIcon.get();
        if (icon == null) {
            icon = new ImageIcon(paintToImageThenScale(c));
            if (delegate instanceof ImageIcon) {
                if (renderEnabled) {
                    enabledIcon = new SoftReference<ImageIcon>(icon);
                } else {

                    // Note that LookAndFeel#getDisabledIcon only operates upon ImageIcon (despite
                    // having a parameter that takes any Icon). Therefore if 'delegate' is an
                    // ImageIcon we need to render it disabled ourselves, since this class does
                    // not extend ImageIcon.
                    if (c instanceof JComponent) {
                        icon = (ImageIcon) UIManager.getLookAndFeel().getDisabledIcon(
                                (JComponent) c, icon);
                    }
                    disabledIcon = new SoftReference<ImageIcon>(icon);
                }
            }
        }
        icon.paintIcon(c, g, x, y);
    }

    /**
     * Paints to an image at 100% then performs bicubic scaling to 'scaleFactor'. This approach has
     * consistently produced better quality results. Although some icon painting might use
     * primitive operations (rather than drawing images), it has actually been found to be
     * problematic. For example, the dot in Metal Radio buttons looks 'cracked' at 150% scaling;
     * it appears they draw a Rectangle and 4 lines as an optimisation, and the pieces break
     * apart!
     *
     * TODO: Consider a whitelist of icons that render better directly to a scaled Graphics2D
     * instance? As yet I've not actually seen one, but then I haven't checked through them.
     *
     * UPDATE: huh, this also hit HiDPI in JDK-9: https://bugs.openjdk.java.net/browse/JDK-8160986
     * and looks to have been fixed just a few days ago:
     * http://hg.openjdk.java.net/jdk9/jdk9/jdk/rev/a8d963d7d32d
     */
    private BufferedImage paintToImageThenScale(Component c) {
        BufferedImage image = new BufferedImage(
                getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = null;
        try {

            // While this extra copy may seem like a cost, in ONE **very rough** test (radio buttons
            // in Windows L&F @ 150% scaling) it only added 10% to the mean execution time of this
            // method. Given that, the decision should be more about quality not performance.
            BufferedImage unscaledImage = new BufferedImage(
                    delegate.getIconWidth(), delegate.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            g2 = unscaledImage.createGraphics();
            delegate.paintIcon(c, g2, 0, 0);
            g2.dispose();

            g2 = image.createGraphics();
            g2.drawImage(unscaledImage, scaleOperation, 0, 0);
        } finally {
            if (g2 != null) {
                g2.dispose();
            }
        }
        return image;
    }

    /*- Alternative rendering method, kept temporarily in case it proves useful...
    private BufferedImage paintToScaledImage(Component c) {
        BufferedImage image =
                new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = null;
        try {
            g2 = image.createGraphics();
            g2.scale(scaleFactor, scaleFactor);
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            delegate.paintIcon(c, g2, 0, 0);
        } finally {
            if (g2 != null) {
                g2.dispose();
            }
        }
        return image;
    }
    */

    @Override
    public int getIconWidth() {
        return Math.round(delegate.getIconWidth() * scaleFactor);
    }

    @Override
    public int getIconHeight() {
        return Math.round(delegate.getIconHeight() * scaleFactor);
    }
}
