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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import juzu.Param;
import juzu.Resource;
import juzu.Response;
import juzu.Route;
import juzu.impl.common.JSON;
import juzu.impl.common.Tools;
import juzu.impl.request.Request;
import juzu.request.ClientContext;
import juzu.request.RequestContext;
import juzu.request.UserContext;
import org.gatein.portal.content.ContentDescription;
import org.gatein.portal.content.ContentProvider;
import org.gatein.portal.content.ContentType;
import org.gatein.portal.content.ProviderRegistry;
import org.gatein.portal.content.Result;
import org.gatein.portal.mop.description.DescriptionService;
import org.gatein.portal.mop.description.DescriptionState;
import org.gatein.portal.mop.hierarchy.GenericScope;
import org.gatein.portal.mop.hierarchy.NodeContext;
import org.gatein.portal.mop.hierarchy.Scope;
import org.gatein.portal.mop.layout.ElementState;
import org.gatein.portal.mop.layout.LayoutService;
import org.gatein.portal.mop.navigation.NavigationContext;
import org.gatein.portal.mop.navigation.NavigationService;
import org.gatein.portal.mop.navigation.NodeState;
import org.gatein.portal.mop.page.PageKey;
import org.gatein.portal.mop.page.PageService;
import org.gatein.portal.mop.page.PageState;
import org.gatein.portal.mop.permission.SecurityService;
import org.gatein.portal.mop.permission.SecurityState;
import org.gatein.portal.mop.site.SiteKey;
import org.gatein.portal.ui.navigation.UserNode;
import org.gatein.portal.web.layout.RenderingContext;
import org.gatein.portal.web.layout.ZoneLayout;
import org.gatein.portal.web.layout.ZoneLayoutFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PageEditor {

    @Inject
    PageService pageService;

    @Inject
    LayoutService layoutService;

    @Inject
    ZoneLayoutFactory layoutFactory;

    @Inject
    ProviderRegistry providers;
    
    @Inject
    NavigationService navigationService;

    @Inject
    DescriptionService descriptionService;

    @Inject
    SecurityService securityService;
    
    @Resource
    @Route(value = "/parentLinks")
    public Response getParentLinks(UserContext userContext) {
        NavigationContext navigation = navigationService.loadNavigation(SiteKey.portal("classic"));
        UserNode.Model model = new UserNode.Model(descriptionService, userContext.getLocale());
        NodeContext<UserNode, NodeState> root = navigationService.loadNode(model, navigation, Scope.ALL, null);
        List<String> holder = new ArrayList<String>();
        collectPaths(root, holder);
        JSON data = new JSON();
        data.set("parentLinks", holder);
        return Response.status(200).body(data.toString());
    }
    private List<String> collectPaths(NodeContext<UserNode, NodeState> context, List<String> holder) {
        UserNode userNode = context.getNode();
        holder.add(userNode.getLink());
        for (Iterator<NodeContext<UserNode, NodeState>> i = context.iterator(); i.hasNext();) {
            collectPaths(i.next(), holder);
        }
        return holder;
    }
    
    @Resource
    @Route(value = "/checkPage")
    public Response checkPage(String pageName) {
        PageKey pageKey = new PageKey(SiteKey.portal("classic"), pageName);
        org.gatein.portal.mop.page.PageContext pageContext = pageService.loadPage(pageKey);
        JSON data = new JSON();
        data.set("pageExisted", pageContext != null ? true : false);
        return Response.status(200).body(data.toString());
    }
    
    @Resource
    @Route(value = "/switchto/{javax.portlet.z}")
    public Response switchLayout(@Param(name = "javax.portlet.z") String id) throws Exception {
        ZoneLayout layout = (ZoneLayout) layoutFactory.builder(id).build();
        StringBuilder sb = new StringBuilder();
        layout.render(new RenderingContext(), Collections.<String, Result.Fragment>emptyMap(), null, null, sb);

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
        for (ContentProvider<?> provider : providers.getProviders()) {
            JSONObject contentType = new JSONObject();
            ContentType type = provider.getContentType();
            contentType.put("value", type.getValue());
            contentType.put("tagName", type.getTagName());
            contentType.put("displayName", type.getTagName());
            JSONArray contents = new JSONArray();

            Iterable<ContentDescription> descriptions = provider.findContents("", 0, 30);
            for (ContentDescription description : descriptions) {
                JSONObject item = new JSONObject();
                item.put("contentId", description.id);
                item.put("contentType", type.getValue());
                item.put("title", description.displayName);
                item.put("description", description.markup);
                //result.put(item);
                contents.put(item);
            }
            contentType.put("contents", contents);
            result.put(contentType);
        }
        return Response.status(200).body(result.toString());
    }

    private org.gatein.portal.mop.page.PageContext createPage(PageKey pageKey, String pageDisplayName, String parent, String factoryId, UserContext userContext) {
        parent = parent.substring("/portal".length());
        pageDisplayName = pageDisplayName != null && !pageDisplayName.isEmpty() ? pageDisplayName : pageKey.getName();
        // Parse path
        List<String> names = new ArrayList<String>();
        for (String name : Tools.split(parent, '/')) {
            if (name.length() > 0) {
                names.add(name);
            }
        }

        NavigationContext navigation = navigationService.loadNavigation(SiteKey.portal("classic"));
        NodeContext<?, NodeState> root =  navigationService.loadNode(NodeState.model(), navigation, GenericScope.branchShape(names), null);
        // Get our node from the navigation
        NodeContext<?, NodeState> current = root;
        for (String name : names) {
            current = current.get(name);
            if (current == null) {
                break;
            }
        }

        //
        NodeContext<?, NodeState> pageNode = root.get(pageKey.getName());
        if (pageNode == null) {
            pageNode = current.add(null, pageKey.getName(), new NodeState.Builder().pageRef(pageKey).build());
        } else {
            NodeState sate = pageNode.getState();
            pageNode.setState(new NodeState.Builder(sate).pageRef(pageKey).build());
        }
        navigationService.saveNode(pageNode, null);
        descriptionService.saveDescription(pageNode.getId(), userContext.getLocale(), new DescriptionState(pageDisplayName, null));
        org.gatein.portal.mop.page.PageContext page = new org.gatein.portal.mop.page.PageContext(
                pageKey, 
                new PageState.Builder().factoryId(factoryId).displayName(pageDisplayName).build());

        pageService.savePage(page);
        return page;
    }

    @Resource
    @Route(value = "/savelayout/{javax.portlet.layoutid}")
    public Response saveLayout(RequestContext context, @Param(name = "javax.portlet.layoutid") String layoutId) throws Exception {
        JSONObject requestData = getRequestData(context);
        JSON result = new JSON();
        
        PageKey pageKey = PageKey.parse(requestData.getString("pageKey"));
        String pageDisplayName = requestData.getString("pageDisplayName");
        String parent = requestData.getString("parentLink");
        String factoryId = requestData.getString("factoryId");
        org.gatein.portal.mop.page.PageContext pageContext = createPage(pageKey, pageDisplayName, parent, factoryId, Request.getCurrent().getUserContext());
        
        if ("newpage".equals(layoutId)) {
            result.set("redirect", parent + "/" + pageKey.getName());
            layoutId = pageContext.getLayoutId();
        }
        
        NodeContext<JSONObject, ElementState> pageStructure = buildPageStructure(layoutId, requestData);
        if(requestData != null && pageStructure != null) {
            return Response.status(200).body(result.toString()).withCharset(Charset.forName("UTF-8")).withMimeType("application/json");

        } else if(pageStructure== null) {
            return Response.notFound("Can not edit because can not load layout with id " + layoutId);

        } else {
            return Response.status(400).body("Data is null");
        }
    }
    
    private NodeContext<JSONObject, ElementState> buildPageStructure(String layoutId, JSONObject requestData) throws JSONException {
        NodeContext<JSONObject, ElementState> pageStructure = null;
        pageStructure = (NodeContext<JSONObject, ElementState>) layoutService.loadLayout(ElementState.model(), layoutId, null);

        if(requestData != null && pageStructure != null) {
            org.exoplatform.portal.pom.data.JSONContainerAdapter adapter = new org.exoplatform.portal.pom.data.JSONContainerAdapter(requestData, pageStructure);

            layoutService.saveLayout(adapter, requestData, pageStructure, null);

            String pageKey = requestData.getString("pageKey");
            PageKey key = PageKey.parse(pageKey);
            org.gatein.portal.mop.page.PageContext page = pageService.loadPage(key);
            String pageId = page.getData().id;

            //Update layout
            String factoryId = requestData.getString("factoryId");
            String pageDisplayName = requestData.getString("pageDisplayName");
            if (factoryId != null && pageKey != null && !factoryId.isEmpty() && !pageKey.isEmpty()) {
                page.setState(page.getState().builder().factoryId(factoryId).displayName(pageDisplayName).build());
                pageService.savePage(page);
            }

            //save permission for page
            JSONArray accessPermissions = requestData.getJSONArray("accessPermissions");
            String[] accessPerms = new String[accessPermissions.length()];
            for(int i = 0; i < accessPermissions.length(); i++) {
                accessPerms[i] = accessPermissions.getString(i);
            }

            JSONArray editPermissions = requestData.getJSONArray("editPermissions");
            String[] editPerms = new String[editPermissions.length()];
            for(int i = 0; i < editPermissions.length(); i++) {
                editPerms[i] = editPermissions.getString(i);
            }

            SecurityState securityState = new SecurityState(accessPerms, editPerms);
            securityService.savePermission(pageId, securityState);
        }
        return pageStructure;
    }

    private JSONObject getRequestData(RequestContext context) throws Exception {
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
