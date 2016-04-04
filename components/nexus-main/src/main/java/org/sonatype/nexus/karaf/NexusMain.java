/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.karaf;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Version;

/**
 * Nexus alternative to Karaf's main launcher which checks the Java version before launching.
 * 
 * @since 3.0
 */
public class NexusMain
    extends org.apache.karaf.main.Main
{

  private static final Version MINIMUM_JAVA_VERSION = new Version(1, 8, 0);

  private static final String KARAF_INSTANCES = "karaf.instances";

  private static final String KARAF_DATA = "karaf.data";

  Logger log = Logger.getLogger(this.getClass().getName());

  public NexusMain(final String[] args) {
    super(args);
  }

  /**
   * Adapted from {@link org.apache.karaf.main.Main#main(String[])} to call our constructor.
   */
  public static void main(final String[] args) throws Exception {
    while (true) {
      boolean restart = false;
      System.setProperty("karaf.restart", "false");
      final NexusMain main = new NexusMain(args);
      try {
        main.launch();
      }
      catch (Throwable ex) {
        // Also log to sytem.err in case logging is not yet initialized
        System.err.println(ex.getMessage());

        main.log.log(Level.SEVERE, "Could not launch framework", ex);
        main.destroy();
        main.setExitCode(-1);
      }
      try {
        main.awaitShutdown();
        boolean stopped = main.destroy();
        restart = Boolean.getBoolean("karaf.restart");
        main.updateInstancePidAfterShutdown();
        if (!stopped) {
          if (restart) {
            System.err.println("Timeout waiting for framework to stop.  Restarting now.");
          }
          else {
            System.err.println("Timeout waiting for framework to stop.  Exiting VM.");
            main.setExitCode(-3);
          }
        }
      }
      catch (Throwable ex) {
        main.setExitCode(-2);
        System.err.println("Error occurred shutting down framework: " + ex);
        ex.printStackTrace();
      }
      finally {
        if (!restart) {
          System.exit(main.getExitCode());
        }
        else {
          System.gc();
        }
      }
    }
  }

  /**
   * Launch method is called by static main as well as Pax-Exam via reflection.
   */
  @Override
  public void launch() throws Exception {
    requireMinimumJavaVersion();

    // ensure karaf.data is set
    String dataDir = System.getProperty(KARAF_DATA);
    if (dataDir == null) {
      throw new RuntimeException("Missing required system-property: " + KARAF_DATA);
    }

    // if karaf.instances is not set, automatically set it under karaf.data
    String instancesDir = System.getProperty(KARAF_INSTANCES);
    if (instancesDir == null) {
      instancesDir = new File(new File(dataDir), "instances").getAbsolutePath();
      System.setProperty(KARAF_INSTANCES, instancesDir);
    }

    log.info("Launching Nexus..."); // temporary logging just to show custom launcher is being used in ITs
    super.launch();
    log.info("...launched Nexus!");
  }

  private static void requireMinimumJavaVersion() {
    String currentVersion = System.getProperty("java.version");
    if (MINIMUM_JAVA_VERSION.compareTo(new Version(currentVersion.replace('_', '.'))) > 0) {
      // logging is not configured yet, so use console
      System.err.println("Nexus requires minimum java.version: " + MINIMUM_JAVA_VERSION);
      System.exit(-1);
    }
  }
}
