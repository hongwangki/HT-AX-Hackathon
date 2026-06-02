document.addEventListener("DOMContentLoaded", () => {
    const memberList = document.querySelector("#member-list");
    const addMemberButton = document.querySelector("#add-member");
    const filePicker = document.querySelector("#attachment-picker");
    const fileInput = document.querySelector(".file-submission-input");
    const fileGuide = document.querySelector("#file-guide");
    const selectedFileList = document.querySelector("#selected-file-list");

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

    if (filePicker && fileInput && fileGuide && selectedFileList) {
        const selectedFiles = new DataTransfer();

        const formatFileSize = (size) => {
            if (size < 1024) {
                return `${size} bytes`;
            }
            if (size < 1024 * 1024) {
                return `${(size / 1024).toFixed(1)} KB`;
            }
            return `${(size / (1024 * 1024)).toFixed(1)} MB`;
        };

        const isSameFile = (first, second) =>
            first.name === second.name
            && first.size === second.size
            && first.lastModified === second.lastModified;

        const renderSelectedFiles = () => {
            selectedFileList.replaceChildren();
            Array.from(selectedFiles.files).forEach((file, index) => {
                const item = document.createElement("li");
                item.className = "selected-file-item";

                const fileInfo = document.createElement("span");
                fileInfo.className = "selected-file-info";

                const fileName = document.createElement("strong");
                fileName.textContent = file.name;

                const fileSize = document.createElement("small");
                fileSize.textContent = formatFileSize(file.size);

                const removeButton = document.createElement("button");
                removeButton.className = "text-button";
                removeButton.type = "button";
                removeButton.textContent = "삭제";
                removeButton.addEventListener("click", () => {
                    const remainingFiles = Array.from(selectedFiles.files)
                        .filter((_, fileIndex) => fileIndex !== index);
                    selectedFiles.items.clear();
                    remainingFiles.forEach((remainingFile) => selectedFiles.items.add(remainingFile));
                    fileInput.files = selectedFiles.files;
                    renderSelectedFiles();
                });

                fileInfo.append(fileName, fileSize);
                item.append(fileInfo, removeButton);
                selectedFileList.append(item);
            });

            fileGuide.textContent = selectedFiles.files.length
                ? `${selectedFiles.files.length}개 파일이 추가되었습니다.`
                : "추가할 파일을 선택해 주세요.";
        };

        filePicker.addEventListener("change", () => {
            const newFile = filePicker.files[0];
            if (newFile && !Array.from(selectedFiles.files).some((file) => isSameFile(file, newFile))) {
                selectedFiles.items.add(newFile);
            }
            fileInput.files = selectedFiles.files;
            filePicker.value = "";
            renderSelectedFiles();
        });
    }

    renumberMembers();
    bindRemoveButtons();
});
