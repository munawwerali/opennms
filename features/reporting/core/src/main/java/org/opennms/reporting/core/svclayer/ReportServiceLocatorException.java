/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2011 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *     along with OpenNMS(R).  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information contact: 
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.reporting.core.svclayer;

/**
 * Used by ReportServiceLocator
 *
 * @author <a href="mailto:jonathan@opennms.org">Jonathan Sartin</a>
 */
public class ReportServiceLocatorException extends RuntimeException {

    private static final long serialVersionUID = -7839336888035725570L;

    /**
     * <p>Constructor for ReportServiceLocatorException.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public ReportServiceLocatorException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for ReportServiceLocatorException.</p>
     *
     * @param cause a {@link java.lang.Throwable} object.
     */
    public ReportServiceLocatorException(Throwable cause) {
        super(cause);
    }

    /**
     * <p>Constructor for ReportServiceLocatorException.</p>
     *
     * @param message a {@link java.lang.String} object.
     * @param cause a {@link java.lang.Throwable} object.
     */
    public ReportServiceLocatorException(String message, Throwable cause) {
        super(message, cause);
    }

}
