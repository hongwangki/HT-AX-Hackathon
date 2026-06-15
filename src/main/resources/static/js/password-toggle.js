document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".password-toggle").forEach((button) => {
        const input = button.parentElement?.querySelector("input[type='password'], input[type='text']");
        if (!input) {
            return;
        }

        button.addEventListener("click", () => {
            const shouldShow = input.type === "password";
            input.type = shouldShow ? "text" : "password";
            button.classList.toggle("is-visible", shouldShow);
            button.setAttribute("aria-label", shouldShow ? "비밀번호 숨기기" : "비밀번호 보기");
            button.setAttribute("aria-pressed", String(shouldShow));
        });
    });
});
