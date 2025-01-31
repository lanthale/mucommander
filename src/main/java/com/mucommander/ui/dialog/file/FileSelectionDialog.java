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

package com.mucommander.ui.dialog.file;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jidesoft.hints.ListDataIntelliHints;
import com.mucommander.cache.TextHistory;
import com.mucommander.commons.file.filter.AndFileFilter;
import com.mucommander.commons.file.filter.AttributeFileFilter;
import com.mucommander.commons.file.filter.AttributeFileFilter.FileAttribute;
import com.mucommander.commons.file.filter.FileFilter;
import com.mucommander.commons.file.filter.WildcardFileFilter;
import com.mucommander.ui.dialog.DialogToolkit;
import com.mucommander.ui.dialog.FocusDialog;
import com.mucommander.ui.layout.YBoxPanel;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.table.FileTable;

/**
 * This dialog allows the user to add (mark) or remove (unmark) files from the current selection,
 * based on a match criterium and string.
 *
 * @author Maxence Bernard
 */
public class FileSelectionDialog extends FocusDialog implements ActionListener {

    /** Add to or remove from selection ? */	 
    private final boolean addToSelection;

    private final JTextField selectionField;

    private final JCheckBox cbCaseSensitive;
    private final JCheckBox cbIncludeFolders;

    private final JButton btnOk;

    private final MainFrame mainFrame;
	
    /** 
     * Is selection case-sensitive? (initially false)
     * <br>Note: this field is static so the value is kept after the dialog is OKed.
     */ 
    private static boolean caseSensitive = false;

    /** 
     * Does the selection include folders? (initially false)
     * <br>Note: this field is static so the value is kept after the dialog is OKed.
     */ 
    private static boolean includeFolders = false;

    /** 
     * Keyword which has last been typed to mark or unmark files.
     * <br>Note: this field is static so the value is kept after the dialog is OKed.
     */ 
    private static String keywordString = "*";
	

    private final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(320,0);	
    private final static Dimension MAXIMUM_DIALOG_DIMENSION = new Dimension(400,10000);	


    /**
     * Creates a new 'mark' or 'unmark' dialog.
     *
     * @param addToSelection if <code>true</code>, files matching
     */
    public FileSelectionDialog(MainFrame mainFrame, boolean addToSelection) {
        super(mainFrame, i18n(addToSelection?"file_selection_dialog.mark":"file_selection_dialog.unmark"), mainFrame);
	
        this.mainFrame = mainFrame;
        this.addToSelection = addToSelection;

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        YBoxPanel northPanel = new YBoxPanel(5);
        JLabel label = new JLabel(i18n(addToSelection?"file_selection_dialog.mark_description":"file_selection_dialog.unmark_description")+" :");
        northPanel.add(label);

        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));

        // selectionField is initialized with last textfield's value (if any)
        selectionField = new JTextField(keywordString);
        selectionField.addActionListener(this);
        selectionField.setSelectionStart(0);
        selectionField.setSelectionEnd(keywordString.length());

        List<String> filesHistory = TextHistory.getInstance().getList(TextHistory.Type.FILE_NAME);
        new ListDataIntelliHints<>(selectionField, filesHistory).setCaseSensitive(false);

        tempPanel.add(selectionField);
        northPanel.add(tempPanel);

        // Add some vertical space
        northPanel.addSpace(10);
		
        cbCaseSensitive = new JCheckBox(i18n("file_selection_dialog.case_sensitive"), caseSensitive);
        northPanel.add(cbCaseSensitive);

        cbIncludeFolders = new JCheckBox(i18n("file_selection_dialog.include_folders"), includeFolders);
        northPanel.add(cbIncludeFolders);
		
        northPanel.addSpace(10);
        northPanel.add(Box.createVerticalGlue());

        contentPane.add(northPanel, BorderLayout.NORTH);

        btnOk = new JButton(i18n(addToSelection?"file_selection_dialog.mark":"file_selection_dialog.unmark"));
        contentPane.add(DialogToolkit.createOKCancelPanel(btnOk, new JButton(i18n("cancel")), getRootPane(), this), BorderLayout.SOUTH);

        // Selection field receives initial keyboard focus
        setInitialFocusComponent(selectionField);

        setMinimumSize(MINIMUM_DIALOG_DIMENSION);
        setMaximumSize(MAXIMUM_DIALOG_DIMENSION);
    }



	@Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        FileTable activeTable = mainFrame.getActiveTable();

        // Action coming from the selection dialog
        if (source == btnOk || source == selectionField) {
            // Save values for next time this dialog is invoked
            caseSensitive = cbCaseSensitive.isSelected();
            includeFolders = cbIncludeFolders.isSelected();

            keywordString = selectionField.getText();

            // Instantiate the main file IMAGE_FILTER
            FileFilter filter = new WildcardFileFilter(keywordString, caseSensitive);

            // If folders are excluded, add a regular file IMAGE_FILTER and chain it with an AndFileFilter
            if (!includeFolders) {
                filter = new AndFileFilter(
                    new AttributeFileFilter(FileAttribute.FILE),
                    filter
                );
            }

            // Mark/unmark the files using the IMAGE_FILTER
            activeTable.getFileTableModel().setFilesMarked(filter, addToSelection);

            // Notify registered listeners that currently marked files have changed on this FileTable
            activeTable.fireMarkedFilesChangedEvent();

            activeTable.repaint();
        }
		
        dispose();
    }

}
