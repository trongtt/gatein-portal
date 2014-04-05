/**
 * Copyright (C) 2014 eXo Platform SAS.
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

package org.exoplatform.portal.gadget.core;

import org.apache.shindig.gadgets.admin.BasicGadgetAdminStore;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Module to load the gadget administration information.
 *
 * @version $Id: $
 */
public class GateInGadgetAdminModule extends AbstractModule {

  private static final String GADGET_ADMIN_CONFIG = "config/gadget-admin.json";
  private static final Log LOG = ExoLogger.getLogger(GateInGadgetAdminModule.class);

  @Override
  protected void configure() {
    bind(GadgetAdminStore.class).toProvider(GateInGadgetAdminStoreProvider.class);
  }

  @Singleton
  public static class GateInGadgetAdminStoreProvider implements Provider<GadgetAdminStore> {
    private BasicGadgetAdminStore store;

    @Inject
    public GateInGadgetAdminStoreProvider(BasicGadgetAdminStore store) {
      this.store = store;
      loadStore();
    }

    private void loadStore() {
      try {
        GateInContainerConfigLoader currentLoader = GateInGuiceServletContextListener.getCurrentLoader();
        String gadgetAdminString = currentLoader.loadContentAsString(GADGET_ADMIN_CONFIG, "UTF-8");
        this.store.init(gadgetAdminString);
      } catch (Throwable t) {
        LOG.error("Can't init gadget admin store", t);
      }
    }

    public GadgetAdminStore get() {
      return store;
    }
  }
}