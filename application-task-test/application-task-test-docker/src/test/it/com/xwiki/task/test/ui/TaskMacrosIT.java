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

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterAll
    void cleanup(TestUtils setup)
    {
        DocumentReference ref1 = new DocumentReference("xwiki", "TaskManager", "TaskFromTemplate");
        setup.deletePage(ref1);
        DocumentReference refSubwiki1 = new DocumentReference("wiki1", "TaskManager", "TaskFromTemplate");
        setup.deletePage(refSubwiki1);
        DocumentReference ref2 = new DocumentReference("xwiki", "TaskManager", "TaskDependency2");
        setup.deletePage(ref2);
        DocumentReference refSubwiki2 = new DocumentReference("wiki1", "TaskManager", "TaskDependency2");
        setup.deletePage(refSubwiki2);
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
}
