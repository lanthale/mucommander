/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.dialog.pref.theme;

import com.mucommander.ui.chooser.*;
import com.mucommander.ui.dialog.pref.PreferencesDialog;
import com.mucommander.ui.theme.ThemeData;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * @author Maxence Bernard, Nicolas Rinaudo
 */
class ColorButton extends JPanel implements ActionListener, ColorChangeListener {

    /** ThemeData from which to retrieve the color chooser's values. */
    private final ThemeData themeData;

    /** Identifier of the color that's being edited. */
    private final int colorId;

    /** Dialog on which the color chooser should be centered and modal to. */
    private final PreferencesDialog parent;

    /** The preview component that is repainted when the current color changes (can be null) */
    private final JComponent previewComponent;

    /** Name of the preview component's property that gets updated with the current color of this button (can be null) */
    private final String previewColorPropertyName;

    private java.util.List<JComponent> updatedPreviewComponents;

    /** Button's border. */
    private final Border border = BorderFactory.createEtchedBorder();

    /** The color button */
    private final JButton button;

    /** Current color displayed in the button */
    private Color currentColor;


    public ColorButton(PreferencesDialog parent, ThemeData themeData, int colorId) {
        this(parent, themeData, colorId, null, null);
    }

    ColorButton(PreferencesDialog parent, ThemeData themeData, int colorId, String previewColorPropertyName) {
        this(parent, themeData, colorId, previewColorPropertyName, null);
    }

    ColorButton(PreferencesDialog parent, ThemeData themeData, int colorId, String previewColorPropertyName, JComponent previewComponent) {
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setHgap(0);
        flowLayout.setVgap(0);
        setLayout(flowLayout);

        this.themeData = themeData;
        this.colorId = colorId;
        this.parent = parent;
        this.previewComponent = previewComponent;
        this.previewColorPropertyName = previewColorPropertyName;

        if (previewColorPropertyName != null && previewComponent != null) {
            addUpdatedPreviewComponent(previewComponent);
        }
 
        button = new JButton() {
            @Override
            public Dimension getPreferredSize() {return new Dimension(70, 30);}

            @Override
            public void paint(Graphics g) {
                int width  = getWidth();
                int height = getHeight();

                // Fill the button with the specified color
                g.setColor((ColorButton.this).currentColor);
                g.fillRect(0, 0, width, height);

                // Paint custom border
                border.paintBorder(this, g, 0, 0, width, height);
            }
        };

        button.addActionListener(this);
        button.setBorderPainted(true);

        add(button);

        // Add a ColorPicker only if this component is supported by the current environment
        if (ColorPicker.isSupported()) {
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.addColorChangeListener(this);
            add(colorPicker);
        }

        setCurrentColor(themeData.getColor(colorId), false);
    }


    void addUpdatedPreviewComponent(JComponent previewComponent) {
        if (previewColorPropertyName == null) {
            return;
        }
        if (updatedPreviewComponents == null) {
            updatedPreviewComponents = new ArrayList<>();
        }
        updatedPreviewComponents.add(previewComponent);
        previewComponent.putClientProperty(previewColorPropertyName, currentColor);
    }

	int getColorId() {
		return colorId;
	}

	Color getCurrentColor() {
		return currentColor;
	}

    private void setCurrentColor(Color color, boolean initiatedByUser) {
        currentColor = color;
        if (themeData.isColorDifferent(colorId, currentColor)) {
            initiatedByUser &= themeData.setColor(colorId, currentColor);
        }
        button.repaint();

        if (updatedPreviewComponents != null && previewColorPropertyName != null) {
            for (JComponent updatedPreviewComponent : updatedPreviewComponents) {
                updatedPreviewComponent.putClientProperty(previewColorPropertyName, color);
            }
        }
        if (initiatedByUser) {
            parent.componentChanged(null);
        }
    }


    private ColorChooser createColorChooser() {
        if(previewComponent!=null && previewColorPropertyName!=null && (previewComponent instanceof PreviewLabel)) {
            try {
                return new ColorChooser(currentColor, (PreviewLabel)((PreviewLabel)previewComponent).clone(), previewColorPropertyName);
            } catch(CloneNotSupportedException ignored) {}
        }
        return new ColorChooser(currentColor, new PreviewLabel(), PreviewLabel.BACKGROUND_COLOR_PROPERTY_NAME);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        ColorChooser chooser = createColorChooser();
        ColorChooser.createDialog(parent, chooser).showDialog();

        setCurrentColor(chooser.getColor(), true);
    }

    @Override
    public void colorChanged(ColorChangeEvent event) {setCurrentColor(event.getColor(), true);}
}
