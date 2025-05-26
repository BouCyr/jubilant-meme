package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ActivityId;

import java.util.Optional;

@FunctionalInterface
public interface ActivityFinder {
    Optional<Activity> byId(ActivityId activityId);
}
