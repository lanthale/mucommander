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

import com.mucommander.commons.runtime.OsFamily;
import com.mucommander.utils.text.Translator;
import com.mucommander.ui.action.*;
import com.mucommander.ui.main.MainFrame;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * Minimizes the {@link MainFrame} this action is associated with.
 *
 * @author Maxence Bernard
 * @see com.mucommander.ui.action.impl.MaximizeWindowAction
 */
public class MinimizeWindowAction extends TcAction {

    private MinimizeWindowAction(MainFrame mainFrame, Map<String, Object> properties) {
        super(mainFrame, properties);
    }

    @Override
    public void performAction() {
        mainFrame.setExtendedState(JFrame.ICONIFIED);
    }

	@Override
	public ActionDescriptor getDescriptor() {
		return new Descriptor();
	}


    public static final class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "MinimizeWindow";
    	
		public String getId() {
		    return ACTION_ID;
		}

		public ActionCategory getCategory() {
		    return ActionCategory.WINDOW;
		}

		public KeyStroke getDefaultAltKeyStroke() {
		    return null;
		}

		public KeyStroke getDefaultKeyStroke() {
		    return KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.META_DOWN_MASK);
		}

        @Override
        public String getLabel() {
            // Use a special label for Mac OS X, if it exists, use the standard action label otherwise
            String macLabelKey = ActionProperties.getActionLabelKey(ACTION_ID)+".mac_os_x";
            if (OsFamily.MAC_OS_X.isCurrent() && Translator.hasValue(macLabelKey, false)) {
                return Translator.get(macLabelKey);
            }
            return super.getLabel();
        }

        public TcAction createAction(MainFrame mainFrame, Map<String,Object> properties) {
            return new MinimizeWindowAction(mainFrame, properties);
        }
    }
}