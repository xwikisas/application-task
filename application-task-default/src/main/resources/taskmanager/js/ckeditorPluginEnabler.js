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
require(['jquery', 'deferred!ckeditor'], function($, ckeditorPromise) {
  // Values computed on the server side, rendered on every page by TaskCKEditorConfigUIExtension.
  const configElement = document.getElementById('taskmanager-ckeditor-config');
  const config = configElement ? JSON.parse(configElement.textContent) : {};
  const defaultInlineStatus = config.defaultInlineStatus;
  // Map the macros that display a task checkbox to the parameter that holds the checkbox state.
  const checkboxMacros = {
    task: {
      parameter: 'status',
      getValue: function (checked) {
        return checked ? 'Done' : defaultInlineStatus;
      }
    },
    checkbox: {
      parameter: 'checked',
      getValue: function (checked) {
        return String(checked);
      }
    }
  };

  function getCheckboxMacroWidget(editor, checkbox) {
    // The checkbox macro renders a task macro, so the closest macro widget is not always the one we need to update.
    const macroName = checkbox.getAscendant(function (element) {
      return element.hasClass && element.hasClass('taskbox');
    }) ? 'checkbox' : 'task';
    let widget = null;
    checkbox.getAscendant(function (element) {
      // Look only at the widget wrappers.
      widget = element.type === CKEDITOR.NODE_ELEMENT ? editor.widgets.getByElement(element, true) : null;
      return widget && widget.name === 'xwiki-macro' && widget.data.name === macroName;
    });
    return widget;
  }

  // In the WYSIWYG editor the task might not exist yet, and any change saved through the REST API would be overwritten
  // when the edited content is saved, so we update the macro parameters instead.
  function updateCheckboxMacro(editor, checkbox) {
    const widget = getCheckboxMacroWidget(editor, checkbox);
    if (!widget) {
      return;
    }
    const macro = checkboxMacros[widget.data.name];
    const checked = checkbox.$.checked;
    const parameters = $.extend({}, widget.data.parameters);
    Object.keys(parameters).forEach(function (key) {
      if (key.toLowerCase() === macro.parameter) {
        delete parameters[key];
      }
    });
    parameters[macro.parameter] = macro.getValue(checked);

    editor.fire('saveSnapshot');
    // Keep the attribute in sync with the property so that the Undo/Redo history captures the checkbox state.
    if (checked) {
      checkbox.setAttribute('checked', 'checked');
    } else {
      checkbox.removeAttribute('checked');
    }
    widget.setData('parameters', parameters);
    editor.fire('saveSnapshot');
  }

  function listenToTaskCheckboxes(editor) {
    const attachListener = function () {
      const editable = editor.editable();
      editable.attachListener(editable, 'change', function (event) {
        const target = event.data.getTarget();
        if (target.is && target.is('input') && target.hasClass('task-status')) {
          updateCheckboxMacro(editor, target);
        }
      });
    };
    // The listeners attached to the editable are removed each time the edited content is reloaded.
    editor.on('contentDom', attachListener);
    if (editor.status === 'ready' && editor.editable()) {
      attachListener();
    }
  }

  ckeditorPromise.done(function(ckeditor) {
    // The plugin is a JavaScript resource of the application JAR, served through the current document because the
    // current user can view it.
    ckeditor.plugins.addExternal('xwiki-task-insert',
      XWiki.currentDocument.getURL('jsx', 'resource=taskmanager/js/ckeditorPlugin.js'));
    // Handle the task checkboxes in all the editors, independently of the 'xwiki-task-insert' plugin which might not be
    // enabled.
    Object.values(ckeditor.instances).forEach(listenToTaskCheckboxes);
    ckeditor.on('instanceCreated', function (event) {
      listenToTaskCheckboxes(event.editor);
    });
  });
});
