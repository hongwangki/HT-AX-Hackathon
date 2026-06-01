document.addEventListener("DOMContentLoaded", () => {
    const memberList = document.querySelector("#member-list");
    const addMemberButton = document.querySelector("#add-member");
    const fileInput = document.querySelector("input[type='file']");
    const fileGuide = document.querySelector("#file-guide");

    const renumberMembers = () => {
        const rows = memberList.querySelectorAll(".member-row");
        rows.forEach((row, index) => {
            row.querySelector(".member-number").textContent = index + 1;
            row.querySelectorAll("input").forEach((input) => {
                input.name = input.name.replace(/members\[\d+]/, `members[${index}]`);
                input.id = input.id.replace(/members\d+/, `members${index}`);
            });
            row.querySelector(".remove-member").hidden = rows.length === 1;
        });
    };

    const bindRemoveButtons = () => {
        memberList.querySelectorAll(".remove-member").forEach((button) => {
            button.onclick = () => {
                if (memberList.querySelectorAll(".member-row").length > 1) {
                    button.closest(".member-row").remove();
                    renumberMembers();
                }
            };
        });
    };

    addMemberButton.addEventListener("click", () => {
        const source = memberList.querySelector(".member-row");
        const clone = source.cloneNode(true);
        clone.querySelectorAll("input").forEach((input) => input.value = "");
        clone.querySelectorAll(".field-error").forEach((error) => error.textContent = "");
        memberList.appendChild(clone);
        renumberMembers();
        bindRemoveButtons();
    });

    fileInput.addEventListener("change", () => {
        fileGuide.textContent = fileInput.files.length
            ? `${fileInput.files.length}개 파일 선택됨`
            : "선택된 파일 없음";
    });

    renumberMembers();
    bindRemoveButtons();
});
