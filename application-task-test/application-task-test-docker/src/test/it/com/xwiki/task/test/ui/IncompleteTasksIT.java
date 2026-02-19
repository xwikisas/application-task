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

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ImportAdministrationSectionPage;
import org.xwiki.contrib.application.task.test.po.TaskAdminPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest(
    properties = {
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.packaging.PackagePlugin" },
    // Needed for the dependency to the mentions macro that uses solr.
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr:14.10",
        "com.xwiki.date:macro-date-api",
        "com.xwiki.date:macro-date-default"
    }, resolveExtraJARs = true)
public class IncompleteTasksIT
{
    private static final String SPACE = "SpaceWithIncompleteTasks";

    private static final LocalDocumentReference CONTENT_CHANGED_TO_INCOMPLETE_TASKS =
        new LocalDocumentReference(Arrays.asList(SPACE, "ContentChangedToIncompleteTasks"), "WebHome");

    private static final String XAR_INCOMPLETE_TASKS = "IncompleteTasks.xar";

    private AdministrationPage adminPage;

    private ImportAdministrationSectionPage sectionPage;

    @BeforeAll
    void setup(TestUtils setup, TestConfiguration testConfiguration)
    {

        setup.loginAsSuperAdmin();
        adminPage = AdministrationPage.gotoPage();
        sectionPage = adminPage.clickImportSection();

        if (sectionPage.isPackagePresent(XAR_INCOMPLETE_TASKS)) {
            sectionPage.deletePackage(XAR_INCOMPLETE_TASKS);
        }
        File file = getFileToUpload(testConfiguration, XAR_INCOMPLETE_TASKS);
        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(XAR_INCOMPLETE_TASKS);

        this.sectionPage.selectReplaceHistoryOption();
        this.sectionPage.clickImportAsBackup();
        this.sectionPage.importPackage();

    }

    @Test
    public void testContentChangedToIncompleteTasks(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        TaskAdminPage taskAdminPage = TaskAdminPage.gotoPage();
        assertTrue(taskAdminPage.countIncompleteTasks() > 0);
        taskAdminPage.inferMissingDataForAllTasks();
        taskAdminPage.refreshIncompleteTasks();
        assertEquals(0, taskAdminPage.countIncompleteTasks());
    }

    private File getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), "XARImport/" + filename);
    }
}
