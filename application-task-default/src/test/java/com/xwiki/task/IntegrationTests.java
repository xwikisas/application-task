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

package com.xwiki.task;

import java.text.SimpleDateFormat;

import org.junit.runner.RunWith;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.script.service.ScriptService;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xwiki.task.model.Task;
import com.xwiki.task.script.TaskManagerScriptService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0
 */
@RunWith(RenderingTestSuite.class)
@AllComponents
public class IntegrationTests
{

    @RenderingTestSuite.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerMockComponent(SkinExtension.class, "ssx");
        componentManager.registerMockComponent(SkinExtension.class, "jsx");
        componentManager.registerMockComponent(ConfigurationSource.class, "taskmanager");
        componentManager.registerMockComponent(ScriptService.class, "taskmanager");
        componentManager.registerMockComponent(TaskManagerScriptService.class);
        TaskManager taskManager = componentManager.registerMockComponent(TaskManager.class);
        Task task = new Task();
        task.setReference(new DocumentReference("xwiki", "Sandbox", "Task"));
        task.setName("Test name");
        task.setDuedate(new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2023"));
        task.setNumber(1);
        task.setAssignee(new DocumentReference("xwiki", "XWiki", "User1"));
        task.setStatus("Done");
        when(taskManager.getTaskByReference(any())).thenReturn(task);
        ConfigurationSource prefs = componentManager.registerMockComponent(ConfigurationSource.class, "wiki");
        when(prefs.getProperty("dateformat", "yyyy/MM/dd HH:mm")).thenReturn("yyyy/MM/dd HH:mm");
    }
}
