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

package com.mucommander.ui.dialog.pref.general;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;

import com.mucommander.conf.TcConfigurations;
import com.mucommander.conf.TcPreference;
import com.mucommander.conf.TcPreferences;
import com.mucommander.conf.TcPreferencesAPI;
import com.mucommander.utils.text.Translator;
import com.mucommander.ui.dialog.pref.PreferencesDialog;
import com.mucommander.ui.dialog.pref.PreferencesPanel;
import com.mucommander.ui.dialog.pref.component.PrefTextField;
import com.mucommander.ui.layout.XAlignedComponentPanel;
import com.mucommander.ui.layout.YBoxPanel;

/**
 * 'Mail' preferences panel.
 *
 * @author Maxence Bernard
 */
class MailPanel extends PreferencesPanel {

    /** Name of the user */
    private final PrefTextField nameField;
	
    /** Email address of the user */
    private final PrefTextField emailField;
	
    /** IP/hostname to the SMTP server */
    private final PrefTextField smtpField;
	
    /** TCP port to the SMTP server */
    private final PrefTextField portField;


    MailPanel(PreferencesDialog parent) {
        super(parent, Translator.get("prefs_dialog.mail_tab"));

        setLayout(new BorderLayout());

        YBoxPanel mainPanel = new YBoxPanel(5);
        mainPanel.setBorder(BorderFactory.createTitledBorder(Translator.get("prefs_dialog.mail_settings")));

        // Text fields panel
        XAlignedComponentPanel compPanel = new XAlignedComponentPanel();

        // Name field
        nameField = new PrefTextField(getVariable(TcPreference.MAIL_SENDER_NAME, "")) {
			public boolean hasChanged() {
				return !nameField.getText().equals(getVariable(TcPreference.MAIL_SENDER_NAME, ""));
			}
        };
        compPanel.addRow(Translator.get("prefs_dialog.mail_name"), nameField, 10);
		
        // Email field
        emailField = new PrefTextField(getVariable(TcPreference.MAIL_SENDER_ADDRESS, "")) {
			public boolean hasChanged() {
				return !emailField.getText().equals(getVariable(TcPreference.MAIL_SENDER_ADDRESS, ""));
			}
        };
        compPanel.addRow(Translator.get("prefs_dialog.mail_address"), emailField, 10);

        // SMTP field
        smtpField = new PrefTextField(getVariable(TcPreference.SMTP_SERVER, "")) {
			public boolean hasChanged() {
				return !smtpField.getText().equals(getVariable(TcPreference.SMTP_SERVER, ""));
			}
        };
        compPanel.addRow(Translator.get("prefs_dialog.mail_server"), smtpField, 10);

        // SMTP port field
        portField = new PrefTextField(getVariable(TcPreference.SMTP_PORT, ""+TcPreferences.DEFAULT_SMTP_PORT)) {
			public boolean hasChanged() {
				return !portField.getText().equals(String.valueOf(getVariable(TcPreference.SMTP_PORT, TcPreferences.DEFAULT_SMTP_PORT)));
			}
        };
        compPanel.addRow(Translator.get("server_connect_dialog.port"), portField, 10);

        mainPanel.add(compPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.NORTH);
        
        nameField.addDialogListener(parent);
    	emailField.addDialogListener(parent);
    	smtpField.addDialogListener(parent);
    	portField.addDialogListener(parent);
    }


    @Override
    protected void commit() {
        final TcPreferencesAPI pref = TcConfigurations.getPreferences();
    	pref.setVariable(TcPreference.MAIL_SENDER_NAME, nameField.getText());
    	pref.setVariable(TcPreference.MAIL_SENDER_ADDRESS, emailField.getText());
    	pref.setVariable(TcPreference.SMTP_SERVER, smtpField.getText());
    	pref.setVariable(TcPreference.SMTP_PORT, portField.getText());
    }
}
