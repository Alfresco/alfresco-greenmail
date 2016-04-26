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
 * 2011 - Alfresco Software, Ltd.
 * Alfresco Software has modified source of this file
 * The details of changes as svn diff can be found in svn at location root/projects/3rd-party/src 
 */
package com.icegreen.greenmail.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the client input reader with a bunch of convenience methods, allowing lookahead=1
 * on the underlying character stream.
 * TODO need to look at encoding, and whether we should be wrapping an InputStream instead.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class ImapRequestLineReader {
    private InputStream input;
    private OutputStream output;
    
    private static final Logger log = LoggerFactory.getLogger(ImapRequestLineReader.class);
    private StringBuffer debugBuffer;

    private boolean nextSeen = false;
    private char nextChar; // unknown

    ImapRequestLineReader(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
        this.debugBuffer = new StringBuffer();
    }

    /**
     * Reads the next regular, non-space character in the current line. Spaces are skipped
     * over, but end-of-line characters will cause a {@link ProtocolException} to be thrown.
     * This method will continue to return
     * the same character until the {@link #consume()} method is called.
     *
     * @return The next non-space character.
     * @throws ProtocolException If the end-of-line or end-of-stream is reached.
     */
    public char nextWordChar() throws ProtocolException {
        char next = nextChar();
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        if (next == '\r' || next == '\n') {
            throw new ProtocolException("Missing argument.");
        }

        return next;
    }

    /**
     * Reads the next character in the current line. This method will continue to return
     * the same character until the {@link #consume()} method is called.
     *
     * @return The next character.
     * @throws ProtocolException If the end-of-stream is reached.
     */
    public char nextChar() throws ProtocolException {
        if (!nextSeen) {
            int next = -1;

            try {
                next = input.read();
            } catch (IOException e) {
//                e.printStackTrace();
                throw new ProtocolException("Error reading from stream.");
            }
            if (next == -1) {
                throw new ProtocolException("Unexpected end of stream.");
            }

            nextSeen = true;
            nextChar = (char) next;
//            System.out.println( "Read '" + nextChar + "'" );
        }

        return nextChar;
    }

    /**
     * Moves the request line reader to end of the line, checking that no non-space
     * character are found.
     *
     * @throws ProtocolException If more non-space tokens are found in this line,
     *                           or the end-of-file is reached.
     */
    public void eol() throws ProtocolException {
        char next = nextChar();

        // Ignore trailing spaces.
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        // handle DOS and unix end-of-lines
        if (next == '\r') {
            consume();
            next = nextChar();
        }

        // Check if we found extra characters.
        if (next != '\n') {
            // TODO debug log here and other exceptions
            throw new ProtocolException("Expected end-of-line, found more characters.");
        }
    }

    /**
     * Consumes the current character in the reader, so that subsequent calls to the request will
     * provide a new character. This method does *not* read the new character, or check if
     * such a character exists. If no current character has been seen, the method moves to
     * the next character, consumes it, and moves on to the subsequent one.
     *
     * @throws ProtocolException if a the current character can't be obtained (eg we're at
     *                           end-of-file).
     */
    public char consume() throws ProtocolException {
        char current = nextChar();
        if (log.isDebugEnabled()){
        	debugBuffer.append(current);     	
        }
        nextSeen = false;
        nextChar = 0;
        return current;
    }


    /**
     * Reads and consumes a number of characters from the underlying reader,
     * filling the char array provided.
     *
     * @param holder A char array which will be filled with chars read from the underlying reader.
     * @throws ProtocolException If a char can't be read into each array element.
     */
    public void read(char[] holder) throws ProtocolException {
        int readTotal = 0;
        try {
        	
            byte[] bytes = new byte[holder.length];
            while (readTotal < holder.length) {
                int count = 0;
                count = input.read(bytes, 0, holder.length - readTotal);
                if (count == -1) {
                    throw new ProtocolException("Unexpected end of stream.");
                }
                for (int i=0; i< count; i++)
                {
                    holder[readTotal++] = (char) ((int) bytes[i] & 0xff);
                }
            }
            // Unset the next char.
            nextSeen = false;
            nextChar = 0;
        } catch (IOException e) {
            throw new ProtocolException("Error reading from stream.");
        }

    }

    /**
     * Sends a server command continuation request '+' back to the client,
     * requesting more data to be sent.
     */
    public void commandContinuationRequest()
            throws ProtocolException {
        try {
            output.write('+');
            output.write(' ');			
            output.write('\r');
            output.write('\n');
            output.flush();
        } catch (IOException e) {
            throw new ProtocolException("Unexpected exception in sending command continuation request.");
        }
    }

    public void consumeLine()
            throws ProtocolException {
        char next = nextChar();
        while (next != '\n') {
            consume();
            next = nextChar();
        }
        consume();
    }
    
    public void debugRequest(boolean isLoginRequest, ImapSession session){
        if(log.isDebugEnabled()){
        	String debugRequest = debugBuffer.toString().trim();
        	if (isLoginRequest){
        		debugRequest = debugRequest.substring(0, debugBuffer.indexOf("\""));
        	}
        	log.debug("[SessionId:" + session.getSessionId() + "] " + debugRequest);
        }
    }
}
