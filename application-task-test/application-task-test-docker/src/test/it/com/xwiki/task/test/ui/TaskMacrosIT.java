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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openqa.selenium.support.Color;
import org.xwiki.contrib.application.task.test.po.DateMacroAdminPage;
import org.xwiki.contrib.application.task.test.po.DateMacroPage;
import org.xwiki.contrib.application.task.test.po.KanbanBoardMacro;
import org.xwiki.contrib.application.task.test.po.KanbanBoardMacroPage;
import org.xwiki.contrib.application.task.test.po.KanbanCard;
import org.xwiki.contrib.application.task.test.po.KanbanColumn;
import org.xwiki.contrib.application.task.test.po.TaskCardMacro;
import org.xwiki.contrib.application.task.test.po.TaskCardMacroPage;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerInlinePage;
import org.xwiki.contrib.application.task.test.po.TaskReportMacro;
import org.xwiki.contrib.application.task.test.po.ViewPageWithTasks;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.tag.test.po.AddTagsPane;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI Tests for Task Manager application macros.
 *
 * @version $Id$
 * @since 3.11.0
 */
@UITest(
    // Needed for the dependency to the mentions macro that uses solr.
    extraJARs = { "org.xwiki.platform:xwiki-platform-eventstream-store-solr:15.10", "com.xwiki.date:macro-date-api",
        "com.xwiki.date:macro-date-default" }, extensionOverrides = {
    @ExtensionOverride(extensionId = "com.google.code.findbugs:jsr305", overrides = {
        "features=com.google.code.findbugs:annotations" }) }, properties = {
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.tag.TagPlugin" }, resolveExtraJARs = true)
public class TaskMacrosIT
{
    private final LocalDocumentReference pageWithTaskReportWithParameters =
        new LocalDocumentReference("Main", "TaskReportParametersTest");

    private final LocalDocumentReference pageWithTasks = new LocalDocumentReference("Main", "PageWithTasks");

    private final LocalDocumentReference pageWithTags = new LocalDocumentReference("Main", "PageWithTags");

    private final LocalDocumentReference pageWithTags2 = new LocalDocumentReference("Main", "pageWithTags2");

    private final LocalDocumentReference pageWithTaskListMacro =
        new LocalDocumentReference("Main", "PageWithTaskListMacro");

    private final LocalDocumentReference pageWithTaskCardMacro =
        new LocalDocumentReference("Main", "PageWithTaskCardMacro");

    private final LocalDocumentReference pageWithKanbanMacro =
        new LocalDocumentReference("Main", "PageWithKanbanMacro");

    private final LocalDocumentReference pageWithKanbanMacroParameters =
        new LocalDocumentReference("Main", "PageWithKanbanMacroParameters");

    private final LocalDocumentReference pageWithKanbanMacroOrdered =
        new LocalDocumentReference("Main", "PageWithKanbanMacroOrdered");

    private final LocalDocumentReference pageWithKanbanMacroSpace =
        new LocalDocumentReference("Main", "PageWithKanbanMacroSpace");

    private final LocalDocumentReference pageWithKanbanMacroLimit =
        new LocalDocumentReference("Main", "PageWithKanbanMacroLimit");

    private final LocalDocumentReference pageWithDateMacro = new LocalDocumentReference("Main", "PageWithDateMacro");

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterAll
    void cleanup(TestUtils setup)
    {
        deleteTaskFromWiki("TaskFromTemplate", setup);
        deleteTaskFromWiki("TaskDependency2", setup);
        deleteTaskFromWiki("TaskK1", setup);
        deleteTaskFromWiki("TaskK2", setup);
        deleteTaskFromWiki("TaskK3", setup);
    }

    @ParameterizedTest
    @WikisSource(extensions = { "com.xwiki.task:application-task-ui", "org.xwiki.platform:xwiki-platform-tag-ui/15.10" })
    @Order(10)
    void taskReportParametersTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        createPagesWithTasks(wiki, setup);
        createPageFromTemplate("TaskFromTemplate", "TaskManager.TaskManagerTemplates.TaskManagerTemplateProvider");

        DocumentReference testRef2 = new DocumentReference(pageWithTaskReportWithParameters, wiki);
        setup.createPage(testRef2, getMacroContent("taskReport.vm"));
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        viewPage.waitUntilPageIsReady();

        // Checks the "limit" parameter, limit=2.
        TaskReportMacro reportMacro1 = new TaskReportMacro("reportid1");
        assertEquals(2, reportMacro1.getTasks().size());

        // Checks the columns parameter, columns="name,assignee".
        assertEquals(2, reportMacro1.getColumnCount());
        assertEquals(List.of("Task", "Assignee"), reportMacro1.getColumnNames());

        assertEquals("Task 1 do this", reportMacro1.getTask(0).getContent());
        assertEquals("tag1", reportMacro1.getTask(1).getContent());

        TaskReportMacro reportMacro2 = new TaskReportMacro("reportid2");

        // Checks the columns parameter, columns="name,duedate,assignee,owner,completeDate".
        assertEquals(5, reportMacro2.getColumnCount());
        assertEquals(List.of("Task", "Deadline", "Assignee", "Location", "Completed on"),
            reportMacro2.getColumnNames());

        // Checks the "sortBy" parameter, sortBy=name.
        assertEquals("Complete this @Admin as late as 2023/01/01 12:00", reportMacro2.getTask(0).getContent());
        assertEquals("Do this task as well", reportMacro2.getTask(1).getContent());
        assertEquals("tag2", reportMacro2.getTask(2).getContent());
        assertEquals("TaskFromTemplate2001/01/01 01:01", reportMacro2.getTask(3).getContent());

        // Checks the "status" parameter, status=Done.
        assertTrue(reportMacro2.getTask(0).isChecked());
        assertTrue(reportMacro2.getTask(1).isChecked());
        assertTrue(reportMacro2.getTask(2).isChecked());
        assertTrue(reportMacro2.getTask(3).isChecked());

        // Checks the "tags" parameter, tags="tag1,tag2".
        TaskReportMacro reportMacro4 = new TaskReportMacro("reportid4");
        assertEquals("tag1", reportMacro4.getTask(0).getContent());
        assertEquals("tag2", reportMacro4.getTask(1).getContent());

        // Checks the "pages" parameter, pages="TaskManager.WebHome".
        TaskReportMacro reportMacro5 = new TaskReportMacro("reportid5");
        assertEquals("TaskFromTemplate2001/01/01 01:01", reportMacro5.getTask(0).getContent());
    }

    @Test
    @Order(15)
    void reportersParametersTest(TestUtils setup)
    {
        setup.gotoPage(pageWithTaskReportWithParameters);
        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        viewPage.waitUntilPageIsReady();

        // Checks the "reporters" parameter, reporters="XWiki.Admin".
        TaskReportMacro reportMacro3 = new TaskReportMacro("reportid3");
        assertEquals("Complete this @Admin as late as 2023/01/01 12:00", reportMacro3.getTask(0).getContent());
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(20)
    void taskListTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        setup.gotoPage(pageWithTasks);
        ViewPageWithTasks viewPageTask = new ViewPageWithTasks();
        viewPageTask.waitUntilPageIsReady();

        // We take the task ids from the page where they were created, depending on the previous tests the ids will
        // change.
        String tag1 = viewPageTask.getTasks().get(0).getTaskId();
        String tag2 = viewPageTask.getTasks().get(1).getTaskId();
        String tag3 = viewPageTask.getTasks().get(2).getTaskId();
        String tag4 = viewPageTask.getTasks().get(3).getTaskId();

        assertEquals("Task 1 do this", viewPageTask.getTaskMacroContent(0));
        assertEquals("Do this as well", viewPageTask.getTaskMacroContent(1));
        assertEquals("Do this task as well", viewPageTask.getTaskMacroContent(2));
        assertEquals("Complete this @Admin as late as 2023/01/01 12:00", viewPageTask.getTaskMacroContent(3));

        String macro = String.format("{{tasks ids=\"%s, %s, %s, %s\" /}}", tag1, tag2, tag3, tag4);

        DocumentReference testRef = new DocumentReference(pageWithTaskListMacro, wiki);
        setup.createPage(testRef, macro);

        ViewPageWithTasks viewPageListTasks = new ViewPageWithTasks();
        viewPageListTasks.waitUntilPageIsReady();

        // Checks that the task list macro correctly shows the tasks respective to their ids.
        assertEquals(4, viewPageListTasks.getTasks().size());
        assertEquals("Task 1 do this", viewPageListTasks.getTaskMacroContent(0));
        assertEquals("Do this as well", viewPageListTasks.getTaskMacroContent(1));
        assertEquals("Do this task as well", viewPageListTasks.getTaskMacroContent(2));
        assertEquals("Complete this @Admin as late as 2023/01/01 12:00", viewPageListTasks.getTaskMacroContent(3));

        assertEquals(tag1, viewPageListTasks.getTasks().get(0).getTaskId());
        assertEquals(tag2, viewPageListTasks.getTasks().get(1).getTaskId());
        assertEquals(tag3, viewPageListTasks.getTasks().get(2).getTaskId());
        assertEquals(tag4, viewPageListTasks.getTasks().get(3).getTaskId());

        // Clean up.
        setup.deletePage(pageWithTasks);
        setup.deletePage(pageWithTags);
        setup.deletePage(pageWithTags2);
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(25)
    void taskCardTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        createPageFromTemplate("TaskDependency2", "TaskManager.TaskManagerTemplates.TaskManagerTemplateProvider");

        DocumentReference testRef = new DocumentReference(pageWithTaskCardMacro, wiki);
        setup.createPage(testRef, getMacroContent("taskCard.vm"));

        TaskCardMacroPage page = new TaskCardMacroPage();
        TaskCardMacro taskCard0 = page.getTaskCard(0);
        assertEquals("Done", taskCard0.getStatus());
        assertEquals("TaskFromTemplate", taskCard0.getTitle());
        assertEquals("01/01/2001 01:01:01", taskCard0.getDueDate());

        // Adding a task dependency.
        taskCard0.goToTaskPage();
        ViewPage viewPage = new ViewPage();
        viewPage.editInline();
        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.setDependency("TaskManager.TaskDependency2");
        inlinePage.clickSaveAndView();

        setup.gotoPage(pageWithTaskCardMacro);
        TaskCardMacroPage page2 = new TaskCardMacroPage();
        TaskCardMacro taskCard = page2.getTaskCard(0);
        assertTrue(taskCard.hasDependencies());
        assertEquals(1, taskCard.getDependenciesCount());

        // Checks the information about the task added as a dependency.
        assertEquals("TaskDependency2", taskCard.getDependency(0).getDependencyTitle());
        assertEquals("Done", taskCard.getDependency(0).getDependencyStatus());
        assertTrue(taskCard.getDependency(0).isDone());

        // Checks the dependencies="false" property.
        TaskCardMacro taskCard2 = page2.getTaskCard(1);
        assertFalse(taskCard2.hasDependencies());
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(35)
    void kanbanMacroTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        createTaskFromTemplateKanban("TaskK1", Task.STATUS_IN_PROGRESS, true, "XWiki.UserTest", "01/01/2002 01:01:01");
        createTaskFromTemplateKanban("TaskK2", Task.STATUS_IN_PROGRESS, false, "XWiki.UserTest", "01/01/2002 01:01:01");
        createTaskFromTemplateKanban("TaskK3", Task.STATUS_IN_PROGRESS, true, "XWiki.UserTest2", "01/01/2003 01:01:01");
        setup.createPage(pageWithTasks, "{{task reference=\"Task_1\"}}Task 1 do this{{/task}}");

        DocumentReference testRef = new DocumentReference(pageWithKanbanMacro, wiki);
        setup.createPage(testRef, "{{kanbanboard/}}");

        KanbanBoardMacroPage kanbanPage = new KanbanBoardMacroPage();
        KanbanBoardMacro board = kanbanPage.getKanbanMacro();

        KanbanColumn todo = board.getColumn(0);
        assertEquals("ToDo", todo.getType());
        List<KanbanCard> cards = todo.getTaskCards();
        assertEquals(1, cards.size());

        KanbanCard card = cards.get(0);
        assertEquals("TaskK2", card.getTask());
        assertTrue(card.getLink().contains("view/TaskManager/TaskK2"));
        assertEquals("UserTest", card.getFieldValue("Assignee"));
        assertEquals("01/01/2002 01:01:01", card.getFieldValue("Due Date"));
        assertEquals(0, card.getProgressPercentage());

        KanbanColumn inProgress = board.getColumn(1);
        assertEquals("InProgress", inProgress.getType());
        List<KanbanCard> inProgressCards = inProgress.getTaskCards();
        assertEquals(3, inProgressCards.size());

        KanbanCard inProgressCard0 = inProgressCards.get(0);
        assertEquals("TaskK1", inProgressCard0.getTask());
        assertTrue(inProgressCard0.getLink().contains("view/TaskManager/TaskK1"));
        assertEquals("UserTest", inProgressCard0.getFieldValue("Assignee"));
        assertEquals("01/01/2002 01:01:01", inProgressCard0.getFieldValue("Due Date"));
        assertEquals(50, inProgressCard0.getProgressPercentage());

        KanbanCard inProgressCard1 = inProgressCards.get(1);
        assertEquals("TaskK3", inProgressCard1.getTask());
        assertTrue(inProgressCard1.getLink().contains("view/TaskManager/TaskK3"));
        assertEquals("UserTest2", inProgressCard1.getFieldValue("Assignee"));
        assertEquals("01/01/2003 01:01:01", inProgressCard1.getFieldValue("Due Date"));
        assertEquals(50, inProgressCard1.getProgressPercentage());

        KanbanCard inProgressCard2 = inProgressCards.get(2);
        assertEquals("Task 1 do this", inProgressCard2.getTask());
        assertTrue(inProgressCard2.getLink().contains("view/Task_1"));

        KanbanColumn completedTask = board.getColumn(2);
        assertEquals("Done", completedTask.getType());
        List<KanbanCard> completedTaskCards = completedTask.getTaskCards();
        assertEquals(1, completedTaskCards.size());

        KanbanCard completedTaskCard = completedTaskCards.get(0);
        assertEquals("TaskDependency2", completedTaskCard.getTask());
        assertTrue(completedTaskCard.getLink().contains("view/TaskManager/TaskDependency2"));
        assertEquals("", completedTaskCard.getFieldValue("Assignee"));
        assertEquals("01/01/2001 01:01:01", completedTaskCard.getFieldValue("Due Date"));
        assertEquals(100, completedTaskCard.getProgressPercentage());
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(45)
    void kanbanMacroWithParametersTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference testRef = new DocumentReference(pageWithKanbanMacroParameters, wiki);
        setup.createPage(testRef, "{{kanbanboard user=\"XWiki.UserTest\" colors=\"yellow,#3e6970,pink\" "
            + "columns=\"InProgress,Done\" columnWidth=\"20%\"/}}");

        KanbanBoardMacroPage kanbanPage = new KanbanBoardMacroPage();
        KanbanBoardMacro board = kanbanPage.getKanbanMacro();

        // Checks that the columns parameter works, the column "To Do" isn't present.
        KanbanColumn kanbanColumn0 = board.getColumn(0);
        assertEquals("InProgress", kanbanColumn0.getType());

        // Checks that the colors parameter works and that the personalized colors are applied.
        assertEquals("rgb(255, 204, 51)", kanbanColumn0.getHeaderColor());

        // Checks that the columnWidth parameter works, columnWidth="20%".
        assertEquals("20%", kanbanColumn0.getWidth());
        List<KanbanCard> cards = kanbanColumn0.getTaskCards();

        // Checks that only tasks which have UserTest assigned appear.
        assertEquals(1, cards.size());
        KanbanCard card = cards.get(0);
        assertEquals("TaskK1", card.getTask());
        assertEquals("UserTest", card.getFieldValue("Assignee"));
        assertEquals(50, card.getProgressPercentage());

        KanbanColumn kanbanColumn1 = board.getColumn(1);
        assertEquals("Done", kanbanColumn1.getType());
        assertEquals("#3e6970", Color.fromString(kanbanColumn1.getHeaderColor()).asHex());
        assertEquals("20%", kanbanColumn1.getWidth());
        List<KanbanCard> cards1 = kanbanColumn1.getTaskCards();

        // There are no completed tasks which have UserTest assigned.
        assertEquals(0, cards1.size());
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(55)
    void kanbanMacroOrderedTasksTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference testRef = new DocumentReference(pageWithKanbanMacroOrdered, wiki);
        setup.createPage(testRef, "{{kanbanboard columns=\"InProgress\" order=\"assignee desc\"/}}");

        KanbanBoardMacroPage kanbanPage = new KanbanBoardMacroPage();
        KanbanBoardMacro board = kanbanPage.getKanbanMacro();
        KanbanColumn kanbanColumn = board.getColumn(0);
        assertEquals("InProgress", kanbanColumn.getType());
        List<KanbanCard> cards = kanbanColumn.getTaskCards();

        // Checks that the tasks are ordered by the assignee in reversed order.
        assertEquals(3, cards.size());
        KanbanCard cardTask0 = cards.get(0);
        assertEquals("TaskK3", cardTask0.getTask());
        assertEquals("UserTest2", cardTask0.getFieldValue("Assignee"));
        KanbanCard cardTask1 = cards.get(1);
        assertEquals("TaskK1", cardTask1.getTask());
        assertEquals("UserTest", cardTask1.getFieldValue("Assignee"));
        KanbanCard cardTask2 = cards.get(2);
        assertEquals("Task 1 do this", cardTask2.getTask());
        assertEquals("", cardTask2.getFieldValue("Assignee"));
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(65)
    void kanbanMacroSpaceTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference testRef = new DocumentReference(pageWithKanbanMacroSpace, wiki);
        setup.createPage(testRef, "{{kanbanboard space=\"TaskManager\"/}}");

        // Checks that only tasks created in the TaskManager space appear.
        KanbanBoardMacroPage kanbanPage = new KanbanBoardMacroPage();
        KanbanBoardMacro board = kanbanPage.getKanbanMacro();

        KanbanColumn todo = board.getColumn(0);
        assertEquals("ToDo", todo.getType());
        List<KanbanCard> cards = todo.getTaskCards();
        assertEquals(1, cards.size());

        KanbanCard card = cards.get(0);
        assertEquals("TaskK2", card.getTask());
        assertTrue(card.getLink().contains("view/TaskManager/TaskK2"));

        KanbanColumn inProgress = board.getColumn(1);
        assertEquals("InProgress", inProgress.getType());
        List<KanbanCard> inProgressCards = inProgress.getTaskCards();
        assertEquals(2, inProgressCards.size());

        KanbanCard inProgressCard0 = inProgressCards.get(0);
        assertEquals("TaskK1", inProgressCard0.getTask());
        assertTrue(inProgressCard0.getLink().contains("view/TaskManager/TaskK1"));

        KanbanCard inProgressCard1 = inProgressCards.get(1);
        assertEquals("TaskK3", inProgressCard1.getTask());
        assertTrue(inProgressCard1.getLink().contains("view/TaskManager/TaskK3"));

        KanbanColumn completedTask = board.getColumn(2);
        assertEquals("Done", completedTask.getType());
        List<KanbanCard> completedTaskCards = completedTask.getTaskCards();
        assertEquals(1, completedTaskCards.size());

        KanbanCard completedTaskCard = completedTaskCards.get(0);
        assertEquals("TaskDependency2", completedTaskCard.getTask());
        assertTrue(completedTaskCard.getLink().contains("view/TaskManager/TaskDependency2"));
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(75)
    void kanbanMacroLimitTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference testRef = new DocumentReference(pageWithKanbanMacroLimit, wiki);
        setup.createPage(testRef, "{{kanbanboard columns=\"InProgress\" limit=\"2\"/}}");

        KanbanBoardMacroPage kanbanPage = new KanbanBoardMacroPage();
        KanbanBoardMacro board = kanbanPage.getKanbanMacro();
        KanbanColumn kanbanColumn = board.getColumn(0);
        assertEquals("InProgress", kanbanColumn.getType());
        List<KanbanCard> cards = kanbanColumn.getTaskCards();

        // Checks that the limit parameter works, there are 3 tasks but only 2 are shown.
        assertEquals(2, cards.size());
        KanbanCard cardTask0 = cards.get(0);
        assertEquals("TaskK1", cardTask0.getTask());

        KanbanCard cardTask1 = cards.get(1);
        assertEquals("TaskK3", cardTask1.getTask());

        setup.deletePage(pageWithTasks);
    }

    @ParameterizedTest
    @WikisSource(extensions = "com.xwiki.task:application-task-ui")
    @Order(85)
    void dateMacroTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference testRef = new DocumentReference(pageWithDateMacro, wiki);
        setup.createPage(testRef, "{{date value=\"2026/01/05 11:36\"/}}");

        DateMacroPage page = new DateMacroPage();
        assertTrue(page.isDateDisplayed());
        assertEquals("2026/01/05 11:36", page.getDisplayedDate());

        // Change the date macro display format in the admin section.
        DateMacroAdminPage adminPage = DateMacroAdminPage.gotoPage();
        adminPage.setDisplayDateFormat("MMM YYYY");
        adminPage.clickSave();
        assertEquals("MMM YYYY", adminPage.getDisplayDateFormat());

        // Checks that the date display format has been applied.
        setup.gotoPage(pageWithDateMacro);
        DateMacroPage page2 = new DateMacroPage();
        assertTrue(page2.isDateDisplayed());
        assertEquals("Jan 2026", page2.getDisplayedDate());

        // Change back the macro display format to default.
        DateMacroAdminPage adminPage2 = DateMacroAdminPage.gotoPage();
        adminPage2.clearDisplayDateFormat();
        adminPage2.clickSave();
        assertEquals("", adminPage2.getDisplayDateFormat());
    }

    private String getMacroContent(String filename)
    {
        try (InputStream inputStream = getClass().getResourceAsStream("/taskMacros/" + filename)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to load " + filename + " from resources.");
            }

            return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .filter(line -> !line.trim().startsWith("##")).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read macro file: " + filename, e);
        }
    }

    private void createPagesWithTasks(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());

        DocumentReference taskRef = new DocumentReference(pageWithTasks, wiki);
        setup.createPage(taskRef, getMacroContent("tasks.vm"));

        DocumentReference refTag1 = new DocumentReference(pageWithTags, wiki);
        setup.createPage(refTag1, "{{task reference=\"Task_10\"}}tag1{{/task}}");
        addTagToPage(setup, refTag1, "tag1");

        DocumentReference refTag2 = new DocumentReference(pageWithTags2, wiki);
        setup.createPage(refTag2, "{{task reference=\"Task_11\" status=\"Done\"}}tag2{{/task}}");
        addTagToPage(setup, refTag2, "tag2");
    }

    private void addTagToPage(TestUtils setup, DocumentReference ref, String tag)
    {
        setup.gotoPage(ref);
        TaggablePage taggablePage = new TaggablePage();
        AddTagsPane tagsPane = taggablePage.addTags();
        tagsPane.setTags(tag);
        tagsPane.add();
    }

    private void createPageFromTemplate(String title, String template)
    {
        TaskManagerHomePage taskManagerHomePage = TaskManagerHomePage.gotoPage();
        CreatePagePage createPage = taskManagerHomePage.createPage();

        createPage.getDocumentPicker().setTitle(title);
        createPage.setTemplate(template);
        createPage.clickCreate();

        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.setDueDate("01/01/2001 01:01:01");
        inlinePage.setStatus(Task.STATUS_DONE);
        inlinePage.setProject("Other");
        inlinePage.setSeverity("Low");
        inlinePage.clickSaveAndView();
    }

    private void createTaskFromTemplateKanban(String title, String status, Boolean setStatus, String assignee,
        String dueDate)
    {
        TaskManagerHomePage taskManagerHomePage = TaskManagerHomePage.gotoPage();
        CreatePagePage createPage = taskManagerHomePage.createPage();

        createPage.getDocumentPicker().setTitle(title);
        createPage.setTemplate("TaskManager.TaskManagerTemplates.TaskManagerTemplateProvider");
        createPage.clickCreate();

        TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
        inlinePage.setDueDate(dueDate);
        if (setStatus) {
            inlinePage.setStatus(status);
            inlinePage.setProgress("50");
        }
        inlinePage.setAssignee(assignee);
        inlinePage.clickSaveAndView();
    }

    private void deleteTaskFromWiki(String taskName, TestUtils setup)
    {
        DocumentReference ref = new DocumentReference("xwiki", "TaskManager", taskName);
        setup.deletePage(ref);
        DocumentReference refSubwiki = new DocumentReference("wiki1", "TaskManager", taskName);
        setup.deletePage(refSubwiki);
    }
}
