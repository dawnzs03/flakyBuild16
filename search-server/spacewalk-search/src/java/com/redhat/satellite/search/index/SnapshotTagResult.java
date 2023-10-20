/*
 * Copyright (c) 2008--2010 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

package com.redhat.satellite.search.index;

import org.apache.lucene.document.Document;

/**
 * SnapshotTagResult
 * @version $Rev$
 */
public class SnapshotTagResult extends Result {
    private String serverId;
    private String snapshotId;

    /**
     * Constructor
     */
    public SnapshotTagResult() {
        super();
        serverId = "N/A";
    }

    /**
     * Constructs a result object
     * @param rankIn order of results returned from lucene
     * @param scoreIn score of this hit as defined by lucene query
     * @param doc lucene document containing data fields
     */
    public SnapshotTagResult(int rankIn, float scoreIn, Document doc) {
        if (doc.getField("id") != null) {
            setId(doc.getField("id").stringValue());
        }
        if (doc.getField("name") != null) {
            setName(doc.getField("name").stringValue());
        }
        if (doc.getField("serverId") != null) {
            setServerId(doc.getField("serverId").stringValue());
        }
        if (doc.getField("snapshotId") != null) {
            setSnapshotId(doc.getField("snapshotId").stringValue());
        }
        setRank(rankIn);
        setScore(scoreIn);
    }


    /**
     * @return the serverId
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * @param serverIdIn the serverId to set
     */
    public void setServerId(String serverIdIn) {
        this.serverId = serverIdIn;
    }

    /**
     * @return the snapshotId
     */
    public String getSnapshotId() {
        return snapshotId;
    }

    /**
     * @param snapshotIdIn the snapshotId to set
     */
    public void setSnapshotId(String snapshotIdIn) {
        this.snapshotId = snapshotIdIn;
    }


}
