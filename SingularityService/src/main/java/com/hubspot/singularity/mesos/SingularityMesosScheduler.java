package com.hubspot.singularity.mesos;

import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.InverseOffer;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos;
import org.apache.mesos.v1.scheduler.Protos.Event;

public abstract class SingularityMesosScheduler {

  /**
   * First event received when the scheduler subscribes. This contains the
   * frameworkId generated by mesos that should be used if the scheduler needs
   * to reconnect in the future.
   *
   * @param subscribed Data regarding your subscription
   */
  public abstract CompletableFuture<Void> subscribed(Protos.Event.Subscribed subscribed);

  /**
   * Received whenever there are new resources that are offered to the
   * scheduler. Each offer corresponds to a set of resources on an agent.
   * Until the scheduler accepts or declines an offer the resources are
   * considered allocated to the scheduler.
   *
   * @param offers A list of offers from mesos
   */
  public abstract CompletableFuture<Void> resourceOffers(List<Offer> offers);

  /**
   * Received whenever there are resources requested back from the scheduler.
   * Each inverse offer specifies the agent, and optionally specific
   * resources. Accepting or Declining an inverse offer informs the allocator
   * of the scheduler's ability to release the specified resources without
   * violating an SLA. If no resources are specified then all resources on the
   * agent are requested to be released.
   *
   * @param offers A list of reverse offers from mesos
   */
  public abstract void inverseOffers(List<InverseOffer> offers);

  /**
   * Received when a particular offer is no longer valid (e.g., the agent
   * corresponding to the offer has been removed) and hence needs to be
   * rescinded. Any future calls ('Accept' / 'Decline') made by the scheduler
   * regarding this offer will be invalid.
   *
   * @param offerId the recinded offer
   */
  public abstract CompletableFuture<Void> rescind(OfferID offerId);

  /**
   * Received when a particular inverse offer is no longer valid (e.g., the
   * agent corresponding to the offer has been removed) and hence needs to be
   * rescinded. Any future calls ('Accept' / 'Decline') made by the scheduler
   * regarding this inverse offer will be invalid.
   *
   * @param offerId The rescind inverse offer id
   */
  public abstract void rescindInverseOffer(OfferID offerId);

  /**
   * Received whenever there is a status update that is generated by the
   * executor or agent or master. Status updates should be used by executors
   * to reliably communicate the status of the tasks that they manage. It is
   * crucial that a terminal update (see TaskState in v1/mesos.proto) is sent
   * by the executor as soon as the task terminates, in order for Mesos to
   * release the resources allocated to the task. It is also the
   * responsibility of the scheduler to explicitly acknowledge the receipt of
   * a status update. See 'Acknowledge' in the 'Call' section below for the
   * semantics.
   *
   * @param update Contains info about the current tasks status
   */
  public abstract CompletableFuture<Boolean> statusUpdate(TaskStatus update);

  /**
   * Received when a custom message generated by the executor is forwarded by
   * the master. Note that this message is not interpreted by Mesos and is
   * only forwarded (without reliability guarantees) to the scheduler. It is
   * up to the executor to retry if the message is dropped for any reason.
   *
   * @param message Message sent from executor
   */
  public abstract void message(Protos.Event.Message message);

  /**
   * Received when an agent is removed from the cluster (e.g., failed health
   * checks) or when an executor is terminated. Note that, this event
   * coincides with receipt of terminal UPDATE events for any active tasks
   * belonging to the agent or executor and receipt of 'Rescind' events for
   * any outstanding offers belonging to the agent. Note that there is no
   * guaranteed order between the 'Failure', 'Update' and 'Rescind' events
   * when an agent or executor is removed.
   *
   * @param failure Information regarding the current failure
   */
  public abstract void failure(Protos.Event.Failure failure);

  /**
   * Received when there is an unrecoverable error in the scheduler (e.g.,
   * scheduler failed over, rate limiting, authorization errors etc.). The
   * scheduler should abort on receiving this event.
   *
   * @param message Error message
   */
  public abstract void error(String message);

  /**
   * Periodic message sent by the Mesos master according to
   * 'Subscribed.heartbeat_interval_seconds'. If the scheduler does not
   * receive any events (including heartbeats) for an extended period of time
   * (e.g., 5 x heartbeat_interval_seconds), there is likely a network
   * partition. In such a case the scheduler should close the existing
   * subscription connection and resubscribe using a backoff strategy.
   */
  public abstract void heartbeat(Event event);

  /**
   * Called when an uncaught exception occurs when processing events form the mesos master
   * in any of the above methods
   */
  public abstract void onUncaughtException(Throwable t);

  /**
   * Called when an uncaught exception occurs while attempting to connect to the mesos master
   */
  public abstract void onSubscribeException(Throwable t);

  /**
   * Singularity-specific methods used elsewhere in the code to determine scheduler
   * state and wrap certain actions
   */
  public abstract SchedulerState getState();

  public abstract long getEventBufferSize();

  public abstract void notifyStopping();

  public abstract void reconnectMesos();

  public abstract void notLeader();

  public abstract void pauseForDatastoreReconnect();

  public abstract void setZkConnectionState(ConnectionState connectionState);

  public abstract void agentLost(AgentID agentId);

  public abstract boolean isRunning();

  public abstract Optional<MasterInfo> getMaster();

  public abstract void start() throws Exception;

  public abstract void killAndRecord(
    SingularityTaskId taskId,
    Optional<RequestCleanupType> requestCleanupType,
    Optional<TaskCleanupType> taskCleanupType,
    Optional<Long> originalTimestamp,
    Optional<Integer> retries,
    Optional<String> user
  );

  public void killAndRecord(
    SingularityTaskId taskId,
    RequestCleanupType requestCleanupType,
    Optional<String> user
  ) {
    killAndRecord(
      taskId,
      Optional.of(requestCleanupType),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      user
    );
  }

  public void killAndRecord(
    SingularityTaskId taskId,
    TaskCleanupType taskCleanupType,
    Optional<String> user
  ) {
    killAndRecord(
      taskId,
      Optional.empty(),
      Optional.of(taskCleanupType),
      Optional.empty(),
      Optional.empty(),
      user
    );
  }

  public abstract Optional<Long> getLastOfferTimestamp();

  public abstract Optional<Double> getHeartbeatIntervalSeconds();

  /*
   * for testing only
   */
  public abstract void setSubscribed();
}
