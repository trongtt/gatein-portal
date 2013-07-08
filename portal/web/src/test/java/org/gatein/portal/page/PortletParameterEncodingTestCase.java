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
package org.gatein.portal.page;

import org.gatein.portal.AbstractPortalTestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
@RunWith(Arquillian.class)
public class PortletParameterEncodingTestCase extends AbstractPortalTestCase {

    @Deployment(testable = false)
    public static WebArchive createPortal() {
        WebArchive portal = AbstractPortalTestCase.createPortal();
        portal.addAsWebInfResource(new StringAsset(descriptor(Portlet1.class).exportAsString()), "portlet.xml");
        return portal;
    }

    @ArquillianResource
    URL deploymentURL;

    @Drone
    WebDriver driver;

    static HashMap<String, String[]> result = new HashMap<String, String[]>();

    @Test
    public void testParameterEncoding() throws Exception {

        driver.get(deploymentURL.toString() + "page1");
        driver.findElements(By.name("_name")).get(0).sendKeys("a%a");
        driver.findElements(By.name("_value")).get(0).sendKeys("b%b");
        driver.findElements(By.tagName("button")).get(0).click();

        String[] values = result.get("a%a");
        System.out.println(result.keySet());
        Assert.assertEquals(1, result.keySet().size());
        Assert.assertNotNull(values);
        Assert.assertEquals("b%b", values[0]);

        driver.get(deploymentURL.toString() + "page1");
        driver.findElements(By.name("_name")).get(0).sendKeys("a&a");
        driver.findElements(By.name("_value")).get(0).sendKeys("b&b");
        driver.findElements(By.tagName("button")).get(0).click();
        
        Assert.assertTrue(result.keySet().size() == 1);
        values = result.get("a&a");
        Assert.assertNotNull(values);
        Assert.assertEquals("b&b", values[0]);

        driver.findElements(By.name("_name")).get(0).sendKeys("c");
        driver.findElements(By.name("_value")).get(0).sendKeys("d");
        driver.findElements(By.tagName("button")).get(0).click();

        Assert.assertTrue(result.keySet().size() == 2);
        values = result.get("a&a");
        Assert.assertNotNull(values);
        Assert.assertEquals("b&b", values[0]);
        values = result.get("c");
        Assert.assertNotNull(values);
        Assert.assertEquals("d", values[0]);
    }

    public static class Portlet1 extends GenericPortlet {
        @Override
        public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
            HashMap<String, String[]> p = new HashMap<String, String[]>(request.getParameterMap());
            for (Iterator<String> i = p.keySet().iterator();i.hasNext();) {
                String s = i.next();
                if (s.startsWith("_")) {
                    i.remove();
                }
            }

            String name = request.getParameter("_name");
            String value = request.getParameter("_value");
            if (name != null && value != null) {
                String[] values = p.get(name);
                if (values == null) {
                    values = new String[] { value };
                } else {
                    values = Arrays.copyOf(values, values.length + 1);
                    values[values.length - 1] = value;
                }
                p.put(name, values);
            } else {
                throw new PortletException("Invalid request");
            }

            for (Map.Entry<String, String[]> parameter : p.entrySet()) {
                response.setRenderParameter(parameter.getKey(), parameter.getValue());
            }
        }

        @Override
        public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            result = new HashMap<String, String[]>(request.getParameterMap());

            PortletURL addURL = response.createActionURL();
            addURL.setParameters(request.getPrivateParameterMap());
            out.append("<form action='").append(addURL.toString()).append("' method='POST' class='well form-inline'>\n");
            out.append("<input type='text' name='_name' class='input-small' placeholder='Name'>\n");
            out.append("<input type='text' name='_value' class='input-small' placeholder='Value'>\n");
            out.append("<button type='submit' class='btn btn-primary'>Add</button>\n");
            out.append("</form>");

            //
            out.close();
        }
    }
}