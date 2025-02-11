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

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.junit.jupiter.api.Order;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.url.internal.standard.StandardURLStringEntityReferenceResolver;

import com.xwiki.task.model.Task;

import org.xwiki.contrib.application.task.test.po.TaskManagerAdminConfigurationPage;
import org.xwiki.contrib.application.task.test.po.TaskManagerGanttMacro;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerInlinePage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;

/**
 * UI Tests for the notifications received when being assigned to a task.
 *
 * @version $Id$
 * @since 3.7
 */
@UITest
public class GanttIT
{
    private static final String TEST_USERNAME = "TestUser";

    private static final String PASSWORD = "password";

    private static final DocumentReference GANTT_MACROS_PAGE = new DocumentReference("xwiki", "Test Space", "TestTaskGantt");

    private static final DocumentReference TASK_MACROS_PAGE =
        new DocumentReference("xwiki", "Test Space", "TestTaskMacros");

    private static final String GANTT_MACRO =
        "\n\n{{taskgantt readonly=\"false\" hideNoDueDate=\"false\"}}{{/taskgantt}}";

    private static final String GANTT_MACRO_READONLY = "\n\n{{taskgantt readonly=\"true\" hideNoDueDate=\"true\"}}{{/taskgantt}}";

    private static final String GANTT_MACRO_FILTER_ASSIGNEES =
        "\n\n{{taskgantt assignees=\"XWiki.Admin\" hideNoDueDate=\"false\"}}{{/taskgantt}}";

    private static final String GANTT_MACRO_FILTER_PROJECTS =
        "\n\n{{taskgantt projects=\"Other,Test Project\" hideNoDueDate=\"true\"}}{{/taskgantt}}";

    private static final String GANTT_MACRO_FILTER_SPACES = "\n\n{{taskgantt spaces=\"Test Space\" hideNoDueDate=\"false\"}}{{/taskgantt}}";

    private static final String GANTT_MACROS =
        GANTT_MACRO + GANTT_MACRO_READONLY + GANTT_MACRO_FILTER_ASSIGNEES + GANTT_MACRO_FILTER_PROJECTS
            + GANTT_MACRO_FILTER_SPACES;

    private static final String TASK_MACROS =
        "{{task reference=\"Test Space/Task_0\" reporter=\"XWiki.Admin\" "
            + "createDate=\"2001/01/01 12:00\" status=\"Done\" " + "completeDate=\"2001/01/01 12:00\"}}"
            + "Do this {{mention reference=\"XWiki.Admin\"/}} as late as {{date value=\"2001/01/03 12:00\"/}}"
            + "{{/task}}";

    @AfterAll
    public void teardownTaskPages(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(TASK_MACROS_PAGE);
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Test 0"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Test 1"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Test 2"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "No due date test"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Rights test view"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Rights test edit"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Project_Test Project"));
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", "Project_Test Project 2"));
        logout(setup);
    }

    public void logout(TestUtils setup)
    {
        setup.setSession(null);
        setup.getDriver().navigate().refresh();
    }

    @BeforeAll
    void setup(TestUtils setup) throws Exception
    {
        teardownTaskPages(setup);
        setup.loginAsSuperAdmin();
        setup.createUser(TEST_USERNAME, PASSWORD, "");

        TaskManagerAdminConfigurationPage taskManagerConfigPage = TaskManagerAdminConfigurationPage.gotoPage();
        taskManagerConfigPage.addNewProject("Test Project");
        taskManagerConfigPage = TaskManagerAdminConfigurationPage.gotoPage();
        taskManagerConfigPage.addNewProject("Test Project 2");

        // Create test tasks.

        createTaskPage("Test 0", "02/01/2001 00:00:00", "03/01/2001 00:00:00",
            "XWiki.Admin", "Other", "Low", Task.STATUS_IN_PROGRESS);
        createTaskPage("Test 1", "01/01/2001 00:00:00", "02/01/2001 00:00:00",
            "XWiki." + TEST_USERNAME, "Test Project", "Low", Task.STATUS_IN_PROGRESS);
        createTaskPage("Test 2", "01/01/2001 00:00:00", "02/01/2001 00:00:00",
            "", "Test Project 2", "Low", Task.STATUS_IN_PROGRESS);

        createTaskPage("No due date test", "01/01/2001 00:00:00", " ",
            "XWiki.Admin", "Other", "Medium", Task.STATUS_IN_PROGRESS);

        createTaskPage("Rights test view", "01/01/2001 00:00:00", "02/01/2001 00:00:00",
            "", "Other", "Low", Task.STATUS_IN_PROGRESS);
        setup.setRights(new DocumentReference("xwiki", "TaskManager", "Rights test view"), null,
            "XWiki." + TEST_USERNAME, "view", false);

        createTaskPage("Rights test edit", "01/01/2001 00:00:00", "02/01/2001 00:00:00",
            "", "Test Project 2", "Low", Task.STATUS_IN_PROGRESS);
        setup.setRights(new DocumentReference("xwiki", "TaskManager", "Rights test edit"), null,
            "XWiki." + TEST_USERNAME, "edit", false);

        setup.createPage(TASK_MACROS_PAGE, TASK_MACROS, "Task Macros");

        // Create page with multiple Gantt Macros, to also test for interference between multiple instances
        // of the macro in a page.
        setup.createPage(GANTT_MACROS_PAGE, GANTT_MACROS, "Gantt Macros");

        logout(setup);
        TaskManagerHomePage.gotoPage();
        setup.login(TEST_USERNAME, PASSWORD);
    }

    /**
     * Test that only tasks which the current user can view are shown in the diagram.
     *
     * @since 3.7
     */
    @Test
    @Order(1)
    void respectRights(TestUtils setup)
    {
        setup.gotoPage(GANTT_MACROS_PAGE);
        List<TaskManagerGanttMacro> gantts = TaskManagerGanttMacro.getGanttMacrosOnCurrentPage();
        for(int i = 0; i < gantts.size(); i++) {
            Assertions.assertFalse(gantts.get(i).getTaskIds().contains("xwiki:TaskManager.Rights test view"), 
                "" + i + "-th gantt diagram bypasses view rights");
        }
        Assertions.assertTrue(gantts.get(0).getTaskIds().contains("xwiki:TaskManager.Rights test edit"));
        gantts.get(0).dragTask("xwiki:TaskManager.Rights test edit", 150);
        WebElement notification = setup.getDriver().findElement(By.cssSelector(".xnotification"));
        // Editing was not successful for the uneditable task.
        Assertions.assertTrue(notification.getAttribute("class").contains("xnotification-error"));
        // Clear the notification.
        notification.click();
    }

    /**
     * Test that the various gantt parameters work.
     *
     * @since 3.7
     */
    @Test
    @Order(2)
    void respectParameters(TestUtils setup)
    {
        List<TaskManagerGanttMacro> gantts = TaskManagerGanttMacro.getGanttMacrosOnCurrentPage();
        List<String> ids;

        // Assignee filter and hide no due date
        ids = gantts.get(2).getTaskIds();
        Assertions.assertEquals(3, ids.size(), ids.toString());
        Assertions.assertTrue(ids.contains("xwiki:TaskManager.Test 0"), ids.toString());
        Assertions.assertTrue(ids.contains("xwiki:TaskManager.No due date test"), ids.toString());
        Assertions.assertTrue(ids.contains("xwiki:Test Space.Task_0.WebHome"), ids.toString());

        // Project filter
        ids = gantts.get(3).getTaskIds();
        Assertions.assertEquals(2, ids.size(), ids.toString());
        Assertions.assertTrue(ids.contains("xwiki:TaskManager.Test 0"), ids.toString());
        Assertions.assertTrue(ids.contains("xwiki:TaskManager.Test 1"), ids.toString());

        // Space filter
        ids = gantts.get(4).getTaskIds();
        Assertions.assertEquals(1, ids.size(), ids.toString());
        Assertions.assertTrue(ids.contains("xwiki:Test Space.Task_0.WebHome"), ids.toString());
        
        // Readonly
        gantts.get(1).dragTask("xwiki:TaskManager.Test 0", 100);
        WebElement notification = setup.getDriver().findElement(By.cssSelector(".xnotification"));
        // Editing was not successful for the read only task.
        Assertions.assertTrue(notification.getAttribute("class").contains("xnotification-error"));
        // Clear the notification.
        notification.click();
    }

    /**
     * Test that changes made in the diagram are saved.
     *
     * @since 3.7
     */
    @Test
    @Order(3)
    void taskBarInteractions(TestUtils setup)
    {
        TaskManagerGanttMacro gantt = TaskManagerGanttMacro.getGanttMacrosOnCurrentPage().get(0);
        String taskPageId = "xwiki:TaskManager.Test 0";

        // Test that the task was opened in a new tab.
        gantt.openTaskPage(taskPageId);
        Object[] windowHandles = setup.getDriver().getWindowHandles().toArray();
        Assertions.assertEquals(2, windowHandles.length);
        setup.getDriver().switchTo().window((String) windowHandles[1]);
        Assertions.assertEquals(
            new DocumentReference("xwiki", "TaskManager", "Test 0"),
            new StandardURLStringEntityReferenceResolver()
                .resolve(setup.getDriver().getCurrentUrl(), EntityType.DOCUMENT)
        );
        setup.getDriver().close();
        setup.getDriver().switchTo().window((String) windowHandles[0]);
    }

    private void createTaskPage(String title, String startDate, String dueDate, String assignee, String project,
        String severity, String status)
    {
        TaskManagerHomePage taskManagerHomePage = TaskManagerHomePage.gotoPage();
        CreatePagePage createPage = taskManagerHomePage.createPage();

        createPage.getDocumentPicker().setTitle(title);
        createPage.setTemplate("TaskManager.TaskManagerTemplateProvider");
        createPage.clickCreate();

        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.setAssignee(assignee);
        inlinePage.setDueDate(dueDate);
        inlinePage.setStartDate(startDate);
        inlinePage.setStatus(status);
        inlinePage.setProject(project);
        inlinePage.setSeverity(severity);
        inlinePage.clickSaveAndView();
    }
}
