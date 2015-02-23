/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest.measurements.fetch;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.jrobin.core.RrdException;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.RrdGraphAttribute;
import org.opennms.web.rest.MeasurementsRestService;
import org.opennms.web.rest.measurements.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Used to fetch measurements from RRD files.
 *
 * @author Jesse White <jesse@opennms.org>
 */
public abstract class AbstractRrdBasedFetchStrategy implements MeasurementFetchStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRrdBasedFetchStrategy.class);

    private final ResourceDao m_resourceDao;

    public AbstractRrdBasedFetchStrategy(final ResourceDao resourceDao) {
        m_resourceDao = resourceDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchResults fetch(long start, long end, long step, int maxrows,
            List<Source> sources) throws Exception {

        final Map<String, Object> constants = Maps.newHashMap();

        final Map<Source, String> rrdsBySource = Maps.newHashMap();
        
        for (final Source source : sources) {
            // Grab the resource
            final OnmsResource resource = m_resourceDao.getResourceById(source
                    .getResourceId());
            if (resource == null) {
                LOG.error("No resource with id: {}", source.getResourceId());
                return null;
            }

            // Grab the attribute
            final RrdGraphAttribute rrdGraphAttribute = resource
                    .getRrdGraphAttributes().get(source.getAttribute());
            if (rrdGraphAttribute == null) {
                LOG.error("No attribute with name: {}", source.getAttribute());
                return null;
            }

            // Gather the values from strings.properties
            for (final Map.Entry<String, String> propertyEntry : resource.getStringPropertyAttributes().entrySet()) {
                final String propertyName = propertyEntry.getKey();

                // Attempt to cast the value as a double, fall back to keeping it as a string
                Object propertyValue;
                try {
                    propertyValue = MeasurementsRestService.toDouble(propertyEntry.getValue());
                } catch (Throwable t) {
                    propertyValue = propertyEntry.getValue();
                }

                constants.put(String.format("%s.%s", source.getLabel(), propertyName),
                        propertyValue);
            }

            // Build the path to the archive
            final String rrdFile = System.getProperty("rrd.base.dir")
                    + File.separator + rrdGraphAttribute.getRrdRelativePath();

            rrdsBySource.put(source, rrdFile);
        }

        // Use to store the results
        final SortedMap<Long, Map<String, Double>> rows = Maps.newTreeMap();

        // Fetch
        final long actualStep = getRows(start, end, step, maxrows, rrdsBySource, rows);

        return new FetchResults(rows, actualStep, constants);
    }

    /**
     * Performs the actual retrieval of the values from the RRD/JRB files.
     */
    protected abstract long getRows(long start, long end, long step, int maxrows,
            Map<Source, String> rrdsBySource,
            SortedMap<Long, Map<String, Double>> rows) throws RrdException;

}
