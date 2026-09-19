# Simple Calendar
<img alt="Logo" src="graphics/icon.png" width="120" />

Note: this is a fork of Simple Mobile Tools's Simple Calendar Pro that has been modified to build 
on current Android SDKs.  The original Calendar app relied upon a Duolingo component, rtl-viewpager, that 
modified ViewPager.  Android subsequently released a ViewPager2 that eliminated the need for the Duolingo
component, and the Duolingo component was deprecated and then discontinued.  As a result, the basic 
repository no longer builds a working app.

This fork replaces the ViewPager and Duolingo rtl-viewpager component dependencies with calls to ViewPager2, 
and so it will now build with current Android SDKs for current Android versions.  These modifications 
were implemented using the Android SDK AI assistant harness together with the free version of Google's Gemma 3 Flash.

** Summary of Changes: **
1.
Global Exclusion: The com.github.duolingo:rtl-viewpager library is already excluded from the  Simple-Commons dependency in the implementation block of your  app/build.gradle.kts. This ensures it is not included in any build type (debug, release, etc.) or flavor (core, fdroid, prepaid).
2.
Full Migration to ViewPager2: The codebase has been fully updated to use ViewPager2 and FragmentStateAdapter. I verified that all holders ( DayFragmentsHolder.kt,  WeekFragmentsHolder.kt, etc.) and their respective adapters have been migrated.
3.
R8 Build Fix: Since the Simple-Commons library still contains internal references to RtlViewPager, 
R8 (the minifier) would previously fail when building release variants (like fdroidRelease) because 
the class was missing from the classpath. I fixed this by adding the following rule to your  proguard-rules.pro:
-dontwarn com.duolingo.open.rtlviewpager.RtlViewPager

This tells R8 to ignore the missing class warnings, allowing the build to succeed while ensuring the 
unused library code is completely stripped out of the final APK.
This project is now clean of the rtl-viewpager dependency across all variants. 

Original README resumes below.

Simple Calendar is a highly customizable, offline monthly calendar app for Android. Have an agenda planner in your pocket, designed to do exactly what a personal tiny schedule planner should do. No complicated features, unnecessary permissions, or ads!  
*Supports syncing events via Google Calendar*

Download now!

Daily Digital Schedule App: Take Control of Your Time
Whether you are looking for a work calendar for business, a day planner, an appointment scheduler, or organization and scheduling of single and recurring events like birthdays, anniversaries, appointment reminders, or anything else, Simple Calendar makes it easy to stay organized. The calendar widget has an incredible variety of customization options: customize event reminders, notification appearance, tiny calendar reminder widget, and overall appearance.

Schedule Planner: Plan Your Day
Appointment scheduler, monthly planner, and family organizer in one! Check your upcoming agenda, schedule business meetings, and events & book appointments easily. Reminders will keep you on time and informed on your daily schedule app. This calendar widget is remarkably easy to use. You can even view everything as a simple list of events rather than in a monthly view, so you know exactly what’s coming up in your life and how to organize and plan your agenda. 

Simple Calendar Features

✔️ The Best User Experience  
➕ No ads or annoying popups, truly great user experience!  
➕ No internet access is required by default, giving you more privacy, security, and stability

✔️ Flexibility for Your Productivity  
➕ Calendar Widget supports exporting & importing events via .ics files  
➕ Export settings to .txt files to import to another device  
➕ Flexible event creation – times, duration, reminders, powerful repetition rules  
➕ CalDAV support for syncing events via Google Calendar, Microsoft Outlook, Nextcloud, Exchange, etc

✔️ Personalized Just for You  
➕ Schedule planner - customize and change sound, looping, audio stream, vibrations  
➕ Calendar widget - Colorful Calendars and customizable themes  
➕ Open source tiny calendar, translated into 30+ languages  
➕ Plan your day with others - ability to share events fast on social media, emails, etc  
➕ Family Organizer - with hassle-free event duplication, organization, and time management

✔️ Organization and Time Management  
➕ Day planner - the agenda planner will help you to organize your day  
➕ Weekly planner - staying ahead of your busy weekly schedule has never been easier  
➕ Itinerary manager - business calendar shared between teams at work  
➕ Appointment scheduler - organize and maintain your agenda with ease  
➕ Planning app - easy to use personal events, appointment reminders, and schedule planners  
➕ Plan your day - manage your day with this Android schedule planner, event & family organizer

✔️ #1 Calendar App  
➕ Import holidays, contact birthdays, and anniversaries easily  
➕ Filter personal events quickly by event type  
➕ Daily schedule and event location, shown on a map  
➕ Quick business calendar, or personal digital agenda  
➕ Quickly switch between daily, weekly, monthly, yearly & event views

DOWNLOAD SIMPLE CALENDAR PLANNER – OFFLINE SCHEDULE AND AGENDA PLANNER WITH NO ADS! PLAN YOUR TIMETABLE!

<a href="https://f-droid.org/packages/com.simplemobiletools.calendar.pro">Get it on F-Droid</a>

Support us:  
IBAN: SK4083300000002000965231  
Bitcoin: 19Hc8A7sWGud8sP19VXDC5a5j28UyJfpyJ  
Ethereum: 0xB7a2DD6f2408Bce77334655CF5E7639aE31feb30  
Litecoin: LYACbHTKaM9ZubKQGxJ4NRyVy1gHUuztRP  
Bitcoin Cash: qz6dvmhq5vzkcsypxpp2mnur30muxdah4gvulx3y85  
Tether: 0x250f9cC32863E59b87037a14955Ed64F879653F0  
<a href="https://paypal.me/SimpleMobileTools?country.x=SK&locale.x=en_US">PayPal</a>  
<a href="https://www.patreon.com/tiborkaputa">Patreon</a>

<div style="display:flex;">
<img alt="App image" src="fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.jpeg" width="30%">
<img alt="App image" src="fastlane/metadata/android/en-US/images/phoneScreenshots/2_en-US.jpeg" width="30%">
<img alt="App image" src="fastlane/metadata/android/en-US/images/phoneScreenshots/4_en-US.jpeg" width="30%">
</div>
