/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.juzu.pages;

import javax.inject.Named;

import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.webui.page.PageQueryAccessList;
import juzu.SessionScoped;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Hai Thanh Nguyen</a>
 * @version $Id$
 *
 */
@Named("session")
@SessionScoped
public class Session
{
   private Query<Page> query;
   
   private PageQueryAccessList listAccess;
   
   public void setQuery(Query<Page> query)
   {
      this.query = query;
   }
   
   public Query<Page> getQuery() {
      return query;
   }
   
   public void setListAccess(PageQueryAccessList listAccess) {
      this.listAccess = listAccess;
   }
   
   public PageQueryAccessList getListAccess() {
      return listAccess;
   }
}
