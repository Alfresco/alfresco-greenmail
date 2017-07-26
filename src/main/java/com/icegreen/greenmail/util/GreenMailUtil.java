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
/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
*
* 
* 2010 - Alfresco Software, Ltd.
* Alfresco Software has modified source of this file
* The details of changes as svn diff can be found in svn at location root/projects/3rd-party/src 
*/
package com.icegreen.greenmail.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.imap.commands.IdRange;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 29, 2006 Changed newMimeMessage(String param) for UTF-8 support.
 */
public class GreenMailUtil {
    /**
     * used internally for {@link #random()}
     */
    private static int generateCount = 0;
    private static final String generateSet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPRSTUVWXYZ23456789";
    private static final int generateSetSize = generateSet.length();

    private static GreenMailUtil instance = new GreenMailUtil();
    private GreenMailUtil() {
        //empty
    }

    public static GreenMailUtil instance() {
        return instance;
    }

    /**
     * Writes the content of an input stream to an output stream
     *
     * @throws IOException
     */
    public static void copyStream(final InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = src.read(buffer)) > -1) {
            dest.write(buffer, 0, read);
        }
        dest.flush();
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from an input stream
     */
    public static  MimeMessage newMimeMessage(InputStream inputStream)  {
        try {
            return new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from a string
     *
     * @throws MessagingException
     */
    public static MimeMessage newMimeMessage(String mailString) throws MessagingException
    {
        byte[] bytes = null;
        try {
            bytes = mailString.getBytes(ImapConstants.EIGHT_BIT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            bytes = mailString.getBytes();
        }
        return newMimeMessage(new ByteArrayInputStream(bytes));
    }

    public static boolean hasNonTextAttachments(Part m) {
        try {
            Object content = m.getContent();
            if (content instanceof MimeMultipart) {
                MimeMultipart mm = (MimeMultipart) content;
                for (int i=0;i<mm.getCount();i++) {
                    BodyPart p = mm.getBodyPart(i);
                    if (hasNonTextAttachments(p)) {
                        return true;
                    }
                }
                return false;
            } else {
                return !m.getContentType().trim().toLowerCase().startsWith("text");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Returns the number of lines in any string
     */
    public static int getLineCount(String str) {
        BufferedReader reader = new BufferedReader(new StringReader(str));
        int ret = 0;
        try {
            while (reader.readLine() != null) {
                ret++;
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The content of an email (or a Part)
     */
    public static String getBody(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return all.substring(i + 4, all.length());
    }

    /**
     * @return The headers of an email (or a Part)
     */
    public static String getHeaders(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return all.substring(0, i);
    }

    /**
     * @return The both header and body for an email (or a Part)
     */
    public static String getWholeMessage(Part msg) {
        try {
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
            msg.writeTo(bodyOut);
            try {
                return bodyOut.toString(ImapConstants.EIGHT_BIT_ENCODING).trim();
            } catch (UnsupportedEncodingException e) {
                return bodyOut.toString().trim();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getBodyAsBytes(Part msg) {
        try {
            return getBody(msg).getBytes(ImapConstants.EIGHT_BIT_ENCODING);
        } catch (UnsupportedEncodingException e) {
        return getBody(msg).getBytes();
    }
    }

    public static  byte[] getHeaderAsBytes(Part part) {
        try {
            return getHeaders(part).getBytes(ImapConstants.EIGHT_BIT_ENCODING);
        } catch (UnsupportedEncodingException e) {
        return getHeaders(part).getBytes();
    }
    }

    /**
     * @return same as {@link #getWholeMessage(javax.mail.Part)} }
     */
    public static String toString(Part msg) {
        return getWholeMessage(msg);
    }


    /**
     * Generates a random generated password consisting of letters and digits
     * with a length variable between 5 and 8 characters long.
     * Passwords are further optimized for displays
     * that could potentially display the characters <i>1,l,I,0,O,Q</i> in a way
     * that a human could easily mix them up.
     *
     * @return
     */
    public static String random() {
        Random r = new Random();
        int nbrOfLetters = r.nextInt(3) + 5;
        return random(nbrOfLetters);
    }

    public static String random(int nbrOfLetters) {
        Random r = new Random();
        StringBuffer ret = new StringBuffer();
        for (/* empty */; nbrOfLetters > 0; nbrOfLetters--) {
            int pos = (r.nextInt(generateSetSize) + (++generateCount)) % generateSetSize;
            ret.append(generateSet.charAt(pos));
        }
        return ret.toString();
    }

    public static void sendTextEmailTest(String to, String from, String subject, String msg) {
        try {
            sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTP);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static  void sendTextEmailSecureTest(String to, String from, String subject, String msg) {
        try {
            sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTPS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAddressList(Address[] addresses) {
        if (null == addresses) {
            return null;
        }
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < addresses.length; i++) {
            if (i>0) {
                ret.append(", ");
            }
            ret.append(addresses[i].toString());
        }
        return ret.toString();
    }

    public static void sendTextEmail(String to, String from, String subject, String msg, final ServerSetup setup) {
        try {
            Session session = getSession(setup);

            Address[] tos = new javax.mail.Address[0];
            tos = new InternetAddress[]{new InternetAddress(to)};
            Address[] froms = new InternetAddress[]{new InternetAddress(from)};
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSubject(subject);
            mimeMessage.setFrom(froms[0]);

            mimeMessage.setText(msg);
            Transport.send(mimeMessage, tos);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Session getSession(final ServerSetup setup) {
        Properties props = new Properties();
        props.put("mail.smtps.starttls.enable", Boolean.TRUE);
        if (setup.isSecure()) {
            props.setProperty("mail.smtp.socketFactory.class", DummySSLSocketFactory.class.getName());
        }
        props.setProperty("mail.transport.protocol", setup.getProtocol());
        props.setProperty("mail.smtp.port", String.valueOf(setup.getPort()));
        props.setProperty("mail.smtps.port", String.valueOf(setup.getPort()));
        Session session = Session.getInstance(props, null);
        return session;
    }

    public static void sendAttachmentEmail(String to, String from, String subject, String msg, final byte[] attachment, final String contentType, final String filename, final String description, final ServerSetup setup) throws MessagingException, IOException {
        Session session = getSession(setup);

        Address[] tos = new InternetAddress[]{new InternetAddress(to)};
        Address[] froms = new InternetAddress[]{new InternetAddress(from)};
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(subject);
        mimeMessage.setFrom(froms[0]);

        MimeMultipart multiPart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();
        multiPart.addBodyPart(textPart);
        textPart.setText(msg);

        MimeBodyPart binaryPart = new MimeBodyPart();
        multiPart.addBodyPart(binaryPart);

        DataSource ds = new DataSource() {
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(attachment);
            }

            public OutputStream getOutputStream() throws IOException {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                byteStream.write(attachment);
                return byteStream;
            }

            public String getContentType() {
                return contentType;
            }

            public String getName() {
                return filename;
            }
        };
        binaryPart.setDataHandler(new DataHandler(ds));
        binaryPart.setFileName(filename);
        binaryPart.setDescription(description);

        mimeMessage.setContent(multiPart);
        Transport.send(mimeMessage, tos);
    }

    public static IdRange[] convertUidsToIdRangeArray(List<Long> uids)
    {
        if (uids == null || uids.size() == 0)
        {
            return new IdRange[0];
        }

        List<Long> uidsLocal = new LinkedList<Long>(uids);
        Collections.sort(uidsLocal);

        List<IdRange> ids = new LinkedList<IdRange>();

        IdRange currentIdRange = new IdRange(uidsLocal.get(0));
        for (Long uid : uidsLocal)
        {
            if (uid == currentIdRange.getHighVal())
            {
            }
            else if (uid > currentIdRange.getHighVal() && (uid == currentIdRange.getHighVal() + 1))
            {
                currentIdRange = new IdRange(currentIdRange.getLowVal(), uid);
            }
            else
            {
                ids.add(currentIdRange);
                currentIdRange = new IdRange(uid);
            }
        }

        if (!ids.contains(currentIdRange))
        {
            ids.add(currentIdRange);
        }

        return ids.toArray(new IdRange[ids.size()]);
    }

    public static String uidsToRangeString(List<Long> uids)
    {
        return idRangesToString(convertUidsToIdRangeArray(uids));
    }

    public static String idRangeToString(IdRange idRange)
    {
        return idRange.getHighVal() == idRange.getLowVal() ? "" + idRange.getLowVal() : idRange.getLowVal() + ":" + idRange.getHighVal();
    }

    public static String idRangesToString(IdRange[] idRanges)
    {
        StringBuilder sb = new StringBuilder();

        for (IdRange idRange : idRanges)
        {
            if (sb.length() > 0)
            {
                sb.append(",");
            }
            sb.append(idRangeToString(idRange));
        }

        return sb.toString();
    }
}
