package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

@FunctionalInterface
public interface ActivityWriter {

    Activity save(Activity activity);
}
