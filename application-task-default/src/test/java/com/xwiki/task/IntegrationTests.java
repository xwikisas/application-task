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

import javax.inject.Provider;

import org.junit.runner.RunWith;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.date.script.DateScriptService;
import com.xwiki.task.internal.TaskReferenceUtils;
import com.xwiki.task.model.Task;
import com.xwiki.task.script.TaskScriptService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        componentManager.registerMockComponent(ConfigurationSource.class, "datemacro");
        componentManager.registerMockComponent(ScriptService.class, "taskmanager");
        componentManager.registerMockComponent(ScriptService.class, "datemacro");
        componentManager.registerMockComponent(TaskScriptService.class);
        componentManager.registerMockComponent(DateScriptService.class);
        ContextualLocalizationManager localizationManager =
            componentManager.registerMockComponent(ContextualLocalizationManager.class);

        Provider<XWikiContext> contextProvider =
            componentManager.registerMockComponent(
                new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        XWikiContext context = componentManager.registerMockComponent(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        ContextualAuthorizationManager authorizationManager =
            componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        TaskManager taskManager = componentManager.registerMockComponent(TaskManager.class);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User1");
        DocumentReference ref1 = new DocumentReference("xwiki", "Sandbox", "Task");
        DocumentReference ref2 = new DocumentReference("xwiki", "Sandbox", "Task2");
        DocumentReference ref3 = new DocumentReference("xwiki", "Sandbox", "Task3");

        TaskReferenceUtils taskReferenceUtils =
            componentManager.registerMockComponent(TaskReferenceUtils.class);
        when(taskReferenceUtils.resolve("Sandbox.Task", null)).thenReturn(ref1);
        when(taskReferenceUtils.resolve("Sandbox.Task2", null)).thenReturn(ref2);
        when(taskReferenceUtils.resolve("Sandbox.Task3", null)).thenReturn(ref3);
        when(taskReferenceUtils.serializeAsDocumentReference(eq(ref1), any())).thenReturn("Sandbox.Task");
        when(taskReferenceUtils.serializeAsDocumentReference(eq(ref2), any())).thenReturn("Sandbox.Task2");
        when(taskReferenceUtils.serializeAsDocumentReference(eq(ref3), any())).thenReturn("Sandbox.Task3");

        Task task = new Task();
        task.setReference(ref1);
        task.setName("Test name");
        task.setDuedate(dateFormat.parse("01/01/2023"));
        task.setNumber(1);
        task.setStatus(Task.STATUS_DONE);
        task.setReporter(user);
        task.setCreateDate(dateFormat.parse("01/01/2023"));
        task.setCompleteDate(dateFormat.parse("01/01/2023"));

        Task task2 = new Task();
        task2.setReference(ref2);
        task2.setName("Test name");
        task2.setDuedate(dateFormat.parse("01/01/2023"));
        task2.setNumber(2);
        task2.setStatus(Task.STATUS_DONE);
        task2.setReporter(user);
        task2.setCreateDate(dateFormat.parse("01/01/2023"));
        task2.setCompleteDate(dateFormat.parse("01/01/2023"));

        Task task3 = new Task();
        task3.setReference(ref3);

        when(taskManager.getTask(3)).thenReturn(task3);
        when(taskManager.getTask(2)).thenReturn(task2);
        when(taskManager.getTask(1)).thenReturn(task);
        when(taskManager.getTask((EntityReference) ref1)).thenReturn(task);
        when(taskManager.getTask((EntityReference) ref2)).thenReturn(task2);
        when(context.getUserReference()).thenReturn(user);
        when(authorizationManager.hasAccess(Right.VIEW, ref1)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, ref2)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, ref3)).thenReturn(false);
        when(localizationManager.getTranslationPlain(any())).thenReturn("Message");

        ConfigurationSource prefs = componentManager.registerMockComponent(ConfigurationSource.class, "wiki");
        when(prefs.getProperty("dateformat", "yyyy/MM/dd HH:mm")).thenReturn("yyyy/MM/dd HH:mm");

        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
        componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "macro");
    }
}
