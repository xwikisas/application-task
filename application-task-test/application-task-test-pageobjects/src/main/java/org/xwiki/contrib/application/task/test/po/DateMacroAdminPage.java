/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.application.task.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object of the Date Macro administration section.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class DateMacroAdminPage extends ViewPage
{
    @FindBy(id = "DateMacro.Code.Configuration_DateMacro.Code.ConfigurationClass_0_displayDateFormat")
    private WebElement displayDateFormatInput;

    @FindBy(css = ".bottombuttons input[type='submit']")
    private WebElement saveButton;

    public static DateMacroAdminPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=Date%20Macro");
        return new DateMacroAdminPage();
    }

    public String getDisplayDateFormat()
    {
        return displayDateFormatInput.getAttribute("value");
    }

    public void setDisplayDateFormat(String format)
    {
        displayDateFormatInput.clear();
        displayDateFormatInput.sendKeys(format);
    }

    public void clearDisplayDateFormat()
    {
        displayDateFormatInput.clear();
    }

    public void clickSave()
    {
        saveButton.click();
    }
}
