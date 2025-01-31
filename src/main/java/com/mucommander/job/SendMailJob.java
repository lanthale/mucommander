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

package com.mucommander.job;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.MimeTypes;
import com.mucommander.commons.file.util.FileSet;
import com.mucommander.commons.io.StreamUtils;
import com.mucommander.commons.io.base64.Base64OutputStream;
import com.mucommander.conf.TcConfigurations;
import com.mucommander.conf.TcPreference;
import com.mucommander.conf.TcPreferences;
import com.mucommander.utils.text.Translator;
import com.mucommander.ui.dialog.file.ProgressDialog;
import com.mucommander.ui.main.MainFrame;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * This job sends one or several files by email.
 *
 * @author Maxence Bernard
 */
public class SendMailJob extends TransferFileJob {

    /** True after connection to mail server has been established */
    private boolean connectedToMailServer;

    /** Error dialog title */
    private final String errorDialogTitle;
	
	
    /////////////////////
    // Mail parameters //
    /////////////////////

    /** Email recipient(s) */
    private String recipientString;
    /** Email subject */
    private final String mailSubject;
    /** Email body */
    private final String mailBody;

    /** SMTP server */
    private final String mailServer;
    /** From name */
    private final String fromName;
    /** From address */
    private final String fromAddress;
	
    /** Email boundary string, delimits the end of the body and attachments */
    private final String boundary;

    /** Connection variable */
    private BufferedReader in;
    /** OutputStream to the SMTP server */
    private OutputStream out;
    /** Base64OutputStream to the SMTP server */
    private Base64OutputStream out64;
    /** Socket connection to the SMTP server */
    private Socket socket;

	
    private final static String CLOSE_TEXT = Translator.get("close");
    private final static int CLOSE_ACTION = 11;
	
	
    public SendMailJob(ProgressDialog progressDialog, MainFrame mainFrame, FileSet filesToSend, String recipientString, String mailSubject, String mailBody) {
        super(progressDialog, mainFrame, filesToSend);

        this.boundary = "trolcommander" + System.currentTimeMillis();
        this.recipientString = recipientString;
        this.mailSubject = mailSubject;
        this.mailBody = mailBody+"\n\n"+"Sent by trolCommander - http://www.trolsoft.ru\n";

        this.mailServer = TcConfigurations.getPreferences().getVariable(TcPreference.SMTP_SERVER);
        this.fromName = TcConfigurations.getPreferences().getVariable(TcPreference.MAIL_SENDER_NAME);
        this.fromAddress = TcConfigurations.getPreferences().getVariable(TcPreference.MAIL_SENDER_ADDRESS);
    
        this.errorDialogTitle = Translator.get("email_dialog.error_title");
    }

    /**
     * Returns true if mail preferences have been set.
     */
    public static boolean mailPreferencesSet() {
        return TcConfigurations.getPreferences().isVariableSet(TcPreference.SMTP_SERVER)
            && TcConfigurations.getPreferences().isVariableSet(TcPreference.MAIL_SENDER_NAME)
            && TcConfigurations.getPreferences().isVariableSet(TcPreference.MAIL_SENDER_ADDRESS);
    }


    /**
     * Shows an error dialog with a single action : close, and stops the job.
     */
    private void showErrorDialog(String message) {
        showErrorDialog(errorDialogTitle, message, new String[]{CLOSE_TEXT}, new int[]{CLOSE_ACTION});
        interrupt();
    }
	
	
    /////////////////////////////////////////////
    // Methods taking care of sending the mail //
    /////////////////////////////////////////////

    private void openConnection() throws IOException {
        this.socket = new Socket(mailServer, TcConfigurations.getPreferences().getVariable(TcPreference.SMTP_PORT, TcPreferences.DEFAULT_SMTP_PORT));
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = socket.getOutputStream();
        this.out64 = new Base64OutputStream(out, true);
		
        this.connectedToMailServer = true;
    }

    private void sendBody() throws IOException {
        // here you are supposed to send your username
        readWriteLine("HELO trolCommander");
        // warning : some mail server validate the sender address and will fail is an invalid
        // address is provided
        readWriteLine("MAIL FROM: "+fromAddress);
		
        List<String> recipients = new ArrayList<>();
        recipientString = splitRecipientString(recipientString, recipients);
        for (String recipient : recipients)
            readWriteLine("RCPT TO: <" + recipient + ">");
        readWriteLine("DATA");
        writeLine("MIME-Version: 1.0");
        writeLine("Subject: "+this.mailSubject);
        writeLine("From: "+this.fromName+" <"+this.fromAddress+">");
        writeLine("To: "+recipientString);
        writeLine("Content-Type: multipart/mixed; boundary=\"" + boundary +"\"");
        writeLine("\r\n--" + boundary);

        // Send the body
        //        writeLine( "Content-Type: text/plain; charset=\"us-ascii\"\r\n");
        writeLine("Content-Type: text/plain; charset=\"utf-8\"\r\n");
        writeLine(this.mailBody+"\r\n\r\n");
        writeLine("\r\n--" +  boundary );        
    }
    

    /**
     * Parses the specified string, replaces delimiter characters if needed and adds recipients  (String instances) to the given Vector.
     *
     * @param recipientsStr String containing one or several recipients that need to be separated by ',' and/or ';' characters.
     */
    private String splitRecipientString(String recipientsStr, List<String> recipients) {

        // /!\ this piece of code is far from being bulletproof, but I'm too lazy now to rewrite it
        StringBuilder newRecipientsSb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(recipientsStr, ",;");
        while(st.hasMoreTokens()) {
            String rec = st.nextToken().trim();
            int pos1, pos2;
            if ((pos1 = rec.indexOf('<')) != -1 && (pos2 = rec.indexOf('>', pos1+1)) != -1) {
                recipients.add(rec.substring(pos1+1, pos2));
            } else {
                recipients.add(rec);
            }

            newRecipientsSb.append(rec);
            if (st.hasMoreTokens()) {
                newRecipientsSb.append(", ");
            }
        }
		
        return newRecipientsSb.toString();
    }
	
	
    /**
     * Send file as attachment encoded in Base64, and returns true if file was successfully
     * and completely transferred.
     */ 
    private void sendAttachment(AbstractFile file) throws IOException {
        // Send MIME type of attachment file
        String mimeType = MimeTypes.getMimeType(file);
        // Default mime type
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        writeLine("Content-Type:"+mimeType+"; name="+file.getName());
        writeLine("Content-Disposition: attachment;filename=\""+file.getName()+"\"");
        writeLine("Content-transfer-encoding: base64\r\n");

        try (InputStream fileIn = setCurrentInputStream(file.getInputStream())) {
            // Write file to socket
            StreamUtils.copyStream(fileIn, out64);

            // Writes padding bytes without closing the stream.
            out64.writePadding();

            writeLine("\r\n--" + boundary);
        }
    }
	
    private void sayGoodBye() throws IOException {
        writeLine("\r\n\r\n--" + boundary + "--\r\n");
        readWriteLine(".");
        readWriteLine("QUIT");
    }


    private void closeConnection() {
        try {
            socket.close();
            in.close();
            out64.close();
        } catch(Exception ignore) {}
    }
    
    private void readWriteLine(String s) throws IOException {
        out.write((s + "\r\n").getBytes(StandardCharsets.UTF_8));
        in.readLine();
    }

    private void writeLine(String s) throws IOException {
        out.write((s + "\r\n").getBytes(StandardCharsets.UTF_8));
    }


    ////////////////////////////////////
    // TransferFileJob implementation //
    ////////////////////////////////////

    @Override
    protected boolean processFile(AbstractFile file, Object recurseParams) {
        if (getState() == State.INTERRUPTED)
            return false;

        // Send file attachment
        try {
            sendAttachment(file);
        } catch(IOException e) {
            showErrorDialog(Translator.get("email.send_file_error", file.getName()));
            return false;
        }

        // If this was the last file, notify the mail server that the mail is over
        if(getCurrentFileIndex()==getNbFiles()-1) {
            try {
                // Say goodbye to the server
                sayGoodBye();
            }
            catch(IOException e) {
                showErrorDialog(Translator.get("email.goodbye_failed"));
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean hasFolderChanged(AbstractFile folder) {
        // This job does not modify anything
        return false;
    }


    ///////////////////////
    // Overridden method //
    ///////////////////////

    /**
     * This method is called when this job starts, before the first call to {@link #processFile(AbstractFile,Object) processFile()} is made.
     * This method here does nothing, but it can be overridden by subclasses to perform some first-time initializations.
     */
    @Override
    protected void jobStarted() {
        super.jobStarted();

        // Open socket connection to the mail server, and say hello
        try {
            openConnection();
        } catch(IOException e) {
            showErrorDialog(Translator.get("email.server_unavailable", mailServer));
        }

        if (getState() == State.INTERRUPTED)
            return;

        // Send mail body
        try {
            sendBody();
        } catch(IOException e) {
            showErrorDialog(Translator.get("email.connection_closed"));
        }
    }


    /**
     * Method overridden to close connection to the mail server.
     */
    @Override
    protected void jobStopped() {
        super.jobStopped();

        // Close the connection
        closeConnection();
    }

    @Override
    public String getStatusString() {
        if (connectedToMailServer) {
            return Translator.get("email.sending_file", getCurrentFilename());
        } else {
            return Translator.get("email.connecting_to_server", mailServer);
        }
    }
}
