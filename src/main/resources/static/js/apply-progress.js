// Live progress sidebar for the apply form: updates the ring percentage and
// per-section states (미작성 / 작성 중 / 완료) as the applicant fills the form.
document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.querySelector(".progress-sidebar");
    const form = document.querySelector(".form-card");
    if (!sidebar || !form) {
        return;
    }

    const ringBar = document.querySelector("#progress-ring-bar");
    const percentLabel = document.querySelector("#progress-percent");
    const memberList = document.querySelector("#member-list");
    const fileInput = form.querySelector(".file-submission-input");
    const selectedFileList = document.querySelector("#selected-file-list");
    const CIRCUMFERENCE = 2 * Math.PI * 52;

    const trimmedValue = (selector) => {
        const field = form.querySelector(selector);
        return field ? field.value.trim() : "";
    };

    const collectChecks = () => ({
        team: [
            trimmedValue("#teamName") !== "",
            /^[0-9\-]{9,20}$/.test(trimmedValue("#representativePhone")),
            trimmedValue("#password").length >= 4,
        ],
        members: Array.from(memberList.querySelectorAll(".member-row input"))
            .map((input) => input.value.trim() !== ""),
        idea: [
            form.querySelector("input[name='category']:checked") !== null,
            trimmedValue("#topic") !== "",
            trimmedValue("#content") !== "",
        ],
    });

    const applyStepState = (stepId, mark, checks) => {
        const step = document.querySelector(stepId);
        const done = checks.length > 0 && checks.every(Boolean);
        const started = checks.some(Boolean);
        step.classList.toggle("is-done", done);
        step.classList.toggle("is-active", !done && started);
        step.querySelector(".step-mark").textContent = done ? "✓" : mark;
        step.querySelector(".step-state").textContent = done ? "완료" : (started ? "작성 중" : "미작성");
    };

    const applyFilesState = () => {
        const step = document.querySelector("#step-files");
        const count = fileInput ? fileInput.files.length : 0;
        step.classList.toggle("is-done", count > 0);
        step.querySelector(".step-mark").textContent = count > 0 ? "✓" : "4";
        step.querySelector(".step-state").textContent = count > 0 ? `${count}개 첨부` : "선택사항";
    };

    const update = () => {
        const checks = collectChecks();
        applyStepState("#step-team", "1", checks.team);
        applyStepState("#step-members", "2", checks.members);
        applyStepState("#step-idea", "3", checks.idea);
        applyFilesState();

        const required = [...checks.team, ...checks.members, ...checks.idea];
        const percent = Math.round((required.filter(Boolean).length / required.length) * 100);
        percentLabel.textContent = `${percent}%`;
        ringBar.style.strokeDashoffset = CIRCUMFERENCE * (1 - percent / 100);
    };

    form.addEventListener("input", update);
    form.addEventListener("change", update);
    // Member rows are added/removed and attachments are set programmatically,
    // so input events alone do not cover them.
    new MutationObserver(update).observe(memberList, { childList: true });
    if (selectedFileList) {
        new MutationObserver(update).observe(selectedFileList, { childList: true });
    }

    update();
});
