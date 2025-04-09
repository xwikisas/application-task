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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerInlinePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerViewPage;
import org.xwiki.contrib.application.task.test.po.ViewPageWithTasks;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private final DocumentReference pageWithTaskMacros = new DocumentReference("xwiki", "Main", "Test");

    private final DocumentReference pageWithComplexTaskMacros = new DocumentReference("xwiki", "Main", "Test2");

    private final DocumentReference pageWithTaskRaport = new DocumentReference("xwiki", "Main", "Test3");

    private static final String SIMPLE_TASKS = "{{task reference=\"Task_1\"}}Do this{{/task}}\n\n"
        + "{{task reference=\"Task_2\" status=\"Done\"}}Do this as well{{/task}}";

    private static final String COMPLEX_TASKS =
        "{{task reference=\"Task_3\" reporter=\"XWiki.Admin\" createDate=\"2023/01/01 12:00\" status=\"Done\" "
            + "completeDate=\"2023/01/01 12:00\"}}"
            + "Do this {{mention reference=\"XWiki.Admin\"/}} as late as {{date value=\"2023/01/01 12:00\"/}}"
            + "{{/task}}";

    private static final String TASK_REPORT_MACRO = "{{task-report /}}";

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(pageWithTaskMacros);
        setup.deletePage(pageWithComplexTaskMacros);
    }

    @Test
    @Order(1)
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

    @Test
    @Order(2)
    void simpleTaskMacros(TestUtils setup)
    {
        setup.createPage(pageWithTaskMacros, SIMPLE_TASKS, pageWithTaskMacros.getName());
        ViewPageWithTasks page = new ViewPageWithTasks();
        // Check first, unchecked macro.
        assertEquals("Do this", page.getTaskMacroContent(0));
        assertEquals("#1", page.getTaskMacroLink(0).getText());
        assertTrue(page.getTaskMacroLink(0).getAttribute("href").contains("/xwiki/bin/view/Task_1"));
        assertFalse(page.isTaskMacroCheckboxChecked(0));
        page.clickTaskMacroCheckbox(0);
        // Check second, checked macro.
        assertEquals("Do this as well", page.getTaskMacroContent(1));
        assertEquals("#2", page.getTaskMacroLink(1).getText());
        assertTrue(page.getTaskMacroLink(1).getAttribute("href").contains("/xwiki/bin/view/Task_2"));
        assertTrue(page.isTaskMacroCheckboxChecked(1));
        page.clickTaskMacroCheckbox(1);
        // Refresh the page and make sure the changes are saved.
        setup.getDriver().navigate().refresh();
        page = new ViewPageWithTasks();
        assertTrue(page.isTaskMacroCheckboxChecked(0));
        assertFalse(page.isTaskMacroCheckboxChecked(1));
    }

    @Test
    @Order(3)
    void taskManagerHomePage()
    {
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

    @Test
    @Order(4)
    void complexTaskMacros(TestUtils setup)
    {
        setup.createPage(pageWithComplexTaskMacros, COMPLEX_TASKS, pageWithComplexTaskMacros.getName());
        ViewPageWithTasks page = new ViewPageWithTasks();
        assertEquals("Do this @Admin as late as 2023/01/01 12:00", page.getTaskMacroContent(0));
        assertEquals("#3", page.getTaskMacroLink(0).getText());
        assertTrue(page.getTaskMacroLink(0).getAttribute("href").contains("/xwiki/bin/view/Task_3"));
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

    @Test
    @Order(5)
    void taskMacroAndTaskPageRelation(TestUtils setup)
    {
        TaskManagerViewPage viewPage = new TaskManagerViewPage();
        viewPage.edit();
        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.waitUntilPageIsReady();
        // Changing a property of the page should change the macro call.
        inlinePage.setStatus("ToDo");
        inlinePage.clickSaveAndView();
        setup.gotoPage(pageWithComplexTaskMacros);
        ViewPageWithTasks viewPageWithTaskMacro = new ViewPageWithTasks();
        assertFalse(viewPageWithTaskMacro.isTaskMacroCheckboxChecked(0));
        // Changing the property of the macro call should change the page.
        viewPageWithTaskMacro.clickTaskMacroCheckbox(0);
        viewPageWithTaskMacro.getTaskMacroLink(0).click();
        viewPage = new TaskManagerViewPage();
        assertEquals("Done", viewPage.getStatus());
    }

    @Test
    @Order(6)
    void taskReport(TestUtils setup)
    {
        setup.createPage(pageWithTaskRaport, TASK_REPORT_MACRO);
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
        assertEquals("#3\nDo this @Admin as late as 2023/01/01 12:00",
            taskReport.getCell(row, taskTileCellIndex).getText());
        assertEquals("01/01/2023 12:00:00", taskReport.getCell(row, taskDeadlineCellIndex).getText());
        assertEquals("Admin", taskReport.getCell(row, taskAssigneeCellIndex).getText());
        assertEquals(pageWithComplexTaskMacros.getName(), taskReport.getCell(row, taskLocationCellIndex).getText());
    }

    @Test
    @Order(7)
    void deleteTaskPage(TestUtils setup) throws Exception
    {
        // Deleting the page that contains task macros should also delete the task pages.
        setup.gotoPage(pageWithTaskMacros);
        assertTrue(setup.pageExists("Task_1", "WebHome"));
        assertTrue(setup.pageExists("Task_2", "WebHome"));
        setup.deletePage(pageWithTaskMacros);
        assertFalse(setup.pageExists("Task_1", "WebHome"));
        assertFalse(setup.pageExists("Task_2", "WebHome"));
    }

    @Test
    @Order(8)
    void deleteTaskMacro(TestUtils setup) throws Exception
    {
        // Deleting a task page should also delete the task macro from the owner page.
        setup.gotoPage(pageWithComplexTaskMacros);
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        assertEquals(1, viewPage.getTaskMacros().size());
        viewPage.getTaskMacroLink(0).click();
        ViewPage taskPage = new ViewPage();
        taskPage.deletePage().confirmDeletePage();
        setup.gotoPage(pageWithComplexTaskMacros);
        viewPage = new ViewPageWithTasks();
        assertEquals(0, viewPage.getTaskMacros().size());
    }


}
