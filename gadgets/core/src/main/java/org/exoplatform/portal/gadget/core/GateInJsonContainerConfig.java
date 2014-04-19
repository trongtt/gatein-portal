/**
 * Copyright (C) 2009 eXo Platform SAS.
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

import java.util.Iterator;
import java.util.Map;

import org.apache.shindig.common.Nullable;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.ContainerConfigException;
import org.apache.shindig.config.ExpressionContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.exoplatform.container.RootContainer;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * A fork of the class JsonContainerConfig, designed to override the loading of container.js User: Minh Hoang TO -
 * hoang281283@gmail.com Date: 1/10/11 Time: 2:12 PM
 */
@Singleton
public class GateInJsonContainerConfig extends ExpressionContainerConfig {
    /**
     * Creates a new configuration from files.
     *
     * @throws ContainerConfigException
     */
    @Inject
    public GateInJsonContainerConfig(@Named("shindig.containers.default") String containers,
            @Nullable @Named("shindig.host") String host, @Nullable @Named("shindig.port") String port,
            @Nullable @Named("shindig.contextroot") String contextRoot, Expressions expressions)
            throws ContainerConfigException {
        super(expressions);
        JsonContainerConfigLoader.getTransactionFromFile(containers, host, port, contextRoot, this).commit();
    }

    /**
     * Creates a new configuration from a JSON Object, for use in testing.
     */
    public GateInJsonContainerConfig(JSONObject json, Expressions expressions) throws ContainerConfigException {
        super(expressions);
        Transaction transaction = newTransaction();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            JSONObject optJSONObject = json.optJSONObject((String) keys.next());
            if (optJSONObject != null) {
                transaction.addContainer(JsonContainerConfigLoader.parseJsonContainer(optJSONObject));
            }
        }
        transaction.commit();
    }

    @Override
    public Map<String, Object> getProperties(String container) {
        Map<String, Object> pros = config.get(container);
        if (pros == null) {
            if (RootContainer.getInstance().getPortalContainer(container) != null) {
                pros = config.get(ContainerConfig.DEFAULT_CONTAINER);
            }
        }
        return pros;
    }

    @Override
    public Object getProperty(String container, String property) {
        Object val = super.getProperty(container, property);

        if (val == null) {
            Map<String, Object> containerData = getProperties(container);
            if (containerData != null) {
                val = containerData.get(property);
            }
        }
        return val;
    }
}
