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

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;


/**
 * EHLO/HELO command.
 * <p/>
 * <p/>
 * TODO: What does HELO do if it's already been called before?
 * </p>
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.1">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.1 </a>.
 * </p>
 */
public class HeloCommand
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        extractHeloName(conn, commandLine);
        state.clearMessage();
        conn.println("250 " + conn.getServerGreetingsName());
    }

    private void extractHeloName(SmtpConnection conn,
                                 String commandLine) {
        String heloName;

        if (commandLine.length() > 5)
            heloName = commandLine.substring(5);
        else
            heloName = null;

        conn.setHeloName(heloName);
    }
}