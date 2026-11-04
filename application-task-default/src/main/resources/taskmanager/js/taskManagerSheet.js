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
// Applies the same rules as the TaskStatusSyncListener, so that the form shows the values that will be saved.
require(['jquery', 'moment'], function ($, moment) {
  $(function() {
    const statusSelect = $("select[id='TaskManager.TaskManagerClass_0_status']");
    const progressInput = $("input[id='TaskManager.TaskManagerClass_0_progress']");
    let previousStatus = statusSelect.val();

    const setDateIfEmpty = function(property) {
      const dateInput = $("input[name='TaskManager.TaskManagerClass_0_" + property + "']");
      if (dateInput.length && dateInput.val() == '') {
        dateInput.val(moment().format(moment().toMomentFormatString(dateInput.data('format'))));
      }
    };

    // Updates the dates that depend on the status, whatever changed it.
    const onStatusChanged = function(status) {
      if (status == 'Done') {
        setDateIfEmpty('completeDate');
      } else if (previousStatus == 'Done') {
        $("input[name='TaskManager.TaskManagerClass_0_completeDate']").val('');
      }
      if (status == 'InProgress') {
        setDateIfEmpty('startDate');
      }
      previousStatus = status;
    };

    progressInput.on('change', function() {
      const value = $(this).val();
      if (value == '') {
        return;
      }
      const progress = parseInt(value);
      const status = statusSelect.val();
      let newStatus = status;
      if (progress >= 100) {
        newStatus = 'Done';
      } else if (status == 'Done' || (progress > 0 && status == 'ToDo')) {
        newStatus = 'InProgress';
      }
      if (newStatus != status) {
        statusSelect.val(newStatus);
        onStatusChanged(newStatus);
      }
    });

    statusSelect.on('change', function() {
      const status = $(this).val();
      if (status == 'Done') {
        progressInput.val(100);
      } else if (status == 'ToDo' || previousStatus == 'Done') {
        progressInput.val(0);
      }
      onStatusChanged(status);
    });
  });
});
