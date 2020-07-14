/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package com.icegreen.greenmail.imap.commands;

import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.fail;

public class SearchTermBuilderTest {

    @Test
    public void testCreateDateSearchTermWithNonEnglishLocale()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.FRENCH);
            SearchTermBuilder searchTermBuilder = SearchTermBuilder.create(SearchKey.SINCE.name());
            String dateStr = "12-Jul-2020";
            searchTermBuilder.addParameter(dateStr);
            searchTermBuilder.build();
        }
        catch (IllegalArgumentException e)
        {
            fail("Date cannot be parsed");
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testCreateDateSearchTermWithNonEnglishLocaleFalsePositive()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.FRENCH);
            SearchTermBuilder searchTermBuilder = SearchTermBuilder.create(SearchKey.SINCE.name());
            String dateStr = "12-juil.-2020";
            searchTermBuilder.addParameter(dateStr);
            searchTermBuilder.build();
            fail("Date cannot be parsed");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }
}
