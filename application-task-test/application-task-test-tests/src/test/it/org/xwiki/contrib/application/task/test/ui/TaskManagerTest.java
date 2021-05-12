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
package org.xwiki.contrib.application.task.test.ui;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

/**
 * UI tests for the Task Manager application.
 *
 * @version $Id: 6859af29866c6e4e0b50cd75df551f54afb6df53 $
 * @since 4.3M2
 */
public class TaskManagerTest extends AbstractTest
{
    // Login as superadmin to have delete rights.
    @Rule
    public SuperAdminAuthenticationRule superAdmin = new SuperAdminAuthenticationRule(getUtil());

    // Note: we use a dot in the page name to verify it's supported by the FAQ application and we use an accent to
    // verify encoding.
    private static final String TASK_TEST_SPACE = "TaskManager";

    private static final String TASK_TEST_NAME = "MyTestTask";

    @Test
    public void testTaskManager()
    {
        // Delete pages that we create in the test
        getUtil().deletePage(TASK_TEST_SPACE, TASK_TEST_NAME);

        // Navigate to the FAQ app by clicking in the Application Panel.
        // This verifies that the FAQ application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Task Manager");

        // Verify we're on the right page!
        Assert.assertEquals(TaskManagerHomePage.getSpace(), vp.getMetaDataValue("space"));
        Assert.assertEquals(TaskManagerHomePage.getPage(), vp.getMetaDataValue("page"));

        // The code below is commented out because the test won't work since the application is not an Application Within Minute anymore
        /*TaskManagerHomePage homePage = new TaskManagerHomePage();
        homePage.clickAddNewEntry();
        homePage.setEntryName(TASK_TEST_NAME);

        TaskManagerInlinePage entryInlinePage = homePage.clickAddEntry();
        Assert.assertEquals("XWiki.superadmin", entryInlinePage.getReporter());
        
        entryInlinePage.setName("My super task");
        entryInlinePage.setSeverity("High");
        entryInlinePage.setAssignee("XWiki.superadmin");
        entryInlinePage.setStatus("Done");
        entryInlinePage.setProgress("33");
        entryInlinePage.clickSaveAndView();
        
        TaskManagerViewPage entryViewPage = TaskManagerViewPage.gotoPage("TaskManager", "MyTestTask");

        Assert.assertEquals("My super task", entryViewPage.getName());
        Assert.assertEquals("High", entryViewPage.getSeverity());
        Assert.assertEquals("superadmin", entryViewPage.getAssignee());
        // TODO: there is bug with the save of Status
        // Assert.assertEquals("Done", entryViewPage.getStatus());
        Assert.assertEquals("33%", entryViewPage.getProgress());
         */
    }
}
