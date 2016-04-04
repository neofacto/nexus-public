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
package org.sonatype.nexus.repository.storage;

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.common.concur.ONeedRetryException;

/**
 * A component maintenance facet that assumes that Components have the same lifecycle as their
 * single Assets.
 *
 * @since 3.0
 */
@Named
public class SingleAssetComponentMaintenance
    extends DefaultComponentMaintenanceImpl
{
  /**
   * Deletes both the asset and its component.
   */
  @Transactional(retryOn = ONeedRetryException.class)
  protected void deleteAssetTx(final EntityId assetId) {
    StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = tx.findAsset(assetId, tx.findBucket(getRepository()));
    if (asset == null) {
      return;
    }
    final EntityId componentId = asset.componentId();
    if (componentId == null) {
      // Assets without components should be deleted on their own
      super.deleteAssetTx(assetId);
    }
    else {
      // Otherwise, delete the component, which in turn cascades down to the asset
      deleteComponentTx(componentId);
    }
  }
}
