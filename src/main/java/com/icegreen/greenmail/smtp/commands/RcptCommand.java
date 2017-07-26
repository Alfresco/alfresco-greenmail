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
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

import javax.mail.internet.AddressException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * RCPT command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.3">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.3</a>.
 * </p>
 */
public class RcptCommand
        extends SmtpCommand {
    static Pattern param = Pattern.compile("RCPT TO:\\s?<([^>]+)>",
            Pattern.CASE_INSENSITIVE);

    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        Matcher m = param.matcher(commandLine);

        try {
            if (m.matches()) {
                if (state.getMessage().getReturnPath() != null) {
                    String to = m.group(1);

                    MailAddress toAddr = new MailAddress(to);

                    String err = manager.checkRecipient(state, toAddr);
                    if (err != null) {
                        conn.println(err);

                        return;
                    }

                    state.getMessage().addRecipient(toAddr);
                    conn.println("250 OK");
                } else {
                    conn.println("503 MAIL must come before RCPT");
                }
            } else {
                conn.println("501 Required syntax: 'RCPT TO:<email@host>'");
            }
        } catch (AddressException e) {
            conn.println("501 Malformed email address. Use form email@host");

        }
    }
}