package haitai.ht_ax_hackathon.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StatusAccessService {

    static final ZoneId SUBMISSION_ZONE = ZoneId.of("Asia/Seoul");
    static final Instant SUBMISSION_DEADLINE = ZonedDateTime.of(
            2026, 8, 6, 0, 0, 0, 0, SUBMISSION_ZONE
    ).toInstant();

    private final Clock clock;

    /** Result check is open by default after a restart; submissions still honor the hard deadline. */
    private final AtomicBoolean statusCheckOpen = new AtomicBoolean(true);

    /** Applications are accepted until an admin closes the window. Resets to open on restart. */
    private final AtomicBoolean applyOpen = new AtomicBoolean(true);

    public StatusAccessService() {
        this(Clock.system(SUBMISSION_ZONE));
    }

    StatusAccessService(Clock clock) {
        this.clock = clock;
    }

    public boolean isStatusCheckOpen() {
        return statusCheckOpen.get();
    }

    public void setStatusCheckOpen(boolean open) {
        statusCheckOpen.set(open);
    }

    /**
     * Submission additionally has a hard deadline. The result-check switch can still be used
     * after the deadline, but it can never reopen task submissions once the deadline has passed.
     */
    public boolean isSubmissionOpen() {
        return statusCheckOpen.get() && clock.instant().isBefore(SUBMISSION_DEADLINE);
    }

    public boolean isApplyOpen() {
        return applyOpen.get();
    }

    public void setApplyOpen(boolean open) {
        applyOpen.set(open);
    }
}
