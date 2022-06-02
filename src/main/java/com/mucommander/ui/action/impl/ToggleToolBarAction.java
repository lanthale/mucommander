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

package com.mucommander.ui.action.impl;

import com.mucommander.conf.TcConfigurations;
import com.mucommander.conf.TcPreference;
import com.mucommander.conf.TcPreferences;
import com.mucommander.utils.text.Translator;
import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionDescriptor;
import com.mucommander.ui.action.TcAction;
import com.mucommander.ui.main.MainFrame;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.util.Map;

/**
 * This action shows/hides the current MainFrame's {@link com.mucommander.ui.main.toolbar.ToolBar} depending on its
 * current visible state: if it is visible, hides it, if not shows it.
 *
 * <p>This action's label will be updated to reflect the current visible state.
 *
 * <p>Each time this action is executed, the new current visible state is stored in the configuration so that
 * new MainFrame windows will use it to determine whether the ToolBar has to be made visible or not.
 *
 * @author Maxence Bernard
 */
public class ToggleToolBarAction extends TcAction {

    private ToggleToolBarAction(MainFrame mainFrame, Map<String, Object> properties) {
        super(mainFrame, properties);
        updateLabel(TcConfigurations.getPreferences().getVariable(TcPreference.TOOLBAR_VISIBLE, TcPreferences.DEFAULT_TOOLBAR_VISIBLE));
    }

    private void updateLabel(boolean visible) {
        setLabel(Translator.get(visible?Descriptor.ACTION_ID+".hide":Descriptor.ACTION_ID+".show"));
    }

    @Override
    public void performAction() {
        JPanel toolBarPanel = mainFrame.getToolBarPanel();
        boolean visible = !toolBarPanel.isVisible();
        // Save the last toolbar visible state in the configuration, this will become the default for new MainFrame windows.
        TcConfigurations.getPreferences().setVariable(TcPreference.TOOLBAR_VISIBLE, visible);
        // Change the label to reflect the new toolbar state
        updateLabel(visible);
        // Show/hide the toolbar
        toolBarPanel.setVisible(visible);
        mainFrame.validate();
    }

	@Override
	public ActionDescriptor getDescriptor() {
		return new Descriptor();
	}


    public static final class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "ToggleToolBar";
    	
		public String getId() {
		    return ACTION_ID;
		}

		public ActionCategory getCategory() {
		    return ActionCategory.VIEW;
		}

		public KeyStroke getDefaultAltKeyStroke() {
		    return null;
		}

		public KeyStroke getDefaultKeyStroke() {
		    return null;
		}

        @Override
        public String getLabelKey() {
		    return ACTION_ID + ".show";
		}

        public TcAction createAction(MainFrame mainFrame, Map<String,Object> properties) {
            return new ToggleToolBarAction(mainFrame, properties);
        }
    }
}