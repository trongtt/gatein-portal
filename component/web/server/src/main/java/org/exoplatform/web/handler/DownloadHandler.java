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

package org.exoplatform.web.handler;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SARL
 * Author : LeBienThuy
 * thuy.le@exoplatform.com
 * Dec 9, 2006
 */
public class DownloadHandler extends WebRequestHandler {

    public String getHandlerName() {
        return "download";
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        execute(context.getController(), context.getRequest(), context.getResponse());
        return true;
    }

    public void execute(WebAppController controller, HttpServletRequest req, HttpServletResponse res) throws Exception {
        String resourceId = req.getParameter("resourceId");
        res.setHeader("Cache-Control", "private max-age=600, s-maxage=120");
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        DownloadService dservice = (DownloadService) container.getComponentInstanceOfType(DownloadService.class);
        DownloadResource dresource = dservice.getDownloadResource(resourceId);
        if (dresource == null) {
            res.setContentType("text/plain");
            res.getWriter().write("NO DOWNDLOAD RESOURCE CONTENT  OR YOU DO NOT HAVE THE RIGHT TO ACCESS THE CONTENT");
            return;
        }
        String userAgent = req.getHeader("User-Agent");
        if (dresource.getDownloadName() != null) {
            if (userAgent != null && userAgent.contains("Firefox")) {
                res.setHeader("Content-Disposition", "attachment; filename*=utf-8''"
                        + URLEncoder.encode(dresource.getDownloadName(), "UTF-8") + "");
            } else {
                res.setHeader("Content-Disposition", "attachment;filename=\""
                        + URLEncoder.encode(dresource.getDownloadName(), "UTF-8") + "\"");
            }
        }
        res.setContentType(dresource.getResourceMimeType());
        dresource.write(res.getOutputStream());
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return false;
    }
}
