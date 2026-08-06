package haitai.ht_ax_hackathon.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StatusAccessServiceTest {

    @Test
    void submissionIsOpenImmediatelyBeforeDeadlineWhenAdminSwitchIsOn() {
        StatusAccessService service = serviceAt(StatusAccessService.SUBMISSION_DEADLINE.minusNanos(1));

        assertThat(service.isSubmissionOpen()).isTrue();
    }

    @Test
    void submissionClosesExactlyAtDeadline() {
        StatusAccessService service = serviceAt(StatusAccessService.SUBMISSION_DEADLINE);

        assertThat(service.isSubmissionOpen()).isFalse();
    }

    @Test
    void adminCannotReopenSubmissionAfterDeadline() {
        StatusAccessService service = serviceAt(StatusAccessService.SUBMISSION_DEADLINE.plusSeconds(1));

        service.setStatusCheckOpen(false);
        service.setStatusCheckOpen(true);

        assertThat(service.isStatusCheckOpen()).isTrue();
        assertThat(service.isSubmissionOpen()).isFalse();
    }

    @Test
    void adminCanCloseSubmissionBeforeDeadline() {
        StatusAccessService service = serviceAt(StatusAccessService.SUBMISSION_DEADLINE.minusSeconds(1));

        service.setStatusCheckOpen(false);

        assertThat(service.isSubmissionOpen()).isFalse();
    }

    private StatusAccessService serviceAt(Instant instant) {
        return new StatusAccessService(Clock.fixed(instant, StatusAccessService.SUBMISSION_ZONE));
    }
}
