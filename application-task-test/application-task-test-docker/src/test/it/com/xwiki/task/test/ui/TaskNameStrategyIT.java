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
package com.xwiki.task.test.ui;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.application.task.test.po.NameStrategiesPage;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerInlinePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI Tests for tasks that follow the configured name strategy.
 *
 * @version $Id$
 * @since 3.11.0
 */
@UITest(properties = {
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.EntityNameValidation\\.AdministrationJSON" },
    // Needed for the dependency to the mentions macro that uses solr.
    extraJARs = { "org.xwiki.platform:xwiki-platform-eventstream-store-solr:15.10",
        "com.xwiki.date:macro-date-api",
        "com.xwiki.date:macro-date-default" }, extensionOverrides = {
    @ExtensionOverride(extensionId = "com.google.code.findbugs:jsr305", overrides = {
        "features=com.google.code.findbugs:annotations" }) }, resolveExtraJARs = true)
public class TaskNameStrategyIT
{
    private final DocumentReference taskPage = new DocumentReference("xwiki", "TaskManager", "cTaskc");

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterAll
    void cleanup(TestUtils setup)
    {
        setup.deletePage(taskPage);
    }

    @Test
    @Order(1)
    void addCharacterReplacementStrategyTest(TestUtils testUtils)
    {
        NameStrategiesPage page = NameStrategiesPage.gotoPage();

        page.addReplacementCharacter("b", "c");

        testUtils.getDriver().waitUntilPageIsReloaded();
        assertTrue(page.hasReplacementCharacter("b", "c"));

        createTaskFromTemplate("bTaskb");
        TaskManagerViewPage viewPage = new TaskManagerViewPage();
        assertEquals("cTaskc", viewPage.getName());

        NameStrategiesPage page2 = NameStrategiesPage.gotoPage();
        page2.deleteReplacementCharacter("b", "c");
        assertFalse(page2.hasReplacementCharacter("b", "c"));
    }

    private void createTaskFromTemplate(String title)
    {
        TaskManagerHomePage taskManagerHomePage = TaskManagerHomePage.gotoPage();
        CreatePagePage createPage = taskManagerHomePage.createPage();

        createPage.getDocumentPicker().setTitle(title);
        createPage.setTemplate("TaskManager.TaskManagerTemplates.TaskManagerTemplateProvider");
        createPage.clickCreate();

        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.clickSaveAndView();
    }
}
