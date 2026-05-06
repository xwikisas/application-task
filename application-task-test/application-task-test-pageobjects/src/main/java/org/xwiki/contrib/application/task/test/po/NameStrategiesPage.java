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
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page Object for the Name Strategies administration section.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class NameStrategiesPage extends ViewPage
{
    private static final String MODAL_ID = "createReplacementCharacterModal";

    @FindBy(id = "addNewCharacter")
    private WebElement addNewCharacterButton;

    public static NameStrategiesPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=nameStrategies");
        return new NameStrategiesPage();
    }

    public void addReplacementCharacter(String forbidden, String replacement)
    {
        addNewCharacterButton.click();
        getDriver().waitUntilElementIsVisible(By.id(MODAL_ID));

        setCharacter("newForbiddenCharacter", forbidden);
        setCharacter("newReplacementCharacter", replacement);

        WebElement button =
            getDriver().findElement(By.xpath("//div[@id='" + MODAL_ID + "']//button[contains(@class,'btn-primary')]"));
        button.click();

        getDriver().waitUntilElementDisappears(By.id(MODAL_ID));
    }

    public boolean hasReplacementCharacter(String forbidden, String replacement)
    {
        return !getDriver().findElements(By.xpath("//table[@id='replacementCharacters']//tbody//tr"
                + "[td[contains(@class,'forbiddenCharacter') and normalize-space(.)='" + forbidden + "']"
                + " and td[contains(@class,'replacementCharacter') and normalize-space(.)='" + replacement + "']]"))
            .isEmpty();
    }

    public void deleteReplacementCharacter(String forbidden, String replacement)
    {
        WebElement deleteLink = getDriver().findElement(By.xpath("//table[@id='replacementCharacters']//tbody//tr"
            + "[td[contains(@class,'forbiddenCharacter') and normalize-space(.)='" + forbidden + "']"
            + " and td[contains(@class,'replacementCharacter') and normalize-space(.)='" + replacement + "']]"
            + "//a[contains(@class,'actiondelete')]"));

        deleteLink.click();
        getDriver().waitUntilElementIsVisible(By.id("removeReplacementCharacterModal"));

        WebElement confirmButton = getDriver().findElement(
            By.xpath("//div[@id='removeReplacementCharacterModal']//button[contains(@class,'btn-danger')]"));
        confirmButton.click();
        getDriver().waitUntilElementDisappears(By.id("removeReplacementCharacterModal"));
    }

    private void setCharacter(String id, String value)
    {
        WebElement input = getDriver().findElement(By.id(id));
        input.clear();
        input.sendKeys(value);
    }
}
