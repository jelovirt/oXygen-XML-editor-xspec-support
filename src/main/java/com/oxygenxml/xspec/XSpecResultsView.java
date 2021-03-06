package com.oxygenxml.xspec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.oxygenxml.xspec.XSpecUtil.OperationCanceledException;
import com.oxygenxml.xspec.jfx.BrowserInteractor;
import com.oxygenxml.xspec.jfx.SwingBrowserPanel;
import com.oxygenxml.xspec.jfx.bridge.Bridge;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import ro.sync.exml.workspace.api.editor.transformation.TransformationFeedback;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarToggleButton;

/**
 * A view taht uses a JavaFX WebEngine to present the results of running an XSpec 
 * scenario.   
 * @author alex_jitianu
 */
public class XSpecResultsView extends JPanel {
  /**
   * View ID.
   */
  static final String RESULTS = "com.oxygenxml.xspec.results";
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(XSpecResultsView.class.getName());
  /**
   * A Javascript that will present just the failed tests.
   */
  protected static final String SHOW_ONLY_FAILED_TESTS = "showOnlyFailedTests();";
  /**
   * A Javascript that will present all the tests.
   */
  protected static final String SHOW_ALL_TESTS = "showAllTests();";
  
  static {
    Platform.setImplicitExit(false);
  }
  /**
   * The JavaFX browser used to present the results.
   */
  private SwingBrowserPanel panel;
  /**
   * A bridge between Javascript and Java. XSpec Javascript will call on these methods.
   * 
   * We keep this reference because of:
   * 
   * https://bugs.openjdk.java.net/browse/JDK-8170085
   */
  private Bridge xspecBridge;
  /**
   * Our workspace access.
   */
  private StandalonePluginWorkspace pluginWorkspace;
  /**
   * Currently loaded XSpec.
   */
  private URL xspec;
  /**
   * Show just the tests that failed.
   */
  private JButton runButton;
  
  /**
   * Show just the tests that failed.
   */
  private JButton showFailuresOnly;
  
  /**
   * Constructor.
   * 
   * @param pluginWorkspace Oxygen workspace access.
   */
  public XSpecResultsView(final StandalonePluginWorkspace pluginWorkspace) {
    this.pluginWorkspace = pluginWorkspace;
    panel = new SwingBrowserPanel(new BrowserInteractor() {
      @Override
      public void pageLoaded() {
        if (xspec != null) {
          if (xspecBridge != null) {
            // A little house keeping.
            xspecBridge.dispose();
          }
          // The XSpec results were loaded by the WebEngine.
          // Install the Javascript->Java bridge.
          xspecBridge = Bridge.install(panel.getWebEngine(), XSpecResultsView.this, pluginWorkspace, xspec);
          
          applyTestFilter();
        }
      }
      
      @Override
      public void alert(String message) {
        // A debugging method for the javascript. Redirect Javascript alerts to the console.
        logger.info(message);
      }
    });
    
    panel.loadContent("");
    
    setLayout(new BorderLayout());
    
    JPanel toolbar = new JPanel(new BorderLayout());
    JPanel left = new JPanel();
    
    // Run scenario
    Action runAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        runButton.setEnabled(false);
        try {
          XSpecUtil.runScenario(pluginWorkspace, XSpecResultsView.this, new TransformationFeedback() {
            @Override
            public void transformationStopped() {
              runButton.setEnabled(true);
            }
            @Override
            public void transformationFinished(boolean success) {
              runButton.setEnabled(true);
            }
          });
        } catch (OperationCanceledException e) {
          // canceled by user.
          runButton.setEnabled(true);
        }
      }
    };
    
    ImageIcon runIcon = new ImageIcon(getClass().getClassLoader().getResource("run16.png"));
    runAction.putValue(Action.SMALL_ICON, runIcon);
    runAction.putValue(Action.SHORT_DESCRIPTION, "Run XSpec");
    runButton = new ToolbarButton(runAction, false);
    left.add(runButton);
    
    // Show failures.
    Action showFailuresAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        applyTestFilter();
      }
    };
    
    ImageIcon ic = new ImageIcon(getClass().getClassLoader().getResource("failures.gif"));
    showFailuresAction.putValue(Action.SMALL_ICON, ic);
    showFailuresAction.putValue(Action.SHORT_DESCRIPTION, "Show only failures");
    showFailuresOnly = new ToolbarToggleButton(showFailuresAction);
    left.add(showFailuresOnly);
    
    toolbar.add(left, BorderLayout.WEST);
    add(toolbar, BorderLayout.NORTH);
    
    add(panel, BorderLayout.CENTER);
  }
  
  /**
   * Applies the filtering criteria on the test results.
   */
  private void applyTestFilter() {
    boolean selected = showFailuresOnly.isSelected();
    if (selected) {
      panel.executeScript(SHOW_ONLY_FAILED_TESTS);
    } else {
      panel.executeScript(SHOW_ALL_TESTS);
    }
  }
  
  public void setFilterTests(boolean filter) {
    showFailuresOnly.setSelected(filter);
    
    applyTestFilter();
  }
  
  /**
   * Loads the results of an XSpec script.
   * 
   * @param xspec The executed XSpec.
   * @param results The results.
   */
  public void load(URL xspec, URL results) {
    this.xspec = xspec;
    
    pluginWorkspace.showView(RESULTS, false);
    
    panel.loadURL(results);
  }

  /**
   * Loads the results of an XSpec script.
   * 
   * @param xspec The executed XSpec.
   * @param results The results.
   */
  public void loadContent(String content) {
    this.xspec = null;
    
    panel.loadContent(content);
    
    pluginWorkspace.showView(RESULTS, false);
  }  
  
  /**
   * Dispose.
   */
  public void dispose() {
    if (xspecBridge != null) {
      xspecBridge.dispose();
    }
  }
  
  public URL getXspec() {
    return xspec;
  }
  
  public WebEngine getEngineForTests() {
    return panel.getWebEngine();
  }
}
