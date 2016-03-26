package org.opennms.netmgt.dao.api;


import java.util.List;
import java.util.Set;

import org.opennms.netmgt.model.topology.BroadcastDomain;
import org.opennms.netmgt.model.topology.SharedSegment;

public interface BridgeTopologyDao {
        
    
    // clear topology for all BroadcastDomain that are empty
    void clean();
    // update broadcastdomain
    void save(BroadcastDomain domain);
    // Load the topology from the scratch
    void load(BridgeBridgeLinkDao bridgeBridgeLinkDao,BridgeMacLinkDao bridgeMacLinkDao);
    
    List<SharedSegment> getNodeSharedSegments(BridgeBridgeLinkDao bridgeBridgeLinkDao,BridgeMacLinkDao bridgeMacLinkDao, int nodeid);

    void delete(BroadcastDomain domain);

    // getting BroacastDomain for calculation
    BroadcastDomain get(int nodeid);
    
    Set<BroadcastDomain> getAll();

}
