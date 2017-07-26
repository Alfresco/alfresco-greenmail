/*
 * #%L
 * Alfresco greenmail implementation
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
/* -------------------------------------------------------------------
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
* -------------------------------------------------------------------
* 
* 2012 - Alfresco Software, Ltd.
* Alfresco Software has modified source of this file
* The details of changes as svn diff can be found in svn at location root/projects/3rd-party/src 
*/
package com.icegreen.greenmail.imap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import com.icegreen.greenmail.AbstractServer;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.InternetPrintWriter;

/**
 * The handler class for IMAP connections.
 * TODO: This is a quick cut-and-paste hack from POP3Handler. This, and the ImapServer
 * should probably be rewritten from scratch.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class ImapHandler extends Thread implements ImapConstants {

    private ImapRequestHandler requestHandler = new ImapRequestHandler();
    private ImapSession session;

    /**
     * The TCP/IP socket over which the IMAP interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The reader associated with incoming characters.
     */
    private BufferedReader in;

    /**
     * The socket's input stream.
     */
    private InputStream ins;

    /**
     * The writer to which outgoing messages are written.
     */
    private InternetPrintWriter out;

    /**
     * The socket's output stream
     */
    private OutputStream outs;

    UserManager userManager;
    private ImapHostManager imapHost;
    private AbstractServer server;

    public ImapHandler(UserManager userManager, ImapHostManager imapHost, Socket socket) {
        this.userManager = userManager;
        this.imapHost = imapHost;
        this.socket = socket;
    }

    public ImapHandler(UserManager userManager, ImapHostManager imapHost, Socket socket, AbstractServer server){
        this(userManager, imapHost, socket);
        this.server = server;
    }

    public void forceConnectionClose(final String message) {
        ImapResponse response = new ImapResponse(outs);
        response.byeResponse(message);
        resetHandler();
    }

    public void run() {

        String remoteHost = "";
        String remoteIP = "";

        try {
            ins = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), EIGHT_BIT_ENCODING), 4096);
            remoteIP = socket.getInetAddress().getHostAddress();
            remoteHost = socket.getInetAddress().getHostName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            outs = new BufferedOutputStream(socket.getOutputStream(), 1024);
            out = new InternetPrintWriter(outs, true);
            ImapResponse response = new ImapResponse(outs);

            // Write welcome message
            StringBuffer responseBuffer =
                    new StringBuffer(256)
                    .append(VERSION)
                    .append(" Server ")
                    .append("GreenMail")
                    .append(" ready");
            response.okResponse(null, responseBuffer.toString());

            session = new ImapSessionImpl(imapHost,
                    userManager,
                    this,
                    socket.getInetAddress().getHostName(),
                    socket.getInetAddress().getHostAddress());

            while (requestHandler.handleRequest(ins, outs, session)) {
            }

        } catch (Exception e) {
//            e.printStackTrace();
        } finally {
            resetHandler();
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    void resetHandler() {

        // Close and clear streams, sockets

        try {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch(NullPointerException ignored) {
                //empty
            }
        } catch (IOException ioe) {
            // Ignoring exception on close
        } finally {
            socket = null;
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            in = null;
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            out = null;
        }

        try {
            if (outs != null) {
                outs.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            outs = null;
        }

//        synchronized ( this ) {
//            // Interrupt the thread to recover from internal hangs
//            if ( handlerThread != null ) {
//                handlerThread.interrupt();
//                handlerThread = null;
//            }
//        }

        // de-register from server
        if (this.server != null){
            this.server.deregisterHandler(this);
        }

        // Clear user data
        session = null;
    }

    /**
     * Implements a "stat".  If the handler is currently in
     * a transaction state, this amounts to a rollback of the
     * mailbox contents to the beginning of the transaction.
     * This method is also called when first entering the
     * transaction state to initialize the handler copies of the
     * user inbox.
     */
    private void stat() {
//        userMailbox = new Vector();
//        userMailbox.addElement(DELETED);
//        for (Iterator it = userInbox.list(); it.hasNext(); ) {
//            String key = (String) it.next();
//            MovingMessage mc = userInbox.retrieve(key);
//            // Retrieve can return null if the mail is no longer in the store.
//            // In this case we simply continue to the next key
//            if (mc == null) {
//                continue;
//            }
//            userMailbox.addElement(mc);
//        }
//        backupUserMailbox = (Vector) userMailbox.clone();
    }

    /**
     * Write and flush a response string.  The response is also logged.
     * Should be used for the last line of a multi-line response or
     * for a single line response.
     *
     * @param responseString the response string sent to the client
     */
    final void writeLoggedFlushedResponse(String responseString) {
        out.println(responseString);
        out.flush();
    }

    /**
     * Write a response string.  The response is also logged.
     * Used for multi-line responses.
     *
     * @param responseString the response string sent to the client
     */
    final void writeLoggedResponse(String responseString) {
        out.println(responseString);
    }
}

