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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Order;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.ApplicationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.EventTypePreferences;
import org.xwiki.contrib.application.task.test.po.TaskManagerAdminConfigurationPage;
import org.xwiki.contrib.application.task.test.po.TaskManagerHomePage;
import org.xwiki.contrib.application.task.test.po.TaskManagerInlinePage;
import org.xwiki.contrib.application.task.test.po.ViewPageWithTasks;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.CreatePagePage;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.xwiki.task.internal.notifications.TaskChangedEvent;
import com.xwiki.task.internal.notifications.TaskChangedEventDescriptor;
import com.xwiki.task.model.Task;

/**
 * UI Tests for the notifications received when being assigned to a task.
 *
 * @version $Id$
 * @since 3.7
 */
@UITest(properties = {
    // Required for filters preferences
    "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"}, extraJARs = {
    // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
    // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
    "org.xwiki.platform:xwiki-platform-notifications-filters-default:14.10",
    // The Solr store is not ready yet to be installed as an extension, so we need to add it to WEB-INF/lib
    // manually. See https://jira.xwiki.org/browse/XWIKI-21594
    "org.xwiki.platform:xwiki-platform-eventstream-store-solr:14.10"}, resolveExtraJARs = true)
public class NotificationIT
{
    @RegisterExtension
    static GreenMailExtension mail = new GreenMailExtension(ServerSetupTest.SMTP);

    private static String TEST_USERNAME = "NotificationTestUser";

    private static String TEST_EDITOR_USERNAME = "NotificationTestEditor";

    private static String PASSWORD = "password";

    private final DocumentReference TASK_MACRO_PAGE =
        new DocumentReference("xwiki", "Main", "TestTaskMacroNotifications");

    private final String TASK_MACRO_CONTENT =
        "{{task reference=\"Task_0\" createDate=\"2024/12/20 15:13\" status=\"Done\" reporter=\"\"}}My task{{mention reference=\""
            + TEST_USERNAME + "\" anchor=\"sss-QXfgr\" style=\"FULL_NAME\"/}}{{/task}}";

    private String TEST_TASK_NAME = "Test Task";

    private String TEST_PROJECT_NAME = "Test Project";

    @AfterAll
    public void deleteTaskPages(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(new DocumentReference("xwiki", "TaskManager", TEST_TASK_NAME));
        setup.deletePage(TASK_MACRO_PAGE);
        logout(setup);
    }

    public void logout(TestUtils setup)
    {
        setup.setSession(null);
        refresh(setup);
    }

    @BeforeAll
    void setup(TestUtils setup, TestConfiguration config) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.createUser(TEST_USERNAME, PASSWORD, "", "email", "testUser@xwiki.org");
        setup.createUser(TEST_EDITOR_USERNAME, PASSWORD, "", "email", "testEditor@xwiki.org");
        // Configure the SMTP host/port for the wiki so that it points to GreenMail.
        configureEmail(setup, config);

        TaskManagerAdminConfigurationPage taskManagerConfigPage = TaskManagerAdminConfigurationPage.gotoPage();

        taskManagerConfigPage.addNewProject(TEST_PROJECT_NAME);

        NotificationsUserProfilePage userNotificationPreferences = NotificationsUserProfilePage.gotoPage(TEST_USERNAME);
        userNotificationPreferences.disableAllParameters();
        ApplicationPreferences taskApplicationPreferences =
            userNotificationPreferences.getApplication(new TaskChangedEventDescriptor().getApplicationName());
        taskApplicationPreferences.setAlertState(BootstrapSwitch.State.ON);
        taskApplicationPreferences.setEmailState(BootstrapSwitch.State.ON);
    }

    @BeforeEach
    void beforeEach(TestUtils setup)
    {
        doAsUser(setup, TEST_USERNAME, () -> {
            TaskManagerHomePage.gotoPage();
            clearAllNotifications();
        });
    }

    /**
     * Test that the initial assignee notification is received for newly created Task Pages.
     *
     * @since 3.7
     */
    @Test
    @Order(1)
    void taskPageInitialNotification(TestUtils setup)
    {
        // Create a new task.
        doAsUser(setup, TEST_EDITOR_USERNAME, () -> {
            TaskManagerHomePage taskManagerHomePage = TaskManagerHomePage.gotoPage();
            CreatePagePage createPage = taskManagerHomePage.createPage();

            createPage.getDocumentPicker().setTitle(TEST_TASK_NAME);
            createPage.setTemplate("TaskManager.TaskManagerTemplateProvider");
            createPage.clickCreate();

            TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
            inlinePage.setAssignee(TEST_USERNAME);
            inlinePage.setDueDate("01/01/2001 01:01:01");
            inlinePage.setStatus(Task.STATUS_IN_PROGRESS);
            inlinePage.setProject(TEST_PROJECT_NAME);
            inlinePage.setSeverity("Low");
            inlinePage.clickSaveAndView();
        });

        doAsUser(setup, TEST_USERNAME, () -> {

            TaskManagerHomePage.gotoPage();

            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + TEST_USERNAME, "xwiki", 1);

            // Expected: Only one notification is sent, for the assignee.
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            Assertions.assertEquals(1, tray.getNotificationsCount());
            Assertions.assertEquals(TaskChangedEvent.class.getName(), tray.getNotificationType(0));
            // List<String> groupedEventDetails = getNotificationDetails(setup, 0);
            // expectEventDetailsText(groupedEventDetails, "Assignee changed from Unknown User to NotificationTestUser");
            // System.out.println(TaskChangedEvent.class.getName()); // "com.xwiki.task.internal.notifications.TaskChangedEvent"
            // "notification-event-details"

            // Verify that the mail has been received.
            mail.waitForIncomingEmail(30000L, 1);
            System.out.println("Mails: " + mail.getReceivedMessages());
            Assertions.assertEquals(1, mail.getReceivedMessages().length);
        });
    }

    /**
     * Test that the notifications for all fields are received for Task Pages.
     *
     * @since 3.7
     */
    @Test
    @Order(2)
    void taskPageNotifications(TestUtils setup)
    {
        doAsUser(setup, TEST_EDITOR_USERNAME, () -> {
            setup.gotoPage("TaskManager", TEST_TASK_NAME, "edit");
            TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
            inlinePage.setDueDate("02/02/2002 02:02:02");
            inlinePage.setStatus(Task.STATUS_DONE);
            inlinePage.setProject(TEST_PROJECT_NAME);
            inlinePage.setSeverity("High");
            inlinePage.setProgress("25%");
            inlinePage.clickSaveAndView();
        });

        doAsUser(setup, TEST_USERNAME, () -> {
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + TEST_USERNAME, "xwiki", 4);
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            Assertions.assertEquals(4, tray.getNotificationsCount());
            Assertions.assertEquals(TaskChangedEvent.class.getName(), tray.getNotificationType(0));

            // List<String> groupedEventDetails = getNotificationDetails(setup, 0);
            // System.out.println("looool" + groupedEventDetails);
            // expectEventDetailsText(groupedEventDetails, "Priority changed from Medium to High");
            // expectEventDetailsText(groupedEventDetails, "Status changed from ToDo to Done");
            // expectEventDetailsText(groupedEventDetails, "Deadline set to 30/03/1970 14:12:01");
        });
    }

    /**
     * Test that the initial assignee notification is received for newly created Task Macros.
     *
     * @since 3.7
     */
    @Test
    @Order(3)
    void taskMacroInitial(TestUtils setup)
    {
        doAsUser(setup, TEST_EDITOR_USERNAME, () -> {
            setup.createPage(TASK_MACRO_PAGE, TASK_MACRO_CONTENT, TASK_MACRO_PAGE.getName());
        });

        doAsUser(setup, TEST_USERNAME, () -> {
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + TEST_USERNAME, "xwiki", 1);
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            Assertions.assertEquals(1, tray.getNotificationsCount());
            Assertions.assertEquals(TaskChangedEvent.class.getName(), tray.getNotificationType(0));
        });
    }

    /**
     * Test that the notifications for the status update when ticking a Task Macro checkbox.
     *
     * @since 3.7
     */
    @Test
    @Order(4)
    void tasMacroCheckboxNotification(TestUtils setup)
    {
        doAsUser(setup, TEST_EDITOR_USERNAME, () -> {
            setup.gotoPage(TASK_MACRO_PAGE);
            ViewPageWithTasks viewPageWithTaskMacro = new ViewPageWithTasks();
            viewPageWithTaskMacro.clickTaskMacroCheckbox(0);
        });

        doAsUser(setup, TEST_USERNAME, () -> {
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + TEST_USERNAME, "xwiki", 1);
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            Assertions.assertEquals(1, tray.getNotificationsCount());
            Assertions.assertEquals(TaskChangedEvent.class.getName(), tray.getNotificationType(0));
        });
    }

    /**
     * Test that no notifications are received when the filterPreference is off.
     *
     * @since 3.7
     */
    @Test
    @Order(5)
    void respectTheNotificationPreference(TestUtils setup) throws Exception
    {
        doAsUser(setup, TEST_USERNAME, () -> {
            try {
                NotificationsUserProfilePage userNotificationPreferences =
                    NotificationsUserProfilePage.gotoPage(TEST_USERNAME);

                ApplicationPreferences taskApplicationPreferences =
                    userNotificationPreferences.getApplication(new TaskChangedEventDescriptor().getApplicationName());
                taskApplicationPreferences.setAlertState(BootstrapSwitch.State.OFF);
                taskApplicationPreferences.setEmailState(BootstrapSwitch.State.OFF);
                setup.gotoPage("TaskManager", TEST_TASK_NAME);
                clearAllNotifications();
                refresh(setup);
            } catch (Exception e) {
                Assertions.fail("Exception while setting application notification preferences: " + e);
            }
        });

        doAsUser(setup, TEST_EDITOR_USERNAME, () -> {
            setup.gotoPage("TaskManager", TEST_TASK_NAME, "edit");
            TaskManagerInlinePage inlinePage = new TaskManagerInlinePage();
            inlinePage = new TaskManagerInlinePage();
            inlinePage.setDueDate("01/01/2001 01:01:01");
            inlinePage.setStatus(Task.STATUS_DONE);
            inlinePage.setSeverity("High");
            inlinePage.clickSaveAndView();
        });

        doAsUser(setup, TEST_USERNAME, () -> {
            Assertions.assertThrows(TimeoutException.class,
                () -> NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + TEST_USERNAME, "xwiki", 1));

            // See that the preferences remain the same.
            NotificationsUserProfilePage userNotificationPreferences =
                NotificationsUserProfilePage.gotoPage(TEST_USERNAME);
            userNotificationPreferences.getApplicationPreferences()
                .forEach((String appName, ApplicationPreferences pref) -> {
                    pref.getEventTypePreferences().forEach((String evTypeName, EventTypePreferences evTypePref) -> {
                        assertEquals(BootstrapSwitch.State.OFF, evTypePref.getAlertState());
                        assertEquals(BootstrapSwitch.State.OFF, evTypePref.getEmailState());
                    });
                });
        });
    }

    private void clearAllNotifications()
    {
        new NotificationsTrayPage().clearAllNotifications();
    }

    private void refresh(TestUtils setup)
    {
        setup.getDriver().navigate().refresh();
    }

    private void configureEmail(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host",
            testConfiguration.getServletEngine().getHostIP(), "port", "3025", "sendWaitTime", "0");
    }

    private List<String> getNotificationDetails(TestUtils setup, int notificationNumber)
    {
        new NotificationsTrayPage().showNotificationTray();

        WebElement detailsDiv =
            setup.getDriver().findElementsWithoutWaiting(By.cssSelector("li#tmNotifications div.notification-event"))
                .get(notificationNumber).findElement(By.cssSelector(".notification-event-details"));
        return detailsDiv.findElements(By.cssSelector("tr")).stream().map((elem) -> {
            return elem.findElement(By.cssSelector(".description")).getAttribute("textContent");
        }).collect(Collectors.toList());
    }

    private void expectEventDetailsText(List<String> groupedEventDetails, String text)
    {
        Assertions.assertTrue(groupedEventDetails.contains(text),
            "Expected message not found in " + groupedEventDetails);
    }

    private void doAsUser(TestUtils setup, String username, Runnable action)
    {
        TaskManagerHomePage.gotoPage();
        logout(setup);
        setup.login(username, PASSWORD);
        TaskManagerHomePage.gotoPage();
        action.run();
        logout(setup);
    }
}
