package org.drools.process.instance;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.SessionConfiguration;
import org.drools.WorkingMemory;
import org.drools.common.AbstractWorkingMemory;
import org.drools.common.InternalRuleBase;
import org.drools.common.InternalWorkingMemory;
import org.drools.definition.process.Process;
import org.drools.event.ActivationCreatedEvent;
import org.drools.event.DefaultAgendaEventListener;
import org.drools.event.ProcessEventSupport;
import org.drools.event.RuleFlowGroupDeactivatedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.process.core.event.EventFilter;
import org.drools.process.core.event.EventTypeFilter;
import org.drools.process.instance.event.SignalManager;
import org.drools.process.instance.event.SignalManagerFactory;
import org.drools.process.instance.timer.TimerManager;
import org.drools.ruleflow.core.RuleFlowProcess;
import org.drools.runtime.process.EventListener;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemManager;
import org.drools.workflow.core.node.EventTrigger;
import org.drools.workflow.core.node.StartNode;
import org.drools.workflow.core.node.Trigger;

public class ProcessRuntimeImpl implements InternalProcessRuntime {
	
	private AbstractWorkingMemory workingMemory;
	
	private ProcessInstanceManager processInstanceManager;
	private SignalManager signalManager;
	private TimerManager timerManager;
	private ProcessEventSupport processEventSupport;
	
	public ProcessRuntimeImpl(AbstractWorkingMemory workingMemory) {
		this.workingMemory = workingMemory;
		initProcessInstanceManager();
		initSignalManager();
		timerManager = new TimerManager(workingMemory, workingMemory.getTimerService());
        processEventSupport = new ProcessEventSupport();
        initProcessEventListeners();
        initProcessActivationListener();
	}
	
	private void initProcessInstanceManager() {
		String processInstanceManagerClass = ((SessionConfiguration) workingMemory.getSessionConfiguration()).getProcessInstanceManagerFactory();
		try {
			processInstanceManager = ((ProcessInstanceManagerFactory) Class.forName(processInstanceManagerClass).newInstance()).createProcessInstanceManager(workingMemory);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void initSignalManager() {
		String signalManagerClass = ((SessionConfiguration) workingMemory.getSessionConfiguration()).getSignalManagerFactory();
		try {
			signalManager = ((SignalManagerFactory) Class.forName(signalManagerClass).newInstance()).createSignalManager(workingMemory);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
    public ProcessInstance startProcess(final String processId) {
        return startProcess(processId, null);
    }

    public ProcessInstance startProcess(String processId,
                                        Map<String, Object> parameters) {
        try {
            workingMemory.startOperation();
            if ( !workingMemory.getActionQueue().isEmpty() ) {
            	workingMemory.executeQueuedActions();
            }
            final Process process = ((InternalRuleBase) workingMemory.getRuleBase()).getProcess( processId );
            if ( process == null ) {
                throw new IllegalArgumentException( "Unknown process ID: " + processId );
            }
            org.drools.process.instance.ProcessInstance processInstance = startProcess( process, parameters );

            if ( processInstance != null ) {
                // start process instance
                getProcessEventSupport().fireBeforeProcessStarted( processInstance, ((InternalWorkingMemory) workingMemory).getKnowledgeRuntime() );
                ((org.drools.process.instance.ProcessInstance) processInstance).start();
                getProcessEventSupport().fireAfterProcessStarted( processInstance, ((InternalWorkingMemory) workingMemory).getKnowledgeRuntime() );
            }
            return processInstance;
        } finally {
            workingMemory.endOperation();
        }
    }

    private org.drools.process.instance.ProcessInstance startProcess(final Process process,
                                         Map<String, Object> parameters) {
        ProcessInstanceFactory conf = ProcessInstanceFactoryRegistry.INSTANCE.getProcessInstanceFactory( process );
        if ( conf == null ) {
            throw new IllegalArgumentException( "Illegal process type: " + process.getClass() );
        }
        return conf.createProcessInstance( process,
        								   workingMemory,
                                           parameters );
    }

    public ProcessInstanceManager getProcessInstanceManager() {
        return processInstanceManager;
    }
    
    public TimerManager getTimerManager() {
    	return timerManager;
    }
    
    public SignalManager getSignalManager() {
    	return signalManager;
    }

    public Collection<org.drools.runtime.process.ProcessInstance> getProcessInstances() {
        return processInstanceManager.getProcessInstances();
    }

    public ProcessInstance getProcessInstance(long id) {
        return processInstanceManager.getProcessInstance( id );
    }

    public void removeProcessInstance(ProcessInstance processInstance) {
        processInstanceManager.removeProcessInstance( processInstance );
    }
    
    private void initProcessEventListeners() {
        for ( Process process : ((InternalRuleBase) workingMemory.getRuleBase()).getProcesses() ) {
            if ( process instanceof RuleFlowProcess ) {
                StartNode startNode = ((RuleFlowProcess) process).getStart();
                List<Trigger> triggers = startNode.getTriggers();
                if ( triggers != null ) {
                    for ( Trigger trigger : triggers ) {
                        if ( trigger instanceof EventTrigger ) {
                            final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();
                            String type = null;
                            for ( EventFilter filter : filters ) {
                                if ( filter instanceof EventTypeFilter ) {
                                    type = ((EventTypeFilter) filter).getType();
                                }
                            }
                            signalManager.addEventListener( type,
                                                            new StartProcessEventListener( process.getId(),
                                                                                           filters,
                                                                                           trigger.getInMappings() ) );
                        }
                    }
                }
            }
        }
    }
    
    public ProcessEventSupport getProcessEventSupport() {
    	return processEventSupport;
    }

    public void addEventListener(final ProcessEventListener listener) {
        this.processEventSupport.addEventListener( listener );
    }

    public void removeEventListener(final ProcessEventListener listener) {
        this.processEventSupport.removeEventListener( listener );
    }

    public List<ProcessEventListener> getProcessEventListeners() {
        return processEventSupport.getEventListeners();
    }

    private class StartProcessEventListener implements EventListener {
    	
	    private String              processId;
	    private List<EventFilter>   eventFilters;
	    private Map<String, String> inMappings;
	
	    public StartProcessEventListener(String processId,
	                                     List<EventFilter> eventFilters,
	                                     Map<String, String> inMappings) {
	        this.processId = processId;
	        this.eventFilters = eventFilters;
	        this.inMappings = inMappings;
	    }
	
	    public String[] getEventTypes() {
	        return null;
	    }
	
	    public void signalEvent(String type,
	                            Object event) {
	        for ( EventFilter filter : eventFilters ) {
	            if ( !filter.acceptsEvent( type,
	                                       event ) ) {
	                return;
	            }
	        }
	        Map<String, Object> params = null;
	        if ( inMappings != null && !inMappings.isEmpty() ) {
	            params = new HashMap<String, Object>();
	            for ( Map.Entry<String, String> entry : inMappings.entrySet() ) {
	                if ( "event".equals( entry.getValue() ) ) {
	                    params.put( entry.getKey(),
	                                event );
	                } else {
	                    params.put( entry.getKey(),
	                                entry.getValue() );
	                }
	            }
	        }
	        startProcess( processId,
	                      params );
	    }
	}

    private void initProcessActivationListener() {
        workingMemory.addEventListener( new DefaultAgendaEventListener() {
            public void activationCreated(ActivationCreatedEvent event,
                                          WorkingMemory workingMemory) {
                String ruleFlowGroup = event.getActivation().getRule().getRuleFlowGroup();
                if ( "DROOLS_SYSTEM".equals( ruleFlowGroup ) ) {
                    // new activations of the rule associate with a state node
                    // signal process instances of that state node
                    String ruleName = event.getActivation().getRule().getName();
                    if ( ruleName.startsWith( "RuleFlowStateNode-" ) ) {
                        int index = ruleName.indexOf( "-",
                                                      18 );
                        index = ruleName.indexOf( "-",
                                                  index + 1 );
                        String eventType = ruleName.substring( 0,
                                                               index );
                        signalManager.signalEvent( eventType,
                                                   event );
                    }
                }
            }
        } );
        workingMemory.addEventListener( new DefaultAgendaEventListener() {
            public void afterRuleFlowGroupDeactivated(final RuleFlowGroupDeactivatedEvent event,
                                                      final WorkingMemory workingMemory) {
                signalManager.signalEvent( "RuleFlowGroup_" + event.getRuleFlowGroup().getName(),
                                           null );
            }
        } );
    }

	public void abortProcessInstance(long processInstanceId) {
		ProcessInstance processInstance = getProcessInstance(processInstanceId);
		if ( processInstance == null ) {
            throw new IllegalArgumentException( "Could not find process instance for id " + processInstanceId );
        }
        ((org.drools.process.instance.ProcessInstance) processInstance).setState( ProcessInstance.STATE_ABORTED );
	}

	public WorkItemManager getWorkItemManager() {
		return workingMemory.getWorkItemManager();
	}

	public void signalEvent(String type, Object event) {
		signalManager.signalEvent(type, event);
	}

	public void signalEvent(String type, Object event, long processInstanceId) {
		signalManager.signalEvent(processInstanceId, type, event);
	}
	
	public void setProcessEventSupport(ProcessEventSupport processEventSupport) {
		this.processEventSupport = processEventSupport;
	}
	
	public void dispose() {
        this.processEventSupport.reset();
        this.timerManager.dispose();
	}

}
