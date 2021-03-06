
=======
# oXygen-XML-editor-xspec-support

An oxygen XML Editor plugin intended to help those that create XSpec scenarios.

Install as Add-On
-----------------

1. In the oXygen menu, open _Help » Install new add-ons..._
2. In the field _Show add-ons from_, add this URL: `https://raw.githubusercontent.com/xspec/oXygen-XML-editor-xspec-support/master/build/update_site.xml`
3. The _XSpec Helper View_ and the _XSpec Framework (required by the XSpec Helper view plugin)_ add-ons will be displayed. Follow the steps to install them both and restart oXygen.
4. Optional: check _Enable automatic updates checking_ in _Options » Preferences » Add-ons_ to get update notifications

Alternative installation method
-----

1. download the plugin [ZIP package](https://github.com/AlexJitianu/oXygen-XML-editor-xspec-support/raw/master/build/xspec.support-1.0-SNAPSHOT-plugin.zip) and unzip it inside `{OxygenInstallDir}/plugins`
2. download the framework [ZIP package](https://github.com/AlexJitianu/oXygen-XML-editor-xspec-support/raw/master/build/xspec.zip) and unzip it inside `{OxygenInstallDir}/frameworks`


Known issues
----
The plugin relies on a special framework. This might pose a problem is you are using project 
files and the preferences page "Locations" is set to "Project". In this cases please load a 
new project or switch the previously mentioned page to "Global". I'll think of a better 
solution for the future.

How to use it
-----------

1. Inside Oxygen open an XSpec file.
1. On the toolbar click on the action "XSpec Run"
1. A view named "XSpec Test Results" will be opened. 

**Note**: At this point you can switch to the XSLT and use only the "Run" actions in the view to execute the scenarios.


#### What you can do inside the "XSpec Test Results" view:

1. For each test threre is a "Show" action that will select in the editor the coresponding test
1. For each scenario there is a "Run" action that will run just that scenario
1. For a failed test you can click on it and the diff between the Expected/Actual results will be opened
 

How to customize it
-------------------
On the XML XSpec report, an XSLT is applied that generates HTML. This HTML is opened inside the view. The XSLT in question 
is: `{pluginDirectory}/frameworks/xspec/src/reporter/unit-report-oxygen.xsl`

