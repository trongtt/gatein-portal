/**
 * Copyright (C) 2013 eXo Platform SAS.
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

package org.exoplatform.services.organization.idm;

import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.AttributesManager;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.SortOrder;
import org.picketlink.idm.api.Transaction;
import org.picketlink.idm.api.User;
import org.picketlink.idm.api.query.UserQuery;
import org.picketlink.idm.api.query.UserQueryBuilder;
import org.picketlink.idm.impl.api.SimpleAttribute;
import org.picocontainer.Startable;

public class DisabledUserMigrationService implements Startable {
    private PicketLinkIDMServiceImpl idmService_;

    private boolean enableAllUserDuringBoot;
    private int batch;

    private static final Log log = ExoLogger.getExoLogger(DisabledUserMigrationService.class);

    public DisabledUserMigrationService(InitParams initParams, PicketLinkIDMService idmService, OrganizationService orgService) {
        this.idmService_ = (PicketLinkIDMServiceImpl) idmService;

        this.enableAllUserDuringBoot = Boolean.parseBoolean(initParams.getValueParam("enableAllUserDuringBoot").getValue());
        String b = initParams.getValueParam("batch").getValue();
        this.batch = Integer.parseInt(b == null ? "100" : b);
    }

    public void enableAll() throws Exception {
        final long startTime = System.currentTimeMillis();
        startTransaction();
        final int size = idmService_.getIdentitySession().getPersistenceManager().getUserCount();

        log.info("Starting enable for {} users", size);
        int first = 0;
        int length = batch > size ? size : batch;

        while (length > 0) {
            try {
                log.info("enable for user from {} to {}",first, (first + length));
                startTransaction();
                UserQueryBuilder qb = idmService_.getIdentitySession().createUserQueryBuilder();
                List<User> users = load(qb, first, length);
                for (User user : users) {
                    setEnabled(user.getId(), true, true);
                }
                endTransaction();
            } catch (Exception e) {
                recoverFromIDMError(e);
                break;
            }
            first = first + batch;
            length = batch + first > size ? size - first : batch;
        }

        log.info("Finish enable all user in : {}ms", (System.currentTimeMillis() - startTime));
    }

    private List<org.picketlink.idm.api.User> load(UserQueryBuilder qb, int index, int length) throws Exception {
        qb.sort(SortOrder.ASCENDING).page(index, length);
        UserQuery query = qb.createQuery();
        return idmService_.getIdentitySession().list(query);
    }

    public void setEnabled(String userName, boolean enabled, boolean broadcast) throws Exception {
        Attribute[] attrs = new Attribute[] { new SimpleAttribute(UserDAOImpl.USER_ENABLED, String.valueOf(enabled)) };

        IdentitySession session = idmService_.getIdentitySession();
        AttributesManager am = session.getAttributesManager();
        am.updateAttributes(userName, attrs);
    }

    @Override
    public void start() {
        if (enableAllUserDuringBoot) {
            log.info("Going to enable all users");
            try {
                enableAll();
            } catch (Exception e) {
                log.error(e);
            }
        } else {
            log.info("Skipped enable all users");
        }
    }

    @Override
    public void stop() {
    }

    public void startTransaction() throws Exception {
        if (!idmService_.getIdentitySession().getTransaction().isActive()) {
            idmService_.getIdentitySession().beginTransaction();
        }
    }

    public void endTransaction() throws Exception {
        if (idmService_.getIdentitySession().getTransaction().isActive()) {
            idmService_.getIdentitySession().getTransaction().commit();
        }
    }

    public void recoverFromIDMError(Exception e) {
        log.error(e);
        try {
            // We need to restart Hibernate transaction if it's available. First rollback old one and then start new one
            Transaction idmTransaction = idmService_.getIdentitySession().getTransaction();
            if (idmTransaction.isActive()) {
                idmTransaction.rollback();
                idmTransaction.start();
                log.info("IDM error recovery finished. Old transaction has been rolled-back and new transaction has been started");
            }
        } catch (Exception e1) {
            log.warn("Error during recovery of old error", e1);
        }
    }
}
