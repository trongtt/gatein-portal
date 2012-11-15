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

package org.exoplatform.sample.portal.web;

import org.exoplatform.portal.application.StandaloneApplication;

import javax.servlet.ServletConfig;

/**
 * The PortalApplication class is an implementation of the WebuiApplication abstract class
 * which defines the type of application that can be deployed in our framework (that includes 
 * portal, portlets, widgets...)
 * 
 * This class is a wrapper of all portal information such as ResourceBundle for i18n, the current 
 * ExoContainer in use as well as the init parameters defined along with the servlet web.xml
 */
public class CustomStandaloneApplication extends StandaloneApplication
{
   /**
    * The constructor references resource resolvers that allows the ApplicationResourceResolver to
    * extract files from different locations such as the current war or external one such as the resource 
    * one where several static files are shared among all portal instances.
    * 
    * 
    * @param config, the servlet config that contains init params such as the path location of
    * the XML configuration file for the WebUI framework
    */
   public CustomStandaloneApplication(ServletConfig config) throws Exception
   {
      super(config);
      System.out.println("\n\n =========== It's using CustomStandaloneApplication");
   }
}
