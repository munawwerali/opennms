//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2006 Apr 25: Added support for configuring path outages
// 2006 Apr 05: Modifed replaceNotifications to preserve notice order.
// 2004 Jun 03: Modified to allow rules other than IPADDR IPLIKE.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//

package org.opennms.web.admin.notification.noticeWizard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.resource.Vault;
import org.opennms.netmgt.config.NotificationFactory;
import org.opennms.netmgt.config.notifications.Notification;
import org.opennms.netmgt.config.notifications.Parameter;
import org.opennms.netmgt.config.notifications.Varbind;
import org.opennms.netmgt.filter.FilterDaoFactory;
import org.opennms.netmgt.filter.FilterParseException;
import org.opennms.web.Util;

/**
 * A servlet that handles the data comming in from the notification wizard jsps.
 * 
 * @author <A HREF="mailto:jason@opennms.org">Jason Johns </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
public class NotificationWizardServlet extends HttpServlet {
   
    //SOURCE_PAGE_EVENTS_VIEW is more of a tag than an actual page - can't be used for navigation as is
    public static final String SOURCE_PAGE_OTHER_WEBUI = "eventslist";
    
    public static final String SOURCE_PAGE_NOTICES = "eventNotices.jsp";
    
    public static final String SOURCE_PAGE_NOTIFS_FOR_UEI = "notifsForUEI.jsp";

    public static final String SOURCE_PAGE_UEIS = "chooseUeis.jsp";

    public static final String SOURCE_PAGE_RULE = "buildRule.jsp";

    public static final String SOURCE_PAGE_VALIDATE = "validateRule.jsp";

    public static final String SOURCE_PAGE_PATH_OUTAGE = "buildPathOutage.jsp";

    public static final String SOURCE_PAGE_VALIDATE_PATH_OUTAGE = "validatePathOutage.jsp";

    public static final String SOURCE_PAGE_PATH = "choosePath.jsp";

    public static final String SOURCE_PAGE_NOTIFICATION_INDEX = "../index.jsp";

    private static final String SQL_DELETE_CRITICAL_PATH = "DELETE FROM pathoutage WHERE nodeid=?";

    private static final String SQL_SET_CRITICAL_PATH = "INSERT INTO pathoutage (nodeid, criticalpathip, criticalpathservicename) VALUES (?, ?, ?)";

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sourcePage = request.getParameter("sourcePage");
        HttpSession user = request.getSession(true);

        StringBuffer rule = new StringBuffer("");
        StringBuffer redirectString = new StringBuffer();
        
        try {
            NotificationFactory.init();
        } catch (Exception e) {
            throw new ServletException("Failed to initialize NotificationFactory:",e);
        }

        if (sourcePage.equals(SOURCE_PAGE_NOTICES)) {
            String userAction = request.getParameter("userAction");

            if (userAction.equals("delete")) {
                try {
                    NotificationFactory.getInstance().removeNotification(request.getParameter("notice"));
                    redirectString.append(SOURCE_PAGE_NOTICES);
                } catch (Exception e) {
                    throw new ServletException("Couldn't save/reload notifications configuration file.", e);
                }
            } else if (userAction.equals("edit")) {
                edit(request, user, redirectString);
            } else if (userAction.equals("new")) {
                Notification newNotice = new Notification();
                newNotice.setRule("IPADDR IPLIKE *.*.*.*");
                newNotice.setNumericMessage("111-%noticeid%");
                newNotice.setSubject("Notice #%noticeid%");
                newNotice.setStatus("off");

                user.setAttribute("newNotice", newNotice);

                redirectString.append(SOURCE_PAGE_UEIS);
            } else if (userAction.equals("on") || userAction.equals("off")) {
                try {
                    NotificationFactory.getInstance().updateStatus(request.getParameter("notice"), userAction);
                    redirectString.append(SOURCE_PAGE_NOTICES);
                } catch (Exception e) {
                    throw new ServletException("Couldn't save/reload notifications configuration file.", e);
                }
            }
        } else if (sourcePage.equals(SOURCE_PAGE_UEIS)) {
            Notification newNotice = (Notification) user.getAttribute("newNotice");
            newNotice.setUei(request.getParameter("uei"));

            Map params = new HashMap();
            rule.append(newNotice.getRule());
            rule = toSingleQuote(rule);
            params.put("newRule", rule.toString());

            redirectString.append(SOURCE_PAGE_RULE).append(makeQueryString(params));
        } else if (sourcePage.equals(SOURCE_PAGE_RULE)) {
            rule.append(request.getParameter("newRule"));
            rule = toSingleQuote(rule);
            rule = stripExtraWhite(rule.toString());
            rule = stripServices(rule.toString());
            rule = checkParens(rule);

            String services[] = request.getParameterValues("services");
            if (services != null) {
                rule.append(" & ").append(" (");

                for (int i = 0; i < services.length; i++) {
                    rule.append("is").append(services[i]);
                    if (i < services.length - 1)
                        rule.append(" | ");
                }

                rule.append(" )");
            }

            String notServices[] = request.getParameterValues("notServices");
            if (notServices != null) {
                rule.append(" & ").append(" (");

                for (int i = 0; i < notServices.length; i++) {
                    rule.append("!is").append(notServices[i]);
                    if (i < notServices.length - 1)
                        rule.append(" & ");
                }

                rule.append(" )");
            }

            Map params = new HashMap();
            params.put("newRule", rule.toString());
            if (services != null) {
                params.put("services", services);
            }
            if (notServices != null) {
                params.put("notServices", notServices);
            }

            // page to redirect to, either validate or skip validation
            String redirectPage = request.getParameter("nextPage");

            // now lets see if the rule is syntactically valid
            try {
                FilterDaoFactory.getInstance().validateRule(rule.toString());
            } catch (FilterParseException e) {
                // page to redirect to if the rule is invalid
                params.put("mode", "failed");
                redirectPage = SOURCE_PAGE_RULE;
            }

            // save the rule if we are bypassing validation
            if (redirectPage.equals(SOURCE_PAGE_PATH)) {
                Notification newNotice = (Notification) user.getAttribute("newNotice");
                newNotice.setRule(rule.toString());
            }

            redirectString.append(redirectPage).append(makeQueryString(params));
        } else if (sourcePage.equals(SOURCE_PAGE_VALIDATE)) {
            String userAction = request.getParameter("userAction");

            if (userAction.equals("rebuild")) {
                Map params = new HashMap();
                params.put("newRule", request.getParameter("newRule"));
                String services[] = request.getParameterValues("services");
                if (services != null)
                    params.put("services", services);
                params.put("mode", "rebuild");

                redirectString.append(SOURCE_PAGE_RULE).append(makeQueryString(params));
            } else {
                Notification newNotice = (Notification) user.getAttribute("newNotice");
                newNotice.setRule(request.getParameter("newRule"));

                redirectString.append(SOURCE_PAGE_PATH);
            }
        } else if (sourcePage.equals(SOURCE_PAGE_PATH)) {
            Notification newNotice = (Notification) user.getAttribute("newNotice");
            newNotice.setDestinationPath(request.getParameter("path"));

            String description = request.getParameter("description");
            if (description != null && !description.trim().equals(""))
                newNotice.setDescription(description);
            else
                newNotice.setDescription(null);

            newNotice.setTextMessage(request.getParameter("textMsg"));

            String subject = request.getParameter("subject");
            if (subject != null && !subject.trim().equals(""))
                newNotice.setSubject(subject);
            else
                newNotice.setSubject(null);

            String numMessage = request.getParameter("numMsg");
            if (numMessage != null && !numMessage.trim().equals(""))
                newNotice.setNumericMessage(numMessage);
            else
                newNotice.setNumericMessage(null);

            String oldName = newNotice.getName();
            newNotice.setName(request.getParameter("name"));

            String varbindName=request.getParameter("varbindName");
            String varbindValue=request.getParameter("varbindValue");
            
            Varbind varbind=newNotice.getVarbind();           
            if(varbindName!=null && !varbindName.trim().equals("") && varbindValue!=null && !varbindValue.trim().equals("")) {

                if(varbind==null) {
                    varbind=new Varbind();
                    newNotice.setVarbind(varbind);
                }
                varbind.setVbname(varbindName);
                varbind.setVbvalue(varbindValue);
            } else {
                //Must do this to allow clearing out varbind definitions
                newNotice.setVarbind(null);
            }
            
            try {
                    // replacing a path with a new name.
                    NotificationFactory.getInstance().replaceNotification(oldName, newNotice);
            } catch (Exception e) {
                throw new ServletException("Couldn't save/reload notification configuration file.", e);
            }
            String suppliedReturnPage=(String)user.getAttribute("noticeWizardReturnPage");
            if(suppliedReturnPage!=null && !suppliedReturnPage.equals("")) {
                redirectString.append(suppliedReturnPage);
            } else {
                redirectString.append(SOURCE_PAGE_NOTICES);
            }
        } else if (sourcePage.equals(SOURCE_PAGE_PATH_OUTAGE)) {
            rule.append(request.getParameter("newRule"));
            rule = toSingleQuote(rule);
            rule = stripExtraWhite(rule.toString());
            rule = stripServices(rule.toString());
            rule = checkParens(rule);
            String newRule = rule.toString();
            String redirectPage = SOURCE_PAGE_VALIDATE_PATH_OUTAGE;
	    String criticalIp = request.getParameter("criticalIp");
            Map params = new HashMap();
	    if (newRule != null)
	      params.put("newRule", newRule);
	    if (request.getParameter("criticalSvc") != null)
	      params.put("criticalSvc", request.getParameter("criticalSvc"));
	    if (request.getParameter("showNodes") != null)
	      params.put("showNodes", request.getParameter("showNodes"));
	    if (criticalIp != null && !criticalIp.equals("")) {
	      params.put("criticalIp", criticalIp);
              try {
                FilterDaoFactory.getInstance().validateRule("IPADDR IPLIKE " + criticalIp);
              } catch (FilterParseException e) {
                // page to redirect to if the critical IP is invalid
                params.put("mode", "Critical path IP failed");
                redirectPage = SOURCE_PAGE_PATH_OUTAGE;
              }
            }
            try {
                FilterDaoFactory.getInstance().validateRule(newRule);
            } catch (FilterParseException e) {
                // page to redirect to if the rule is invalid
                params.put("mode", "Current rule failed");
                redirectPage = SOURCE_PAGE_PATH_OUTAGE;
            }
            redirectString.append(redirectPage).append(makeQueryString(params));
        } else if (sourcePage.equals(SOURCE_PAGE_VALIDATE_PATH_OUTAGE)) {
            String redirectPage = SOURCE_PAGE_NOTIFICATION_INDEX;
            String userAction = request.getParameter("userAction");
            String criticalIp = request.getParameter("criticalIp");
            String criticalSvc = request.getParameter("criticalSvc");
            String newRule = request.getParameter("newRule");
            Map params = new HashMap();
            if (userAction != null && userAction.equals("rebuild")) {
                params.put("newRule", newRule);
                params.put("criticalIp", criticalIp);
                params.put("criticalSvc", criticalSvc);
	        if (request.getParameter("showNodes") != null)
	            params.put("showNodes", request.getParameter("showNodes"));
                redirectPage = SOURCE_PAGE_PATH_OUTAGE;
            } else {
                try {
                    updatePaths(newRule, criticalIp, criticalSvc);
                } catch (FilterParseException e) {
                    params.put("mode", "Update failed");
                    redirectPage = SOURCE_PAGE_PATH_OUTAGE;
                } catch (SQLException e) {
                    params.put("mode", "Update failed");
                    redirectPage = SOURCE_PAGE_PATH_OUTAGE;
                }
            }
            redirectString.append(redirectPage).append(makeQueryString(params));
        } else if (sourcePage.equals(SOURCE_PAGE_OTHER_WEBUI)) {
            //We've come from elsewhere in the Web UI page, and will have a UEI.  
            //If there are existing notices for this UEI, then go to a page listing them allowing editing.  
            //If there are none, then create a notice, populate the UEI, and go to the buildRule page.
            String returnPage=request.getParameter("returnPage");
            user.setAttribute("noticeWizardReturnPage", returnPage);
            String uei=request.getParameter("uei");
            try {
                if(NotificationFactory.getInstance().hasUei(uei)) {
                    //There are existing notifications for this UEI - goto a listing page
                    Map params = new HashMap();
                    params.put("uei", uei);                   
                    redirectString.append(SOURCE_PAGE_NOTIFS_FOR_UEI).append(makeQueryString(params));
                } else {
                    newNotifWithUEI(request, user, redirectString,rule);
                }
            } catch (IOException e) {
                throw new ServletException("IOException while checking if there is an existing notification for UEI "+uei, e);
            } catch (MarshalException e) {
                throw new ServletException("Marshalling Exception while checking if there is an existing notification for UEI "+uei, e);
            } catch (ValidationException e) {
                throw new ServletException("Validation Exception while checking if there is an existing notification for UEI "+uei, e);
            } 
        } else if (sourcePage.equals(SOURCE_PAGE_NOTIFS_FOR_UEI)) {
            String userAction=request.getParameter("userAction");
            if("edit".equals(userAction)) {
                edit(request, user, redirectString);
            } else if ("new".equals(userAction)) {
                newNotifWithUEI(request, user, redirectString, rule);
            }
        }

        if (redirectString.toString().equals(""))
            throw new ServletException("no redirect specified for this wizard!");

        response.sendRedirect(redirectString.toString());
    }
    private void newNotifWithUEI(HttpServletRequest request, HttpSession user, StringBuffer redirectString, StringBuffer rule) throws ServletException {
        String uei=request.getParameter("uei");
        Notification newNotice = new Notification();
        newNotice.setRule("IPADDR IPLIKE *.*.*.*");
        newNotice.setNumericMessage("111-%noticeid%");
        newNotice.setSubject("Notice #%noticeid%");
        newNotice.setStatus("on");
        newNotice.setUei(uei);

        Map params = new HashMap();
        rule.append(newNotice.getRule());
        rule = toSingleQuote(rule);
        params.put("newRule", rule.toString());

        user.setAttribute("newNotice", newNotice);
        redirectString.append(SOURCE_PAGE_RULE).append(makeQueryString(params));                         
    }
    
    //Common code for two source pages that can't really be considered the same
    private void edit(HttpServletRequest request, HttpSession user, StringBuffer redirectString) throws ServletException {
        Notification oldNotice = null;

        try {
            oldNotice = NotificationFactory.getInstance().getNotification(request.getParameter("notice"));
        } catch (Exception e) {
            throw new ServletException("couldn't get a copy of the notification to edit.", e);
        }

        // copy the old path into the new path
        Notification newNotice = copyNotice(oldNotice);
        user.setAttribute("newNotice", newNotice);

        redirectString.append(SOURCE_PAGE_UEIS);    
    }
    /**
     * 
     */
    private Notification copyNotice(Notification oldNotice) {
        Notification newNotice = new Notification();

        newNotice.setName(oldNotice.getName());
        newNotice.setWriteable(oldNotice.getWriteable());
        newNotice.setDescription(oldNotice.getDescription());
        newNotice.setUei(oldNotice.getUei());
        newNotice.setRule(oldNotice.getRule());
        newNotice.setDestinationPath(oldNotice.getDestinationPath());
        newNotice.setNoticeQueue(oldNotice.getNoticeQueue());
        newNotice.setTextMessage(oldNotice.getTextMessage());
        newNotice.setSubject(oldNotice.getSubject());
        newNotice.setNumericMessage(oldNotice.getNumericMessage());
        newNotice.setStatus(oldNotice.getStatus());
	newNotice.setVarbind(oldNotice.getVarbind());

        Parameter parameters[] = oldNotice.getParameter();
        for (int i = 0; i < parameters.length; i++) {
            Parameter newParam = new Parameter();
            newParam.setName(parameters[i].getName());
            newParam.setValue(parameters[i].getValue());

            newNotice.addParameter(newParam);
        }

        return newNotice;
    }

    private String makeQueryString(Map map) {
        StringBuffer buffer = new StringBuffer();
        String separator = "?";

        Iterator i = map.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            Object value = map.get(key);
            if (value instanceof String[]) {
                String[] list = (String[]) value;
                for (int j = 0; j < list.length; j++) {
                    buffer.append(separator).append(key).append("=").append(Util.encode(list[j]));
                    separator = "&";
                }
            } else {
                buffer.append(separator).append(key).append("=").append(Util.encode((String) value));
            }
            separator = "&";
        }

        return buffer.toString();
    }

    private static StringBuffer toSingleQuote(StringBuffer buffer) {
        for (int i = 0; (i < buffer.length()); i++) {
            if ((i < buffer.length() - 5) && (buffer.substring(i, i + 6).equals("&quot;"))) {
                buffer.replace(i, i + 6, "'");
            } else if (buffer.charAt(i) == '"') {
                buffer.replace(i, i + 1, "'");
            }
        }
        return buffer;
    }

    private static StringBuffer stripExtraWhite(String s) {
        String myregex = "\\s+";
        Pattern pattern = Pattern.compile(myregex);
        Matcher matcher = pattern.matcher(s);
        String mys = matcher.replaceAll(" ");
        myregex = "^\\s";
        pattern = Pattern.compile(myregex);
        matcher = pattern.matcher(mys);
        mys = matcher.replaceAll("");
        myregex = "\\s$";
        pattern = Pattern.compile(myregex);
        matcher = pattern.matcher(mys);
        StringBuffer buffer = new StringBuffer(matcher.replaceAll(""));
        return buffer;
    }

    private static StringBuffer stripServices(String s) {
        String myregex = "\\s*\\&\\s*\\(\\s*\\!?is.+";
        Pattern pattern = Pattern.compile(myregex);
        Matcher matcher = pattern.matcher(s);
        StringBuffer buffer = new StringBuffer(matcher.replaceAll(""));
        return buffer;
    }

    private static StringBuffer checkParens(StringBuffer buffer) {
        if(buffer.length()==0) {
            return buffer;
        }
        if ((buffer.charAt(0) != '(') || (buffer.charAt(buffer.length() - 1) != ')')) {
            buffer.append(")");
            buffer.insert(0, "(");
        }
        return buffer;
    }

    private void deleteCriticalPath(int node, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(SQL_DELETE_CRITICAL_PATH);
        stmt.setInt(1, node);
        stmt.execute();
        stmt.close();
    }

    private void setCriticalPath(int node, String criticalIp, String criticalSvc, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(SQL_SET_CRITICAL_PATH);
        stmt.setInt(1, node);
        stmt.setString(2, criticalIp);
        stmt.setString(3, criticalSvc);
        stmt.execute();
        stmt.close();
    }

    private void updatePaths(String rule, String criticalIp, String criticalSvc)
                                 throws FilterParseException, SQLException {
        Connection conn = Vault.getDbConnection();
        //StringBuffer buffer = new StringBuffer();
	SortedMap nodes = FilterDaoFactory.getInstance().getNodeMap(rule);
        try {
            Iterator i = nodes.keySet().iterator();
            while(i.hasNext()) {
                Integer key = (Integer)i.next();
                deleteCriticalPath(key.intValue(), conn);
                if(criticalIp != null && !criticalIp.equals("")) {
                    setCriticalPath(key.intValue(), criticalIp, criticalSvc, conn);
                }
            }
        } finally {
            Vault.releaseDbConnection(conn);
        }
    }

}
