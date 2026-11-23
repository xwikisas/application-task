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
package org.xwiki.contrib.application.task.test.po;

import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.test.ui.po.BootstrapSwitch;

/**
 * The notification preferences of a user profile, with a {@link #disableAllParameters()} that waits for each change
 * to be saved.
 *
 * @version $Id$
 * @since 3.12.1
 */
public class TaskNotificationsUserProfilePage extends NotificationsUserProfilePage
{
    private static final String ALERT_FORMAT = "alert";

    private static final String EMAIL_FORMAT = "email";

    /**
     * @param username the user profile document name
     * @return the notifications profile tab page object
     */
    public static TaskNotificationsUserProfilePage gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username, "view", "category=notifications");
        return new TaskNotificationsUserProfilePage();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The parent changes the automatic watch mode without waiting for it to be saved. Since every save shows the same
     * "Saved!" message, each following wait for a switch change then returns on the message of the previous save,
     * and the last change can still be pending (or not even sent) when the caller leaves the page, which drops it.
     * Wait for every save instead, the automatic watch mode included.
     */
    @Override
    public void disableAllParameters()
    {
        // Don't select the NONE option again when it's already selected: Firefox saves it again.
        if (getAutoWatchModeValue() != AutowatchMode.NONE) {
            setAutoWatchMode(AutowatchMode.NONE);
        }
        try {
            for (String applicationId : getApplicationPreferences().keySet()) {
                // Each call waits for the save, if the state changes.
                setApplicationState(applicationId, ALERT_FORMAT, BootstrapSwitch.State.OFF);
                setApplicationState(applicationId, EMAIL_FORMAT, BootstrapSwitch.State.OFF);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to disable the notifications of all the applications.", e);
        }
    }
}
