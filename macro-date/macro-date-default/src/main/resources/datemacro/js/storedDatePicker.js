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
require(['jquery', 'moment', 'moment-jdateformatparser', 'xwiki-events-bridge'], function($, moment) {
  'use strict';
  // Convert the stored dates written in a previous storage format to the current one, before letting the date time
  // picker parse them with the current one.
  let convert = function(input) {
    let value = input.val().trim();
    let formats = input.data('storageFormats').map(format => moment().toMomentFormatString(format));
    for (const format of formats) {
      let date = moment(value, format, true);
      if (date.isValid()) {
        input.val(date.format(formats[0]));
        return;
      }
    }
  };
  let init = function(event, data) {
    let inputs = $((data && data.elements) || document).find('input.stored-date');
    inputs.each(function() {
      let input = $(this);
      if (input.val()) {
        convert(input);
      }
      input.removeClass('stored-date').addClass('datetime');
    });
    if (inputs.length) {
      // Initialize the date time pickers, that look for the inputs inside the given elements.
      $(document).trigger('xwiki:dom:updated', {'elements': inputs.parent().toArray()});
    }
  };
  $(document).on('xwiki:dom:updated', init);
  $(init);
});
