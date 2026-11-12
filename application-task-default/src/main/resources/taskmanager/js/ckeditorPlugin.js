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
(function () {
  'use strict';
  const $ = jQuery;
  // Values computed on the server side, rendered on every page by TaskCKEditorConfigUIExtension.
  const configElement = document.getElementById('taskmanager-ckeditor-config');
  const config = configElement ? JSON.parse(configElement.textContent) : {};
  const taskDateFormat = config.storageDateFormat;
  const translations = config.translations || {};
  const taskMacroName = translations.taskMacroName;
  const taskMacroDescription = translations.taskMacroDescription;
  const checkboxMacroName = translations.checkboxMacroName;
  const checkboxMacroDescription = translations.checkboxMacroDescription;
  const dateMacroName = translations.dateMacroName;
  const dateMacroDescription = translations.dateMacroDescription;
  const macroContentPlaceholder = translations.macroContentPlaceholder;
  const refGenURL = XWiki.currentDocument.getRestURL() + '/task-reference';
  const insertTaskCommand = 'insert-prefilled-task-macro';
  const insertCheckboxCommand = 'insert-prefilled-checkbox-macro';
  // The macros displaying a checkbox followed by editable content, which can be listed and nested from the keyboard,
  // mapped to the command inserting a new one.
  const checkableMacros = {
    task: insertTaskCommand,
    checkbox: insertCheckboxCommand
  };

  function isCheckableWidget(widget, macroName) {
    return !!widget && widget.name === 'xwiki-macro' && checkableMacros.hasOwnProperty(widget.data.name)
      && (!macroName || widget.data.name === macroName);
  }

  // The nested editable holding the macro content (the task and checkbox macros have only one).
  function getContentEditable(widget) {
    return Object.values(widget.editables || {})[0] || null;
  }

  function isEmptyContent(widget) {
    const editable = getContentEditable(widget);
    const text = editable ? editable.getText().replace(/[​ ]/g, ' ').trim() : '';
    return text === '' || text === macroContentPlaceholder;
  }

  // The task or checkbox in whose content the given one is placed directly, if any.
  function getParentWidget(editor, widget) {
    const container = widget.wrapper.getParent();
    return Object.values(editor.widgets.instances).find(function (candidate) {
      const editable = isCheckableWidget(candidate) && getContentEditable(candidate);
      return editable && editable.equals(container);
    }) || null;
  }

  // The previous sibling, if it's a macro of the same kind that can hold the given one in its content.
  function getPreviousSiblingWidget(editor, widget) {
    let previous = widget.wrapper.getPrevious(function (node) {
      // Skip the white space between blocks.
      return node.type === CKEDITOR.NODE_ELEMENT || CKEDITOR.tools.trim(node.getText()) !== '';
    });
    previous = previous && previous.type === CKEDITOR.NODE_ELEMENT ? editor.widgets.getByElement(previous, true)
      : null;
    return isCheckableWidget(previous, widget.data.name) && getContentEditable(previous) ? previous : null;
  }

  // Select the content of the macro (the placeholder) so that the user can type it right away.
  function editMacroContent(editor, widget) {
    const editable = getContentEditable(widget);
    if (!editable) {
      return;
    }
    editable.focus();
    const range = editor.createRange();
    range.selectNodeContents(editable);
    range.shrink(CKEDITOR.SHRINK_TEXT);
    range.select();
  }

  // The macro widget data holds the parameters either as values or as {name, value} objects (preserving the case of the
  // parameter name), depending on the XWiki version.
  function getMacroParameter(widget, name) {
    const parameter = (widget.data.parameters || {})[name];
    return parameter && typeof parameter === 'object' ? parameter.value : parameter;
  }

  // The macro insertion ends with a refresh of the edited content, which re-creates all the widgets and restores the
  // selection saved before the insertion, so the caret is not reliably placed in the inserted macro (at best it is at the
  // start of its content, before the placeholder). Find the inserted macro, using a parameter whose value is unique,
  // and select its content.
  function insertMacroAndEditContent(editor, macroCall, idParameter) {
    const handler = editor.on('afterCommandExec', function (event) {
      if (event.data.name !== 'xwiki-macro-insert') {
        return;
      }
      handler.removeListener();
      const id = macroCall.parameters[idParameter];
      const widget = Object.values(editor.widgets.instances).find(function (candidate) {
        return candidate.name === 'xwiki-macro' && candidate.data.name === macroCall.name
          && getMacroParameter(candidate, idParameter) === id;
      });
      if (widget) {
        editMacroContent(editor, widget);
      }
    });
    editor.execCommand('xwiki-macro-insert', macroCall);
  }

  function placeCaretAfter(editor, widget) {
    const paragraph = editor.document.createElement('p');
    paragraph.appendBogus();
    paragraph.insertAfter(widget.wrapper);
    const range = editor.createRange();
    range.moveToElementEditStart(paragraph);
    range.select();
  }

  // Move the macro in the DOM while keeping the caret where it is, inside the macro content.
  function moveWidget(editor, move) {
    const selection = editor.getSelection();
    const bookmarks = selection.createBookmarks(true);
    move();
    editor.getSelection().selectBookmarks(bookmarks);
  }

  function indent(editor, widget) {
    const previous = getPreviousSiblingWidget(editor, widget);
    if (previous) {
      moveWidget(editor, function () {
        getContentEditable(previous).append(widget.wrapper);
      });
    }
  }

  function outdent(editor, widget) {
    const parent = getParentWidget(editor, widget);
    if (parent) {
      moveWidget(editor, function () {
        widget.wrapper.insertAfter(parent.wrapper);
      });
    }
  }

  function exitOrAdd(editor, widget) {
    if (!isEmptyContent(widget)) {
      // Add a new macro of the same kind after the current one, the same way the quick actions do.
      const insertCommand = checkableMacros[widget.data.name];
      placeCaretAfter(editor, widget);
      if (editor.getCommand(insertCommand)) {
        editor.execCommand(insertCommand);
      }
    } else if (getParentWidget(editor, widget)) {
      outdent(editor, widget);
    } else {
      // Leave the list: replace the empty macro with a new paragraph.
      placeCaretAfter(editor, widget);
      editor.widgets.del(widget);
    }
  }

  const keyHandlers = {
    13: exitOrAdd,
    9: indent
  };
  keyHandlers[CKEDITOR.SHIFT + 9] = outdent;

  function handleKeys(event) {
    const editor = event.editor;
    const handler = keyHandlers[event.data.keyCode];
    // The task or checkbox whose content is being edited. It's not set when the macro widget itself is selected, in
    // which case Enter keeps opening the macro editor.
    const widget = editor.widgets && editor.widgets.widgetHoldingFocusedEditable;
    if (!handler || editor.readOnly || !isCheckableWidget(widget)) {
      return;
    }
    editor.fire('saveSnapshot');
    handler(editor, widget);
    editor.fire('saveSnapshot');
    // Prevent the default behavior, including Tab moving the focus out of the editor when the macro can't be moved.
    event.cancel();
  }

  CKEDITOR.plugins.add('xwiki-task-insert', {
    init: function (editor) {
      // Create, nest and leave tasks and checkboxes from the keyboard while editing their content. Use a high priority so
      // that we handle the keys before the default commands.
      editor.on('key', handleKeys, null, null, 1);
      editor.addCommand(insertCheckboxCommand, {
        exec: function (editor) {
          insertMacroAndEditContent(editor, {
            name: 'checkbox',
            inline: false,
            parameters: {
              // Same format as the id generated by the macro editor (see checkboxInit.js).
              id: String(Math.random()).slice(-5)
            },
            content: macroContentPlaceholder
          }, 'id');
        }
      });
      require(['xwiki-meta', 'moment', 'moment-jdateformatparser'], function (xm, moment) {
        editor.addCommand('insert-prefilled-date-macro', {
          exec: function (editor) {
            var dateFormat = moment().toMomentFormatString(taskDateFormat);
            var currentDate = moment(new Date()).format(dateFormat);
            editor.execCommand('xwiki-macro-insert', {
              name: 'date',
              inline: true,
              parameters: {
                value: currentDate
              }
            });
          }
        });
        editor.addCommand(insertTaskCommand, {
          exec: function (editor) {
            let taskReference = '/Tasks/Task_' + Date.now();
            let creatorValue = xm.userReference.relativeTo(new XWiki.WikiReference(XWiki.currentWiki)).toString();
            var dateFormat = moment().toMomentFormatString(taskDateFormat);
            var createDateParam = moment(new Date()).format(dateFormat);
            $.ajax({
              url: refGenURL,
              method: 'GET',
              dataType: 'text'
            })
              .done(function (ref) {
                taskReference = ref;
              })
              .always(function () {
                insertMacroAndEditContent(editor, {
                  name: 'task',
                  inline: false,
                  parameters: {
                    reference: taskReference,
                    createDate: createDateParam,
                    reporter: creatorValue
                  },
                  content: macroContentPlaceholder
                }, 'reference');
              });
          }
        });
      });
    },
    afterInit: function (editor) {
      if (!editor.quickActions) {
        return;
      }
      editor.quickActions.addAction({
        group: 'Content',
        id: 'macro-task',
        name: taskMacroName,
        iconClass: 'fa fa-align-left',
        description: taskMacroDescription,
        command: {
          name: insertTaskCommand
        }
      });
      editor.quickActions.addAction({
        group: 'Content',
        id: 'macro-checkbox',
        name: checkboxMacroName,
        iconClass: 'fa fa-check-square-o',
        description: checkboxMacroDescription,
        command: {
          name: insertCheckboxCommand
        }
      });
      editor.quickActions.addAction({
        group: 'Content',
        id: 'macro-date',
        name: dateMacroName,
        iconClass: 'fa fa-align-left',
        description: dateMacroDescription,
        command: {
          name: 'insert-prefilled-date-macro'
        }
      });
    }
  });
})();
