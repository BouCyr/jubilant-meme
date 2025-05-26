package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ActivityId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class ActivityStorage implements
        ActivityFinder,
        ActivityFinderByContract,
        ActivityFinderByContractAndPresta,
        ActivityWriter{

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityStorage.class);

    private final ActivityRepository activityRepository;

    public ActivityStorage(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public Activity save(Activity activity) {
        return activityRepository.save(activity);
    }

    @Override
    public Optional<Activity> byId(ActivityId activityId) {
        return this.activityRepository.findById(activityId.id());
    }

    public List<Activity> byContractAndItem(ContractId contractId, SalesSystemId salesSystemId){
        return this.activityRepository.findByContractIdAndSalesSystemId(contractId.id(), salesSystemId.id());
    }
    public List<Activity> byContract(ContractId contractId){
        return this.activityRepository.findByContractId(contractId.id());
    }

}
