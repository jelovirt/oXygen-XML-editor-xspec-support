package com.oxygenxml.xspec.jfx.bridge;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;

import com.oxygenxml.xspec.XSpecResultsView;
import com.oxygenxml.xspec.XSpecUtil;
import com.oxygenxml.xspec.XSpecUtil.OperationCanceledException;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.actions.TextActionsProvider;
import ro.sync.exml.workspace.api.editor.page.text.xml.WSXMLTextEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.xml.WSXMLTextNodeRange;
import ro.sync.exml.workspace.api.editor.page.text.xml.XPathException;
import ro.sync.exml.workspace.api.editor.transformation.TransformationFeedback;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * A bridge between JavaScript and Java. JavaScript code will be able to invoke 
 * these methods.
 *  
 * @author alex_jitianu
 */
public class Bridge {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(Bridge.class.getName());

  /**
   * Form control editing context.
   */
  private URL xspec;

  private PluginWorkspace pluginWorkspace;

  private XSpecResultsView resultsPresenter;

  /**
   * Constructor.
   * @param pluginWorkspace 
   * @param resultsPresenter2 
   * 
   * @param context Form control editing context.
   */
  private Bridge(PluginWorkspace pluginWorkspace, XSpecResultsView resultsPresenter, URL xspec) {
    this.pluginWorkspace = pluginWorkspace;
    this.resultsPresenter = resultsPresenter;
    this.xspec = xspec;
  }

  public static Bridge install(WebEngine engine, XSpecResultsView resultsPresenter, PluginWorkspace pluginWorkspace, URL xspec) {
    JSObject window = (JSObject) engine.executeScript("window");
    Bridge value = new Bridge(pluginWorkspace, resultsPresenter, xspec);
    window.setMember("xspecBridge", value);

    return value;
  }

  /**
   * Invoked from Javascript. Shows an XSpect test (<expect> element).
   * 
   * @param testName Test name.
   * @param scenarioName Scenario name.
   * @param scenarioLocation System id of the file that contains the scenario.
   */
  public void showTest(final String testName, final String scenarioName, final String scenarioLocation) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        showTestAWT(testName, scenarioName, scenarioLocation);
      }
    });
  }

  private void showTestAWT(String testName, String scenarioName, String scenarioLocation) {
    // Just in case the file is no longer opened.
    URL toOpen = xspec;
    try {
      toOpen = new URL(scenarioLocation);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }
    pluginWorkspace.open(toOpen);

    WSEditor editor = pluginWorkspace.getEditorAccess(toOpen, PluginWorkspace.MAIN_EDITING_AREA);

    WSEditorPage currentPage = editor.getCurrentPage();
    if (currentPage instanceof WSXMLTextEditorPage) {
      WSXMLTextEditorPage textpage = (WSXMLTextEditorPage) currentPage;
      String xpath = "//*:expect[@label=\"" + testName
          + "\" or *:label=\"" + testName
          + "\"][parent::*:scenario[@label=\"" + scenarioName
          + "\" or *:label/text()=\"" + scenarioName
          + "\"]]";

      if (logger.isDebugEnabled()) {
        logger.debug("Show test XPath: " + xpath);
      }

      try {
        WSXMLTextNodeRange[] ranges = textpage.findElementsByXPath(xpath);
        if (ranges != null && ranges.length > 0) {

          if (logger.isDebugEnabled()) {
            logger.debug("Got range: " + ranges[0]);
          }

          int start = textpage.getOffsetOfLineStart(ranges[0].getStartLine()) + ranges[0].getStartColumn() - 1;
          int end = textpage.getOffsetOfLineEnd(ranges[0].getStartLine()) - 1;

          textpage.select(start, end);


        } else {
          logger.warn("Unable to identify test");
        }
      } catch (XPathException e) {
        e.printStackTrace();
      } catch (BadLocationException e) {
        e.printStackTrace();
      }

    }
  }

  /**
   * Invoked from Javascript. Runs an XSpect scenario.
   * 
   * @param scenarioName The name of the scenario.
   * @param scenarioLocation System id of the file that contains the scenario.
   */
  public void runScenario(final String scenarioName, final String scenarioLocation) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        runScenarioAWT(scenarioName, scenarioLocation);
      }
    });
  }

  private void runScenarioAWT(String scenarioName, String scenarioLocation) {

    URL toOpen = xspec;
    try {
      toOpen = new URL(scenarioLocation);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }
    
    WSEditor e = getEditorAccess(toOpen);

    final WSEditor editor = e;

    WSEditorPage currentPage = editor.getCurrentPage();
    if (currentPage instanceof WSXMLTextEditorPage) {

      // Step 1. Locate the scenario.
      final WSXMLTextEditorPage textpage = (WSXMLTextEditorPage) currentPage;
      String xpath = "//*:scenario[@label=\"" + scenarioName
          + "\" or *:label=\"" + scenarioName
          + "\"]";

      if (logger.isDebugEnabled()) {
        logger.debug("Xpath " + xpath);
      }
      
      try {
        WSXMLTextNodeRange[] ranges = textpage.findElementsByXPath(xpath);
        if (logger.isDebugEnabled()) {
          logger.debug("ranges " + ranges);
        }
        if (ranges != null && ranges.length > 0) {
          int start = textpage.getOffsetOfLineStart(ranges[0].getStartLine()) + ranges[0].getStartColumn();

          // Step 2. Put a @focus on the scenario
          textpage.setCaretPosition(start);

          JTextArea textComponent = (JTextArea) textpage.getTextComponent();
          textpage.setCaretPosition(start);
          WSXMLTextNodeRange[] focusAttr = textpage.findElementsByXPath("@focus");
          if (focusAttr != null && focusAttr.length > 0) {
            // TODO The presence of the @focus will run the scenario. But we should identify and remove all
            // others @focus.

            //            int startAttr = textpage.getOffsetOfLineStart(focusAttr[0].getStartLine()) + focusAttr[0].getStartColumn() - 1;
            //            int endAttr = textpage.getOffsetOfLineStart(focusAttr[0].getEndLine()) + focusAttr[0].getEndColumn() - 1;
            //            textComponent.replaceRange("focus=\"true\"", startAttr, endAttr);

          } else {
            // Look for a place where to put @focus
            String text = textComponent.getText(start, Math.min(20, textComponent.getDocument().getLength() - start));
            if (logger.isDebugEnabled()) {
              logger.debug("Read: " + text);
            }

            // We've read something like: <x:scenario labe="... 
            // or something like: <x:scenario>
            int position = 0;
            char ch = 0;
            while (position < text.length() && ch != ' ' && ch != '>') {
              ch = text.charAt(position);
              position ++;
            };

            if (position < text.length()) {
              // we have an insert location.
              textComponent.insert(" focus=\"true\"", start + position - 1);
            }
          }

          // Save the change. We will start an ANT transformation so we need the 
          // file to be saved.
          // TODO Perhaps it would be best to run a temporary copy, one that
          // we can modify as we see fit!
          editor.save();

          WSEditor xspecToExecute = e;
          if (!xspec.equals(toOpen)) {
            xspecToExecute = getEditorAccess(xspec);
          }
          
          // Step 3. Run the scenario
          XSpecUtil.runScenario(
              xspecToExecute,
              (StandalonePluginWorkspace) pluginWorkspace, 
              resultsPresenter,
              new TransformationFeedback() {
                @Override
                public void transformationStopped() {}
                @Override
                public void transformationFinished(boolean success) {
                  SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                      // Step 4. Undo the change.

                      // TODO The transformation is performed on a thread. The user 
                      // can touch the editor during this time. An UNDO now can't guarantee 
                      // the fact that we will UNDO the @focus attribute set above.
                      TextActionsProvider actionsProvider = textpage.getActionsProvider();
                      Object object = actionsProvider.getTextActions().get("Edit/Edit_Undo");
                      if (object != null) {
                        actionsProvider.invokeAction(object);
                        editor.save();
                      }
                    }
                  });
                }
              });
        } else {
          logger.warn("Unable to indetify scenario");
        }
      } catch (XPathException ex) {
        ex.printStackTrace();
      } catch (BadLocationException ex) {
        ex.printStackTrace();
      } catch (OperationCanceledException e1) {
        // The user canceled the operation.
      }
    }
  }

  private WSEditor getEditorAccess(URL toOpen) {
    WSEditor e = pluginWorkspace.getEditorAccess(toOpen, PluginWorkspace.MAIN_EDITING_AREA);
    if (e == null) {
      // Just in case the file is no longer opened.
      pluginWorkspace.open(toOpen);

      e = pluginWorkspace.getEditorAccess(toOpen, PluginWorkspace.MAIN_EDITING_AREA);
    }
    return e;
  }

  /**
   * Temporary files created for the Diff Files.
   */
  private Map<Integer, File> compareFiles = new HashMap<Integer, File>(); 

  /**
   * Shows the Oxygen Diff files with the given content.
   *  
   * @param left Left side content.
   * @param right Right side content.
   */
  public void showDiff(String left, String right) {
    if (logger.isDebugEnabled()) {
      logger.debug("Show diff");

      logger.debug("Left "  + left.getClass());
      logger.debug("Right "  + right.getClass());

      logger.debug("Left content " + left);
      logger.debug("Right content " + right);
    }

    int leftKey = left.hashCode();
    int rightKey = right.hashCode();

    File f1 = compareFiles.get(leftKey);
    File f2 = compareFiles.get(rightKey);

    try {
      if (f1 == null) {
        f1 = File.createTempFile("result_", ".xml");
        FileOutputStream fos = new FileOutputStream(f1);
        try {
          fos.write(((String)left).getBytes("UTF-8"));
        } finally {
          fos.close();
        }
        compareFiles.put(leftKey, f1);
      }

      if (f2 == null) {
        f2 = File.createTempFile("expected_", ".xml");
        FileOutputStream fos2 = new FileOutputStream(f2);
        try {
          fos2.write(((String)right).getBytes("UTF-8"));
        } finally {
          fos2.close();
        }

        compareFiles.put(rightKey, f2);
      } 
      
      
      final URL url1 = f1.toURI().toURL();
      final URL url2 = f2.toURI().toURL();
      
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          ((StandalonePluginWorkspace)pluginWorkspace).openDiffFilesApplication(url1, url2);
        }
      });


    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * The bridge will not be used anymore. Dispose any resources kept internally.
   */
  public void dispose() {
    Collection<File> values = compareFiles.values();
    for (File file : values) {
      file.delete();
    }

    compareFiles.clear();
  }
}
