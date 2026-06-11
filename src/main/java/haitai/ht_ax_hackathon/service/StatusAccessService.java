package haitai.ht_ax_hackathon.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StatusAccessService {

    private final AtomicBoolean statusCheckOpen = new AtomicBoolean(false);

    public boolean isStatusCheckOpen() {
        return statusCheckOpen.get();
    }

    public void setStatusCheckOpen(boolean open) {
        statusCheckOpen.set(open);
    }
}
