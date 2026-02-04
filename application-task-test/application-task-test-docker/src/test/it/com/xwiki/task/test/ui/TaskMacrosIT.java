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
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.contrib.application.task.test.po.TaskElement;
import org.xwiki.contrib.application.task.test.po.TaskReportMacro;
import org.xwiki.contrib.application.task.test.po.ViewPageWithTasks;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.tag.test.po.AddTagsPane;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.tag.test.po.TaggablePage;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UITest(
    // Needed for the dependency to the mentions macro that uses solr.
    extraJARs = { "org.xwiki.platform:xwiki-platform-eventstream-store-solr:14.10", "com.xwiki.date:macro-date-api",
        "com.xwiki.date:macro-date-default" },
    extensionOverrides = { @ExtensionOverride(extensionId = "com.google.code.findbugs:jsr305", overrides =  {
        "features=com.google.code.findbugs:annotations" }) },
    properties = { "xwikiCfgPlugins=com.xpn.xwiki.plugin.tag.TagPlugin" }, resolveExtraJARs = true)
public class TaskMacrosIT
{
    private final LocalDocumentReference pageWithTaskReportWithParameters =
        new LocalDocumentReference("Main", "TaskReportTest");

    private final LocalDocumentReference pageWithTaskMacros = new LocalDocumentReference("Main", "PageWithTaskMacro");

    private final LocalDocumentReference pageWithKanbanMacros = new LocalDocumentReference("Main", "KanbanMacroTest");

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(pageWithTaskMacros);
    }

    @Test
    @Order(1)
    void taskReportWithParameters(TestUtils setup)
    {
        setup.createPage(pageWithTaskMacros, getMacroContent("tasks.vm"));
        createPagesWithTags(setup);

        DocumentReference testRef = new DocumentReference(pageWithTaskReportWithParameters);
        setup.createPage(testRef, getMacroContent("taskReportMacros.vm"));

        ViewPageWithTasks viewPage = new ViewPageWithTasks();
        TableLayoutElement taskReport = viewPage.getTaskReportLivedataTable(0);
        taskReport.waitUntilReady();
        assertEquals(6, taskReport.countRows());
    }

    @ParameterizedTest
    @WikisSource()
    @Order(3)
    void kanbanMacrosTest(WikiReference wiki, TestUtils setup)
    {
        setup.setCurrentWiki(wiki.getName());
        DocumentReference testRef = new DocumentReference(pageWithKanbanMacros, wiki);
        //setup.createPage(testRef, getMacroContent("kanban.vm"));
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

    private void createPagesWithTags(TestUtils setup)
    {
        final DocumentReference pageWithTags = new DocumentReference("xwiki", "Main", "pageWithTags");

        setup.deletePage(pageWithTags);
        final DocumentReference pageWithTags2 = new DocumentReference("xwiki", "XWiki", "pageWithTags2");
        setup.createPage(pageWithTags, "{{task reference=\"Task_10\"}}tag1{{/task}}");
        setup.gotoPage(pageWithTags);
        TaggablePage taggablePage = new TaggablePage();
        AddTagsPane tagsPane = taggablePage.addTags();
        tagsPane.setTags("alpha, beta, gamma");
        tagsPane.add();

        setup.deletePage(pageWithTags2);
        setup.createPage(pageWithTags2, "{{task reference=\"Task_11\" status=\"Done\"}}tag2{{/task}}");
        setup.gotoPage(pageWithTags2);
        TaggablePage taggablePage2 = new TaggablePage();
        AddTagsPane tagsPane2 = taggablePage2.addTags();
        tagsPane2.setTags("z, x, y");
        tagsPane2.add();
    }
}
