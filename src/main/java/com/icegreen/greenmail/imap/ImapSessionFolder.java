/*
 * #%L
 * Alfresco greenmail implementation
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
 * 2010 - Alfresco Software, Ltd.
 * Alfresco Software has modified source of this file
 * The details of changes as svn diff can be found in svn at location root/projects/3rd-party/src 
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;
import com.icegreen.greenmail.foedus.util.MsgRangeFilter;

import jakarta.mail.Flags;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import java.util.*;

public class ImapSessionFolder implements MailFolder, FolderListener {
    private MailFolder _folder;
    private ImapSession _session;
    private boolean _readonly;
    private boolean _sizeChanged;
    private List _expungedMsns = Collections.synchronizedList(new LinkedList());
    private Map _modifiedFlags = Collections.synchronizedMap(new TreeMap());

    public ImapSessionFolder(MailFolder folder, ImapSession session, boolean readonly) {
        _folder = folder;
        _session = session;
        _readonly = readonly;
        // TODO make this a weak reference (or make sure deselect() is *always* called).
        _folder.addListener(this);
    }

    public void deselect() {
        _folder.removeListener(this);
        _folder = null;
    }

    public int getMsn(long uid) throws FolderException
    {
        return _folder.getMsn(uid);
    }

    public void signalDeletion() {
        _folder.signalDeletion();
    }

    public List getMessages(MsgRangeFilter msgRangeFilter) {
        return _folder.getMessages(msgRangeFilter);
    }

    public List getMessages() {
        return _folder.getMessages();
    }

    public List getNonDeletedMessages() {
        return _folder.getNonDeletedMessages();
    }

    public boolean isReadonly() {
        return _readonly;
    }

    public int[] getExpunged() throws FolderException {
        synchronized (_expungedMsns) {
            int[] expungedMsns = new int[_expungedMsns.size()];
            for (int i = 0; i < expungedMsns.length; i++) {
                int msn = ((Integer) _expungedMsns.get(i)).intValue();
                expungedMsns[i] = msn;
            }
            _expungedMsns.clear();

            // TODO - renumber any cached ids (for now we assume the _modifiedFlags has been cleared)\
            if (!(_modifiedFlags.isEmpty() && !_sizeChanged)) {
                throw new IllegalStateException("Need to do this properly...");
            }
            return expungedMsns;
        }
    }

    public List getFlagUpdates() throws FolderException {
        if (_modifiedFlags.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List retVal = new ArrayList();
        retVal.addAll(_modifiedFlags.values());
        _modifiedFlags.clear();
        return retVal;
    }

    public void expunged(int msn) {
        synchronized (_expungedMsns) {
            _expungedMsns.add(new Integer(msn));
        }
    }

    public void added(int msn) {
        _sizeChanged = true;
    }

    public void flagsUpdated(int msn, Flags flags, Long uid) {
        // This will overwrite any earlier changes
        _modifiedFlags.put(new Integer(msn), new FlagUpdate(msn, uid, flags));
    }

    public void mailboxDeleted() {
        _session.closeConnection("Mailbox " + _folder.getName() + " has been deleted");
    }

    public String getName() {
        return _folder.getName();
    }

    public String getFullName() {
        return _folder.getFullName();
    }

    public Flags getPermanentFlags() {
        return _folder.getPermanentFlags();
    }

    public int getMessageCount() {
        return _folder.getMessageCount();
    }

    public int getRecentCount(boolean reset) {
        return _folder.getRecentCount(reset);
    }

    public long getUidValidity() {
        return _folder.getUidValidity();
    }

    public int getFirstUnseen() {
        return correctForExpungedMessages(_folder.getFirstUnseen());
    }

    /**
     * Adjust an actual mailbox msn for the expunged messages in this mailbox that have not
     * yet been notified.
     * TODO - need a test for this
     */
    private int correctForExpungedMessages(int absoluteMsn) {
        int correctedMsn = absoluteMsn;
        // Loop throught the expunged list backwards, adjusting the msn as we go.
        for (int i = (_expungedMsns.size() - 1); i >= 0; i--) {
            Integer expunged = (Integer) _expungedMsns.get(i);
            if (expunged.intValue() <= absoluteMsn) {
                correctedMsn++;
            }
        }
        return correctedMsn;
    }

    public boolean isSelectable() {
        return _folder.isSelectable();
    }

    public boolean isMarked() {
        return _folder.isMarked();
    }

    public long getUidNext() {
        return _folder.getUidNext();
    }

    public int getUnseenCount() {
        return _folder.getUnseenCount();
    }

    public long appendMessage(MimeMessage message, Flags flags, Date internalDate) throws FolderException
    {
        return _folder.appendMessage(message, flags, internalDate);
    }

    public void store(MovingMessage mail) throws Exception {
        _folder.store(mail);
    }

    public void store(MimeMessage mail) throws Exception {
        _folder.store(mail);
    }

    public SimpleStoredMessage getMessage(long uid) {
        return _folder.getMessage(uid);
    }

    public long[] getMessageUids() {
        return _folder.getMessageUids();
    }

    public void expunge() throws FolderException {
        _folder.expunge();
    }

    public void expunge(long uid) throws FolderException {
        _folder.expunge(uid);
    }

    public long[] search(SearchTerm searchTerm) {
        return _folder.search(searchTerm);
    }

    public long copyMessage(long uid, MailFolder toFolder) throws FolderException {
        return _folder.copyMessage(uid, toFolder);
    }

    public void addListener(FolderListener listener) {
        _folder.addListener(listener);
    }

    public void removeListener(FolderListener listener) {
        _folder.removeListener(listener);
    }

    public IdRange[] msnsToUids(IdRange[] idSet) {
        return new IdRange[0];  //To change body of created methods use Options | File Templates.
    }

    public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        _folder.setFlags(flags, value, uid, silentListener, addUid);
    }

    public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        _folder.replaceFlags(flags, uid, silentListener, addUid);
    }

    public void deleteAllMessages() throws FolderException
    {
        _folder.deleteAllMessages();
    }

    public boolean isSizeChanged() {
        return _sizeChanged;
    }

    public void setSizeChanged(boolean sizeChanged) {
        _sizeChanged = sizeChanged;
    }

    static final class FlagUpdate {
        private int msn;
        private Long uid;
        private Flags flags;

        public FlagUpdate(int msn, Long uid, Flags flags) {
            this.msn = msn;
            this.uid = uid;
            this.flags = flags;
        }

        public int getMsn() {
            return msn;
        }

        public Long getUid() {
            return uid;
        }

        public Flags getFlags() {
            return flags;
        }
    }

}
