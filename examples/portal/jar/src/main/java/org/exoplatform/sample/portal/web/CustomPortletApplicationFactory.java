package org.exoplatform.sample.portal.web;

import org.exoplatform.webui.application.portlet.PortletApplication;
import org.exoplatform.webui.application.portlet.PortletApplicationFactory;

import javax.portlet.PortletConfig;

public class CustomPortletApplicationFactory implements PortletApplicationFactory
{
   public PortletApplication createApplication(PortletConfig portletConfig)
   {
      try
      {
         return new CustomPortletApplication(portletConfig);
      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return null;
   }
}

