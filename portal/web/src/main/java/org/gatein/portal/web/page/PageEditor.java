/*
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

package org.gatein.portal.web.page;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import juzu.Param;
import juzu.Resource;
import juzu.Response;
import juzu.Route;
import juzu.impl.common.JSON;
import juzu.impl.common.Tools;
import juzu.impl.request.Request;
import juzu.request.ClientContext;
import juzu.request.ResourceContext;
import org.gatein.pc.api.Portlet;
import org.gatein.pc.api.info.MetaInfo;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.portal.mop.customization.CustomizationService;
import org.gatein.portal.mop.hierarchy.NodeContext;
import org.gatein.portal.mop.layout.ElementState;
import org.gatein.portal.mop.layout.LayoutService;
import org.gatein.portal.mop.navigation.NavigationService;
import org.gatein.portal.mop.page.PageKey;
import org.gatein.portal.mop.page.PageService;
import org.gatein.portal.web.layout.RenderingContext;
import org.gatein.portal.web.layout.ZoneLayout;
import org.gatein.portal.web.layout.ZoneLayoutFactory;
import org.gatein.portal.web.page.spi.RenderTask;
import org.gatein.portal.web.page.spi.WindowContent;
import org.gatein.portal.web.page.spi.portlet.PortletContentProvider;
import org.gatein.portal.web.portlet.PortletAppManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class PageEditor {

    @Inject
    NavigationService navigationService;

    @Inject
    PageService pageService;

    @Inject
    LayoutService layoutService;

    @Inject
    CustomizationService customizationService;

    @Inject
    PortletContentProvider contentProvider;

    @Inject
    ZoneLayoutFactory layoutFactory;

    @Inject
    PortletAppManager portletAppManager;

    @Resource
    @Route(value = "/switchto/{javax.portlet.z}")
    public Response switchLayout(@Param(name = "javax.portlet.z") String id) throws Exception {
        ZoneLayout layout = (ZoneLayout) layoutFactory.builder(id).build();
        StringBuilder sb = new StringBuilder();
        layout.render(new RenderingContext(null, null, null, true), Collections.<String, Result.Fragment>emptyMap(), null, null, sb);

        JSON data = new JSON();
        data.set("factoryId", id);
        data.set("html", sb.toString());

        return Response.status(200).body(data.toString());
    }
    
    @Resource
    @Route(value = "/upload")
    public Response upload(ClientContext context) throws Exception {
        return Response.status(200).body("uploaded");
    }

    @Resource
    @Route(value = "/contents")
    public Response getAllContents() throws Exception {
        JSONArray result = new JSONArray();
        Set<Portlet> portlets = this.portletAppManager.getAllPortlets();
        for (Portlet portlet : portlets) {
            PortletInfo info = portlet.getInfo();
            MetaInfo meta = info.getMeta();

            JSONObject item = new JSONObject();
            item.put("contentId", info.getApplicationName() + "/" + info.getName());
            item.put("contentType", "portlet");
            item.put("title", meta.getMetaValue("title").getDefaultString());

            result.put(item);
        }
        return Response.status(200).body(result.toString());
    }


    @Resource
    @Route(value = "/savelayout/{javax.portlet.layoutid}")
    public Response saveLayout(ResourceContext context, @Param(name = "javax.portlet.layoutid") String layoutId) throws Exception {
        NodeContext<JSONObject, ElementState> pageStructure = null;
        JSONObject requestData = null;

        pageStructure = (NodeContext<JSONObject, ElementState>) layoutService.loadLayout(ElementState.model(), layoutId, null);
        requestData = getRequestData(context);

        JSON result = new JSON();
        if(requestData != null && pageStructure != null) {
            org.exoplatform.portal.pom.data.JSONContainerAdapter adapter = new org.exoplatform.portal.pom.data.JSONContainerAdapter(requestData, pageStructure);

            layoutService.saveLayout(adapter, requestData, pageStructure, null);

            //Update layout
            String factoryId = requestData.getString("factoryId");
            String pageKey = requestData.getString("pageKey");
            if (factoryId != null && pageKey != null && !factoryId.isEmpty() && !pageKey.isEmpty()) {
                PageKey key = PageKey.parse(pageKey);
                org.gatein.portal.mop.page.PageContext page = pageService.loadPage(key);
                page.setState(page.getState().builder().factoryId(factoryId).build());
                pageService.savePage(page);
            }

            return Response.status(200).body(result.toString()).withCharset(Charset.forName("UTF-8")).withMimeType("application/json");

        } else if(pageStructure== null) {
            return Response.notFound("Can not edit because can not load layout with id " + layoutId);

        } else {
            return Response.status(400).body("Data is null");
        }
    }

    /**
     * Temporary implement to render portlet content without full page context
     */
    @Resource
    @Route(value = "/getContent")
    //TODO: the contentType is not used for now
    public Response getContent(@Param(name = "javax.portlet.contentId") String contentId, @Param(name = "javax.portlet.contentType") String contentType, @Param(name = "javax.portlet.path") String path) {
        WindowContent content = contentProvider.getContent(contentId);
        PageContext.Builder pageBuilder = new PageContext.Builder(contentProvider, customizationService, path);
        RenderTask task = contentProvider.createRender(new WindowContext("", content , pageBuilder.build()));
        Result result = task.execute(Request.getCurrent().getUserContext().getLocale());
        if (result instanceof Result.Fragment) {
            Result.Fragment fragment = (Result.Fragment) result;

            //200 OK
            return Response.status(200).body(new JSON().set("title", fragment.title).set("content", fragment.content).toString()).withCharset(Charset.forName("UTF-8"))
                    .withMimeType("application/json");
        } else {
            //501 Not Implemented
            return Response.status(501).body("Not yet handled " + result).withCharset(Charset.forName("UTF-8"))
                    .withMimeType("application/json");
        }
    }

    private JSONObject getRequestData(ResourceContext context) throws Exception {
        InputStream content = context.getClientContext().getInputStream();

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            if (content != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(content));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return stringBuilder.length() > 0 ? new JSONObject(stringBuilder.toString()) : null;
    }
}
