Changelog
==========

Version 3.4.2 *(2018-04-13)*
----------------------------

 * Hide public notification content if desired so (by fraang)
 * Added optional grid on the monthly view
 * Allow exporting events on SD cards
 * Allow selecting No Sound as a reminder sound
 * Set default event status for CalDAV events as Confirmed

Version 3.4.1 *(2018-03-30)*
----------------------------

 * Reworked custom notification sound, should be more reliable
 * Fixed some glitches related to the monthly view
 * Misc smaller bugfixes and stability improvements

Version 3.4.0 *(2018-02-28)*
----------------------------

 * Rewrote the monthly view
 * Improved the performance at importing events from ics files
 * Added many new country holidays
 * Handle some new third party intents

Version 3.3.2 *(2018-02-21)*
----------------------------

 * Try fixing the off-by-one issue at CalDAV syncing all-day events
 * Couple stability improvements

Version 3.3.1 *(2018-02-19)*
----------------------------

 * Improved CalDAV all-day event importing (by angelsl)
 * Added a FAQ section with a couple initial items
 * Once again fixed some cases of blank or duplicate views

Version 3.3.0 *(2018-02-10)*
----------------------------

 * Fixed blank or duplicate views in some cases (yes, again)
 * Fixed off-by-one day error at syncing all-day events via Nextcloud
 * Make default filenames at export more user-friendly
 * Improved the performance by removing some unnecessary redraws
 * Added a toggle for switching between default snooze interval or always showing an interval picker

Version 3.2.4 *(2018-02-05)*
----------------------------

 * Fixed blank screens in some cases
 * Misc smaller improvements

Version 3.2.3 *(2018-02-01)*
----------------------------

 * Fixed blank screens in some cases
 * Make sure the Add New Event button works when opening the app from a widget
 * Removed the "Default event reminder" from settings, remember last used values
 * Allow selecting Snooze interval at pressing Snooze
 * Allow disabling displaying of What's New
 * Add a Back button at the actionmenu when opening a subview
 * Allow deleting all events at once without reseting event types and other settings

Version 3.2.2 *(2018-01-27)*
----------------------------

 * Fixed some cases of reminders not triggering
 * Properly handle importing events with multiple lines long description
 * Properly show the New Event button whenever appropriate

Version 3.2.1 *(2018-01-22)*
----------------------------

 * Misc minor fixes

Version 3.2.0 *(2018-01-22)*
----------------------------

 * Added an initial implementation of Search
 * Fixed an off-by-one issue at syncing all-day CalDAV events
 * Added a Daily View
 * Allow importing events from .ics files directly in a CalDAV account
 * Try parsing latitude and longitude coordinates from the Location field

Version 3.1.0 *(2018-01-11)*
----------------------------

 * Made some CalDAV sync improvements, especially related to repeatable event exceptions
 * Added a Map button to event location, to display the location in a third party map
 * Handle INSERT and EDIT intent
 * Made Dark theme the default
 * Updated both event list and monthly widget, hopelly making them more reliable
 * Added holidays in Australia
 * Hopefully fixed the off-by-one error at importing/syncing all-day events/holidays

Version 3.0.1 *(2017-12-06)*
----------------------------

 * Fixed missing launcher icons on some devices
 * Properly name the Holidays event type on non-english devices

Version 3.0.0 *(2017-12-04)*
----------------------------

 * Improved primary color customization
 * Fixed notifications on Android 8
 * Fixed a couple issues with importing events
 * Properly export/import custom event type colors

Version 2.12.0 *(2017-11-26)*
----------------------------

 * Allow setting repetition on every for example 4th sunday, even if a month has only 4
 * Some rewrites to the list views

Version 2.11.0 *(2017-11-13)*
----------------------------

 * Adding a toggle for using English language on non-english devices
 * Adding an Adaptive launcher icon
 * Really fixing CalDAV sync
 * Many other smaller improvements under the hood

Version 2.10.1 *(2017-11-08)*
----------------------------

 * Fixed CalDAV syncing

Version 2.10.0 *(2017-11-07)*
----------------------------

 * Allow adding contact anniversaries
 * Couple monthly view improvements
 * Remove the wrongly added Fingerprint permission

Version 2.9.2 *(2017-10-30)*
----------------------------

 * Add PRODID and VERSION to the exported ICS file
 * Handle parsing more formats of contact birthdays
 * Increase the opacity of event titles at weekly and monthly view

Version 2.9.1 *(2017-10-25)*
----------------------------

 * Fix a glitch at automatically reseting regular event type color
 * Use contrast color at weekly view for event labels
 * Properly handle birthdays without a year specified

Version 2.9.0 *(2017-10-21)*
----------------------------

 * Added a Location field
 * Allow adding Contact birthdays
 * Rewrote the widgets, making them more reliable
 * After opening the app from widget and pressing Back, go to the main screen
 * Many other performance and stability improvements

Version 2.8.2 *(2017-10-14)*
----------------------------

 * Increasing the monthly widget font size
 * Couple smaller improvements to monthly widget

Version 2.8.1 *(2017-10-09)*
----------------------------

 * Adding a crashfix

Version 2.8.0 *(2017-10-08)*
----------------------------

 * Reworked the monthly view layout
 * Misc performance/ux improvements

Version 2.7.6 *(2017-09-24)*
----------------------------

 * Add support for events repeating every 30 days
 * Fix event type color at the Event list widget
 * Fix a glitch at wrongly displayed events lasting through midnight
 * Update events imported via .ics files, check last-modified field

Version 2.7.5 *(2017-09-10)*
----------------------------

 * Fixed some CalDAV sync issues
 * Use the next full hour as the default event time, not 13:00 the next day
 * Color the days in yearly view based on event types
 * Allow displaying past events in the event list widget
 * Make sure the Save and Delete buttons have the highest priority at the actionmenu

Version 2.7.4 *(2017-09-02)*
----------------------------

 * Fixed importing .ics files
 * Made errors at importing events more verbose

Version 2.7.3 *(2017-08-29)*
----------------------------

 * Adding some crashfixes

Version 2.7.2 *(2017-08-28)*
----------------------------

 * Misc minor improvements and crashfixes

Version 2.7.1 *(2017-08-28)*
----------------------------

 * Adding minor crashfixes

Version 2.7.0 *(2017-08-27)*
----------------------------

 * Replaced Google Sync with CalDAV sync, please check Settings for enabling it
 * Improved Tablet support
 * Added colored dots marking event types at multiple views

Version 2.6.1 *(2017-07-30)*
----------------------------

 * Properly display events repeating weekly on some views
 * Adding a crashfix

Version 2.6.0 *(2017-07-29)*
----------------------------

 * Added an initial implementation of Google sync

Version 2.5.8 *(2017-07-12)*
----------------------------

 * Hotfixing an issue causing crashes at upgrading the app for some people
 * Build was created from b544749 by cherry-picking 521eeb6 for playstore only

Version 2.5.7 *(2017-07-06)*
----------------------------

 * Added a setting for changing widget font size
 * Added Austrian holidays

Version 2.5.6 *(2017-06-24)*
----------------------------

 * Properly display all-day events on list views

Version 2.5.4 *(2017-06-23)*
----------------------------

 * Added rounded launcher icons for Android Nougat

Version 2.5.3 *(2017-06-14)*
----------------------------

 * Allow editing specific instances of repeating events
 * Show proper date and time at opening a repeatable event instance

Version 2.5.2 *(2017-06-05)*
----------------------------

 * Store timezone and daylight saving info at events
 * Misc smaller crashfixes and improvements

Version 2.5.1 *(2017-05-26)*
----------------------------

 * Adding a crashfix at monthly widget

Version 2.5.0 *(2017-05-24)*
----------------------------

 * Allow displaying some events from the past in the Simple Event List view
 * Make sure the event duration stays the same at changing the event Start value

Version 2.4.3 *(2017-05-18)*
----------------------------

 * Added an easy way to import Holidays in some countries
 * Allow filtering events to export by event type
 * Added Snooze
 * Fixed misc issues with triggering reminders
 * Allow renaming the file used for exporting events to
 * Added Turkish translation by Burhan 2010

Version 2.4.2 *(2017-05-09)*
----------------------------

 * Add more advanced monthly repetition options, like "Last day of the month" or "Every second Tuesday"
 * Fix events repeating by multiple weeks or months
 * Allow exporting events on SD cards
 * Properly display non repeating events

Version 2.4.1 *(2017-05-09)*
----------------------------

 * Add more advanced monthly repetition options, like "Last day of the month" or "Every second Tuesday"
 * Fix events repeating by multiple weeks or months
 * Allow exporting events on SD cards
 * Properly display non repeating events

Version 2.4.0 *(2017-05-08)*
----------------------------

 * Add more advanced monthly repetition options, like "Last day of the month" or "Every second Tuesday"
 * Fix events repeating by multiple weeks or months
 * Allow exporting events on SD cards

Version 2.3.5 *(2017-05-05)*
----------------------------

 * Make urls, phone numbers, map coords and emails in event description clickable
 * Fixed a couple issues mostly related to repeating events
 * Minor crashfixes

Version 2.3.4 *(2017-04-23)*
----------------------------

 * Add a new event repetition type "Repeat x times"
 * Color the dot at the monthly view based on the event types
 * Fix a couple event importing issues

Version 2.3.3 *(2017-04-17)*
----------------------------

 * Fixing a crash at getting the sd card path

Version 2.3.2 *(2017-04-17)*
----------------------------

 * Added more advanced weekly repetition by week days
 * Added sharing individual events by creating temporary .ics files
 * Properly start the week at datepicker by Sunday if selected so
 * Handle opening .ics files from email clients
 * Some bugfixes related to exporting/importing events
 * Misc other bugfixes and UX improvements

Version 2.3.1 *(2017-04-09)*
----------------------------

 * Fix some crashing to people who upgraded 2 database versions at once
 * Add repeatable event if UNTIL is the time of last occurences start

Version 2.3.0 *(2017-04-07)*
----------------------------

 * Replaced raw database exporting with proper .ics file exporting
 * Added color themes

Version 2.2.7 *(2017-04-02)*
----------------------------

 * Misc smaller bugfixes and improvements

Version 2.2.6 *(2017-03-12)*
----------------------------

 * Fix some rare crash at some time of some timezone

Version 2.2.5 *(2017-03-12)*
----------------------------

 * Fix some rare crash at some time of some timezone

Version 2.2.4 *(2017-03-06)*
----------------------------

 * Some crashfixes

Version 2.2.3 *(2017-03-05)*
----------------------------

 * Allow deleting individual occurrences of repeatable events
 * Many smaller improvements and bugfixes

Version 2.2.2 *(2017-02-28)*
----------------------------

 * Added the ability to export raw database of events

Version 2.2.1 *(2017-02-21)*
----------------------------

 * Added a setting for toggling 24-hour format
 * Added buttons for easy event creation to widgets
 * Some .ics file parsing fixes
 * Couple smaller improvements

Version 2.2.0 *(2017-02-19)*
----------------------------

 * Implement customizable repeat intervals
 * Use a lot more fields when importing events from .ics files
 * Add a "Go to today" button in some views
 * Many other smaller improvements and bugfixes

Version 2.1.10 *(2017-02-16)*
----------------------------

 * Fix some issues with importing some .ics files
 * Use the es Spanish translation for ca and gl Spanish too

Version 2.1.9 *(2017-02-12)*
----------------------------

 * Added event types with customizable colors
 * Some crashfixes

Version 2.1.8 *(2017-02-07)*
----------------------------

 * Added a current time indicator at weekly view
 * Removed the Undo function after deleting events
 * Added Slovak translation

Version 2.1.7 *(2017-02-07)*
----------------------------

 * Fix reminders of non repeating events

Version 2.1.6 *(2017-02-06)*
----------------------------

 * Allow setting up to 3 reminders per event

Version 2.1.5 *(2017-02-06)*
----------------------------

 * Adding a crashfix

Version 2.1.4 *(2017-02-05)*
----------------------------

 * Added support for all-day long events
 * Improved support for importing .ics files
 * Many UI changes here and there

Version 2.1.3 *(2017-01-26)*
----------------------------

 * Allow importing events from .ics files
 * Display proper week number at widgets
 * Smaller bugfixes and improvements

Version 2.1.2 *(2017-01-24)*
----------------------------

 * Some smaller bugfixes

Version 2.1.1 *(2017-01-23)*
----------------------------

 * Allow selecting colors by hex codes
 * Added a button for restoring default colors

Version 2.1.0 *(2017-01-22)*
----------------------------

 * Adding an initial implementation of a weekly view

Version 2.0.3 *(2017-01-10)*
----------------------------

 * Use the new version of the colorpicker dialog
 * Some crashfixes

Version 2.0.2 *(2017-01-07)*
----------------------------

 * Allow setting empty notification sound
 * Notification reminders have been tweaked, please update them

Version 2.0.1 *(2017-01-06)*
----------------------------

 * Use a brighter white for the default background
 * Misc smaller changes

Version 2.0.0 *(2017-01-04)*
----------------------------

 * Added more color customization
 * Added a Whats new dialog
 * Mark current day with a circle, underline days with events
 * Many smaller improvements and bugfixes

Version 1.43 *(2016-12-18)*
----------------------------

 * Use 12 or 24 hours time format as appropriate
 * Fixing a crash at going to Settings on some devices

Version 1.42 *(2016-12-10)*
----------------------------

 * Allow changing the reminder notification sound

Version 1.41 *(2016-12-09)*
----------------------------

 * Some smaller bugfixes and updated translations

Version 1.40 *(2016-11-30)*
----------------------------

 * Add a new widget for a list of events
 * Make the reminder alarm more aggressive, waking up sleeping devices
 * Misc bug and crashfixes

Version 1.39 *(2016-11-26)*
----------------------------

 * Activate the notification light at reminders
 * Add an optional vibration to reminders, disabled by default
 * Misc smaller improvements

Version 1.38 *(2016-11-23)*
----------------------------

 * Always show the "Change view" button on the actionbar
 * Add a confirmation dialog before deleting events
 * Reduce the size of todays date on big screens

Version 1.37 *(2016-11-22)*
----------------------------

 * Remove the "Add new event" button from yearly view
 * Misc other smaller improvements

Version 1.36 *(2016-11-19)*
----------------------------

 * Make sure events are sorted properly in the Day view

Version 1.35 *(2016-11-19)*
----------------------------

 * Some crashfixes

Version 1.34 *(2016-11-17)*
----------------------------

 * Misc small fixes

Version 1.33 *(2016-11-07)*
----------------------------

 * Display the day of the week in some views
 * Added Spanish translation

Version 1.32 *(2016-11-03)*
----------------------------

 * Properly highlight events lasting multiple days

Version 1.31 *(2016-11-03)*
----------------------------

 * Couple minor UI improvements

Version 1.30 *(2016-11-02)*
----------------------------

 * Added Hindi translation
 * Some default reminder corrections

Version 1.29 *(2016-11-02)*
----------------------------

 * Allow setting event reminder in other units too, not just minutes
 * Allow setting a default reminder used by new events

Version 1.28 *(2016-10-24)*
----------------------------

 * Misc bugfixes and improvements

Version 1.27 *(2016-10-23)*
----------------------------

 * Added a new view with listing all events one year ahead

Version 1.26 *(2016-10-22)*
----------------------------

 * Added a yearly view
 * Misc bugfixes and performance improvements

Version 1.25 *(2016-10-15)*
----------------------------

 * Add biweekly repetition
 * Remember the last value used at "Custom reminder minutes"

Version 1.24 *(2016-10-05)*
----------------------------

 * Fix notifications of non-repeatable events
 * Add an option to display week numbers, disabled by default

Version 1.23 *(2016-09-24)*
----------------------------

 * Reduce the side padding of the widget

Version 1.22 *(2016-09-20)*
----------------------------

 * Allow swiping between months and days
 * Update the widget when appropriate
 * Underline days with events on the widget instead of using bold
 * Misc UI improvements

Version 1.21 *(2016-09-13)*
----------------------------

 * Add simple daily, weekly, monthly and yearly recurring option
 * Update the widget every 1 hour down from 5 hours
 * Make event End date and time really optional

Version 1.20 *(2016-09-09)*
----------------------------

 * Allow setting first day of the week to sunday

Version 1.19 *(2016-09-07)*
----------------------------

 * Added German translation

Version 1.18 *(2016-08-29)*
----------------------------

 * Rename the app launcher to Calendar
 * Update the launcher icon
 * Fix deleting multiple events at once

Version 1.17 *(2016-08-21)*
----------------------------

 * Add an Invite friends button
 * Allow adding events in the past, but notify the user
 * Set default event reminder to At start
 * Use manually translated month names for reliability
 * Fix day switching arrows for every timezone

Version 1.16 *(2016-08-04)*
----------------------------

 * Make event end date/time optional
 * Show the event description at the events screen

Version 1.15 *(2016-07-31)*
----------------------------

 * Fix a bug with some wrong month names

Version 1.14 *(2016-07-26)*
----------------------------

 * Make the dark theme really dark

Version 1.13 *(2016-07-26)*
----------------------------

 * Properly translate month names

Version 1.12 *(2016-07-21)*
----------------------------

 * Fix date at adding new event

Version 1.11 *(2016-07-18)*
----------------------------

 * Added navigation between screens

Version 1.10 *(2016-07-18)*
----------------------------

 * Added Dark Theme
 * Added Japanese, Swedish and Italian translations
 * Misc minor fixes

Version 1.9 *(2016-07-13)*
----------------------------

 * Do not force portrait mode

Version 1.8 *(2016-07-07)*
----------------------------

 * Implement Event notifications

Version 1.6 *(2016-07-05)*
----------------------------

 * Implement Events (no reminder yet)

Version 1.5 *(2016-07-01)*
----------------------------

 * Adjust everything properly if used on a tablet
 * Show a Rate us button to returning users

Version 1.4 *(2016-06-27)*
----------------------------

 * Add a Google Plus page link to the About section

Version 1.3 *(2016-06-19)*
----------------------------

 * Add a Facebook page link to the About section

Version 1.2 *(2016-06-17)*
----------------------------

 * Always force english locale

Version 1.1 *(2016-06-13)*
----------------------------

 * Allow resizing the widget

Version 1.0 *(2016-06-05)*
----------------------------

 * Initial release
