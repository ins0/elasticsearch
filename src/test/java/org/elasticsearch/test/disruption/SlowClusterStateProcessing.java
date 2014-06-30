/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.test.disruption;

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateNonMasterUpdateTask;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Random;

public class SlowClusterStateProcessing extends SingleNodeDisruption {

    volatile boolean disrupting;
    volatile Thread worker;

    final long intervalBetweenDelaysMin;
    final long intervalBetweenDelaysMax;
    final long delayDurationMin;
    final long delayDurationMax;


    public SlowClusterStateProcessing(Random random) {
        this(null, random);
    }

    public SlowClusterStateProcessing(String disruptedNode, Random random) {
        this(disruptedNode, random, 100, 200, 300, 20000);
    }

    public SlowClusterStateProcessing(String disruptedNode, Random random, long intervalBetweenDelaysMin,
                                      long intervalBetweenDelaysMax, long delayDurationMin, long delayDurationMax) {
        this(random, intervalBetweenDelaysMin, intervalBetweenDelaysMax, delayDurationMin, delayDurationMax);
        this.disruptedNode = disruptedNode;
    }

    public SlowClusterStateProcessing(Random random,
                                      long intervalBetweenDelaysMin, long intervalBetweenDelaysMax, long delayDurationMin,
                                      long delayDurationMax) {
        super(random);
        this.intervalBetweenDelaysMin = intervalBetweenDelaysMin;
        this.intervalBetweenDelaysMax = intervalBetweenDelaysMax;
        this.delayDurationMin = delayDurationMin;
        this.delayDurationMax = delayDurationMax;
    }


    @Override
    public void startDisrupting() {
        disrupting = true;
        worker = new Thread(new BackgroundWorker());
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void stopDisrupting() {
        if (worker == null) {
            return;
        }
        disrupting = false;
        try {
            worker.join(2 * (intervalBetweenDelaysMax + delayDurationMax));
        } catch (InterruptedException e) {
            logger.info("background thread failed to stop");
        }
        worker = null;
    }


    private synchronized boolean interruptClusterStateProcessing(final TimeValue duration) {
        if (disruptedNode == null) {
            return false;
        }
        logger.info("delaying cluster state updates on node [{}] for [{}]", disruptedNode, duration);
        ClusterService clusterService = cluster.getInstance(ClusterService.class, disruptedNode);
        clusterService.submitStateUpdateTask("service_disruption_delay", Priority.IMMEDIATE, new ClusterStateNonMasterUpdateTask() {

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                Thread.sleep(duration.millis());
                return currentState;
            }

            @Override
            public void onFailure(String source, Throwable t) {

            }
        });
        return true;
    }

    @Override
    public TimeValue expectedTimeToHeal() {
        return TimeValue.timeValueSeconds(delayDurationMax + intervalBetweenDelaysMax);
    }

    class BackgroundWorker implements Runnable {

        @Override
        public void run() {
            while (disrupting) {
                try {
                    TimeValue duration = new TimeValue(delayDurationMin + random.nextInt((int) (delayDurationMax - delayDurationMin)));
                    if (!interruptClusterStateProcessing(duration)) {
                        continue;
                    }
                    Thread.sleep(duration.millis());

                    if (disruptedNode == null) {
                        return;
                    }

                } catch (Exception e) {
                    logger.error("error in background worker", e);
                }
            }
        }
    }

}