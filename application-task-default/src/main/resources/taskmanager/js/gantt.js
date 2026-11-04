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
define('taskgantt-translation-keys', {
  prefix: 'taskmanager.gantt.',
  keys: [
    'taskEdit.error.readonly',
    'taskEdit.error.permissions',
    'taskEdit.success',
    'task.noDueDate',
  ],
});

define('taskgantt-localization', ['xwiki-l10n!taskgantt-translation-keys', 'xwiki-meta'], function (l10n, xm) {
  return {
    readonlyError: l10n['taskEdit.error.readonly'],
    permissionError: l10n['taskEdit.error.permissions'],
    successMessage: l10n['taskEdit.success'],
    noDueDateMessage: l10n['task.noDueDate'],
    XWikiLocale: xm.locale,
  };
});

define('taskgantt-config-templates', ['jquery'], function ($) {
  let config;
  try {
    config = JSON.parse($('#taskgantt-config-templates').text());
  } catch (e) {
    // While WYSIWYG is loading, no `#taskgantt-config-templates` element is found, so the parsing fails and
    // locks the entire editor.
    console.warn('Error while parsing taskgantt templates. Continuing with empty templates...');
    config = {'svgCustomDefs': '', 'noTasksMessage': ''};
  }
  return config;
});

define('taskgantt-service-urls', ['jquery'], function ($) {
  const jsonSourceBaseURL =
    new XWiki.Document(
      XWiki.Model.resolve('TaskManager.GanttMacro.JSONTaskSource', XWiki.EntityType.DOCUMENT)
    ).getURL('get')
    + '?xpage=plain&outputSyntax=plain&';
  const updateServiceBaseURL =
    new XWiki.Document(
      XWiki.Model.resolve('TaskManager.GanttMacro.TaskUpdateService', XWiki.EntityType.DOCUMENT)
    ).getURL('get')
    + '?xpage=plain&outputSyntax=plain&';

  function getUpdateServiceURL(task) {
    /**
     * Could be refactored and replaced by a REST API call after the following JIRA issue is fixed:
     * XWIKI-20704: NullPointerException (NPE) when accessing objects with ComputedField properties from REST
     */
    return updateServiceBaseURL + new URLSearchParams({ taskDocument: task.id }) + '&';
    // REST API call skeleton.
    // return new XWiki.Document(
    //   XWiki.Model.resolve(task.id, XWiki.EntityType.DOCUMENT)
    // ).getRestURL('objects/TaskManager.TaskManagerClass/0');
  };
  function getJsonSourceURL(ganttDiv) {
    const params = {};
    ganttDiv = $(ganttDiv);
    if (undefined !== ganttDiv.data('assignees')) {
      params['assignees'] = ganttDiv.data('assignees');
    }
    if (undefined !== ganttDiv.data('reporters')) {
      params['reporters'] = ganttDiv.data('reporters');
    }
    if (undefined !== ganttDiv.data('projects')) {
      params['projects'] = ganttDiv.data('projects');
    }
    if (undefined !== ganttDiv.data('spaces')) {
      params['spaces'] = ganttDiv.data('spaces');
    }
    if (undefined !== ganttDiv.data('date-to')) {
      params['to'] = ganttDiv.data('date-to');
    }
    if (undefined !== ganttDiv.data('date-from')) {
      params['from'] = ganttDiv.data('date-from');
    }
    if (undefined !== ganttDiv.data('hide-no-due-date')) {
      params['hideNoDueDate'] = ganttDiv.data('hide-no-due-date');
    }
    if (undefined !== ganttDiv.data('max-tasks')) {
      params['maxTasks'] = ganttDiv.data('max-tasks');
    }
    const query = $.param(params);
    return jsonSourceBaseURL + query;
  };

  return {
    'getUpdateServiceURL': getUpdateServiceURL,
    'getJsonSourceURL': getJsonSourceURL,
  };
});

define('taskgantt-utils', ['jquery', 'taskgantt-localization'], function ($, l10n) {
  /**
   * Escape text with HTML escapes (needed because js escape() is deprecated).
   */
  function escapeHTML(originalString) {
    return String(originalString)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };
  /**
   *  Used to get absolute position information of an element relative to another element (viewport).
   */
  function normalizePosition(viewport, elem) {
    return {
      x: (elem.x - viewport.x),
      y: (elem.y - viewport.y),
      width: elem.width,
      height: elem.height,
    };
  };
  /**
   * Converts dates from int to js Date.
   * Updates task title when no due date.
   * Updates the last state of a task to use when reverting in case of an error.
   */
  function formatTask(task) {
    if (task.taskClasses.includes('taskgantt-no-due-date')) {
      task.name = escapeHTML(task.taskName) + ' - ' + l10n.noDueDateMessage;
    } else {
      task.name = escapeHTML(task.taskName);
    }
    task.custom_class = escapeHTML(task.taskClasses.join(' '));
    if (!(task.start instanceof Date)) {
      task.start = new Date(task.start);
    }
    if (!(task.end instanceof Date)) {
      task.end = new Date(task.end);
    }
    /**
     * The `editMetadata` takes a snapshot of a task and it is used to detect what properties were updated when editing.
     * It is also used to restore a task to its previous state in the UI after an edit error (permissions, readonly).
     */
    const hasStarted = task.editMetadata && task._start;
    task.editMetadata = {
      oldProgress: task.progress,
      oldStartDate: hasStarted ? task._start : new Date(task.start.getTime()),
      oldEndDate: hasStarted ? task._end : new Date(task.end.getTime()),
    };
  };
  /**
   * Returns an error message if editing this task is not allowed.
   */
  function getForbiddenEditMsg(task, gantt) {
    let msg = null;
    const isReadonly = $(gantt.$svg).closest('.taskgantt').data('readonly');
    if (isReadonly) {
      msg = l10n.readonlyError;
    } else if (task.taskClasses.includes('taskgantt-no-edit')) {
      msg = l10n.permissionError;
    }
    return msg;
  };

  return {
    'escapeHTML': escapeHTML,
    'normalizePosition': normalizePosition,
    'formatTask': formatTask,
    'getForbiddenEditMsg': getForbiddenEditMsg,
  };
});

define('taskgantt-progress-popup', [
  'jquery',
  'taskgantt-localization',
  'taskgantt-utils'
], function ($, l10n, taskutil) {
  function showPopup(popup, ganttDiv, ganttObj, barElem, progressChangeObserver, isResizingProgress) {
    if (isResizingProgress) {
      // The popup is already visible, no need to recompute the location.
      return;
    }
    const viewport = ganttDiv.getBoundingClientRect();
    let bar = ganttObj.bars.find((b) => b.group === barElem);
    const b = taskutil.normalizePosition(viewport, bar.$bar.getBoundingClientRect());
    const pop = taskutil.normalizePosition(
      viewport,
      popup.getElementsByClassName('popuptext')[0].getBoundingClientRect()
    );
    let y = b.y - pop.height - 5;
    let x = b.x + (b.width - pop.width) / 2;
    popup.style.top = y + 'px';
    popup.style.left = x + 'px';
    let popupNumber = $('.gantt-popup-progress-number', popup);
    popupNumber.text(bar.compute_progress() + '%');
    $(popup).addClass('active');
    if (progressChangeObserver) {
      progressChangeObserver.disconnect();
    }
    progressChangeObserver = new MutationObserver(() => { popupNumber.text(bar.compute_progress() + '%'); });
    progressChangeObserver.observe(bar.$bar_progress, { attributes: true });
  };
  function hidePopup(popup, progressChangeObserver, isResizingProgress) {
    if (isResizingProgress) {
      return;
    }
    $(popup).removeClass('active');
    if (progressChangeObserver) {
      progressChangeObserver.disconnect();
    }
  };
  /**
   * Set up a custom popup to display the progress for the specified gantt object.
   */
  function setupProgressPopup(ganttObj) {
    let ganttDiv = $(ganttObj.$svg).closest('.taskgantt')[0];
    let popup = $('div.gantt-popup', ganttDiv)[0];
    // Set up popup event listeners.
    let progressChangeObserver;
    let hoverTimer;
    let isResizingProgress = false;

    $('.handle.progress', ganttDiv).on({
      mousedown: function (e) {
        let bar = $(e.delegateTarget).closest('.bar-wrapper')[0];
        showPopup(popup, ganttDiv, ganttObj, bar, progressChangeObserver, isResizingProgress);
        isResizingProgress = true;
      },
    });
    $(ganttDiv).on({
      mouseup: function (e) {
        isResizingProgress = false;
        hidePopup(popup, progressChangeObserver, isResizingProgress);
      },
    });

    $('.bar-wrapper', ganttDiv).on({
      mouseenter: function (e) {
        let bar = e.currentTarget;
        if (!hoverTimer) {
          hoverTimer = setTimeout(() => {
            showPopup(popup, ganttDiv, ganttObj, bar, progressChangeObserver, isResizingProgress);
          }, 1000);
        }
      },
      mouseleave: function (e) {
        clearTimeout(hoverTimer);
        hoverTimer = null;
        hidePopup(popup, progressChangeObserver, isResizingProgress);
      },
      mousedown: function (e) {
        clearTimeout(hoverTimer);
        hoverTimer = null;
        hidePopup(popup, progressChangeObserver, isResizingProgress);
      },
    });
  };

  return {
    'setupProgressPopup': setupProgressPopup,
  };
});
define('taskgantt-ui', [
  'jquery',
  'taskgantt-utils',
  'taskgantt-localization',
  'taskgantt-progress-popup',
  'taskgantt-config-templates'
], function ($, taskutil, l10n, taskpopup, tgtemplates) {
  /**
   * Add custom svg styles as part of a <defs> tag.
   * This is done to fade out tasks with no due date and turn their text background color red.
   */
  function addSvgStyles(svgElem) {
    var defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
    defs.innerHTML += tgtemplates.svgCustomDefs;
    svgElem.setAttribute('height', svgElem.getAttribute('height') - 100);
    svgElem.appendChild(defs);
  };
  function setGanttDisabledOverlayVisibility(gantt, visible) {
    let overlayElem = $('.taskgantt-noedit-overlay', gantt.$svg.closest('.taskgantt'));
    if (visible) {
      overlayElem.addClass('active');
    } else {
      overlayElem.removeClass('active');
    }
  };
  /**
   * Revert a task bar to its previous position (before modification).
   */
  function revertBar(task, gantt) {
    // Revert to saved state before starting to edit.
    task._start.setTime(task.editMetadata.oldStartDate.getTime());
    task._end.setTime(task.editMetadata.oldEndDate.getTime());
    task.progress = task.editMetadata.oldProgress;

    let bar = gantt.bars.find((bar) => bar.task === task);
    bar.update_bar_position({ x: bar.$bar.ox, width: bar.$bar.owidth });

    // Manually reset progress width change, double click event doesn't trigger otherwise.
    bar.$bar_progress.finaldx = 0;
    bar.update_handle_position();

    bar.group.classList = Array.from(bar.group.classList).filter((clas) => clas != 'active').join(' ');
  };
  /**
   * Render a task bar at its current position, updating text and css classes if necessary.
   */
  function redrawBar(task, gantt) {
    task.taskClasses = task.taskClasses.filter((t) => t != 'taskgantt-no-due-date');
    // Rename if taskgantt-no-due-date was removed, save new state for use in revertBar.
    taskutil.formatTask(task);

    let bar = gantt.bars.find((b) => b.task === task);
    bar.update_bar_position({ x: bar.compute_x() });
    bar.group.setAttribute('class', 'bar-wrapper ' + (task.custom_class || ''));
    bar.bar_group.getElementsByClassName('bar-label')[0].innerHTML = task.name;

    // Manually reset progress width change, double click event doesn't trigger otherwise.
    bar.$bar_progress.finaldx = 0;
    bar.update_handle_position();
  };
  /**
   * Update the task position and show an UI notification with the right formatting and text.
   */
  function getUpdateServiceSuccessCallback(gantt, task) {
    return function(data) {
      try {
        redrawBar(task, gantt);
        new XWiki.widgets.Notification(l10n.successMessage, 'info', '20');
      } catch (e) {
        console.log(e);
      }
      setGanttDisabledOverlayVisibility(gantt, false);
    };
  };
  /**
   * Revert the task position and show an UI notification with the right formatting and text.
   */
  function getUpdateServiceErrorCallback(gantt, task) {
    return function(error) {
      revertBar(task, gantt);
      console.log(error);
      new XWiki.widgets.Notification(taskutil.escapeHTML(error.responseJSON.data), 'error', '20');
      setGanttDisabledOverlayVisibility(gantt, false);
    };
  };

  return {
    'setupProgressPopup': taskpopup.setupProgressPopup,
    'addSvgStyles': addSvgStyles,
    'setGanttDisabledOverlayVisibility': setGanttDisabledOverlayVisibility,
    'redrawBar': redrawBar,
    'revertBar': revertBar,
    'getUpdateServiceSuccessCallback': getUpdateServiceSuccessCallback,
    'getUpdateServiceErrorCallback': getUpdateServiceErrorCallback,
  };
});

/**
 * This module modifies/patches the default library implementations for some functions.
 * It also includes event handlers that fire when interacting with a task.
 */
define('taskgantt-frappe-methods', [
  'jquery',
  'taskgantt-utils',
  'taskgantt-ui',
  'taskgantt-service-urls',
  'xwiki-meta',
], function ($, taskutil, taskui, taskurls, xm) {
  /**
   * Custom implementation of trigger_event from the frappe-gantt(v0.6.1) library.
   * Changed: Bind the parent gantt object to 'this' in the event handlers.
   */
  function custom_trigger_event(event, args) {
    if (this.options['on_' + event]) {
      this.options['on_' + event].apply(this, args); // Changed: Was `apply(null, args)`.
    }
  };
  /**
   * Custom implementation of render from the frappe-gantt(v0.6.1) library.
   * Uses the original render function from the frappe-gantt library.
   * Changed: Adds a custom popup and the custom svg markup for task bar fade on no due date.
   */
  function custom_render() {
    const result = this._render(); // Use the default render implementation first.
    taskui.addSvgStyles(this.$svg); // Changed: SVG specific style definitions, with no css equivalent.
    taskui.setupProgressPopup(this); // Changed: Custom popup for progress, doesn't use frappe-gantt popup.
    return result;
  };
  /**
   * Custom implementation of get_date_info from the frappe-gantt(v0.6.1) library.
   * Uses the original get_date_info function from the frappe-gantt library.
   *
   * Fixes the header text display in multiple view modes.
   * In the default frappe-gantt implementation, the header text is offset by 12h.
   * Example: If a task starts at 06:00, it appears under the 18:00 section in the UI.
   */
  function custom_get_date_info(date, last_date, i) {
    // Use default implementation first because only it has access to
    // the `date_utils` object, let it format the text in the header as it wishes.
    let result = this._get_date_info(date, last_date, i);
    // Below copied from original function.
    const base_pos = {
      x: i * this.options.column_width,
      lower_y: this.options.header_height,
      upper_y: this.options.header_height - 25,
    };

    const x_pos = {
      'Quarter Day_lower': (this.options.column_width * 4),  // Changed: Alignment (waswidth * 4 / 2).
      'Quarter Day_upper': (this.options.column_width * 4) / 2, // Changed: Alignment (was 0). Now date text is centered above hours of a day.
      'Half Day_lower': (this.options.column_width * 2), // Changed: Alignment (was width * 2 / 2).
      'Half Day_upper': (this.options.column_width * 2) / 2, // Changed: Alignment (was (width * 30)/2). Now date text is above 12, not 00.
      Day_lower: this.options.column_width / 2,
      Day_upper: (this.options.column_width * 30) / 2,
      Week_lower: 0,
      Week_upper: (this.options.column_width * 4) / 2,
      Month_lower: this.options.column_width * 0.5, // Changed: Alignment (was width * 2). Now month names are actually above their corresponding column.
      Month_upper: (this.options.column_width * 12) / 2,
      Year_lower: this.options.column_width / 2,
      Year_upper: (this.options.column_width * 30) / 2,
    };
    // Changed: Full result has 2 other fields, leave them be. For the others use the objects above.
    const tweakedPos = {
      upper_x: base_pos.x + x_pos[`${this.options.view_mode}_upper`],
      upper_y: base_pos.upper_y,
      lower_x: base_pos.x + x_pos[`${this.options.view_mode}_lower`],
      lower_y: base_pos.lower_y,
    };
    // Changed: Use the values redefined above to alter the result.
    Object.assign(result, tweakedPos);
    return result;
  };
  /**
   * This is copied from the frappe-gantt library.
   * Redefinition of on() from jQuery so delegate event handlers work on SVG elements.
   */
  function on(element, event, selector, callback) {
    if (!callback) {
      callback = selector;
      event.split(/\s+/).forEach(function (event) {
        element.addEventListener(event, callback);
      });
    } else {
      element.addEventListener(event, function (e) {
        const delegatedTarget = e.target.closest(selector);
        if (delegatedTarget) {
          e.delegatedTarget = delegatedTarget;
          callback.call(this, e, delegatedTarget);
        }
      });
    }
  };
  /**
   * Custom implementation of bind_bar_events from the frappe-gantt(v0.6.1) library.
   * Don't move dependencies when moving a task.
   * This prevents unintentional updates and more freedom when moving tasks.
   */
  function custom_bind_bar_events() {
    let is_dragging = false;
    let x_on_start = 0;
    let y_on_start = 0;
    let is_resizing_left = false;
    let is_resizing_right = false;
    let parent_bar_id = null;
    let bars = []; // instanceof Bar
    //this.bar_being_dragged = null; // Changed: Not needed since the frappe-gantt popup is not used.

    function action_in_progress() {
      return is_dragging || is_resizing_left || is_resizing_right;
    }

    on(this.$svg, 'mousedown', '.bar-wrapper, .handle', (e, element) => {
      const bar_wrapper = $(element).closest('.bar-wrapper')[0]; // Changed: Uses jQuery instead of custom frappe-gantt function.

      if (element.classList.contains('left')) {
        is_resizing_left = true;
      } else if (element.classList.contains('right')) {
        is_resizing_right = true;
      } else if (element.classList.contains('bar-wrapper')) {
        is_dragging = true;
      }

      bar_wrapper.classList.add('active');

      x_on_start = e.offsetX;
      y_on_start = e.offsetY;

      parent_bar_id = bar_wrapper.getAttribute('data-id');
      bars = [this.get_bar(parent_bar_id)]; // Changed: Don't move all dependencies, just this task.

      // this.bar_being_dragged = parent_bar_id; // Changed: Not needed (see above).

      bars.forEach((bar) => {
        const $bar = bar.$bar;
        $bar.ox = $bar.getX();
        $bar.oy = $bar.getY();
        $bar.owidth = $bar.getWidth();
        $bar.finaldx = 0;
      });
    });

    $(this.$svg).on('mousemove', (e) => {
      if (!action_in_progress()) return;
      const dx = e.offsetX - x_on_start;
      e.offsetY - y_on_start;

      bars.forEach((bar) => {
        const $bar = bar.$bar;
        $bar.finaldx = this.get_snap_position(dx);
        this.hide_popup();
        if (is_resizing_left) {
          if (parent_bar_id === bar.task.id) {
            bar.update_bar_position({
              x: $bar.ox + $bar.finaldx,
              width: $bar.owidth - $bar.finaldx,
            });
          } else {
            bar.update_bar_position({
              x: $bar.ox + $bar.finaldx,
            });
          }
        } else if (is_resizing_right) {
          if (parent_bar_id === bar.task.id) {
            bar.update_bar_position({
              width: $bar.owidth + $bar.finaldx,
            });
          }
        } else if (is_dragging) {
          bar.update_bar_position({ x: $bar.ox + $bar.finaldx });
        }
      });
    });

    $(document).on('mouseup', (e) => { // Changed: Use jQuery.
      if (is_dragging || is_resizing_left || is_resizing_right) {
        bars.forEach((bar) => {
          bar.group.classList.remove('active');
          // Changed: Merged document and svg handlers into one, so releasing the mouse button outside svg still registers a task update.
          //this.bar_being_dragged = null; // Changed: Not needed (see above).
          const $bar = bar.$bar;
          if (!$bar.finaldx) {
            return;
          }
          bar.date_changed();
          $bar.finaldx = 0; // Changed: Reset finaldx value so double click handler works.
          bar.set_action_completed();
        });
      }

      is_dragging = false;
      is_resizing_left = false;
      is_resizing_right = false;
    });

    this.bind_bar_progress();
  };
  /**
   * Custom implementation of update_bar_position from the frappe-gantt(v0.6.1) library.
   * Don't enforce 'task must start after dependencies' rule, for more freedom when moving tasks.
   */
  function custom_update_bar_position({ x = null, width = null }) {
    const bar = this.$bar;
    if (x) {
      // Changed: Removed early return statement which enforced the 'task must start after dependencies' rule.
      this.update_attr(bar, 'x', x);
    }
    if (width && width > 0) { // Changed: width > 0 so tasks can get smaller than a column (so you can drag a task to make it last 5 minutes long for example).
      this.update_attr(bar, 'width', width);
    }
    this.update_label_position();
    this.update_handle_position();
    this.update_progressbar_position();
    this.update_arrow_position();
  };
  /**
   * Custom implementation of compute_x from the frappe-gantt(v0.6.1) library.
   * Removed rounding to nearest hour when displaying tasks (so task positions are not snapped to fixed hours).
   */
  function custom_compute_x() {
    const { step, column_width } = this.gantt.options;
    const task_start = this.task._start;
    const gantt_start = this.gantt.gantt_start;

    const diff = (task_start - gantt_start) / 1000 / 60 / 60; // Changed: FLOAT difference in Hours.
    let x = (diff / step) * column_width;

    if (this.gantt.view_is('Month')) {
      const diff = (task_start - gantt_start) / 1000 / 60 / 60 / 24; // Changed: FLOAT difference in Days.
      x = (diff * column_width) / 30;
    }
    return x;
  };
  /**
   * Custom implementation of prepare_values from the frappe-gantt(v0.6.1) library.
   * Uses the original prepare_values function from the frappe-gantt library.
   * Removed rounding to nearest hour when displaying tasks (so tasks with less than 1 hour duration are displayed).
   */
  function custom_prepare_values() {
    taskutil.formatTask(this.task);
    this._prepare_values();
    this.duration =
      ((this.task._end - this.task._start) / 1000 / 60 / 60) / // Changed: FLOAT difference in Hours.
      this.gantt.options.step;
    this.width = this.gantt.options.column_width * this.duration;
    this.progress_width =
      this.gantt.options.column_width *
      this.duration *
      (this.task.progress / 100) || 0;
  };
  /**
   * Custom implementation of compute_start_end_date from the frappe-gantt(v0.6.1) library.
   * Removed rounding to nearest hour when computing the start and end dates.
   * This fixes a bug which removed an hour from the end date the first time the task was moved.
   */
  function custom_compute_start_end_date() {
    const bar = this.$bar;
    const x_in_units = bar.getX() / this.gantt.options.column_width;
    const new_start_date = new Date( // Changed: Removed frappe-gantt's date_utils hour rounding.
      this.gantt.gantt_start.getTime() +
      (x_in_units * this.gantt.options.step) * 1000 * 60 * 60
    );
    const width_in_units = bar.getWidth() / this.gantt.options.column_width;
    const new_end_date = new Date( // Changed: Removed frappe-gantt's date_utils hour rounding.
      new_start_date.getTime() +
      (width_in_units * this.gantt.options.step) * 1000 * 60 * 60
    );

    return { new_start_date, new_end_date };
  };
  /**
   * Disable Frappe's popup.
   */
  function custom_show_popup() {
    return;
  };
  /**
   * Event handler which fires when double clicking a task.
   */
  function on_click(task) {
    window.open(task.link).focus();
  };
  /**
   * Event handler which fires after changing the progress.
   */
  function on_progress_change(task, newProgress) {
    if (newProgress == task.editMetadata.oldProgress) {
      // Useless progress change (event fires on any click, quirk of frappe-gantt).
      return;
    }
    let forbiddenEditReason = taskutil.getForbiddenEditMsg(task, this);
    if (forbiddenEditReason != null) {
      taskui.revertBar(task, this);
      new XWiki.widgets.Notification(forbiddenEditReason, 'error', '20');
      return;
    }

    let params = {
      progress: newProgress,
      form_token: xm.form_token,
    };
    if (task.taskClasses.includes('taskgantt-no-due-date')) {
      // If the user interacts with a 'no-due-date' task, automatically set the dates.
      params.startDate = task._start.getTime();
      params.endDate = task._end.getTime();
    }
    taskui.setGanttDisabledOverlayVisibility(this, true);
    $.post(taskurls.getUpdateServiceURL(task), params)
      .done(taskui.getUpdateServiceSuccessCallback(this, task))
      .fail(taskui.getUpdateServiceErrorCallback(this, task));
  };
  /**
   * Event handler which fires after changing the dates.
   */
  function on_date_change(task, newStartDate, newEndDate) {
    // Frappe automatically subtracts 1 second from the real end date.
    newEndDate.setSeconds(newEndDate.getSeconds() + 1);

    if (newStartDate.getTime() == task.editMetadata.oldStartDate.getTime() &&
      newEndDate.getTime() == task.editMetadata.oldEndDate.getTime()) {
      // Useless progress change (event fires on any click, quirk of frappe-gantt).
      return;
    }
    let forbiddenEditReason = taskutil.getForbiddenEditMsg(task, this);
    if (forbiddenEditReason != null) {
      taskui.revertBar(task, this);
      new XWiki.widgets.Notification(forbiddenEditReason, 'error', '20');
      return;
    }
    let params = {
      form_token: xm.form_token,
    };
    if (newStartDate.getTime() != task.editMetadata.oldStartDate.getTime()) {
      params.startDate = newStartDate.getTime();
    }
    if (newEndDate.getTime() != task.editMetadata.oldEndDate.getTime()) {
      params.endDate = newEndDate.getTime();
    }

    taskui.setGanttDisabledOverlayVisibility(this, true);
    $.post(taskurls.getUpdateServiceURL(task), params)
      .done(taskui.getUpdateServiceSuccessCallback(this, task))
      .fail(taskui.getUpdateServiceErrorCallback(this, task));
  };

  return {
    // Frappe library Gantt class overrides.
    'custom_trigger_event': custom_trigger_event,
    'custom_render': custom_render,
    'custom_get_date_info': custom_get_date_info,
    'custom_bind_bar_events': custom_bind_bar_events,
    // Frappe library Bar class overrides.
    'custom_update_bar_position': custom_update_bar_position,
    'custom_compute_x': custom_compute_x,
    'custom_prepare_values': custom_prepare_values,
    'custom_compute_start_end_date': custom_compute_start_end_date,
    'custom_show_popup': custom_show_popup,
    // Event handlers.
    'on_click': on_click,
    'on_progress_change': on_progress_change,
    'on_date_change': on_date_change,
  };
});

define('taskgantt-frappe', ['taskgantt-frappe-methods', 'frappe-gantt'], function (overrideDefs) {
  /**
   * Override the default behaviour of some functionalities of frappe-gantt.
   */
  if (Gantt.prototype['overrideDone'] === undefined) {
    Gantt.prototype['overrideDone'] = false; // Dummy property so the override is done only once.

    /**
     * Override the Gantt inner class to remove some restrictions between dependencies.
     * For this we need a Gantt instance, as the `Bar` class isn't publicly accessible.
     */
    let dummyElement = document.createElement('div');
    let gantt = new Gantt(dummyElement, [{ id: 'task1' }]);

    gantt.bars[0].__proto__['update_bar_position'] = overrideDefs.custom_update_bar_position;

    gantt.bars[0].__proto__['show_popup'] = overrideDefs.custom_show_popup;

    gantt.bars[0].__proto__['compute_start_end_date'] = overrideDefs.custom_compute_start_end_date;

    gantt.bars[0].__proto__['compute_x'] = overrideDefs.custom_compute_x;

    gantt.bars[0].__proto__['_prepare_values'] = gantt.bars[0].__proto__['prepare_values'];
    gantt.bars[0].__proto__['prepare_values'] = overrideDefs.custom_prepare_values;

    delete gantt;
    delete dummyElement;

    // Override Gantt class methods.

    Gantt.prototype['trigger_event'] = overrideDefs.custom_trigger_event;

    Gantt.prototype['_render'] = Gantt.prototype['render'];
    Gantt.prototype['render'] = overrideDefs.custom_render;

    Gantt.prototype['_get_date_info'] = Gantt.prototype['get_date_info'];
    Gantt.prototype['get_date_info'] = overrideDefs.custom_get_date_info;

    Gantt.prototype['bind_bar_events'] = overrideDefs.custom_bind_bar_events;

    Gantt.prototype['overrideDone'] = true;
  }
});
require([
  'jquery',
  'taskgantt-service-urls',
  'taskgantt-localization',
  'taskgantt-frappe-methods',
  'taskgantt-utils',
  'taskgantt-config-templates',
  'frappe-gantt',
  'taskgantt-frappe'
], function ($, serviceUrls, l10n, overrideDefs, taskutil, tgtemplates) {
  function scrollToToday(ganttDiv) {
    // Scroll to today (if available)
    const scrolledGanttDiv = ganttDiv.querySelector('svg.gantt').parentNode;
    const todayMarker = scrolledGanttDiv?.querySelector('.today-highlight');
    const markerX = Number.parseFloat(todayMarker?.getAttribute('x'));
    if (todayMarker && markerX < Number.parseFloat(ganttDiv.querySelector('svg.gantt').getAttribute('width'))) {
      scrolledGanttDiv.scrollLeft = markerX - scrolledGanttDiv.getBoundingClientRect().width / 2;
    }
  }

  async function spawnGantt(ganttDiv, idx) {

    $(ganttDiv).attr('id', 'taskgantt-gantt' + idx);

    let tasks = await fetch(serviceUrls.getJsonSourceURL(ganttDiv)).then(response => response.json());
    if (tasks.length == 0) {
      // Frappe breaks if it has no tasks to display, so replace it with an error message.
      if (!$(ganttDiv).find('#ganttNoTasksMessage').length) {
        $(ganttDiv).append(tgtemplates.noTasksMessage);
      }
      $(ganttDiv).find('#view-mode-buttons').toggleClass('hidden', true);
      return;
    } else {
      $(ganttDiv).find('#view-mode-buttons').toggleClass('hidden', false);
    }
    tasks.forEach(taskutil.formatTask);
    tasks.forEach((task) => {
      // If the dependencies of this task are not present in this diagram, remove them (otherwise the Gantt breaks).
      let dependencies = task.dependencies.filter((dep) => tasks.any((t) => (t.id == dep)));
      task.dependencies = dependencies;
    });

    let gantt;

    let ganttOptions = {
      view_mode: 'Day',
      popup_trigger: null,
      padding: 15,
      language: l10n.XWikiLocale,
      on_click: overrideDefs.on_click,
      on_progress_change: overrideDefs.on_progress_change,
      on_date_change: overrideDefs.on_date_change,
    };

    try {
      gantt = new Gantt(ganttDiv, tasks, ganttOptions);
    } catch (e) {
      // In the case the language is undefined or not supported by frappe-gantt, fall back to English.
      ganttOptions.language = 'en';
      gantt = new Gantt(ganttDiv, tasks, ganttOptions);
    }

    // Bind viewmode buttons.
    $('#view-mode-buttons > button.view-mode-button', ganttDiv).on('click', (e) => {
      gantt.change_view_mode(e.currentTarget.value);
      scrollToToday(ganttDiv);
    });

    scrollToToday(ganttDiv);

    return gantt;
  };
  // Listener so the tasks are displayed when stepping out of WYSIWYG. Otherwise the tasks would be removed.
  document.on('xwiki:dom:updated', () => {
    Promise.all($.map($('.taskgantt'), spawnGantt));
  });
  Promise.all($.map($('.taskgantt'), spawnGantt));
});
