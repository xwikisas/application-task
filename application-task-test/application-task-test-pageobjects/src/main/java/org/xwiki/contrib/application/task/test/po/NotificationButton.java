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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the Notification menu and provides access to its settings.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class NotificationButton extends BaseElement
{
    private static final By PAGE_ONLY = By.id("notificationPageOnly");

    private static final By PAGE_AND_CHILDREN = By.id("notificationPageAndChildren");

    private static final By WIKI = By.id("notificationWiki");

    @FindBy(css = "#tmNotifications button.dropdown-toggle")
    private WebElement button;

    public NotificationButton open()
    {

        button.click();

        By dropdown = By.cssSelector("#tmNotifications .dropdown-menu");
        getDriver().waitUntilElementIsVisible(dropdown);

        getDriver().waitUntilCondition(
            d -> d.findElement(dropdown).getAttribute("class").contains("open") || d.findElement(dropdown)
                .isDisplayed(), 5);

        return this;
    }

    public NotificationButton setWiki(boolean value)
    {
        setCheckbox(WIKI, value);
        return this;
    }

    public NotificationButton setPageOnly(boolean value)
    {
        setCheckbox(PAGE_ONLY, value);
        return this;
    }

    public NotificationButton setPageAndChildren(boolean value)
    {
        setCheckbox(PAGE_AND_CHILDREN, value);
        return this;
    }

    public boolean isPageOnlyEnabled()
    {
        return getDriver().findElement(PAGE_ONLY).isSelected();
    }

    public boolean isPageAndChildrenEnabled()
    {
        return getDriver().findElement(PAGE_AND_CHILDREN).isSelected();
    }

    public boolean isWikiEnabled()
    {
        return getDriver().findElement(WIKI).isSelected();
    }

    private void setCheckbox(By locator, boolean value)
    {
        WebElement input = getDriver().findElement(locator);

        String id = input.getAttribute("id");

        By switchLocator = By.cssSelector(".bootstrap-switch-id-" + id);

        WebElement toggle = getDriver().findElement(switchLocator);

        getDriver().waitUntilCondition(d -> toggle.isDisplayed(), 10);

        boolean isOn = toggle.getAttribute("class").contains("bootstrap-switch-on");

        if (isOn != value) {
            toggle.click();

            getDriver().waitUntilCondition(d -> getDriver().findElement(switchLocator).getAttribute("class")
                .contains(value ? "bootstrap-switch-on" : "bootstrap-switch-off"), 5);
        }
    }
}
