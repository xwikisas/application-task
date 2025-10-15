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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.application.task.test.po.TaskAdminPage;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerInlinePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerViewPage;
import org.xwiki.contrib.application.task.test.po.ViewPageWithTasks;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.CopyPage;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the Task Manager application.
 *
 * @version $Id$
 * @since 2.6
 */
@UITest(
    // Needed for the dependency to the mentions macro that uses solr.
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr:14.10",
        "com.xwiki.date:macro-date-api",
        "com.xwiki.date:macro-date-default"
    }, resolveExtraJARs = true)
class TaskManagerIT
{
    private final LocalDocumentReference pageWithTaskMacros = new LocalDocumentReference("Main", "Test");

    private final LocalDocumentReference pageWithComplexTaskMacros = new LocalDocumentReference("Main", "Test2");

    private final LocalDocumentReference pageWithTaskRaport = new LocalDocumentReference("Main", "Test3");

    private final LocalDocumentReference docWithTaskboxes = new LocalDocumentReference("Taskboxes", "WebHome");

    private static final String SIMPLE_TASKS = "{{task reference=\"Task_1\"}}Do this{{/task}}\n\n"
        + "{{task reference=\"Task_2\" status=\"Done\"}}Do this as well{{/task}}";

    private static final String COMPLEX_TASKS =
        "{{task reference=\"Task_3\" reporter=\"XWiki.Admin\" createDate=\"2023/01/01 12:00\" status=\"Done\" "
            + "completeDate=\"2023/01/01 12:00\"}}"
            + "Do this {{mention reference=\"XWiki.Admin\"/}} as late as {{date value=\"2023/01/01 12:00\"/}}"
            + "{{/task}}";

    private static final String TASK_REPORT_MACRO = "{{task-report /}}";

    private static final String USER_NAME = "BOB";

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(pageWithTaskMacros);
        setup.deletePage(pageWithComplexTaskMacros);
    }

    @AfterAll
    void teardown(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(pageWithTaskMacros);
        setup.deletePage(pageWithComplexTaskMacros);
    }

    @Test
    @Order(10)
    void applicationsPanelEntry(TestUtils setup)
    {
        // Navigate to the Task Manager application by clicking in the Application Panel.
        // This verifies that the Task Manager application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Task Manager");

        // Verify we're on the right page!
        assertEquals(TaskManagerHomePage.getSpace(), vp.getMetaDataValue("space"));
        assertEquals(TaskManagerHomePage.getPage(), vp.getMetaDataValue("page"));
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(20)
    void simpleTaskMacros(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithTaskMacros, wiki);
        setup.createPage(testRef, SIMPLE_TASKS, pageWithTaskMacros.getName());
        System.out.println("Test123" + testRef);
        ViewPageWithTasks page = new ViewPageWithTasks();
        // Check first, unchecked macro.
        assertEquals("Do this", page.getTaskMacroContent(0));
        assertEquals("#1", page.getTaskMacroLink(0).getText());
        assertTrue(page.getTaskMacroLink(0).getAttribute("href").contains("/view/Task_1"));
        assertFalse(page.isTaskMacroCheckboxChecked(0));
        page.clickTaskMacroCheckbox(0);
        // Check second, checked macro.
        assertEquals("Do this as well", page.getTaskMacroContent(1));
        assertEquals("#2", page.getTaskMacroLink(1).getText());
        assertTrue(page.getTaskMacroLink(1).getAttribute("href").contains("/view/Task_2"));
        assertTrue(page.isTaskMacroCheckboxChecked(1));
        page.clickTaskMacroCheckbox(1);
        // Refresh the page and make sure the changes are saved.
        setup.getDriver().navigate().refresh();
        page = new ViewPageWithTasks();
        assertTrue(page.isTaskMacroCheckboxChecked(0));
        assertFalse(page.isTaskMacroCheckboxChecked(1));
    }

    @ParameterizedTest
    @WikisSource()
    @Order(30)
    void taskManagerHomePage(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        TaskManagerHomePage taskManagerHomePage = TaskManagerHomePage.gotoPage();
        LiveTableElement liveTableElement = taskManagerHomePage.getTaskLiveTable();
        int taskTileCellIndex = liveTableElement.getColumnIndex("Task") + 1;
        int taskStatusCellIndex = liveTableElement.getColumnIndex("Status") + 1;
        assertEquals(2, liveTableElement.getRowCount());
        WebElement row = liveTableElement.getRow(1);
        assertEquals("Do this", liveTableElement.getCell(row, taskTileCellIndex).getText());
        assertEquals("Done", liveTableElement.getCell(row, taskStatusCellIndex).getText());
        row = liveTableElement.getRow(2);
        assertEquals("Do this as well", liveTableElement.getCell(row, taskTileCellIndex).getText());
        assertEquals("In Progress", liveTableElement.getCell(row, taskStatusCellIndex).getText());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(40)
    void complexTaskMacros(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithComplexTaskMacros, wiki);
        setup.createPage(testRef, COMPLEX_TASKS, pageWithComplexTaskMacros.getName());
        ViewPageWithTasks page = new ViewPageWithTasks();
        assertEquals("Do this @Admin as late as 2023/01/01 12:00", page.getTaskMacroContent(0));
        assertEquals("#3", page.getTaskMacroLink(0).getText());
        assertTrue(page.getTaskMacroLink(0).getAttribute("href").contains("/view/Task_3"));
        assertTrue(page.isTaskMacroCheckboxChecked(0));
        page.getTaskMacroLink(0).click();
        TaskManagerViewPage viewPage = new TaskManagerViewPage();
        assertEquals("01/01/2023 12:00:00", viewPage.getCreateDate());
        assertEquals("01/01/2023 12:00:00", viewPage.getCompletionDate());
        assertEquals("01/01/2023 12:00:00", viewPage.getDueDate());
        assertEquals("Admin", viewPage.getAssignee());
        assertEquals("Admin", viewPage.getReporter());
        assertEquals("Done", viewPage.getStatus());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(50)
    void taskMacroAndTaskPageRelation(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithComplexTaskMacros, wiki);
        setup.gotoPage(testRef);
        ViewPageWithTasks page = new ViewPageWithTasks();
        page.getTaskMacroLink(0).click();
        TaskManagerViewPage viewPage = new TaskManagerViewPage();
        viewPage.edit();
        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.waitUntilPageIsReady();
        // Changing a property of the page should change the macro call.
        inlinePage.setStatus("ToDo");
        inlinePage.clickSaveAndView();
        setup.gotoPage(testRef);
        ViewPageWithTasks viewPageWithTaskMacro = new ViewPageWithTasks();
        assertFalse(viewPageWithTaskMacro.isTaskMacroCheckboxChecked(0));
        // Changing the property of the macro call should change the page.
        viewPageWithTaskMacro.clickTaskMacroCheckbox(0);
        viewPageWithTaskMacro.getTaskMacroLink(0).click();
        viewPage = new TaskManagerViewPage();
        assertEquals("Done", viewPage.getStatus());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(60)
    void taskReport(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithTaskRaport, wiki);
        setup.createPage(testRef, TASK_REPORT_MACRO);
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        LiveTableElement taskReport = viewPage.getTaskReportLiveTable();
        taskReport.waitUntilReady();
        assertEquals(3, taskReport.getRowCount());
        int taskTileCellIndex = taskReport.getColumnIndex("Task") + 1;
        int taskDeadlineCellIndex = taskReport.getColumnIndex("Deadline") + 1;
        int taskAssigneeCellIndex = taskReport.getColumnIndex("Assignee") + 1;
        int taskLocationCellIndex = taskReport.getColumnIndex("Location") + 1;
        WebElement row = taskReport.getRow(1);
        assertEquals("#1\nDo this", taskReport.getCell(row, taskTileCellIndex).getText());
        assertEquals("-", taskReport.getCell(row, taskDeadlineCellIndex).getText());
        assertEquals("", taskReport.getCell(row, taskAssigneeCellIndex).getText());
        assertEquals(pageWithTaskMacros.getName(), taskReport.getCell(row, taskLocationCellIndex).getText());
        row = taskReport.getRow(3);
        assertEquals("#3\nDo this  as late as @Admin 2023/01/01 12:00",
            taskReport.getCell(row, taskTileCellIndex).getText());
        assertEquals("01/01/2023 12:00:00", taskReport.getCell(row, taskDeadlineCellIndex).getText());
        assertEquals("Admin", taskReport.getCell(row, taskAssigneeCellIndex).getText());
        assertEquals(pageWithComplexTaskMacros.getName(), taskReport.getCell(row, taskLocationCellIndex).getText());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(63)
    void copyPage(WikiReference wiki, TestUtils setup) throws Exception
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference testRef = new DocumentReference(new LocalDocumentReference(Arrays.asList("Main",
            "CopyMovePage"), "WebHome"), wiki);
        setup.createPage(testRef, "{{task reference=\"/Tasks/Task_1\"}}Hello{{/task}}", "CopyMovePage");
        // Copying a page containing tasks should update their ids and change their owners.
        setup.gotoPage(testRef);
        // Gather all task ids for comparison.
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        List<String> macroids = new ArrayList<>();
        for (int i = 0; i < viewPage.getTaskMacros().size(); i++) {
            macroids.add(viewPage.getTaskMacroLink(i).getText());
        }
        // Copy page.
        CopyPage copyPage = viewPage.copy();
        WebElement title = setup.getDriver().findElement(By.id("targetTitle"));
        title.clear();
        title.sendKeys("Copy");
        title.sendKeys(Keys.ENTER);
        CopyOrRenameOrDeleteStatusPage statusPage = new CopyOrRenameOrDeleteStatusPage();
        statusPage.waitUntilFinished();
        statusPage.gotoNewPage();
        // Gather all task ids for comparison.
        viewPage = new ViewPageWithTasks();
        List<String> copiedMacros = new ArrayList<>();
        for (int i = 0; i < viewPage.getTaskMacros().size(); i++) {
            copiedMacros.add(viewPage.getTaskMacroLink(i).getText());
        }
        // Make sure they are not the same.
        assertNotEquals(macroids, copiedMacros);
        assertEquals(macroids.size(), copiedMacros.size());
        // Make sure the owner was also changed.
        viewPage.getTaskMacroLink(0).click();
        TaskManagerViewPage objPage = new TaskManagerViewPage();
        assertEquals("Copy", objPage.getOwner());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(68)
    void movePage(WikiReference wiki, TestUtils setup) throws Exception
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef =
            new DocumentReference(new LocalDocumentReference(Arrays.asList("Main", "Copy"), "WebHome"), wiki);
        setup.gotoPage(testRef);
        // Gather all task ids for comparison.
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        List<String> macroids = new ArrayList<>();
        for (int i = 0; i < viewPage.getTaskMacros().size(); i++) {
            macroids.add(viewPage.getTaskMacroLink(i).getText());
        }
        // Rename page.
        RenamePage movedPage = viewPage.rename();
        WebElement title = setup.getDriver().findElement(By.id("targetTitle"));
        title.clear();
        title.sendKeys("Rename");
        title.sendKeys(Keys.ENTER);
        CopyOrRenameOrDeleteStatusPage statusPage = new CopyOrRenameOrDeleteStatusPage();
        statusPage.waitUntilFinished().gotoNewPage();
        // Gather all task ids for comparison.
        viewPage = new ViewPageWithTasks();
        List<String> movedIds = new ArrayList<>();
        for (int i = 0; i < viewPage.getTaskMacros().size(); i++) {
            movedIds.add(viewPage.getTaskMacroLink(i).getText());
        }
        // Make sure they are the same since they were moved altogether.
        assertEquals(macroids, movedIds);
        assertEquals(macroids.size(), movedIds.size());
        // Make sure the owner was changed.
        viewPage.getTaskMacroLink(0).click();
        TaskManagerViewPage objPage = new TaskManagerViewPage();
        assertEquals("Rename", objPage.getOwner());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(70)
    void deleteTaskPage(WikiReference wiki, TestUtils setup) throws Exception
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithTaskMacros, wiki);
        // Deleting the page that contains task macros should also delete the task pages.
        setup.gotoPage(testRef);
        assertTrue(setup.pageExists("Task_1", "WebHome"));
        assertTrue(setup.pageExists("Task_2", "WebHome"));
        setup.deletePage(testRef);
        assertFalse(setup.pageExists("Task_1", "WebHome"));
        assertFalse(setup.pageExists("Task_2", "WebHome"));
    }

    @ParameterizedTest
    @WikisSource()
    @Order(80)
    void deleteTaskMacro(WikiReference wiki, TestUtils setup) throws Exception
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithComplexTaskMacros, wiki);
        // Deleting a task page should also delete the task macro from the owner page.
        setup.gotoPage(testRef);
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        assertEquals(1, viewPage.getTaskMacros().size());
        viewPage.getTaskMacroLink(0).click();
        ViewPage taskPage = new ViewPage();
        taskPage.deletePage().confirmDeletePage();
        setup.gotoPage(testRef);
        viewPage = new ViewPageWithTasks();
        assertEquals(0, viewPage.getTaskMacros().size());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(90)
    void checkboxMacro(WikiReference wiki, TestUtils setup,
        TestLocalReference testLocalReference, TestReference testReference)
    {
        DocumentReference testRef = new DocumentReference(docWithTaskboxes, wiki);
        setup.createPage(testRef, "{{checkbox id=\"someId\"}}Hello there{{/checkbox}}");
        ViewPageWithTasks viewPageWithTasks = new ViewPageWithTasks();
        assertEquals("Hello there", viewPageWithTasks.getTaskMacroContent(0));
        assertEquals(false, viewPageWithTasks.isTaskMacroCheckboxChecked(0));
        viewPageWithTasks.clickTaskMacroCheckbox(0);

        WikiEditPage editPage = viewPageWithTasks.editWiki();
        assertEquals("{{checkbox id=\"someId\" checked=\"true\"}}\nHello there\n{{/checkbox}}", editPage.getContent());
        editPage.clickSaveAndView(true);
        viewPageWithTasks = new ViewPageWithTasks();
        assertEquals(true, viewPageWithTasks.isTaskMacroCheckboxChecked(0));
    }

    @Test
    @Order(100)
    void deleteAdminDefaults(TestUtils testUtils)
    {
        testUtils.setGlobalRights("", "XWiki." + USER_NAME, "admin", true);
        testUtils.createUserAndLogin(USER_NAME, "password");
        TaskAdminPage taskAdminPage = TaskAdminPage.gotoPage();
        taskAdminPage.forceEdit();
        List<String> ids = List.of("#projectTable", "#statusTable", "#severityTable");
        List<Integer> expectedResults = List.of(1, 3, 3);
        for (int i = 0; i < ids.size(); i++) {
            assertEquals(expectedResults.get(i), taskAdminPage.countSectionElements(ids.get(i)));
        }
    }
}
