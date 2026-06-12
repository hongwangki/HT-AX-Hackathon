package haitai.ht_ax_hackathon.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StatusAccessService {

    /** Result check and task submission stay open by default after a restart. */
    private final AtomicBoolean statusCheckOpen = new AtomicBoolean(true);

    /** Applications are accepted until an admin closes the window. Resets to open on restart. */
    private final AtomicBoolean applyOpen = new AtomicBoolean(true);

    public boolean isStatusCheckOpen() {
        return statusCheckOpen.get();
    }

    public void setStatusCheckOpen(boolean open) {
        statusCheckOpen.set(open);
    }

    /** The submission window simply follows the result-check switch — one admin toggle for both. */
    public boolean isSubmissionOpen() {
        return statusCheckOpen.get();
    }

    public boolean isApplyOpen() {
        return applyOpen.get();
    }

    public void setApplyOpen(boolean open) {
        applyOpen.set(open);
    }
}
