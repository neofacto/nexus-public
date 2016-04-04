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
package org.sonatype.nexus.internal.httpclient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.httpclient.HttpClientConfigurationStore;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;

/**
 * Orient {@link HttpClientConfigurationStore}.
 *
 * @since 3.0
 */
@Named("orient")
@ManagedLifecycle(phase = STORAGE)
@Singleton
public class OrientHttpClientConfigurationStore
  extends LifecycleSupport
  implements HttpClientConfigurationStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final HttpClientConfigurationEntityAdapter entityAdapter;

  @Inject
  public OrientHttpClientConfigurationStore(@Named("config") final Provider<DatabaseInstance> databaseInstance,
                                            final HttpClientConfigurationEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  protected void doStart() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  private ODatabaseDocumentTx openDb() {
    ensureStarted();
    return databaseInstance.get().acquire();
  }

  @Override
  @Nullable
  public HttpClientConfiguration load() {
    try (ODatabaseDocumentTx db = openDb()) {
      return entityAdapter.singleton.get(db);
    }
  }

  @Override
  public void save(final HttpClientConfiguration configuration) {
    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.singleton.set(db, configuration);
    }
  }
}
