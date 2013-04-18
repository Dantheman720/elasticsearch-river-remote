package org.jboss.elasticsearch.river.remote;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.river.RiversModule;
import org.jboss.elasticsearch.river.remote.mgm.fullupdate.FullUpdateAction;
import org.jboss.elasticsearch.river.remote.mgm.fullupdate.RestFullUpdateAction;
import org.jboss.elasticsearch.river.remote.mgm.fullupdate.TransportFullUpdateAction;
import org.jboss.elasticsearch.river.remote.mgm.lifecycle.JRLifecycleAction;
import org.jboss.elasticsearch.river.remote.mgm.lifecycle.RestJRLifecycleAction;
import org.jboss.elasticsearch.river.remote.mgm.lifecycle.TransportJRLifecycleAction;
import org.jboss.elasticsearch.river.remote.mgm.riverslist.ListRiversAction;
import org.jboss.elasticsearch.river.remote.mgm.riverslist.RestListRiversAction;
import org.jboss.elasticsearch.river.remote.mgm.riverslist.TransportListRiversAction;
import org.jboss.elasticsearch.river.remote.mgm.state.JRStateAction;
import org.jboss.elasticsearch.river.remote.mgm.state.RestJRStateAction;
import org.jboss.elasticsearch.river.remote.mgm.state.TransportJRStateAction;

/**
 * Remote River ElasticSearch Plugin class.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RemoteRiverPlugin extends AbstractPlugin {

	@Inject
	public RemoteRiverPlugin() {
	}

	@Override
	public String name() {
		return "river-remote";
	}

	@Override
	public String description() {
		return "River Remote Plugin";
	}

	public void onModule(RiversModule module) {
		module.registerRiver("remote", RemoteRiverModule.class);
	}

	public void onModule(RestModule module) {
		module.addRestAction(RestFullUpdateAction.class);
		module.addRestAction(RestJRStateAction.class);
		module.addRestAction(RestJRLifecycleAction.class);
		module.addRestAction(RestListRiversAction.class);
	}

	public void onModule(ActionModule module) {
		module.registerAction(FullUpdateAction.INSTANCE, TransportFullUpdateAction.class);
		module.registerAction(JRStateAction.INSTANCE, TransportJRStateAction.class);
		module.registerAction(JRLifecycleAction.INSTANCE, TransportJRLifecycleAction.class);
		module.registerAction(ListRiversAction.INSTANCE, TransportListRiversAction.class);
	}
}
