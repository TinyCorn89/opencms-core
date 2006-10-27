/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/security/CmsDefaultAuthorizationHandler.java,v $
 * Date   : $Date: 2006/10/27 16:01:00 $
 * Version: $Revision: 1.1.2.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.security;

import org.opencms.file.CmsObject;
import org.opencms.main.A_CmsAuthorizationHandler;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.site.CmsSite;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;

/**
 * Defines default authorization methods.<p>
 * 
 * @author Michael Moossen
 *
 * @version $Revision: 1.1.2.1 $ 
 * 
 * @since 6.5.4 
 */
public class CmsDefaultAuthorizationHandler extends A_CmsAuthorizationHandler {

    /** The static log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsDefaultAuthorizationHandler.class);

    /**
     * @see I_CmsAuthorizationHandler#initCmsObject(HttpServletRequest)
     */
    public CmsObject initCmsObject(HttpServletRequest request) {

        // check if "basic" authentification data is provided
        CmsObject cms = checkBasicAuthorization(request);
        // basic authorization successfull?
        if (cms != null) {
            // set the right site
            setSite(request, cms);
            // register the user into OpenCms
            registerUser(request, cms);
            // return successful logged in user
            return cms;
        }
        // failed
        return null;
    }

    /**
     * @see I_CmsAuthorizationHandler#initCmsObject(HttpServletRequest, String, String)
     */
    public CmsObject initCmsObject(HttpServletRequest request, String userName, String pwd) throws CmsException {

        // try to login with the given credentials
        CmsObject cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
        cms.loginUser(userName, pwd);
        // set the right site
        setSite(request, cms);
        // register the user into OpenCms
        registerUser(request, cms);
        // return successful logged in user
        return cms;
    }

    /**
     * Checks if the current request contains http basic authentication information in 
     * the headers, if so the user is tried to log in with this data, and on success a 
     * session is generated.<p>
     * 
     * @param req the current http request
     * 
     * @return the authenticated cms object, or <code>null</code> if failed
     */
    protected CmsObject checkBasicAuthorization(HttpServletRequest req) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Versuche Sie Session zu verifizieren.");
        }
        // no user identified from the session and basic authentication is enabled
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.toUpperCase().startsWith("BASIC ")) {
            // no authentification data is available
            return null;
        }
        // get encoded user and password, following after "BASIC "
        String base64Token = auth.substring(6);

        // decode it, using base 64 decoder
        String token = new String(Base64.decodeBase64(base64Token.getBytes()));
        String username = null;
        String password = null;
        int pos = token.indexOf(":");
        if (pos != -1) {
            username = token.substring(0, pos);
            password = token.substring(pos + 1);
        }
        // authentication in the DB
        try {
            CmsObject cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
            try {
                // try to login as a user first ...
                cms.loginUser(username, password);
            } catch (CmsException exc) {
                // login as user failed, try as webuser ...
                cms.loginWebUser(username, password);
            }
            // authentification was successful create a session
            req.getSession(true);
            return cms;
        } catch (CmsException e) {
            // authentification failed
            return null;
        }
    }

    /**
     * Sets the site matching the given request.<p>
     * 
     * @param request the current request
     * @param cms the cms context to set the site for
     */
    protected void setSite(HttpServletRequest request, CmsObject cms) {

        // get the right site for the request
        CmsSite site = OpenCms.getSiteManager().matchRequest(request);
        // set the requested site root
        cms.getRequestContext().setSiteRoot(site.getSiteRoot());
    }
}